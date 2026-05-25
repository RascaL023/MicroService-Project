package response

type LoginResponse struct {
	UserID      int64    `json:"userId"`
	Username    string   `json:"username"`
	Roles       []string `json:"roles"`
	Permissions []string `json:"permissions"`
	TokenType   string   `json:"tokenType"`
	AccessToken string   `json:"accessToken"`
}
