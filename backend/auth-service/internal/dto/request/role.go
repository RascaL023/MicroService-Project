package request

type RoleRequest struct {
	Role         string  `json:"role"`
	AuthorityIDs []int64 `json:"authorityIds"`
}
