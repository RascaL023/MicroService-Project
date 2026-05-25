package repository

import "github.com/jackc/pgx/v5/pgxpool"

type Repositories struct {
	Users       *UserRepository
	Roles       *RoleRepository
	Authorities *AuthorityRepository
}

func NewPostgres(pool *pgxpool.Pool) Repositories {
	return Repositories{
		Users:       &UserRepository{pool: pool},
		Roles:       &RoleRepository{pool: pool},
		Authorities: &AuthorityRepository{pool: pool},
	}
}
