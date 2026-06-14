package request

type UserStatusRequest struct {
	Status         string `json:"status"`
	RevokeSessions *bool  `json:"revokeSessions,omitempty"`
}

type UserRoleRequest struct {
	Role string `json:"role"`
}
