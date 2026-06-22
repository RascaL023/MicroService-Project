INSERT INTO subjects (id, name, created_at)
VALUES
	(1, 'Java Fundamental', now()),
	(2, 'Logika & Algoritma Bahasa C', now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO schedule_templates (id, name, start_time, end_time, created_at)
VALUES
	(1, 'Pagi 1', '08:00:00', '10:00:00', now()),
	(2, 'Siang 1', '13:00:00', '15:00:00', now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	start_time = EXCLUDED.start_time,
	end_time = EXCLUDED.end_time,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO course_user_cache (id, name, gender, batch, status, deleted_at, synced_at)
VALUES
	(1, 'Admin Divdik', 'L', 2024, 'ACTIVE', NULL, now()),
	(2, 'Chief Instructor', 'P', 2024, 'ACTIVE', NULL, now()),
	(3, 'Asep Hidayat', 'L', 2026, 'ACTIVE', NULL, now()),
	(4, 'Aura Adams', 'P', 2026, 'ACTIVE', NULL, now()),
	(5, 'Budi Santoso', 'L', 2025, 'GRADUATED', NULL, now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	gender = EXCLUDED.gender,
	batch = EXCLUDED.batch,
	status = EXCLUDED.status,
	deleted_at = EXCLUDED.deleted_at,
	synced_at = now();

INSERT INTO groups (id, name, academic_year, status, subject_id, created_at)
VALUES
	(1, 'Kelompok 1 - Java', '2026/2027', 'ON_GOING', 1, now()),
	(2, 'Kelompok 1 - Logika Algoritma', '2026/2027', 'ON_GOING', 2, now()),
	(3, 'Kelompok 2 - Logika Algoritma', '2026/2027', 'ON_GOING', 2, now())
ON CONFLICT (id) DO UPDATE
SET
	name = EXCLUDED.name,
	academic_year = EXCLUDED.academic_year,
	status = EXCLUDED.status,
	subject_id = EXCLUDED.subject_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO subject_materials (id, title, meeting_number, description, subject_id, created_at)
VALUES
	(1, 'Pengenalan Java', 1, 'Dasar sintaks dan struktur program Java.', 1, now()),
	(2, 'Percabangan dan Perulangan Java', 2, 'Kontrol alur pada Java.', 1, now()),
	(3, 'Dasar Logika Algoritma', 1, 'Konsep algoritma dan flowchart.', 2, now()),
	(4, 'Percabangan Bahasa C', 2, 'Percabangan pada Bahasa C.', 2, now()),
	(5, 'Perulangan Bahasa C', 3, 'Perulangan pada Bahasa C.', 2, now())
ON CONFLICT (id) DO UPDATE
SET
	title = EXCLUDED.title,
	meeting_number = EXCLUDED.meeting_number,
	description = EXCLUDED.description,
	subject_id = EXCLUDED.subject_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO subject_modules (id, original_filename, stored_filename, file_path, mime_type, file_size, subject_id, created_at)
VALUES
	(1, 'modul-java-1.pdf', 'java-1.pdf', 'uploads/subjects/java-1.pdf', 'application/pdf', 245760, 1, now()),
	(2, 'modul-c-1.pdf', 'c-1.pdf', 'uploads/subjects/c-1.pdf', 'application/pdf', 198400, 2, now())
ON CONFLICT (id) DO UPDATE
SET
	original_filename = EXCLUDED.original_filename,
	stored_filename = EXCLUDED.stored_filename,
	file_path = EXCLUDED.file_path,
	mime_type = EXCLUDED.mime_type,
	file_size = EXCLUDED.file_size,
	subject_id = EXCLUDED.subject_id,
	deleted_at = NULL;

INSERT INTO group_schedules (id, day_of_week, group_id, schedule_template_id, created_at)
VALUES
	(1, 'MONDAY', 1, 1, now()),
	(2, 'TUESDAY', 2, 2, now()),
	(3, 'WEDNESDAY', 3, 2, now())
ON CONFLICT (id) DO UPDATE
SET
	day_of_week = EXCLUDED.day_of_week,
	group_id = EXCLUDED.group_id,
	schedule_template_id = EXCLUDED.schedule_template_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO enrollments (id, user_id, role, group_id, created_at)
VALUES
	(1, 2, 'INSTRUCTOR', 1, now()),
	(2, 2, 'INSTRUCTOR', 2, now()),
	(3, 2, 'INSTRUCTOR', 3, now()),
	(4, 3, 'LEARNER', 1, now()),
	(5, 3, 'LEARNER', 2, now()),
	(6, 4, 'LEARNER', 2, now()),
	(7, 4, 'LEARNER', 3, now()),
	(8, 5, 'LEARNER', 2, now())
ON CONFLICT (id) DO UPDATE
SET
	user_id = EXCLUDED.user_id,
	role = EXCLUDED.role,
	group_id = EXCLUDED.group_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO group_meetings (id, meeting_date, status, note, started_at, completed_at, group_id, subject_material_id, created_at)
VALUES
	(1, '2026-06-01', 'DONE', 'Pertemuan perdana Java.', '2026-06-01 08:00:00+07', '2026-06-01 10:00:00+07', 1, 1, now()),
	(2, '2026-06-08', 'DONE', 'Kontrol alur Java.', '2026-06-08 08:00:00+07', '2026-06-08 10:00:00+07', 1, 2, now()),
	(3, '2026-06-03', 'DONE', 'Pengantar logika algoritma.', '2026-06-03 13:00:00+07', '2026-06-03 15:00:00+07', 2, 3, now()),
	(4, '2026-06-10', 'DONE', 'Percabangan C.', '2026-06-10 13:00:00+07', '2026-06-10 15:00:00+07', 2, 4, now()),
	(5, '2026-06-17', 'STARTED', 'Perulangan C.', '2026-06-17 13:00:00+07', NULL, 2, 5, now())
ON CONFLICT (id) DO UPDATE
SET
	meeting_date = EXCLUDED.meeting_date,
	status = EXCLUDED.status,
	note = EXCLUDED.note,
	started_at = EXCLUDED.started_at,
	completed_at = EXCLUDED.completed_at,
	group_id = EXCLUDED.group_id,
	subject_material_id = EXCLUDED.subject_material_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO assessments (id, type, title, description, due_at, group_id, group_meeting_id, created_at)
VALUES
	(1, 'ASSIGNMENT', 'Tugas 1', 'Latihan dasar Java.', '2026-06-02 23:59:00+07', 1, 1, now()),
	(2, 'ASSIGNMENT', 'Tugas 2', 'Latihan percabangan Java.', '2026-06-09 23:59:00+07', 1, 2, now()),
	(3, 'ASSIGNMENT', 'Tugas 1', 'Latihan logika dasar.', '2026-06-04 23:59:00+07', 2, 3, now()),
	(4, 'ASSIGNMENT', 'Tugas 2', 'Latihan percabangan C.', '2026-06-11 23:59:00+07', 2, 4, now()),
	(5, 'ASSIGNMENT', 'Tugas 3', 'Latihan perulangan C.', '2026-06-18 23:59:00+07', 2, 5, now()),
	(6, 'QUIZ', 'Quiz 1', 'Quiz konsep dasar.', '2026-06-12 23:59:00+07', 2, 4, now())
ON CONFLICT (id) DO UPDATE
SET
	type = EXCLUDED.type,
	title = EXCLUDED.title,
	description = EXCLUDED.description,
	due_at = EXCLUDED.due_at,
	group_id = EXCLUDED.group_id,
	group_meeting_id = EXCLUDED.group_meeting_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO assessment_grades (id, user_id, score, feedback, graded_by, graded_at, assessment_id)
VALUES
	(1, 3, 100.00, 'Bagus.', 2, now(), 3),
	(2, 4, 100.00, 'Sangat baik.', 2, now(), 3),
	(3, 5, 100.00, 'Historis.', 2, now(), 3),
	(4, 3, 60.00, 'Perlu perbaikan.', 2, now(), 4),
	(5, 4, 98.00, 'Bagus.', 2, now(), 4),
	(6, 5, 50.00, 'Masih kurang.', 2, now(), 4),
	(7, 4, 70.00, 'Cukup.', 2, now(), 5),
	(8, 5, 66.00, 'Perlu latihan lagi.', 2, now(), 5)
ON CONFLICT (id) DO UPDATE
SET
	user_id = EXCLUDED.user_id,
	score = EXCLUDED.score,
	feedback = EXCLUDED.feedback,
	graded_by = EXCLUDED.graded_by,
	graded_at = EXCLUDED.graded_at,
	assessment_id = EXCLUDED.assessment_id,
	updated_at = now();

INSERT INTO assessment_acknowledgements (id, user_id, done_at, created_at, assessment_id)
VALUES
	(1, 3, now(), now(), 3),
	(2, 4, now(), now(), 3),
	(3, 3, now(), now(), 4),
	(4, 4, now(), now(), 4),
	(5, 4, now(), now(), 5)
ON CONFLICT (id) DO UPDATE
SET
	user_id = EXCLUDED.user_id,
	done_at = EXCLUDED.done_at,
	assessment_id = EXCLUDED.assessment_id,
	updated_at = now(),
	deleted_at = NULL;

INSERT INTO audit_logs (id, actor_user_id, service, action, entity_type, entity_id, description, metadata_json, created_at)
VALUES
	(1, 2, 'course-service', 'GROUP_CREATED', 'GROUP', '1', 'Membuat group Kelompok 1 - Java', '{"groupName":"Kelompok 1 - Java"}', now()),
	(2, 2, 'course-service', 'ENROLLMENT_CREATED', 'ENROLLMENT', '4', 'Menambahkan learner ke group Java', '{"userId":3,"groupId":1}', now()),
	(3, 2, 'course-service', 'ASSESSMENT_GRADED', 'ASSESSMENT_GRADE', '4', 'Memberi nilai tugas learner', '{"assessmentId":4,"userId":3,"score":60}', now())
ON CONFLICT (id) DO UPDATE
SET
	actor_user_id = EXCLUDED.actor_user_id,
	service = EXCLUDED.service,
	action = EXCLUDED.action,
	entity_type = EXCLUDED.entity_type,
	entity_id = EXCLUDED.entity_id,
	description = EXCLUDED.description,
	metadata_json = EXCLUDED.metadata_json,
	created_at = EXCLUDED.created_at;

SELECT id, name, academic_year, status
FROM groups
WHERE deleted_at IS NULL
ORDER BY id;
