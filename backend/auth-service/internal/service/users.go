package service

import (
	"context"

	"auth-service/internal/dto/response"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"
)

type UserService struct {
	userRepo *repository.UserRepository
}

func NewUserService(users *repository.UserRepository) *UserService {
	return &UserService{userRepo: users}
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
