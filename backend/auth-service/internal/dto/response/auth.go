package response

type LoginResponse struct {
	UserID      int64    `json:"userId"`
	Email       string   `json:"email"`
	Roles       []string `json:"roles"`
	Permissions []string `json:"permissions"`
	SessionID   string   `json:"sessionId"`
}
