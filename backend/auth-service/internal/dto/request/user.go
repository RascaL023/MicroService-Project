package request

type UserRequest struct {
	Username string  `json:"username"`
	Password string  `json:"password"`
	RoleIDs  []int64 `json:"roleIds"`
}

type UserBanRequest struct {
	IsBanned       *bool `json:"isBanned"`
	RevokeSessions *bool `json:"revokeSessions,omitempty"`
}
