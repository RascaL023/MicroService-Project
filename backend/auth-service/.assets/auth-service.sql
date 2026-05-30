CREATE DATABASE mcr_auth;

CREATE TABLE IF NOT EXISTS users (
	id BIGSERIAL PRIMARY KEY,
	username TEXT NOT NULL UNIQUE,
	hash_password TEXT NOT NULL,
	is_banned BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);


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
    ('user.createAll'),
    ('user.read'),
    ('user.readAll'),
    ('user.update'),
    ('user.updateAll'),
    ('user.delete'),
    ('user.deleteAll');

INSERT INTO authorities_roles(role_id, authority_id) VALUES
    (1, 2),
    (1, 4),
    (1, 6),
    (1, 8),
    (2, 3),
    (2, 5),
    (2, 7);
