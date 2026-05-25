package repository

import (
	"context"
	"errors"

	"auth-service/internal/entity"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type AuthorityRepository struct {
	pool *pgxpool.Pool
}

func (r *AuthorityRepository) FindByID(ctx context.Context, id int64) (entity.Authority, error) {
	var authority entity.Authority
	err := r.pool.QueryRow(ctx, `
		SELECT id, name, created_at, updated_at, deleted_at
		FROM authorities
		WHERE id=$1 AND deleted_at IS NULL
	`, id).Scan(
		&authority.ID,
		&authority.Name,
		&authority.CreatedAt,
		&authority.UpdatedAt,
		&authority.DeletedAt,
	)

	if errors.Is(err, pgx.ErrNoRows) { return entity.Authority{}, ErrNotFound }
	if err != nil { return entity.Authority{}, err }

	return authority, err
}

func (r *AuthorityRepository) FindByIDs(ctx context.Context, ids []int64) ([]entity.Authority, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id, name, created_at, updated_at, deleted_at
		FROM authorities
		WHERE id = ANY($1) AND deleted_at IS NULL
		ORDER BY id
	`, ids)
	if err != nil { return nil, err }
	defer rows.Close()

	authorities := make([]entity.Authority, 0)
	for rows.Next() {
		var authority entity.Authority
		if err := rows.Scan(
			&authority.ID,
			&authority.Name,
			&authority.CreatedAt,
			&authority.UpdatedAt,
			&authority.DeletedAt,
		); err != nil { return nil, err }
		authorities = append(authorities, authority)
	}

	if err := rows.Err(); err != nil { return nil, err }
	if len(authorities) != len(ids) { return nil, ErrNotFound }

	return authorities, nil
}
