package service

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"log"
	"net/url"
	"strings"

	"auth-service/internal/config"
	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"

	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	cfg         config.Config
	users       *repository.UserRepository
	sessions    *repository.SessionRepository
	activations *repository.ActivationRepository
	resets      *repository.PasswordResetRepository
	emailJobs   *repository.EmailJobPublisher
}

func NewAuthService(
	cfg config.Config,
	users *repository.UserRepository,
	sessions *repository.SessionRepository,
	activations *repository.ActivationRepository,
	resets *repository.PasswordResetRepository,
	emailJobs *repository.EmailJobPublisher,
) *AuthService {
	return &AuthService{
		cfg:         cfg,
		users:       users,
		sessions:    sessions,
		activations: activations,
		resets:      resets,
		emailJobs:   emailJobs,
	}
}

func (s *AuthService) Login(ctx context.Context, req request.LoginRequest) (response.LoginResponse, error) {
	email := normalizeEmail(req.Email)
	if email == "" || req.Password == "" {
		fields := make([]FieldError, 0, 2)
		if email == "" {
			fields = append(fields, FieldError{Field: "email", Message: "Email is required"})
		}
		if req.Password == "" {
			fields = append(fields, FieldError{Field: "password", Message: "Password is required"})
		}
		return response.LoginResponse{}, NewValidationError(fields...)
	}

	user, err := s.users.FindByEmail(ctx, email)
	if errors.Is(err, repository.ErrNotFound) {
		return response.LoginResponse{}, ErrWrongCredentials
	}
	if err != nil {
		return response.LoginResponse{}, err
	}
	if user.Status != repository.AccountActive || user.HashPassword == nil {
		return response.LoginResponse{}, ErrForbidden
	}

	if err := bcrypt.CompareHashAndPassword([]byte(*user.HashPassword), []byte(req.Password)); err != nil {
		return response.LoginResponse{}, ErrWrongCredentials
	}

	return s.createSession(ctx, user)
}

func (s *AuthService) RequestActivation(ctx context.Context, req request.ActivationRequest) error {
	email := normalizeEmail(req.Email)
	if email == "" {
		return NewValidationError(FieldError{Field: "email", Message: "Email is required"})
	}

	user, err := s.users.FindByEmail(ctx, email)
	if errors.Is(err, repository.ErrNotFound) {
		log.Printf("activation request ignored: auth identity not found for email=%s", email)
		return err
	}
	if err != nil {
		return err
	}
	if user.Status != repository.AccountPendingActivation {
		log.Printf("activation request ignored: userID=%d email=%s status=%s", user.ID, user.Email, user.Status)
		return ErrConflict
	}

	token, err := generateRandomToken()
	if err != nil {
		return err
	}
	if err := s.activations.Create(ctx, tokenHash(token), user.ID, s.cfg.ActivationTTL); err != nil {
		return err
	}

	if err := s.emailJobs.PublishActivation(ctx, user.Email, s.activationURL(token)); err != nil {
		return err
	}

	log.Printf("activation requested: userID=%d email=%s", user.ID, user.Email)
	return nil
}

func (s *AuthService) CompleteActivation(ctx context.Context, req request.ActivationCompleteRequest) (response.LoginResponse, error) {
	if req.Token == "" || len(req.Password) < 8 {
		fields := make([]FieldError, 0, 2)
		if req.Token == "" {
			fields = append(fields, FieldError{Field: "token", Message: "Token is required"})
		}
		if len(req.Password) < 8 {
			fields = append(fields, FieldError{Field: "password", Message: "Password must be at least 8 characters"})
		}
		return response.LoginResponse{}, NewValidationError(fields...)
	}

	userID, err := s.activations.Consume(ctx, tokenHash(req.Token))
	if errors.Is(err, repository.ErrNotFound) {
		return response.LoginResponse{}, repository.ErrNotFound
	}
	if err != nil {
		return response.LoginResponse{}, err
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		return response.LoginResponse{}, err
	}

	user, err := s.users.SetActivatedPassword(ctx, userID, string(hash))
	if err != nil {
		return response.LoginResponse{}, err
	}

	return s.createSession(ctx, user)
}

func (s *AuthService) RequestPasswordReset(ctx context.Context, req request.PasswordResetRequest) error {
	email := normalizeEmail(req.Email)
	if email == "" {
		return NewValidationError(FieldError{Field: "email", Message: "Email is required"})
	}

	user, err := s.users.FindByEmail(ctx, email)
	if errors.Is(err, repository.ErrNotFound) {
		log.Printf("password reset request ignored: auth identity not found for email=%s", email)
		return nil
	}
	if err != nil {
		return err
	}
	if user.Status != repository.AccountActive || user.HashPassword == nil {
		log.Printf("password reset request ignored: userID=%d email=%s status=%s", user.ID, user.Email, user.Status)
		return nil
	}

	token, err := generateRandomToken()
	if err != nil {
		return err
	}
	if err := s.resets.Create(ctx, tokenHash(token), user.ID, s.cfg.PasswordResetTTL); err != nil {
		return err
	}

	if err := s.emailJobs.PublishPasswordReset(ctx, user.Email, s.passwordResetURL(token)); err != nil {
		return err
	}

	log.Printf("password reset requested: userID=%d email=%s", user.ID, user.Email)
	return nil
}

func (s *AuthService) CompletePasswordReset(ctx context.Context, req request.PasswordResetCompleteRequest) error {
	if req.Token == "" || len(req.Password) < 8 {
		fields := make([]FieldError, 0, 2)
		if req.Token == "" {
			fields = append(fields, FieldError{Field: "token", Message: "Token is required"})
		}
		if len(req.Password) < 8 {
			fields = append(fields, FieldError{Field: "password", Message: "Password must be at least 8 characters"})
		}
		return NewValidationError(fields...)
	}

	userID, err := s.resets.Consume(ctx, tokenHash(req.Token))
	if errors.Is(err, repository.ErrNotFound) {
		return repository.ErrNotFound
	}
	if err != nil {
		return err
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		return err
	}

	if _, err := s.users.SetPassword(ctx, userID, string(hash)); err != nil {
		return err
	}
	_, err = s.sessions.RevokeSubject(ctx, userID)
	return err
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

func (s *AuthService) UpdateUserStatus(ctx context.Context, userID int64, req request.UserStatusRequest) (response.UserResponse, error) {
	status := strings.ToUpper(strings.TrimSpace(req.Status))
	if status == "" {
		return response.UserResponse{}, NewValidationError(FieldError{
			Field:   "status",
			Message: "Status is required",
		})
	}
	if status != repository.AccountActive && status != repository.AccountBanned {
		return response.UserResponse{}, NewValidationError(FieldError{
			Field:   "status",
			Message: "Status must be ACTIVE or BANNED",
		})
	}

	revokeSessions := status == repository.AccountBanned
	if req.RevokeSessions != nil {
		revokeSessions = *req.RevokeSessions
	}

	var user entity.User
	var err error
	if status == repository.AccountBanned {
		if _, err := s.sessions.BanSubject(ctx, userID, revokeSessions); err != nil {
			return response.UserResponse{}, err
		}
		user, err = s.users.SetStatus(ctx, userID, status)
		if err != nil {
			_ = s.sessions.UnbanSubject(ctx, userID)
			return response.UserResponse{}, err
		}
	} else {
		user, err = s.users.SetStatus(ctx, userID, status)
		if err != nil {
			return response.UserResponse{}, err
		}
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

	stored, isBanned, err := s.sessions.GetActive(ctx, token)
	if err != nil {
		return entity.User{}, nil, nil, ErrUnauthorized
	}
	if isBanned {
		return entity.User{}, nil, nil, ErrForbidden
	}

	user := entity.User{
		ID:     stored.Subject,
		Email:  stored.Email,
		Status: repository.AccountActive,
	}
	return user, stored.Roles, stored.Authorities, nil
}

func (s *AuthService) createSession(ctx context.Context, user entity.User) (response.LoginResponse, error) {
	roles, permissions := collectGrants(user)
	sessionID, err := generateRandomToken()
	if err != nil {
		return response.LoginResponse{}, err
	}

	if err := s.sessions.Create(ctx, sessionID, entity.Session{
		Subject:     user.ID,
		Email:       user.Email,
		Roles:       roles,
		Authorities: permissions,
	}, s.cfg.SessionTTL); err != nil {
		return response.LoginResponse{}, err
	}

	return response.LoginResponse{
		UserID:      user.ID,
		Email:       user.Email,
		Roles:       roles,
		Permissions: permissions,
		SessionID:   sessionID,
	}, nil
}

func (s *AuthService) activationURL(token string) string {
	parsed, err := url.Parse(s.cfg.ActivationURL)
	if err != nil {
		return s.cfg.ActivationURL + "?token=" + url.QueryEscape(token)
	}

	query := parsed.Query()
	query.Set("token", token)
	parsed.RawQuery = query.Encode()
	return parsed.String()
}

func (s *AuthService) passwordResetURL(token string) string {
	parsed, err := url.Parse(s.cfg.PasswordResetURL)
	if err != nil {
		return s.cfg.PasswordResetURL + "?token=" + url.QueryEscape(token)
	}

	query := parsed.Query()
	query.Set("token", token)
	parsed.RawQuery = query.Encode()
	return parsed.String()
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

func tokenHash(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}

func normalizeEmail(email string) string {
	return strings.ToLower(strings.TrimSpace(email))
}

func splitAuth(header string) (string, string) {
	const prefix = "Session "
	if len(header) > len(prefix) && header[:len(prefix)] == prefix {
		return "Session", header[len(prefix):]
	}

	return "Session", header
}
