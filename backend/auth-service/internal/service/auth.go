package service

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"errors"
	"strconv"
	"time"

	"auth-service/internal/config"
	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
	"auth-service/internal/repository"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	cfg      config.Config
	users    *repository.UserRepository
	sessions *repository.SessionRepository
}

func NewAuthService(
	cfg config.Config,
	users *repository.UserRepository,
	sessions *repository.SessionRepository,
) *AuthService {
	return &AuthService{
		cfg:      cfg,
		users:    users,
		sessions: sessions,
	}
}

func (s *AuthService) Login(ctx context.Context, req request.LoginRequest) (response.LoginResponse, error) {
	if len(req.Username) == 0 || len(req.Password) < 8 {
		return response.LoginResponse{}, ErrValidation
	}

	user, err := s.users.FindByUsername(ctx, req.Username)
	if errors.Is(err, repository.ErrNotFound) {
		return response.LoginResponse{}, ErrUnauthorized
	}
	if err != nil {
		return response.LoginResponse{}, err
	}

	if user.IsBanned {
		return response.LoginResponse{}, ErrForbidden
	}
	if err := bcrypt.CompareHashAndPassword(
		[]byte(user.HashPassword),
		[]byte(req.Password),
	); err != nil {
		return response.LoginResponse{}, ErrUnauthorized
	}

	roles, permissions := collectGrants(user)
	var tokenType, token string
	if s.cfg.AuthMode == "stateful" {
		tokenType = "Bearer"
		token, err = randomToken()
		if err != nil {
			return response.LoginResponse{}, err
		}

		if err := s.sessions.Create(ctx, token, repository.Session{
			Subject:     user.ID,
			Username:    user.Username,
			Roles:       roles,
			Authorities: permissions,
		}, s.cfg.SessionTTL); err != nil {
			return response.LoginResponse{}, err
		}

	} else {
		tokenType = "Bearer"
		token, err = s.jwtToken(user.ID, roles, permissions)
		if err != nil {
			return response.LoginResponse{}, err
		}
	}

	return response.LoginResponse{
		UserID:      user.ID,
		Username:    user.Username,
		Roles:       roles,
		Permissions: permissions,
		TokenType:   tokenType,
		AccessToken: token,
	}, nil
}

func (s *AuthService) Authenticate(ctx context.Context, authHeader string) (entity.User, []string, []string, error) {
	tokenType, token := splitAuth(authHeader)
	if token == "" {
		return entity.User{}, nil, nil, ErrUnauthorized
	}

	var userID int64
	var err error
	if tokenType == "Bearer" && s.cfg.AuthMode != "stateful" {
		userID, err = s.userIDFromJWT(token)
	} else {
		var stored repository.Session
		stored, err = s.sessions.Get(ctx, token)
		userID = stored.Subject
	}

	if err != nil {
		return entity.User{}, nil, nil, ErrUnauthorized
	}

	user, err := s.users.FindByID(ctx, userID)
	if err != nil {
		return entity.User{}, nil, nil, err
	}

	roles, permissions := collectGrants(user)
	return user, roles, permissions, nil
}

func (s *AuthService) jwtToken(userID int64, roles, permissions []string) (string, error) {
	claims := jwt.MapClaims{
		"sub":         strconv.FormatInt(userID, 10),
		"roles":       roles,
		"permissions": permissions,
		"exp":         time.Now().Add(s.cfg.SessionTTL).Unix(),
	}

	return jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString([]byte(s.cfg.JWTSecret))
}

func (s *AuthService) userIDFromJWT(tokenValue string) (int64, error) {
	token, err := jwt.Parse(tokenValue, func(token *jwt.Token) (any, error) {
		return []byte(s.cfg.JWTSecret), nil
	}, jwt.WithValidMethods([]string{jwt.SigningMethodHS256.Alg()}))
	if err != nil || !token.Valid {
		return 0, ErrUnauthorized
	}
	subject, err := token.Claims.GetSubject()
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(subject, 10, 64)
}

func collectGrants(user entity.User) ([]string, []string) {
	roleSeen := map[string]bool{}
	permissionSeen := map[string]bool{}
	roles := make([]string, 0)
	permissions := make([]string, 0)

	for _, role := range user.Roles {
		if !roleSeen[role.Name] {
			roleSeen[role.Name] = true
			roles = append(roles, role.Name)
		}

		for _, authority := range role.Authorities {
			if !permissionSeen[authority.Name] {
				permissionSeen[authority.Name] = true
				permissions = append(permissions, authority.Name)
			}
		}
	}

	return roles, permissions
}

func randomToken() (string, error) {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}

	return base64.RawURLEncoding.EncodeToString(bytes), nil
}

func splitAuth(header string) (string, string) {
	for _, prefix := range []string{"Bearer ", "Session "} {
		if len(header) > len(prefix) && header[:len(prefix)] == prefix {
			return prefix[:len(prefix)-1], header[len(prefix):]
		}
	}
	return "Session", header
}
