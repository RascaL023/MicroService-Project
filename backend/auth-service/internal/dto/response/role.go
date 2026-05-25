package response

import "time"

type RoleResponse struct {
	ID          int64     `json:"id"`
	Name        string    `json:"name"`
	CreatedAt   time.Time `json:"createdAt"`
	Authorities []string  `json:"authorities,omitempty"`
}
