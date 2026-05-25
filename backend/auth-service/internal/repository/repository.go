package repository

import "github.com/jackc/pgx/v5/pgxpool"

type Repositories struct {
	Users *UserRepository
	Roles *RoleRepository
	// Sessions *SessionRepository
}

func New(pool *pgxpool.Pool) Repositories {
	return Repositories{
		Users: &UserRepository{pool: pool},
		Roles: &RoleRepository{pool: pool},
		// Sessions: &SessionRepository{pool: pool},
	}
}
