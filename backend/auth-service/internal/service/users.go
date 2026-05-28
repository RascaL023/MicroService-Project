package service

import (
	"context"

	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"

	"golang.org/x/crypto/bcrypt"
)

type UserService struct {
	userRepo *repository.UserRepository
	roleRepo *repository.RoleRepository
}

func NewUserService(users *repository.UserRepository, roles *repository.RoleRepository) *UserService {
	return &UserService{userRepo: users, roleRepo: roles}
}

func (s *UserService) Register(ctx context.Context, req request.RegisterRequest) (response.UserResponse, error) {
	userRole, err := s.roleRepo.FindByName(ctx, "USER")
	if err != nil { return response.UserResponse{}, err }

	return s.Create(ctx, request.UserRequest{
		Username: req.Username,
		Password: req.Password,
		RoleIDs:  []int64{userRole.ID},
	})
}

func (s *UserService) Create(ctx context.Context, req request.UserRequest) (response.UserResponse, error) {
	if len(req.Username) < 5 || len(req.Password) < 8 || len(req.RoleIDs) == 0 {
		fields := make([]FieldError, 0, 3)
		if len(req.Username) < 5 {
			fields = append(fields, FieldError{
				Field: "username", 
				Message: "Username must be at least 5 characters",
			})
		}

		if len(req.Password) < 8 {
			fields = append(fields, FieldError{
				Field: "password", 
				Message: "Password must be at least 8 characters",
			})
		}

		if len(req.RoleIDs) == 0 {
			fields = append(fields, FieldError{
				Field: "roleIds", 
				Message: "Role is required",
			})
		}
		return response.UserResponse{}, NewValidationError(fields...)
	}

	if _, err := s.roleRepo.FindByIDs(ctx, req.RoleIDs); err != nil { return response.UserResponse{}, err }

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil { return response.UserResponse{}, err }

	user, err := s.userRepo.Create(ctx, req.Username, string(hash), req.RoleIDs)
	if err != nil { return response.UserResponse{}, err }

	return mapper.ToUserResponse(user), nil
}

func (s *UserService) GetByID(ctx context.Context, id int64) (response.UserResponse, error) {
	user, err := s.userRepo.FindByID(ctx, id)
	if err != nil { return response.UserResponse{}, err }

	return mapper.ToUserResponse(user), nil
}

func (s *UserService) List(ctx context.Context, page, size int) ([]response.UserResponse, int, error) {
	users, total, err := s.userRepo.List(ctx, size, (page-1)*size)
	if err != nil { return nil, 0, err }

	responses := make([]response.UserResponse, 0, len(users))
	for _, user := range users {
		responses = append(responses, mapper.ToUserResponse(user))
	}

	return responses, total, nil
}
