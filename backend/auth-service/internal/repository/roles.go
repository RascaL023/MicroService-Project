package repository

import (
	"context"
	"errors"

	"auth-service/internal/entity"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type RoleRepository struct {
	pool *pgxpool.Pool
}

func (r *RoleRepository) FindByID(ctx context.Context, id int64) (entity.Role, error) {
	var role entity.Role
	err := r.pool.QueryRow(ctx, `
		SELECT id, role, created_at
		FROM roles
		WHERE id=$1 AND deleted_at IS NULL
	`, id).Scan(&role.ID, &role.Role, &role.CreatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return entity.Role{}, ErrNotFound
	}
	if err != nil {
		return entity.Role{}, err
	}
	role.Authorities, err = r.authoritiesForRole(ctx, role.ID)
	return role, err
}

func (r *RoleRepository) FindByName(ctx context.Context, name string) (entity.Role, error) {
	var role entity.Role
	err := r.pool.QueryRow(ctx, `
		SELECT id, role, created_at
		FROM roles
		WHERE role=$1 AND deleted_at IS NULL
	`, name).Scan(&role.ID, &role.Role, &role.CreatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return entity.Role{}, ErrNotFound
	}
	if err != nil {
		return entity.Role{}, err
	}
	return role, nil
}

func (r *RoleRepository) FindByIDs(ctx context.Context, ids []int64) ([]entity.Role, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id, role, created_at
		FROM roles
		WHERE id = ANY($1) AND deleted_at IS NULL
		ORDER BY id
	`, ids)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	roles := make([]entity.Role, 0)
	for rows.Next() {
		var role entity.Role
		if err := rows.Scan(&role.ID, &role.Role, &role.CreatedAt); err != nil {
			return nil, err
		}
		roles = append(roles, role)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	if len(roles) != len(ids) {
		return nil, ErrNotFound
	}
	return roles, nil
}

func (r *RoleRepository) List(ctx context.Context, limit, offset int) ([]entity.Role, int, error) {
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT count(*) FROM roles WHERE deleted_at IS NULL`).Scan(&total); err != nil {
		return nil, 0, err
	}

	rows, err := r.pool.Query(ctx, `
		SELECT id, role, created_at
		FROM roles
		WHERE deleted_at IS NULL
		ORDER BY id
		LIMIT $1 OFFSET $2
	`, limit, offset)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	roles := make([]entity.Role, 0)
	for rows.Next() {
		var role entity.Role
		if err := rows.Scan(&role.ID, &role.Role, &role.CreatedAt); err != nil {
			return nil, 0, err
		}
		role.Authorities, err = r.authoritiesForRole(ctx, role.ID)
		if err != nil {
			return nil, 0, err
		}
		roles = append(roles, role)
	}
	return roles, total, rows.Err()
}

func (r *RoleRepository) Create(ctx context.Context, name string, authorityIDs []int64) (entity.Role, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return entity.Role{}, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var role entity.Role
	err = tx.QueryRow(ctx, `
		INSERT INTO roles (role, created_at)
		VALUES ($1, now())
		RETURNING id, role, created_at
	`, name).Scan(&role.ID, &role.Role, &role.CreatedAt)
	if err != nil {
		if pgErr := (*pgconn.PgError)(nil); errors.As(err, &pgErr) && pgErr.Code == "23505" {
			return entity.Role{}, ErrDuplicate
		}
		return entity.Role{}, err
	}

	for _, authorityID := range authorityIDs {
		if _, err := tx.Exec(ctx, `INSERT INTO authorites_roles (role_id, authority_id) VALUES ($1, $2)`, role.ID, authorityID); err != nil {
			return entity.Role{}, err
		}
	}
	if err := tx.Commit(ctx); err != nil {
		return entity.Role{}, err
	}
	role.Authorities, err = r.authoritiesForRole(ctx, role.ID)
	return role, err
}

func (r *RoleRepository) authoritiesForRole(ctx context.Context, roleID int64) ([]string, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT a.name
		FROM authorities a
		JOIN authorites_roles ar ON ar.authority_id = a.id
		WHERE ar.role_id=$1 AND a.deleted_at IS NULL
		ORDER BY a.name
	`, roleID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	authorities := make([]string, 0)
	for rows.Next() {
		var authority string
		if err := rows.Scan(&authority); err != nil {
			return nil, err
		}
		authorities = append(authorities, authority)
	}
	return authorities, rows.Err()
}
