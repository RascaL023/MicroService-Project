CREATE DATABASE mcr_auth;

CREATE TABLE IF NOT EXISTS users (
	id BIGINT PRIMARY KEY,
	email TEXT NOT NULL,
	hash_password TEXT,
	status TEXT NOT NULL DEFAULT 'PENDING_ACTIVATION',
	email_verified_at TIMESTAMPTZ,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT users_status_check CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'BANNED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_active_idx
ON users (email)
WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS roles (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL UNIQUE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS authorities (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL UNIQUE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS users_roles (
	user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
	PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS authorities_roles (
	authority_id BIGINT NOT NULL REFERENCES authorities(id) ON DELETE CASCADE,
	role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
	PRIMARY KEY (role_id, authority_id)
);

INSERT INTO authorities(name) VALUES
	('user.create'),
	('user.read'),
	('user.update'),
	('user.delete'),
	('user.*')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles(name, created_at) VALUES
	('ADMIN', now()),
	('USER', now())
ON CONFLICT (name) DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN ('user.*')
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN ('user.read', 'user.update', 'user.delete')
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;



SELECT 
    r.name AS Role, a.name AS Authority
FROM authorities_roles ar
    JOIN roles r ON r.id = ar.role_id
    JOIN authorities a ON a.id = ar.authority_id;
