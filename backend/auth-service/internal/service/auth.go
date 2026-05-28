package service

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"errors"

	"auth-service/internal/config"
	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"

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
	if len(req.Username) == 0 || len(req.Password) == 0 {
		fields := make([]FieldError, 0, 2)
		if len(req.Username) == 0 {
			fields = append(fields, FieldError{Field: "username", Message: "Username is required"})
		}
		if len(req.Password) == 0 {
			fields = append(fields, FieldError{Field: "password", Message: "Password is required"})
		}
		return response.LoginResponse{}, NewValidationError(fields...)
	}

	user, err := s.users.FindByUsername(ctx, req.Username)
	if errors.Is(err, repository.ErrNotFound) {
		return response.LoginResponse{}, ErrWrongCredentials
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
		return response.LoginResponse{}, ErrWrongCredentials
	}

	roles, permissions := collectGrants(user)
	token, err := generateRandomToken()
	if err != nil {
		return response.LoginResponse{}, err
	}

	if err := s.sessions.Create(ctx, token, entity.Session{
		Subject:     user.ID,
		Username:    user.Username,
		Roles:       roles,
		Authorities: permissions,
	}, s.cfg.SessionTTL); err != nil {
		return response.LoginResponse{}, err
	}

	return response.LoginResponse{
		UserID:      user.ID,
		Username:    user.Username,
		Roles:       roles,
		Permissions: permissions,
		TokenType:   "Session",
		AccessToken: token,
	}, nil
}

func (s *AuthService) Logout(ctx context.Context, authHeader string) (string, error) {
	_, token := splitAuth(authHeader)
	if token == "" {
		return "", ErrUnauthorized
	}
	if err := s.sessions.Delete(ctx, token); err != nil {
		if errors.Is(err, repository.ErrNotFound) {
			return "", ErrUnauthorized
		}
		return "", err
	}

	return "Logout success", nil
}

func (s *AuthService) UpdateUserBan(ctx context.Context, userID int64, req request.UserBanRequest) (response.UserResponse, error) {
	if req.IsBanned == nil {
		return response.UserResponse{}, NewValidationError(FieldError{
			Field:   "isBanned",
			Message: "isBanned is required",
		})
	}

	isBanned := *req.IsBanned
	revokeSessions := isBanned
	if req.RevokeSessions != nil {
		revokeSessions = *req.RevokeSessions
	}

	user, err := s.users.SetBanned(ctx, userID, isBanned)
	if err != nil {
		return response.UserResponse{}, err
	}

	if isBanned {
		if _, err := s.sessions.BanSubject(ctx, userID, revokeSessions); err != nil {
			return response.UserResponse{}, err
		}
	} else {
		if err := s.sessions.UnbanSubject(ctx, userID); err != nil {
			return response.UserResponse{}, err
		}
	}

	return mapper.ToUserResponse(user), nil
}

func (s *AuthService) Authenticate(ctx context.Context, authHeader string) (entity.User, []string, []string, error) {
	_, token := splitAuth(authHeader)
	if token == "" {
		return entity.User{}, nil, nil, ErrUnauthorized
	}

	stored, err := s.sessions.Get(ctx, token)
	if err != nil {
		return entity.User{}, nil, nil, ErrUnauthorized
	}

	user, err := s.users.FindByID(ctx, stored.Subject)
	if err != nil {
		return entity.User{}, nil, nil, err
	}
	if user.IsBanned {
		return entity.User{}, nil, nil, ErrForbidden
	}

	roles, permissions := collectGrants(user)
	return user, roles, permissions, nil
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

func generateRandomToken() (string, error) {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}

	return base64.RawURLEncoding.EncodeToString(bytes), nil
}

func splitAuth(header string) (string, string) {
	const prefix = "Session "
	if len(header) > len(prefix) && header[:len(prefix)] == prefix {
		return "Session", header[len(prefix):]
	}

	return "Session", header
}
