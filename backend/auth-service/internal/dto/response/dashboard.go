package response

type DashboardSummaryResponse struct {
	TotalUsers        int `json:"totalUsers"`
	ActiveUsers       int `json:"activeUsers"`
	PendingActivation int `json:"pendingActivation"`
	BannedUsers       int `json:"bannedUsers"`
}
