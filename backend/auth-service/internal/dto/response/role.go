package response

import "time"

type RoleResponse struct {
	ID          int64     `json:"id"`
	Role        string    `json:"role"`
	CreatedAt   time.Time `json:"createdAt"`
	Authorities []string  `json:"authorities,omitempty"`
}
