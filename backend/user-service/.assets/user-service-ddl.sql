CREATE DATABASE mcr_user;

CREATE TABLE IF NOT EXISTS batches (
	id INTEGER PRIMARY KEY,
	name TEXT,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS majors (
	id TEXT PRIMARY KEY,
	name TEXT NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS users (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL,
	email TEXT NOT NULL,
	gender CHAR(1) NOT NULL,
	status TEXT NOT NULL DEFAULT 'ACTIVE',
	graduated_at DATE,
	batch_id INTEGER NOT NULL REFERENCES batches(id),
	major_id TEXT NOT NULL REFERENCES majors(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT users_gender_check CHECK (gender IN ('L', 'P')),
	CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'GRADUATED', 'DROP_OUT'))
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_active_idx
ON users (email)
WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS users_batch_id_idx
ON users (batch_id);

CREATE INDEX IF NOT EXISTS users_major_id_idx
ON users (major_id);

CREATE INDEX IF NOT EXISTS users_status_idx
ON users (status);

CREATE INDEX IF NOT EXISTS users_deleted_at_idx
ON users (deleted_at);

CREATE INDEX IF NOT EXISTS batches_deleted_at_idx
ON batches (deleted_at);

CREATE INDEX IF NOT EXISTS majors_deleted_at_idx
ON majors (deleted_at);
