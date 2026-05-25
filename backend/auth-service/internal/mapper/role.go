package mapper

import (
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
)

func ToRoleResponse(role entity.Role) response.RoleResponse {
	return response.RoleResponse{
		ID:          role.ID,
		Role:        role.Role,
		CreatedAt:   role.CreatedAt,
		Authorities: role.Authorities,
	}
}

func ToRoleResponses(roles []entity.Role) []response.RoleResponse {
	responses := make([]response.RoleResponse, 0, len(roles))
	for _, role := range roles {
		responses = append(responses, ToRoleResponse(role))
	}
	return responses
}
