# Gambaran Teknis Codebase

Dokumen ini merangkum struktur teknis project. Detailnya mengikuti kondisi repo saat ini, bukan sekadar rancangan awal.

## 1. Backend Services

Proyek memakai 4 backend service:

| Service | Stack | Fokus |
|---|---|---|
| `auth-service` | Go | Auth, session, role, authority |
| `user-service` | Java Spring Boot | Profil user, batch, jurusan |
| `course-service` | Java Spring Boot | Akademik, group, jadwal, assessment, nilai |
| `notification-service` | Java Spring Boot | Email job consumer |

## 1.1 Auth-Service

Lokasi: `backend/auth-service`

Tanggung jawab:

- autentikasi user;
- aktivasi akun;
- reset password;
- session Redis;
- role dan authority;
- status auth user;
- last login;
- publish job email ke Redis Stream.

Komponen penting:

- `internal/entity`: model user, role, authority, session.
- `internal/repository`: akses PostgreSQL dan Redis.
- `internal/service`: logic auth dan user.
- `internal/http`: router dan handler.
- `internal/config`: load konfigurasi dari `global`.

Endpoint utama:

```text
POST   /api/auths/login
POST   /api/auths/logout
POST   /api/auths/activations/request
POST   /api/auths/activations/complete
POST   /api/auths/passwords/forgot
POST   /api/auths/passwords/reset
GET    /api/auths/users
GET    /api/auths/users/{id}
PATCH  /api/auths/users/{id}/status
PATCH  /api/auths/users/{id}/role
DELETE /api/auths/users/{id}/role
```

Data utama:

- `users`
- `roles`
- `authorities`
- `users_roles`
- `authorities_roles`

## 1.2 User-Service

Lokasi: `backend/user-service`

Tanggung jawab:

- profil user;
- batch;
- jurusan;
- import user dari Excel;
- download template import;
- publish event user.

Entity utama:

- `User`: id, name, email, gender, status, graduatedAt, batch, major.
- `Batch`: id, name.
- `Major`: id, name.
- `UserStatus`: `ACTIVE`, `GRADUATED`, `DROP_OUT`.

Endpoint utama:

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PATCH  /api/users/{id}
DELETE /api/users/{id}
GET    /api/users/lookup
POST   /api/users/import
GET    /api/users/import-template
GET    /api/users/dashboard-summary
GET    /api/batches
POST   /api/batches
PATCH  /api/batches/{id}
GET    /api/majors
POST   /api/majors
PATCH  /api/majors/{id}
```

Event yang dipublish:

- `UserCreated`
- `UserEmailUpdated`
- `UserProfileUpdated`
- `UserStatusChanged`
- `UserDeleted`

## 1.3 Course-Service

Lokasi: `backend/course-service`

Tanggung jawab:

- subject;
- group;
- enrollment;
- schedule dan template waktu;
- meeting;
- subject material dan module;
- assessment;
- acknowledgement learner;
- nilai;
- laporan Excel;
- audit log;
- cache user.

Entity utama:

- `Subject`
- `Group`
- `Enrollment`
- `GroupSchedule`
- `ScheduleTemplate`
- `GroupMeeting`
- `SubjectMaterial`
- `SubjectModule`
- `Assessment`
- `AssessmentGrade`
- `AssessmentAcknowledgement`
- `CourseUserCache`
- `AuditLog`

Endpoint utama:

```text
GET    /api/subjects
POST   /api/subjects
PATCH  /api/subjects/{id}
GET    /api/subject-materials
POST   /api/subject-materials
PATCH  /api/subject-materials/{id}
GET    /api/subject-modules
POST   /api/subject-modules
DELETE /api/subject-modules/{id}

GET    /api/groups
POST   /api/groups
PATCH  /api/groups/{id}
POST   /api/groups/{id}/complete
GET    /api/groups/{id}/details
GET    /api/groups/{id}/gradebook

GET    /api/enrollments
GET    /api/enrollments/me
POST   /api/enrollments
PATCH  /api/enrollments/{id}
DELETE /api/enrollments/{id}

GET    /api/group-schedules
GET    /api/group-schedules/me
POST   /api/group-schedules
PATCH  /api/group-schedules/{id}
DELETE /api/group-schedules/{id}

GET    /api/schedule-templates
POST   /api/schedule-templates
PATCH  /api/schedule-templates/{id}
DELETE /api/schedule-templates/{id}

GET    /api/assessments
POST   /api/assessments
PATCH  /api/assessments/{id}
DELETE /api/assessments/{id}

GET    /api/audit-logs
GET    /api/reports/weekly-grades
```

Business rule penting:

- `DROP_OUT` tetap historis, tapi tidak boleh ditambahkan ke group baru.
- `GRADUATED` tidak boleh ditambahkan sebagai learner.
- Laporan nilai tidak menyertakan learner `DROP_OUT`.
- Acknowledgement dipakai learner untuk mark done assessment offline.
- Reminder instructor fokus ke assessment yang belum dinilai.

## 1.4 Notification-Service

Lokasi: `backend/notification-service`

Tanggung jawab:

- konsumsi Redis Stream `notification:emails`;
- kirim email aktivasi akun;
- kirim email reset password;
- ack job setelah sukses.

Service ini tidak punya database PostgreSQL sendiri.

## 2. Frontend

Lokasi: `frontend`

Frontend memakai SvelteKit dan TypeScript.

Pada development Linux, entry point paling nyaman adalah Nginx di `http://localhost:9000` yang dijalankan lewat `./.assets/scripts/toggle.sh up all`. Nginx meneruskan `/api/*` ke Kong dan route lain ke Vite dev server atau static build.

Route utama:

```text
/login
/activate
/reset-password
/app/*
/admin/*
```

Pola akses:

- `/app/*`: portal pengguna.
- `/admin/*`: portal pengelola.
- banyak halaman dipakai bersama, lalu tombol aksi diatur oleh authority.

Contoh:

```text
USER dengan group.read
-> bisa melihat daftar/detail group
-> tidak melihat tombol edit/delete

CHIEF_INSTRUCTOR dengan group.*
-> bisa melihat group
-> bisa create/update/delete
```

Komponen penting:

- `PortalShell`
- `PageTitle`
- `Pagination`
- `ConfirmModal`
- `Notice`
- `AccessPanel`
- `Icons`

## 3. Auth dan Otorisasi

Session disimpan di Redis. Frontend menyimpan session ringkas di `localStorage`.

Flow request:

```text
Frontend kirim Authorization: Session {sessionId}
-> Kong plugin baca Redis
-> Kong inject header user
-> backend service membaca header
-> backend cek authority
```

Authority memakai format:

```text
resource.action
resource.*
```

Contoh:

```text
user.read
user.update
group-schedule.*
log-access
```

## 4. Integrasi

### REST

REST dipakai untuk request langsung dari frontend dan lookup antar service.

Contoh:

```text
course-service -> user-service lookup user
frontend -> Kong -> course-service
frontend -> Kong -> user-service
```

### Redis Streams

Redis Streams dipakai untuk event async.

```text
user-service -> user:events -> course-service/auth-service
auth-service -> notification:emails -> notification-service
```

## 5. Database dan SQL

SQL ada di folder `.assets` tiap service.

Auth-service:

- `auth-service-ddl.sql`
- `auth-service-dml.sql`

User-service:

- `user-service-ddl.sql`
- `user-service-dml.sql`

Course-service:

- `course-service-ddl.sql`
- `course-service-dml.sql`

Notification-service:

- `notification-service-ddl.sql`
- `notification-service-dml.sql`

Catatan: notification-service tidak punya tabel relasional; file SQL-nya hanya menjelaskan kondisi tersebut.

## 6. SSOT Konfigurasi

Konfigurasi utama aplikasi ada di `global/public-config.yml` dan `global/private-config.yml`. Namun SSOT belum sempurna karena Docker/Kong/Nginx masih punya konfigurasi sendiri.

Bagian yang perlu dicek saat mengubah config:

| Lokasi | Yang Diatur |
|---|---|
| `global/public-config.yml` | service port, database, Redis, stream, URL aktivasi/reset |
| `global/private-config.yml` | secret/password lokal |
| `api-gateway/docker-compose.yml` | container Kong/Redis, env Kong, body size, network |
| `api-gateway/kong/kong.yml` | route, upstream URL, Redis config plugin |
| `reverse-proxy/nginx.dev.conf` | Nginx dev: `/api/*` ke Kong, frontend ke Vite |
| `reverse-proxy/nginx.static.conf` | Nginx static: `/api/*` ke Kong, frontend ke `frontend/build` |

Konsekuensi:

- ganti port service harus cek global config dan Kong;
- ganti Redis prefix harus cek app dan plugin Kong;
- ganti route API harus cek frontend, Kong, dan Nginx;
- ganti port Nginx harus cek cloudflared lokal;
- compose belum otomatis menjadi representasi penuh dari global config.

## 7. Catatan Kualitas

Hal yang perlu dijaga:

- validasi backend tidak boleh bergantung pada frontend;
- event lintas service harus idempotent;
- query dashboard harus hemat call;
- audit log domain-level cukup untuk aksi penting, bukan semua request;
- Docker/Kong/Nginx config perlu dirapikan kalau project mulai masuk deployment serius.
