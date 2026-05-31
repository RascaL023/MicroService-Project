package service

import (
	"context"
	"strings"

	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"
)

type UserService struct {
	userRepo *repository.UserRepository
	roleRepo *repository.RoleRepository
}

func NewUserService(users *repository.UserRepository, roles *repository.RoleRepository) *UserService {
	return &UserService{userRepo: users, roleRepo: roles}
}

func (s *UserService) Create(ctx context.Context, req request.UserRequest) (response.UserResponse, error) {
	email := strings.ToLower(strings.TrimSpace(req.Email))
	if req.UserID == 0 || email == "" || len(req.RoleIDs) == 0 {
		fields := make([]FieldError, 0, 3)
		if req.UserID == 0 {
			fields = append(fields, FieldError{
				Field:   "userId",
				Message: "User ID is required",
			})
		}

		if email == "" {
			fields = append(fields, FieldError{
				Field:   "email",
				Message: "Email is required",
			})
		}

		if len(req.RoleIDs) == 0 {
			fields = append(fields, FieldError{
				Field:   "roleIds",
				Message: "Role is required",
			})
		}
		return response.UserResponse{}, NewValidationError(fields...)
	}

	if _, err := s.roleRepo.FindByIDs(ctx, req.RoleIDs); err != nil {
		return response.UserResponse{}, err
	}

	user, err := s.userRepo.Provision(ctx, req.UserID, email, req.RoleIDs, false)
	if err != nil {
		return response.UserResponse{}, err
	}

	return mapper.ToUserResponse(user), nil
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
