INSERT INTO batches (id, name, created_at)
VALUES
	(2024, 'Angkatan 2024', now()),
	(2025, 'Angkatan 2025', now()),
	(2026, 'Angkatan 2026', now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO majors (id, name, created_at)
VALUES
	('TI', 'Teknik Informatika', now()),
	('SI', 'Sistem Informasi', now()),
	('MI', 'Manajemen Informatika', now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO users (id, name, email, gender, status, graduated_at, batch_id, major_id, created_at)
VALUES
	(1, 'Admin Divdik', 'admin@example.com', 'L', 'ACTIVE', NULL, 2024, 'TI', now()),
	(2, 'Chief Instructor', 'chief.instructor@example.com', 'P', 'ACTIVE', NULL, 2024, 'SI', now()),
	(3, 'Asep Hidayat', 'asep@example.com', 'L', 'ACTIVE', NULL, 2026, 'TI', now()),
	(4, 'Aura Adams', 'aura@example.com', 'P', 'ACTIVE', NULL, 2026, 'SI', now()),
	(5, 'Budi Santoso', 'budi@example.com', 'L', 'GRADUATED', '2026-06-01', 2025, 'MI', now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	email = EXCLUDED.email,
	gender = EXCLUDED.gender,
	status = EXCLUDED.status,
	graduated_at = EXCLUDED.graduated_at,
	batch_id = EXCLUDED.batch_id,
	major_id = EXCLUDED.major_id,
	updated_at = now(),
	deleted_at = NULL;

SELECT
	u.id,
	u.name,
	u.email,
	u.status,
	b.name AS batch_name,
	m.name AS major_name
FROM users u
JOIN batches b ON b.id = u.batch_id
JOIN majors m ON m.id = u.major_id
WHERE u.deleted_at IS NULL
ORDER BY u.id;
