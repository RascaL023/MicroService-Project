INSERT INTO authorities(name) VALUES
	('user.create'), ('user.read'), ('user.update'), ('user.delete'),
	('user.*'),
	('batch.create'), ('batch.read'), ('batch.update'), ('batch.delete'),
	('batch.*'),
	('major.create'), ('major.read'), ('major.update'), ('major.delete'),
	('major.*'),

	('course.create'), ('course.read'), ('course.update'), ('course.delete'),
	('course.*'),
	('group.create'), ('group.read'), ('group.update'), ('group.delete'),
	('group.*'),
    ('group-schedule.create'), ('group-schedule.read'), ('group-schedule.update'), ('group-schedule.delete'),
    ('group-schedule.*'),
	('enrollment.create'), ('enrollment.read'), ('enrollment.update'), ('enrollment.delete'),
	('enrollment.*'),

	('subject.create'), ('subject.read'), ('subject.update'), ('subject.delete'),
	('subject.*'),
	('subject-material.create'), ('subject-material.read'), ('subject-material.update'), ('subject-material.delete'),
	('subject-material.*'),
	('subject-module.create'), ('subject-module.read'), ('subject-module.update'), ('subject-module.delete'),
	('subject-module.*'),

    ('log-access')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles(name, created_at) VALUES
	('ADMIN', now()),
	('CHIEF', now()),
	('CHIEF_DEPUTY', now()),
	('CHIEF_INSTRUCTOR', now()),
	('CHIEF_DEPUTY_INSTRUCTOR', now()),
	('USER', now())
ON CONFLICT (name) DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN (
    'user.*', 'batch.*', 
    'major.*',
    'course.*', 'group.*', 
    'group-schedule.*', 'enrollment.*', 
    'subject.*', 'subject-material.*',
    'subject-module.*',
    'log-access'
) WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN (
    'user.*', 'batch.*', 'major.*'
) WHERE r.name = 'CHIEF'
ON CONFLICT DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN (
    'user.update', 'batch.update', 'major.update'
) WHERE r.name = 'CHIEF_DEPUTY'
ON CONFLICT DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN (
    'course.*', 'group.*', 'group-schedule.*',
    'subject.*', 'subject-material.*', 'subject-module.*',
    'enrollment.*'
) WHERE r.name = 'CHIEF_INSTRUCTOR'
ON CONFLICT DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN (
    'course.update', 'group.update', 'group-schedule.update',
    'subject.update', 'subject-material.update', 'subject-module.update',
    'enrollment.update',

    'course.delete', 'group.delete', 'group-schedule.delete',
    'subject.delete', 'subject-material.delete', 'subject-module.delete',
    'enrollment.delete'
) WHERE r.name = 'CHIEF_DEPUTY_INSTRUCTOR'
ON CONFLICT DO NOTHING;

INSERT INTO authorities_roles(role_id, authority_id)
SELECT r.id, a.id
FROM roles r
JOIN authorities a ON a.name IN (
    'user.read', 'batch.read', 'major.read',
    'course.read', 'group.read', 
    'group-schedule.read', 'enrollment.read', 
    'subject.read', 'subject-material.read',
    'subject-module.read'
) WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;





SELECT 
    r.name AS role,
    string_agg(a.name, ', ' ORDER BY a.name) AS authority
FROM authorities_roles ar
    JOIN roles r ON r.id = ar.role_id
    JOIN authorities a ON a.id = ar.authority_id
GROUP BY r.id ORDER BY r.id;

SELECT 
    u.email AS user_email,
    string_agg(r.name, ', ') AS roles
FROM users_roles ur
    JOIN roles r ON r.id = ur.role_id
    JOIN users u ON u.id = ur.user_id
GROUP BY u.id ORDER BY u.id;
