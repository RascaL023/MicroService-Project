# Sistem Informasi Divisi Pendidikan - Panduan Utama

Proyek ini adalah sistem manajemen pembelajaran berbasis microservices. Stack utamanya terdiri dari Go, Java Spring Boot, SvelteKit, PostgreSQL, Redis, dan Kong API Gateway.

Fokus sistem ini adalah pengelolaan user, role, jurusan, batch, group belajar, enrollment, jadwal, materi, assessment, nilai, laporan, notifikasi email, dan audit log.

## Daftar Isi

- [Ringkasan Proyek](#ringkasan-proyek)
- [Stack Teknologi](#stack-teknologi)
- [Arsitektur Sistem](#arsitektur-sistem)
- [SSOT Konfigurasi](#ssot-konfigurasi)
- [Mulai Cepat](#mulai-cepat)
- [Struktur Proyek](#struktur-proyek)
- [Backend Services](#backend-services)
- [Frontend](#frontend)
- [Dokumentasi SQL](#dokumentasi-sql)
- [Catatan Pengembangan](#catatan-pengembangan)

## Ringkasan Proyek

Sistem ini dibagi menjadi beberapa service dengan tanggung jawab yang jelas:

| Service | Tanggung Jawab |
|---|---|
| `auth-service` | Login, logout, aktivasi akun, reset password, role, authority, session |
| `user-service` | Profil user, batch, jurusan, import user dari Excel |
| `course-service` | Subject, group, enrollment, jadwal, assessment, nilai, laporan, audit log |
| `notification-service` | Konsumsi job email dari Redis Stream dan kirim email via SMTP |
| `frontend` | UI SvelteKit untuk portal pengguna dan portal pengelola |
| `api-gateway` | Kong gateway, routing API, plugin session injector |

Pola utamanya:

- komunikasi sync lewat REST API;
- komunikasi async lewat Redis Streams;
- session disimpan di Redis;
- hak akses menggunakan role dan authority;
- Java service menerima konteks user dari Kong plugin lewat header;
- data user di `course-service` disalin ke `course_user_cache` untuk mengurangi call ke `user-service`.

## Stack Teknologi

| Area | Teknologi |
|---|---|
| Auth service | Go, Chi, pgx, Redis client, bcrypt |
| Java services | Java 21, Spring Boot, JPA/Hibernate, Redis, Maven |
| Frontend | SvelteKit, TypeScript, Vite |
| Database | PostgreSQL |
| Cache/Event | Redis, Redis Streams |
| Gateway | Kong |
| Email | Spring Mail, SMTP |

Custom library Java:

- `global/custom library/SecurityFilter`: filter auth berbasis header/JWT untuk Java service.
- `global/custom library/ApiResponseKit`: format response sukses/error yang seragam.

## Arsitektur Sistem

```text
Frontend SvelteKit
       |
       | HTTP REST
       v
Kong API Gateway
       |
       | route + session injector
       v
Auth-Service      User-Service      Course-Service
   |                   |                  |
   |                   |                  |
PostgreSQL        PostgreSQL         PostgreSQL
   |                   |                  |
   +-------------------+------------------+
                       |
                     Redis
             session, stream, token
                       |
                       v
             Notification-Service
                  SMTP Email
```

Contoh alur login:

```text
User login dari frontend
-> auth-service validasi email/password
-> auth-service buat session di Redis
-> frontend simpan session di localStorage
-> request berikutnya membawa Authorization: Session {sessionId}
-> Kong plugin baca session dari Redis
-> Kong inject header user/role/authority ke backend service
-> backend tetap validasi authority dengan @PreAuthorize atau logic sejenis
```

Contoh alur event user:

```text
user-service update user
-> publish event ke Redis Stream user:events
-> course-service update course_user_cache
-> auth-service bisa merespons event tertentu seperti delete/status change
```

## SSOT Konfigurasi

SSOT konfigurasi project ini belum sepenuhnya bersih.

Secara konsep, `global/public-config.yml` dan `global/private-config.yml` menjadi sumber konfigurasi utama untuk service. Masalahnya, beberapa bagian infrastruktur Docker masih punya konfigurasi sendiri yang hardcoded.

Contoh sumber konfigurasi yang masih terpisah:

| Lokasi | Isi |
|---|---|
| `global/public-config.yml` | port service, nama database, Redis, stream name, URL aktivasi/reset |
| `global/private-config.yml` | secret/password lokal |
| `api-gateway/docker-compose.yml` | container Kong/Redis, env Kong, body size, volume, network mode |
| `api-gateway/kong/kong.yml` | route Kong, upstream service URL, Redis host/port untuk plugin |
| `frontend/.env*` | base URL frontend ke gateway jika dipakai |

Dampaknya:

- port atau host bisa drift antara global config dan `kong.yml`;
- Redis key/prefix bisa beda antara app config dan plugin Kong;
- Docker Compose belum otomatis membaca semua nilai dari global config;
- perubahan konfigurasi perlu dicek di lebih dari satu tempat.

Untuk sekarang ini masih wajar untuk project lokal, tapi belum ideal sebagai SSOT. Sasaran yang lebih rapi:

- semua nilai utama ada di `.env` atau global config;
- Docker Compose mengambil nilai dari env yang sama;
- Kong declarative config dibuat dari template atau env-substitution;
- dokumentasi menjalankan service menyebut jelas konfigurasi mana yang authoritative.

## Mulai Cepat

### Prasyarat

Pastikan sudah ada:

- Java 21+
- Go sesuai versi `go.mod`
- Node.js dan npm
- PostgreSQL
- Redis
- Docker dan Docker Compose untuk menjalankan Kong/Redis via container
- Maven

### Database

Script SQL tersedia di folder `.assets` masing-masing service:

```text
backend/auth-service/.assets/auth-service-ddl.sql
backend/auth-service/.assets/auth-service-dml.sql
backend/user-service/.assets/user-service-ddl.sql
backend/user-service/.assets/user-service-dml.sql
backend/course-service/.assets/course-service-ddl.sql
backend/course-service/.assets/course-service-dml.sql
backend/notification-service/.assets/notification-service-ddl.sql
backend/notification-service/.assets/notification-service-dml.sql
```

Database default dari konfigurasi saat ini:

```text
mcr_auth
mcr_user
mcr_course
```

### Build Custom Library

Java service memakai library lokal. Install dulu ke Maven local repository:

```bash
cd "global/custom library/SecurityFilter"
mvn clean install -DskipTests
```

```bash
cd "global/custom library/ApiResponseKit"
mvn clean install -DskipTests
```

### Jalankan Gateway dan Redis

```bash
cd api-gateway
docker compose up -d
```

Catatan: compose di folder ini fokus ke Kong dan Redis. Backend service tetap perlu dijalankan manual atau lewat mekanisme lain.

### Jalankan Backend Service

Auth service:

```bash
cd backend/auth-service
go run cmd/server/main.go
```

User service:

```bash
cd backend/user-service
./mvnw spring-boot:run
```

Course service:

```bash
cd backend/course-service
./mvnw spring-boot:run
```

Notification service:

```bash
cd backend/notification-service
./mvnw spring-boot:run
```

### Jalankan Frontend

```bash
cd frontend
npm install
npm run dev
```

## Struktur Proyek

```text
/home/rascal/!Project!/
├── backend/
│   ├── auth-service/
│   ├── user-service/
│   ├── course-service/
│   └── notification-service/
├── frontend/
├── api-gateway/
├── global/
│   ├── custom library/
│   ├── public-config.yml
│   └── private-config.yml
├── README.md
├── SUMMARY.md
├── CODEBASE_OVERVIEW.md
├── ARCHITECTURE.md
└── README_DOCUMENTATION.md
```

## Backend Services

### Auth-Service

Fungsi utama:

- login/logout;
- aktivasi akun;
- reset password;
- manajemen user auth;
- role dan authority;
- last login;
- session Redis;
- publish job email ke Redis Stream.

Endpoint utama:

```text
POST   /api/auths/login
POST   /api/auths/logout
POST   /api/auths/activations/request
POST   /api/auths/activations/complete
POST   /api/auths/passwords/forgot
POST   /api/auths/passwords/reset
GET    /api/auths/users
PATCH  /api/auths/users/{id}/status
PATCH  /api/auths/users/{id}/role
DELETE /api/auths/users/{id}/role
```

### User-Service

Fungsi utama:

- CRUD user profile;
- batch;
- jurusan;
- import user dari Excel;
- download template import;
- publish user event;
- status user: `ACTIVE`, `GRADUATED`, `DROP_OUT`.

Endpoint utama:

```text
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PATCH  /api/users/{id}
DELETE /api/users/{id}
POST   /api/users/import
GET    /api/users/import-template
GET    /api/batches
GET    /api/majors
```

### Course-Service

Fungsi utama:

- subject;
- group;
- enrollment;
- jadwal dan template waktu;
- meeting;
- assessment;
- acknowledgement learner;
- gradebook dan nilai;
- laporan nilai mingguan;
- audit log;
- cache user dari event user-service.

Endpoint utama:

```text
GET    /api/subjects
GET    /api/groups
GET    /api/enrollments
GET    /api/group-schedules
GET    /api/schedule-templates
GET    /api/assessments
GET    /api/audit-logs
GET    /api/reports/weekly-grades
```

### Notification-Service

Fungsi utama:

- konsumsi Redis Stream `notification:emails`;
- kirim email aktivasi akun;
- kirim email reset password;
- ack job setelah sukses diproses.

Service ini tidak punya database relasional sendiri.

## Frontend

Frontend memakai SvelteKit dengan dua portal:

- portal pengguna: `/app/*`;
- portal pengelola: `/admin/*`.

Session disimpan di `localStorage`. UI melakukan validasi tampilan berdasarkan authority dari session lokal. Ini hanya untuk UX. Validasi final tetap di backend.

Contoh:

- user dengan `group.read` bisa melihat group;
- user dengan `group.update` atau `group.*` bisa melihat tombol edit;
- user dengan `group.delete` atau `group.*` bisa melihat tombol hapus.

## Dokumentasi SQL

Format SQL sudah dipisah:

- `*-ddl.sql`: schema, tabel, index, constraint;
- `*-dml.sql`: seed awal.

Catatan:

- DDL dibuat berdasarkan entity saat ini.
- DML dibuat untuk local development.
- Jika database lokal sudah lama berjalan dengan `ddl-auto=update`, tetap ada kemungkinan drift dengan file SQL.

## Catatan Pengembangan

Untuk perubahan backend:

- ikuti pola controller -> service -> repository;
- validasi authority di backend tetap wajib;
- jangan mengandalkan hide tombol frontend sebagai security;
- event antar service pakai Redis Streams.

Untuk perubahan frontend:

- gunakan helper API yang sudah ada;
- pakai authority dari session untuk render menu/tombol;
- akses API tetap siap menerima 401/403;
- pertahankan UX portal yang padat, rapi, dan fokus operasional.

## Dokumen Lain

- [SUMMARY.md](./SUMMARY.md): ringkasan cepat.
- [CODEBASE_OVERVIEW.md](./CODEBASE_OVERVIEW.md): referensi teknis.
- [ARCHITECTURE.md](./ARCHITECTURE.md): diagram arsitektur dan flow.
- [README_DOCUMENTATION.md](./README_DOCUMENTATION.md): indeks dokumentasi.
