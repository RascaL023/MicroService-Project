package entity

import "time"

type User struct {
	ID           int64
	Username     string
	HashPassword string
	IsBanned     bool
	CreatedAt    time.Time
	UpdatedAt    *time.Time
	DeletedAt    *time.Time
	Roles        []Role
}
