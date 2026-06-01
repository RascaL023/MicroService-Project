package repository

import (
	"context"
	"errors"

	"auth-service/internal/entity"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

const (
	AccountPendingActivation = "PENDING_ACTIVATION"
	AccountActive            = "ACTIVE"
	AccountBanned            = "BANNED"
)

var ErrDuplicate = errors.New("duplicate record")
var ErrNotFound = errors.New("record not found")

type UserRepository struct {
	pool *pgxpool.Pool
}

func (r *UserRepository) ExistsByEmail(ctx context.Context, email string) (bool, error) {
	var exists bool
	err := r.pool.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM users WHERE email=$1 AND deleted_at IS NULL)`, email).Scan(&exists)
	return exists, err
}

func (r *UserRepository) CreateBootstrapAdmin(ctx context.Context, id int64, email, hashPassword string, roleIDs []int64) (entity.User, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return entity.User{}, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var user entity.User
	err = tx.QueryRow(ctx, `
		INSERT INTO users (id, email, hash_password, status, email_verified_at, created_at)
		VALUES ($1, $2, $3, $4, now(), now())
		ON CONFLICT (id) DO UPDATE
		SET email=$2, updated_at=now()
		RETURNING id, email, hash_password, status, email_verified_at, created_at, updated_at, deleted_at
	`, id, email, hashPassword, AccountActive).Scan(
		&user.ID,
		&user.Email,
		&user.HashPassword,
		&user.Status,
		&user.EmailVerifiedAt,
		&user.CreatedAt,
		&user.UpdatedAt,
		&user.DeletedAt,
	)
	if err != nil {
		if isDuplicate(err) {
			return entity.User{}, ErrDuplicate
		}
		return entity.User{}, err
	}

	if err := replaceRoles(ctx, tx, id, roleIDs); err != nil {
		return entity.User{}, err
	}
	if err := tx.Commit(ctx); err != nil {
		return entity.User{}, err
	}

	user.Roles, err = r.rolesForUser(ctx, user.ID)
	return user, err
}

func (r *UserRepository) Provision(ctx context.Context, userID int64, email string, roleIDs []int64, upstreamStatus string) (entity.User, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return entity.User{}, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	status := AccountPendingActivation
	if upstreamStatus == AccountBanned {
		status = AccountBanned
	}

	_, err = tx.Exec(ctx, `
		UPDATE users
		SET deleted_at=now(), updated_at=now()
		WHERE email=$1 AND id<>$2 AND deleted_at IS NULL
	`, email, userID)
	if err != nil {
		return entity.User{}, err
	}

	var user entity.User
	err = tx.QueryRow(ctx, `
		INSERT INTO users (id, email, status, created_at)
		VALUES ($1, $2, $3, now())
		ON CONFLICT (id) DO UPDATE
		SET email=$2,
			status=CASE
				WHEN $3 = 'BANNED' THEN 'BANNED'
				WHEN users.hash_password IS NULL THEN 'PENDING_ACTIVATION'
				ELSE 'ACTIVE'
			END,
			updated_at=now(),
			deleted_at=NULL
		RETURNING id, email, hash_password, status, email_verified_at, created_at, updated_at, deleted_at
	`, userID, email, status).Scan(
		&user.ID,
		&user.Email,
		&user.HashPassword,
		&user.Status,
		&user.EmailVerifiedAt,
		&user.CreatedAt,
		&user.UpdatedAt,
		&user.DeletedAt,
	)
	if err != nil {
		if isDuplicate(err) {
			return entity.User{}, ErrDuplicate
		}
		return entity.User{}, err
	}

	if err := replaceRoles(ctx, tx, userID, roleIDs); err != nil {
		return entity.User{}, err
	}
	if err := tx.Commit(ctx); err != nil {
		return entity.User{}, err
	}

	user.Roles, err = r.rolesForUser(ctx, user.ID)
	return user, err
}

func (r *UserRepository) UpdateEmail(ctx context.Context, userID int64, email string) (entity.User, error) {
	user, err := r.findOneReturning(ctx, `
		UPDATE users
		SET email=$2, updated_at=now()
		WHERE id=$1 AND deleted_at IS NULL
		RETURNING id, email, hash_password, status, email_verified_at, created_at, updated_at, deleted_at
	`, userID, email)
	if err != nil {
		if isDuplicate(err) {
			return entity.User{}, ErrDuplicate
		}
		return entity.User{}, err
	}
	return user, nil
}

func (r *UserRepository) SyncRoles(ctx context.Context, userID int64, roleIDs []int64) (entity.User, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return entity.User{}, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	if err := replaceRoles(ctx, tx, userID, roleIDs); err != nil {
		return entity.User{}, err
	}
	if err := tx.Commit(ctx); err != nil {
		return entity.User{}, err
	}

	return r.FindByID(ctx, userID)
}

func (r *UserRepository) MarkDeleted(ctx context.Context, id int64) error {
	_, err := r.pool.Exec(ctx, `
		UPDATE users
		SET deleted_at=now(), updated_at=now()
		WHERE id=$1 AND deleted_at IS NULL
	`, id)
	return err
}

func (r *UserRepository) FindByID(ctx context.Context, id int64) (entity.User, error) {
	return r.findOne(ctx, `WHERE u.id=$1 AND u.deleted_at IS NULL`, id)
}

func (r *UserRepository) FindByEmail(ctx context.Context, email string) (entity.User, error) {
	return r.findOne(ctx, `WHERE u.email=$1 AND u.deleted_at IS NULL`, email)
}

func (r *UserRepository) SetActivatedPassword(ctx context.Context, id int64, hashPassword string) (entity.User, error) {
	return r.findOneReturning(ctx, `
		UPDATE users
		SET hash_password=$2, status=$3, email_verified_at=COALESCE(email_verified_at, now()), updated_at=now()
		WHERE id=$1 AND deleted_at IS NULL
		RETURNING id, email, hash_password, status, email_verified_at, created_at, updated_at, deleted_at
	`, id, hashPassword, AccountActive)
}

func (r *UserRepository) SetStatus(ctx context.Context, id int64, status string) (entity.User, error) {
	user, err := r.findOneReturning(ctx, `
		UPDATE users
		SET status=CASE
				WHEN $2 = 'BANNED' THEN 'BANNED'
				WHEN hash_password IS NULL THEN 'PENDING_ACTIVATION'
				ELSE 'ACTIVE'
			END,
			updated_at=now()
		WHERE id=$1 AND deleted_at IS NULL
		RETURNING id, email, hash_password, status, email_verified_at, created_at, updated_at, deleted_at
	`, id, status)
	if err != nil {
		return entity.User{}, err
	}

	return user, nil
}

func (r *UserRepository) List(ctx context.Context, limit, offset int) ([]entity.User, int, error) {
	var total int
	if err := r.pool.QueryRow(ctx, `SELECT count(*) FROM users WHERE deleted_at IS NULL`).Scan(&total); err != nil {
		return nil, 0, err
	}

	rows, err := r.pool.Query(ctx, `
		SELECT id, email, hash_password, status, email_verified_at, created_at, updated_at, deleted_at
		FROM users
	WHERE deleted_at IS NULL
	ORDER BY id
	LIMIT $1 OFFSET $2
	`, limit, offset)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	users := make([]entity.User, 0)
	for rows.Next() {
		var user entity.User
		if err := rows.Scan(
			&user.ID,
			&user.Email,
			&user.HashPassword,
			&user.Status,
			&user.EmailVerifiedAt,
			&user.CreatedAt,
			&user.UpdatedAt,
			&user.DeletedAt,
		); err != nil {
			return nil, 0, err
		}
		user.Roles, err = r.rolesForUser(ctx, user.ID)
		if err != nil {
			return nil, 0, err
		}
		users = append(users, user)
	}

	return users, total, rows.Err()
}

func (r *UserRepository) findOne(ctx context.Context, where string, arg any) (entity.User, error) {
	return r.findOneReturning(ctx, `
		SELECT u.id, u.email, u.hash_password, u.status, u.email_verified_at, u.created_at, u.updated_at, u.deleted_at
		FROM users u
		`+where, arg)
}

func (r *UserRepository) findOneReturning(ctx context.Context, query string, args ...any) (entity.User, error) {
	var user entity.User
	err := r.pool.QueryRow(ctx, query, args...).Scan(
		&user.ID,
		&user.Email,
		&user.HashPassword,
		&user.Status,
		&user.EmailVerifiedAt,
		&user.CreatedAt,
		&user.UpdatedAt,
		&user.DeletedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return entity.User{}, ErrNotFound
	}
	if err != nil {
		return entity.User{}, err
	}
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
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	roles := make([]entity.Role, 0)
	for rows.Next() {
		var role entity.Role
		if err := rows.Scan(&role.ID, &role.Name, &role.CreatedAt, &role.UpdatedAt, &role.DeletedAt); err != nil {
			return nil, err
		}
		role.Authorities, err = r.authoritiesForRole(ctx, role.ID)
		if err != nil {
			return nil, err
		}
		roles = append(roles, role)
	}

	return roles, rows.Err()
}

func (r *UserRepository) authoritiesForRole(ctx context.Context, roleID int64) ([]entity.Authority, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT a.id, a.name, a.created_at, a.updated_at, a.deleted_at
		FROM authorities a
		JOIN authorities_roles ar ON ar.authority_id = a.id
		WHERE ar.role_id=$1 AND a.deleted_at IS NULL
		ORDER BY a.name
	`, roleID)
	if err != nil {
		return nil, err
	}
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
		); err != nil {
			return nil, err
		}
		authorities = append(authorities, authority)
	}

	return authorities, rows.Err()
}

type roleTx interface {
	Exec(ctx context.Context, sql string, arguments ...any) (pgconn.CommandTag, error)
}

func replaceRoles(ctx context.Context, tx roleTx, userID int64, roleIDs []int64) error {
	if _, err := tx.Exec(ctx, `DELETE FROM users_roles WHERE user_id=$1`, userID); err != nil {
		return err
	}
	for _, roleID := range roleIDs {
		if _, err := tx.Exec(ctx, `INSERT INTO users_roles (user_id, role_id) VALUES ($1, $2)`, userID, roleID); err != nil {
			return err
		}
	}
	return nil
}

func isDuplicate(err error) bool {
	var pgErr *pgconn.PgError
	return errors.As(err, &pgErr) && pgErr.Code == "23505"
}
