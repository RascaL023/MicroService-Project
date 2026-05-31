package request

type LoginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type ActivationRequest struct {
	Email string `json:"email"`
}

type ActivationCompleteRequest struct {
	Token    string `json:"token"`
	Password string `json:"password"`
}
