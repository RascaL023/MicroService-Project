package repository

import (
	"context"
	"errors"

	"auth-service/internal/entity"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

var ErrDuplicate = errors.New("duplicate record")
var ErrNotFound = errors.New("record not found")

type UserRepository struct {
	pool *pgxpool.Pool
}

func (r *UserRepository) ExistsByUsername(ctx context.Context, username string) (bool, error) {
	var exists bool
	err := r.pool.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM users WHERE username=$1 AND deleted_at IS NULL)`, username).Scan(&exists)
	return exists, err
}

func (r *UserRepository) Create(ctx context.Context, username, hashPassword string, roleIDs []int64) (entity.User, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil { return entity.User{}, err }
	defer func() { _ = tx.Rollback(ctx) }()

	var user entity.User
	err = tx.QueryRow(ctx, `
		INSERT INTO users (username, hash_password, created_at)
		VALUES ($1, $2, now())
		RETURNING id, username, hash_password, is_banned, created_at, updated_at, deleted_at
	`, username, hashPassword).Scan(
		&user.ID,
		&user.Username,
		&user.HashPassword,
		&user.IsBanned,
		&user.CreatedAt,
		&user.UpdatedAt,
		&user.DeletedAt,
	)
	if err != nil {
		if pgErr := (*pgconn.PgError)(nil); errors.As(err, &pgErr) && pgErr.Code == "23505" {
			return entity.User{}, ErrDuplicate
		}

		return entity.User{}, err
	}

	for _, roleID := range roleIDs {
		if _, err := tx.Exec(ctx, `INSERT INTO users_roles (user_id, role_id) VALUES ($1, $2)`, user.ID, roleID); err != nil {
			return entity.User{}, err
		}
	}

	if err := tx.Commit(ctx); err != nil { return entity.User{}, err }
	user.Roles, err = r.rolesForUser(ctx, user.ID)

	return user, err
}

func (r *UserRepository) FindByID(ctx context.Context, id int64) (entity.User, error) {
	user, err := r.findOne(ctx, `WHERE u.id=$1 AND u.deleted_at IS NULL`, id)
	if err != nil { return entity.User{}, err }

	return user, nil
}

func (r *UserRepository) FindByUsername(ctx context.Context, username string) (entity.User, error) {
	user, err := r.findOne(ctx, `WHERE u.username=$1 AND u.deleted_at IS NULL`, username)
	if err != nil { return entity.User{}, err }

	return user, nil
}

func (r *UserRepository) List(ctx context.Context, limit, offset int) ([]entity.User, int, error) {
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT count(*) FROM users WHERE deleted_at IS NULL`).Scan(&total); err != nil {
		return nil, 0, err
	}

	rows, err := r.pool.Query(ctx, `
		SELECT id, username, hash_password, is_banned, created_at, updated_at, deleted_at
		FROM users
		WHERE deleted_at IS NULL
		ORDER BY id
		LIMIT $1 OFFSET $2
	`, limit, offset)
	if err != nil { return nil, 0, err }
	defer rows.Close()

	users := make([]entity.User, 0)
	for rows.Next() {
		var user entity.User
		if err := rows.Scan(
			&user.ID,
			&user.Username,
			&user.HashPassword,
			&user.IsBanned,
			&user.CreatedAt,
			&user.UpdatedAt,
			&user.DeletedAt,
		); err != nil { return nil, 0, err }
		user.Roles, err = r.rolesForUser(ctx, user.ID)
		if err != nil { return nil, 0, err }
		users = append(users, user)
	}

	return users, total, rows.Err()
}

func (r *UserRepository) findOne(ctx context.Context, where string, arg any) (entity.User, error) {
	var user entity.User
	err := r.pool.QueryRow(ctx, `
		SELECT u.id, u.username, u.hash_password, u.is_banned, u.created_at, u.updated_at, u.deleted_at
		FROM users u
		`+where, arg).Scan(
		&user.ID,
		&user.Username,
		&user.HashPassword,
		&user.IsBanned,
		&user.CreatedAt,
		&user.UpdatedAt,
		&user.DeletedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) { return entity.User{}, ErrNotFound }
	if err != nil { return entity.User{}, err }
	user.Roles, err = r.rolesForUser(ctx, user.ID)

	return user, err
}

func (r *UserRepository) rolesForUser(ctx context.Context, userID int64) ([]entity.Role, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT r.id, r.name, r.created_at, r.updated_at, r.deleted_at
		FROM roles r
		JOIN users_roles ur ON ur.role_id = r.id
		WHERE ur.user_id=$1 AND r.deleted_at IS NULL
		ORDER BY r.id
	`, userID)
	if err != nil { return nil, err }
	defer rows.Close()

	roles := make([]entity.Role, 0)
	for rows.Next() {
		var role entity.Role
		if err := rows.Scan(&role.ID, &role.Name, &role.CreatedAt, &role.UpdatedAt, &role.DeletedAt); err != nil {
			return nil, err
		}
		role.Authorities, err = r.authoritiesForRole(ctx, role.ID)
		if err != nil { return nil, err }
		roles = append(roles, role)
	}

	return roles, rows.Err()
}

func (r *UserRepository) authoritiesForRole(ctx context.Context, roleID int64) ([]entity.Authority, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT a.id, a.name, a.created_at, a.updated_at, a.deleted_at
		FROM authorities a
		JOIN authorites_roles ar ON ar.authority_id = a.id
		WHERE ar.role_id=$1 AND a.deleted_at IS NULL
		ORDER BY a.name
	`, roleID)
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

	return authorities, rows.Err()
}
