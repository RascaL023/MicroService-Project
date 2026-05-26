package mapper

import (
	"auth-service/internal/dto/response"
	"auth-service/internal/entity"
)

func ToUserResponse(user entity.User) response.UserResponse {
	roles := make([]string, 0, len(user.Roles))
	for _, role := range user.Roles {
		roles = append(roles, role.Name)
	}
	return response.UserResponse{ID: user.ID, Username: user.Username, Roles: roles, IsBanned: user.IsBanned}
}
