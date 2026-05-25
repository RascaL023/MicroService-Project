package service

import (
	"context"

	"auth-service/internal/dto/response"
	"auth-service/internal/mapper"
	"auth-service/internal/repository"
)

type RoleService struct {
	roles *repository.RoleRepository
}

func NewRoleService(roles *repository.RoleRepository) *RoleService {
	return &RoleService{roles: roles}
}

func (s *RoleService) GetByID(ctx context.Context, id int64) (response.RoleResponse, error) {
	role, err := s.roles.FindByID(ctx, id)
	if err != nil {
		return response.RoleResponse{}, err
	}
	return mapper.ToRoleResponse(role), nil
}

func (s *RoleService) List(ctx context.Context, page, size int) ([]response.RoleResponse, int, error) {
	roles, total, err := s.roles.List(ctx, size, (page-1)*size)
	if err != nil {
		return nil, 0, err
	}
	return mapper.ToRoleResponses(roles), total, nil
}
