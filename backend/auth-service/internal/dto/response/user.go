package response

type UserResponse struct {
	ID       int64    `json:"id"`
	Username string   `json:"username"`
	Roles    []string `json:"roles"`
	IsBanned bool     `json:"isBanned"`
}
