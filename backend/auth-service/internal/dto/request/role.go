package request

type RoleRequest struct {
	Name         string  `json:"name"`
	AuthorityIDs []int64 `json:"authorityIds"`
}
