package request

type UserRequest struct {
	UserID   int64   `json:"userId"`
	Email    string  `json:"email"`
	Password string  `json:"password,omitempty"`
	RoleIDs  []int64 `json:"roleIds"`
}

type UserStatusRequest struct {
	Status         string `json:"status"`
	RevokeSessions *bool  `json:"revokeSessions,omitempty"`
}
