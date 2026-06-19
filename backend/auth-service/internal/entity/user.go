package entity

import "time"

type User struct {
	ID              int64
	Email           string
	HashPassword    *string
	Status          string
	EmailVerifiedAt *time.Time
	LastLogin       *time.Time
	CreatedAt       time.Time
	UpdatedAt       *time.Time
	DeletedAt       *time.Time
	Roles           []Role
}
