package request

type UserRequest struct {
	UserID   int64   `json:"userId"`
	Email    string  `json:"email"`
	Password string  `json:"password,omitempty"`
	RoleIDs  []int64 `json:"roleIds"`
}

type UserBanRequest struct {
	IsBanned       *bool `json:"isBanned"`
	RevokeSessions *bool `json:"revokeSessions,omitempty"`
}
