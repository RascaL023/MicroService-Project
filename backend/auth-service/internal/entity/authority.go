package entity

import "time"

type Authority struct {
	ID        int64
	Name      string
	CreatedAt time.Time
}
