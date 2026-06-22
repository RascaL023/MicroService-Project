# Sistem Informasi Divisi Pendidikan - Panduan Utama

Proyek ini adalah sistem manajemen pembelajaran berbasis microservices. Stack utamanya terdiri dari Go, Java Spring Boot, SvelteKit, PostgreSQL, Redis, Kong API Gateway, dan Nginx reverse proxy untuk development/testing.

Fokus sistem ini adalah pengelolaan user, role, jurusan, batch, group belajar, enrollment, jadwal, materi, assessment, nilai, laporan, notifikasi email, dan audit log.

## Daftar Isi

- [Ringkasan Proyek](#ringkasan-proyek)
- [Stack Teknologi](#stack-teknologi)
- [Arsitektur Sistem](#arsitektur-sistem)
- [SSOT Konfigurasi](#ssot-konfigurasi)
- [Reverse Proxy Nginx](#reverse-proxy-nginx)
- [Script Development](#script-development)
- [Mulai Cepat](#mulai-cepat)
- [Struktur Proyek](#struktur-proyek)
- [Backend Services](#backend-services)
- [Frontend](#frontend)
- [Dokumentasi SQL](#dokumentasi-sql)

## Ringkasan Proyek

Sistem ini dibagi menjadi beberapa service:

| Service | Tanggung Jawab |
|---|---|
| `auth-service` | Login, logout, aktivasi akun, reset password, role, authority, session |
| `user-service` | Profil user, batch, jurusan, import user dari Excel |
| `course-service` | Subject, group, enrollment, jadwal, assessment, nilai, laporan, audit log |
| `notification-service` | Konsumsi job email dari Redis Stream dan kirim email via SMTP |
| `frontend` | UI SvelteKit untuk portal pengguna dan portal pengelola |
| `api-gateway` | Kong gateway, routing API, plugin session injector |
| `reverse-proxy` | Nginx entry point lokal di `:9000` |

Pola utamanya:

- REST untuk request langsung;
- Redis Streams untuk event async;
- session disimpan di Redis;
- Kong membaca session lewat plugin dan inject konteks user ke backend;
- backend tetap melakukan validasi authority;
- `course-service` memakai `course_user_cache` agar lookup user lebih hemat.

## Stack Teknologi

| Area | Teknologi |
|---|---|
| Auth service | Go, Chi, pgx, Redis client, bcrypt |
| Java services | Java 21, Spring Boot, JPA/Hibernate, Redis, Maven |
| Frontend | SvelteKit, TypeScript, Vite |
| Database | PostgreSQL |
| Cache/Event | Redis, Redis Streams |
| Gateway | Kong |
| Reverse proxy | Nginx |
| Tunnel testing | Cloudflared |
| Email | Spring Mail, SMTP |

Custom library Java:

- `global/custom library/SecurityFilter`: filter auth berbasis header/JWT untuk Java service.
- `global/custom library/ApiResponseKit`: format response sukses/error yang seragam.

## Arsitektur Sistem

```text
Browser / Cloudflared
       |
       v
Nginx :9000
       |
       +-- /api/* -> Kong :8000 -> backend services
       |
       +-- /*     -> Vite :5173 atau frontend/build

Backend services
       |
       +-- PostgreSQL
       +-- Redis session/stream/token
```

Alur login:

```text
User login dari frontend
-> auth-service validasi email/password
-> auth-service buat session di Redis
-> frontend simpan session di localStorage
-> request berikutnya membawa Authorization: Session {sessionId}
-> Kong plugin baca session dari Redis
-> Kong inject header user/role/authority ke backend
-> backend validasi authority
```

## SSOT Konfigurasi

SSOT konfigurasi belum sepenuhnya bersih.

Secara konsep, `global/public-config.yml` dan `global/private-config.yml` menjadi sumber konfigurasi utama aplikasi. Namun Docker/Kong/Nginx masih punya konfigurasi sendiri.

| Lokasi | Isi |
|---|---|
| `global/public-config.yml` | port service, nama database, Redis, stream name, URL aktivasi/reset |
| `global/private-config.yml` | secret/password lokal |
| `api-gateway/docker-compose.yml` | container Kong/Redis, env Kong, volume, network mode |
| `api-gateway/kong/kong.yml` | route Kong, upstream service URL, Redis plugin config |
| `reverse-proxy/nginx.*.conf` | port Nginx, route `/api/*`, rate limit, static/dev frontend |
| `frontend/.env*` | base URL frontend jika dipakai |

Dampaknya:

- port bisa drift antara global config, Kong, dan Nginx;
- Redis prefix bisa beda antara aplikasi dan plugin Kong;
- Docker Compose belum otomatis membaca semua nilai global config;
- perubahan route perlu dicek di frontend, Kong, dan Nginx.

Untuk deployment yang lebih serius, target idealnya adalah satu sumber env/config yang dipakai backend, Docker Compose, Kong, Nginx, dan frontend. Namun karena gak seserius itu juga, jadinya ya gitu...

## Reverse Proxy Nginx

Nginx dipakai sebagai entry point lokal di `http://localhost:9000`.

Alurnya:

```text
localhost:9000/api/* -> Kong localhost:8000
localhost:9000/*     -> Vite localhost:5173 pada mode dev
localhost:9000/*     -> frontend/build pada mode static
```

File:

```text
reverse-proxy/nginx.dev.conf
reverse-proxy/nginx.static.conf
reverse-proxy/mime.types
```

Konfigurasi penting:

```nginx
listen 9000;
client_max_body_size 10m;
limit_req_status 429;
limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=10r/s;
```

Rate limit aktif hanya pada `/api/*`:

```nginx
location /api/ {
    limit_req zone=api_per_ip burst=20 nodelay;
    proxy_pass http://127.0.0.1:8000;
}
```

Cloudflared diarahkan ke Nginx, bukan langsung ke Kong atau frontend:

```yaml
ingress:
  - hostname: dev.rascal.my.id
    service: http://localhost:9000
  - service: http_status:404
```

Catatan: setup cloudflared ini untuk testing online saja, bukan pola produksi final.

## Script Development

Untuk Linux, cara paling enak menjalankan project adalah lewat script:

```bash
./.assets/scripts/toggle.sh up all
```

Script ini mengatur:

- Kong + Redis dari `api-gateway/docker-compose.yml`;
- auth-service;
- user-service;
- course-service;
- notification-service;
- frontend dev server;
- Nginx reverse proxy.

Perintah umum:

```bash
./.assets/scripts/toggle.sh status
./.assets/scripts/toggle.sh build all
./.assets/scripts/toggle.sh logs proxy
./.assets/scripts/toggle.sh logs compose
./.assets/scripts/toggle.sh restart proxy
./.assets/scripts/toggle.sh down all
```

Mode default adalah `DEV_TOGGLE_PROXY_MODE=dev`, sehingga Nginx meneruskan frontend ke Vite `:5173`.
Mode run default adalah `DEV_TOGGLE_RUN_MODE=source`, sehingga service dijalankan langsung dari source:

- `auth-service` memakai `go run ./cmd/server`;
- service Java memakai `./mvnw spring-boot:run -DskipTests`;
- frontend memakai Vite dev server.

Untuk build semua service:

```bash
./.assets/scripts/toggle.sh build all
```

Hasil build utama:

- `backend/auth-service/cmd/bin/main`;
- `backend/user-service/target/user-service-0.0.1-SNAPSHOT.jar`;
- `backend/course-service/target/course-service-0.0.1-SNAPSHOT.jar`;
- `backend/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar`;
- `frontend/build`.

Untuk menjalankan dari hasil build:

```bash
DEV_TOGGLE_RUN_MODE=build ./.assets/scripts/toggle.sh up all
```

Untuk mode static:

```bash
./.assets/scripts/toggle.sh build frontend
DEV_TOGGLE_RUN_MODE=build DEV_TOGGLE_PROXY_MODE=static ./.assets/scripts/toggle.sh up all
```

Pada mode static, script tidak menyalakan frontend dev server karena Nginx langsung melayani `frontend/build`.

## Mulai Cepat

Prasyarat:

- Java 21+
- Go sesuai `go.mod`
- Node.js dan npm
- PostgreSQL
- Docker dan Docker Compose
- Nginx
- Maven atau Maven wrapper

Jalankan semua untuk development Linux:

```bash
./.assets/scripts/toggle.sh up all
```

Akses:

```text
http://localhost:9000
```

Melihat status:

```bash
./.assets/scripts/toggle.sh status
```

Stop semua:

```bash
./.assets/scripts/toggle.sh down all
```

Jika ingin expose lewat cloudflared:

```bash
cloudflared tunnel run lms-dev
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
├── reverse-proxy/
├── global/
├── .assets/scripts/
├── README.md
├── SUMMARY.md
├── CODEBASE_OVERVIEW.md
├── ARCHITECTURE.md
├── README_DOCUMENTATION.md
├── DOCUMENTATION_INDEX.md
├── NGINX_GUIDE.md
└── NGINX_QUICK_REFERENCE.md
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

### User-Service

Fungsi utama:

- CRUD user profile;
- batch;
- jurusan;
- import user dari Excel;
- download template import;
- publish user event;
- status user: `ACTIVE`, `GRADUATED`, `DROP_OUT`.

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

## Dokumentasi SQL

Format SQL sudah dipisah:

- `*-ddl.sql`: schema, tabel, index, constraint;
- `*-dml.sql`: seed awal.

Lokasi:

```text
backend/auth-service/.assets/
backend/user-service/.assets/
backend/course-service/.assets/
backend/notification-service/.assets/
```

## Dokumen Lain

- [SUMMARY.md](./SUMMARY.md): ringkasan cepat.
- [CODEBASE_OVERVIEW.md](./CODEBASE_OVERVIEW.md): referensi teknis.
- [ARCHITECTURE.md](./ARCHITECTURE.md): diagram arsitektur dan flow.
- [README_DOCUMENTATION.md](./README_DOCUMENTATION.md): indeks dokumentasi.
- [DOCUMENTATION_INDEX.md](./DOCUMENTATION_INDEX.md): indeks dokumentasi lengkap.
- [NGINX_GUIDE.md](./NGINX_GUIDE.md): panduan Nginx dan cloudflared.
- [NGINX_QUICK_REFERENCE.md](./NGINX_QUICK_REFERENCE.md): referensi cepat Nginx.
