package entity

import "time"

type User struct {
	ID              int64
	Email           string
	HashPassword    *string
	Status          string
	IsBanned        bool
	EmailVerifiedAt *time.Time
	CreatedAt       time.Time
	UpdatedAt       *time.Time
	DeletedAt       *time.Time
	Roles           []Role
}
