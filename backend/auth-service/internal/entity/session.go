package entity

import "time"

type Session struct {
	Subject     int64     `json:"subject"`
	UserID      int64     `json:"userId,omitempty"`
	Email       string    `json:"email,omitempty"`
	Roles       []string  `json:"roles"`
	Authorities []string  `json:"authorities"`
	IssuedAt    time.Time `json:"issuedAt"`
	ExpiresAt   time.Time `json:"expiresAt"`
}
