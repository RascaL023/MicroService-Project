package response

import "time"

type UserResponse struct {
	ID        int64      `json:"id"`
	Email     string     `json:"email"`
	Status    string     `json:"status"`
	LastLogin *time.Time `json:"lastLogin"`
	Roles     []string   `json:"roles"`
}
