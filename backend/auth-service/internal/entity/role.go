package entity

import "time"

type Role struct {
	ID          int64
	Role        string
	CreatedAt   time.Time
	Authorities []string
}
