# Ringkasan Codebase Proyek

## Fakta Cepat

| Item | Keterangan |
|---|---|
| Jenis project | Sistem manajemen pembelajaran |
| Arsitektur | Microservices dengan API Gateway |
| Backend | 4 service |
| Frontend | 1 aplikasi SvelteKit |
| Bahasa | Go, Java, TypeScript |
| Database | PostgreSQL |
| Cache/Event | Redis dan Redis Streams |
| Auth utama | Session Redis dengan RBAC |
| Entry point lokal | Nginx `http://localhost:9000` |

## Service Utama

### Auth-Service

Peran: pusat autentikasi dan otorisasi.

Fitur:

- login dan logout;
- aktivasi akun via email;
- reset password;
- role dan authority;
- status akun auth seperti `ACTIVE` dan `BANNED`;
- session Redis;
- last login.

### User-Service

Peran: data profil administratif user.

Fitur:

- CRUD user profile;
- batch;
- jurusan;
- import Excel;
- download template import;
- status user course: `ACTIVE`, `GRADUATED`, `DROP_OUT`;
- publish event user.

### Course-Service

Peran: pengelolaan akademik.

Fitur:

- subject;
- group;
- enrollment;
- schedule dan template waktu;
- meeting;
- assessment;
- acknowledgement learner;
- gradebook;
- laporan nilai mingguan;
- audit log.

### Notification-Service

Peran: pengiriman email.

Fitur:

- konsumsi job dari Redis Stream `notification:emails`;
- kirim email aktivasi;
- kirim email reset password;
- tidak punya database relasional sendiri.

## Frontend

Frontend memakai SvelteKit dan dibagi menjadi:

- `/app/*` untuk portal pengguna;
- `/admin/*` untuk portal pengelola.

UI membaca session dari `localStorage`. Menu, halaman, dan tombol aksi ditampilkan berdasarkan authority yang dimiliki user. Ini hanya validasi tampilan. Backend tetap menjadi guard final.

Contoh:

```text
group.read       -> boleh lihat group
group.update     -> boleh edit group
group.delete     -> boleh hapus group
group.*          -> boleh semua aksi group
```

## Integrasi Antar Service

### REST

REST dipakai untuk request langsung:

- frontend ke backend melalui Kong;
- course-service melakukan lookup user ketika dibutuhkan;
- admin/user melakukan CRUD data.

### Redis Streams

Redis Streams dipakai untuk event async:

| Stream | Publisher | Consumer |
|---|---|---|
| `user:events` | user-service | course-service, auth-service |
| `notification:emails` | auth-service | notification-service |

Contoh:

```text
user-service update profile
-> publish UserProfileUpdated
-> course-service update course_user_cache
```

## Model Data Ringkas

Auth-service:

- `users`
- `roles`
- `authorities`
- `users_roles`
- `authorities_roles`

User-service:

- `users`
- `batches`
- `majors`

Course-service:

- `subjects`
- `groups`
- `enrollments`
- `group_schedules`
- `schedule_templates`
- `group_meetings`
- `assessments`
- `assessment_grades`
- `assessment_acknowledgements`
- `subject_materials`
- `subject_modules`
- `audit_logs`
- `course_user_cache`

## Flow Penting

### Login

```text
User input email/password
-> auth-service validasi password
-> auth-service buat session Redis
-> frontend simpan session
-> request berikutnya lewat Kong
-> Kong inject konteks user
-> backend validasi authority
```

### Reset Password

```text
User input email
-> auth-service buat reset token
-> auth-service publish email job
-> notification-service kirim email
-> user buka link reset
-> auth-service hash password baru
-> session lama dicabut
-> user login ulang
```

### Enrollment

```text
Admin/instruktur pilih group dan user
-> course-service cek status user dari cache/lookup
-> DROP_OUT tidak boleh ditambahkan ke group
-> GRADUATED tidak boleh ditambahkan sebagai learner
-> enrollment disimpan
```

## SSOT Konfigurasi

SSOT belum sempurna karena sebagian konfigurasi masih hidup di Docker/Kong/Nginx.

Sumber konfigurasi saat ini:

- `global/public-config.yml` dan `global/private-config.yml`;
- `api-gateway/docker-compose.yml`;
- `api-gateway/kong/kong.yml`;
- `reverse-proxy/nginx.dev.conf` dan `reverse-proxy/nginx.static.conf`;
- env frontend jika dipakai.

Risikonya: port, Redis key, route, dan URL service bisa beda antar file. Untuk local development masih bisa diterima, tapi untuk deployment yang lebih serius sebaiknya semua nilai utama ditarik dari satu sumber env/config yang sama.

Cloudflared untuk testing online diarahkan ke `http://localhost:9000`, sehingga Nginx menjadi entry point sebelum request masuk ke Kong atau frontend.

## Keamanan

Poin utama:

- password di-hash dengan bcrypt;
- session disimpan di Redis dengan TTL;
- authority dicek di backend;
- token aktivasi/reset bersifat sementara;
- soft delete dipakai untuk menjaga data historis;
- audit log disimpan di course-service untuk aksi domain tertentu.

## Mulai Cepat

```bash
./.assets/scripts/toggle.sh up all
```

```bash
./.assets/scripts/toggle.sh status
```

```bash
./.assets/scripts/toggle.sh down all
```

Akses aplikasi dari `http://localhost:9000`. Untuk testing online, jalankan `cloudflared tunnel run lms-dev` setelah stack lokal hidup.

## Dokumen Terkait

- `README.md`: panduan utama.
- `CODEBASE_OVERVIEW.md`: detail teknis service, endpoint, dan integrasi.
- `ARCHITECTURE.md`: diagram arsitektur.
- `README_DOCUMENTATION.md`: indeks dokumentasi.
