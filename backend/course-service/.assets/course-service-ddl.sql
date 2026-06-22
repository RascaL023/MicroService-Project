CREATE DATABASE mcr_course;

CREATE TABLE IF NOT EXISTS subjects (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL UNIQUE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS schedule_templates (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL,
	start_time TIME NOT NULL,
	end_time TIME NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT schedule_templates_time_check CHECK (end_time > start_time)
);

CREATE TABLE IF NOT EXISTS course_user_cache (
	id BIGINT PRIMARY KEY,
	name TEXT,
	gender CHAR(1),
	batch INTEGER,
	status TEXT,
	deleted_at TIMESTAMPTZ,
	synced_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS groups (
	id BIGSERIAL PRIMARY KEY,
	name TEXT NOT NULL,
	academic_year TEXT NOT NULL,
	status TEXT,
	subject_id BIGINT NOT NULL REFERENCES subjects(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT groups_status_check CHECK (status IN ('ON_GOING', 'PASSED'))
);

CREATE TABLE IF NOT EXISTS subject_materials (
	id BIGSERIAL PRIMARY KEY,
	title TEXT NOT NULL,
	meeting_number INTEGER NOT NULL,
	description TEXT,
	subject_id BIGINT NOT NULL REFERENCES subjects(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS subject_modules (
	id BIGSERIAL PRIMARY KEY,
	original_filename TEXT NOT NULL,
	stored_filename TEXT NOT NULL,
	file_path TEXT NOT NULL,
	mime_type TEXT NOT NULL,
	file_size BIGINT NOT NULL,
	subject_id BIGINT NOT NULL REFERENCES subjects(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS group_schedules (
	id BIGSERIAL PRIMARY KEY,
	day_of_week TEXT NOT NULL,
	group_id BIGINT NOT NULL REFERENCES groups(id),
	schedule_template_id BIGINT NOT NULL REFERENCES schedule_templates(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT group_schedules_day_of_week_check CHECK (
		day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
	)
);

CREATE TABLE IF NOT EXISTS enrollments (
	id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL,
	role TEXT NOT NULL,
	group_id BIGINT NOT NULL REFERENCES groups(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT enrollments_role_check CHECK (role IN ('INSTRUCTOR', 'LEARNER'))
);

CREATE TABLE IF NOT EXISTS group_meetings (
	id BIGSERIAL PRIMARY KEY,
	meeting_date DATE NOT NULL,
	status TEXT NOT NULL,
	note TEXT,
	started_at TIMESTAMPTZ NOT NULL,
	completed_at TIMESTAMPTZ,
	group_id BIGINT NOT NULL REFERENCES groups(id),
	subject_material_id BIGINT NOT NULL REFERENCES subject_materials(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT group_meetings_status_check CHECK (status IN ('STARTED', 'DONE'))
);

CREATE TABLE IF NOT EXISTS assessments (
	id BIGSERIAL PRIMARY KEY,
	type TEXT NOT NULL,
	title TEXT NOT NULL,
	description TEXT,
	due_at TIMESTAMPTZ,
	original_filename TEXT,
	stored_filename TEXT,
	file_path TEXT,
	mime_type TEXT,
	file_size BIGINT,
	group_id BIGINT NOT NULL REFERENCES groups(id),
	group_meeting_id BIGINT REFERENCES group_meetings(id),
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	CONSTRAINT assessments_type_check CHECK (type IN ('ASSIGNMENT', 'QUIZ', 'MIDTERM', 'FINAL_EXAM'))
);

CREATE TABLE IF NOT EXISTS assessment_grades (
	id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL,
	score NUMERIC(5, 2) NOT NULL,
	feedback TEXT,
	graded_by BIGINT NOT NULL,
	graded_at TIMESTAMPTZ NOT NULL,
	updated_at TIMESTAMPTZ,
	assessment_id BIGINT NOT NULL REFERENCES assessments(id),
	CONSTRAINT uk_assessment_grades_assessment_user UNIQUE (assessment_id, user_id),
	CONSTRAINT assessment_grades_score_check CHECK (score >= 0 AND score <= 100)
);

CREATE TABLE IF NOT EXISTS assessment_acknowledgements (
	id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL,
	done_at TIMESTAMPTZ NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
	updated_at TIMESTAMPTZ,
	deleted_at TIMESTAMPTZ,
	assessment_id BIGINT NOT NULL REFERENCES assessments(id),
	CONSTRAINT uk_assessment_acknowledgements_assessment_user UNIQUE (assessment_id, user_id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
	id BIGSERIAL PRIMARY KEY,
	actor_user_id BIGINT,
	service TEXT NOT NULL,
	action TEXT NOT NULL,
	entity_type TEXT NOT NULL,
	entity_id TEXT,
	description TEXT,
	metadata_json TEXT,
	created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_groups_subject_id
ON groups (subject_id);

CREATE INDEX IF NOT EXISTS idx_groups_status_deleted
ON groups (status, deleted_at);

CREATE INDEX IF NOT EXISTS idx_subject_materials_subject_meeting
ON subject_materials (subject_id, meeting_number);

CREATE INDEX IF NOT EXISTS idx_subject_modules_subject_id
ON subject_modules (subject_id);

CREATE INDEX IF NOT EXISTS idx_group_schedules_group_deleted
ON group_schedules (group_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_group_schedules_template_deleted
ON group_schedules (schedule_template_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_enrollments_user_group_role_deleted
ON enrollments (user_id, group_id, role, deleted_at);

CREATE INDEX IF NOT EXISTS idx_enrollments_group_role_deleted
ON enrollments (group_id, role, deleted_at);

CREATE INDEX IF NOT EXISTS idx_group_meetings_group_deleted
ON group_meetings (group_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_group_meetings_subject_material_deleted
ON group_meetings (subject_material_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_assessments_group_type_deleted
ON assessments (group_id, type, deleted_at);

CREATE INDEX IF NOT EXISTS idx_assessment_acknowledgements_user
ON assessment_acknowledgements (user_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_assessment_acknowledgements_assessment
ON assessment_acknowledgements (assessment_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created
ON audit_logs (actor_user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
ON audit_logs (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action
ON audit_logs (action);
