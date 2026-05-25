package mapper

import (
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
)

func ToRoleResponse(role entity.Role) response.RoleResponse {
	authorities := make([]string, 0, len(role.Authorities))
	for _, authority := range role.Authorities {
		authorities = append(authorities, authority.Name)
	}

	return response.RoleResponse{
		ID:          role.ID,
		Name:        role.Name,
		CreatedAt:   role.CreatedAt,
		Authorities: authorities,
	}
}

func ToRoleResponses(roles []entity.Role) []response.RoleResponse {
	responses := make([]response.RoleResponse, 0, len(roles))
	for _, role := range roles {
		responses = append(responses, ToRoleResponse(role))
	}
	return responses
}
