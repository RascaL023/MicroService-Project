package service

import (
	"context"
	"strings"

	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"
)

var managedRoles = map[string]bool{
	"CHIEF":                   true,
	"CHIEF_DEPUTY":            true,
	"CHIEF_INSTRUCTOR":        true,
	"CHIEF_DEPUTY_INSTRUCTOR": true,
}

type UserService struct {
	userRepo    *repository.UserRepository
	roleRepo    *repository.RoleRepository
	sessionRepo *repository.SessionRepository
}

func NewUserService(users *repository.UserRepository, roles *repository.RoleRepository, sessions *repository.SessionRepository) *UserService {
	return &UserService{userRepo: users, roleRepo: roles, sessionRepo: sessions}
}

func (s *UserService) GetByID(ctx context.Context, id int64) (response.UserResponse, error) {
	user, err := s.userRepo.FindByID(ctx, id)
	if err != nil {
		return response.UserResponse{}, err
	}

	return mapper.ToUserResponse(user), nil
}

func (s *UserService) List(ctx context.Context, page, size int) ([]response.UserResponse, int, error) {
	users, total, err := s.userRepo.List(ctx, size, (page-1)*size)
	if err != nil {
		return nil, 0, err
	}

	responses := make([]response.UserResponse, 0, len(users))
	for _, user := range users {
		responses = append(responses, mapper.ToUserResponse(user))
	}

	return responses, total, nil
}

func (s *UserService) SetManagedRole(ctx context.Context, id int64, req request.UserRoleRequest) (response.UserResponse, error) {
	roleName := normalizeManagedRole(req.Role)
	if roleName == "" {
		return response.UserResponse{}, NewValidationError(FieldError{
			Field:   "role",
			Message: "Role is required",
		})
	}
	if roleName == repository.DefaultUserRole || roleName == "ADMIN" || !managedRoles[roleName] {
		return response.UserResponse{}, NewValidationError(FieldError{
			Field:   "role",
			Message: "Role must be one managed role",
		})
	}

	user, err := s.userRepo.FindByID(ctx, id)
	if err != nil {
		return response.UserResponse{}, err
	}
	if hasRole(user.Roles, "ADMIN") {
		return response.UserResponse{}, ErrForbidden
	}
	if currentRole := managedRoleName(user.Roles); currentRole != "" && currentRole != roleName {
		return response.UserResponse{}, repository.ErrDuplicate
	}

	role, err := s.roleRepo.FindByName(ctx, roleName)
	if err != nil {
		return response.UserResponse{}, err
	}

	updated, err := s.userRepo.SetManagedRole(ctx, id, role.ID)
	if err != nil {
		return response.UserResponse{}, err
	}
	_, err = s.sessionRepo.RevokeSubject(ctx, id)
	if err != nil {
		return response.UserResponse{}, err
	}

	return mapper.ToUserResponse(updated), nil
}

func (s *UserService) DemoteToDefaultRole(ctx context.Context, id int64) (response.UserResponse, error) {
	user, err := s.userRepo.FindByID(ctx, id)
	if err != nil {
		return response.UserResponse{}, err
	}
	if hasRole(user.Roles, "ADMIN") {
		return response.UserResponse{}, ErrForbidden
	}

	updated, err := s.userRepo.DemoteToDefaultRole(ctx, id)
	if err != nil {
		return response.UserResponse{}, err
	}
	_, err = s.sessionRepo.RevokeSubject(ctx, id)
	if err != nil {
		return response.UserResponse{}, err
	}

	return mapper.ToUserResponse(updated), nil
}

func normalizeManagedRole(role string) string {
	return strings.ToUpper(strings.TrimSpace(role))
}

func managedRoleName(roles []entity.Role) string {
	for _, role := range roles {
		name := strings.ToUpper(role.Name)
		if managedRoles[name] { return name }
	}
	return ""
}

func hasRole(roles []entity.Role, name string) bool {
	for _, role := range roles {
		if strings.EqualFold(role.Name, name) {
			return true
		}
	}
	return false
}
