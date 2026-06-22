# Gambaran Arsitektur

## Topologi Utama

```text
┌──────────────────────────────────────────────────────────────┐
│              Browser lokal / Cloudflared testing             │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               v
┌──────────────────────────────────────────────────────────────┐
│                         Nginx :9000                          │
│  /api/* -> Kong :8000, route lain -> Vite :5173/build        │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               │ HTTP REST
                               │ Authorization: Session {id}
                               v
┌──────────────────────────────────────────────────────────────┐
│                    Kong API Gateway                          │
│  Routing API, plugin session injector, validasi session Redis│
└───────────────┬─────────────────────┬────────────────────────┘
                │                     │
                v                     v
┌────────────────────────┐  ┌────────────────────────┐
│ Auth-Service            │  │ User-Service            │
│ Go                      │  │ Java Spring Boot         │
│ Login, role, authority  │  │ Profil, batch, jurusan   │
│ DB: PostgreSQL          │  │ DB: PostgreSQL           │
│ Session: Redis          │  │ Event: Redis Streams     │
└────────────┬───────────┘  └────────────┬───────────┘
             │                           │
             │                           │ user:events
             │                           v
             │              ┌────────────────────────┐
             │              │ Course-Service          │
             │              │ Java Spring Boot         │
             │              │ Subject, group, nilai    │
             │              │ DB: PostgreSQL           │
             │              │ Cache: course_user_cache │
             │              └────────────┬───────────┘
             │                           │
             │ notification:emails       │
             v                           v
┌────────────────────────┐      ┌────────────────────────┐
│ Notification-Service    │      │ Redis                  │
│ Java Spring Boot        │      │ Session, stream, token │
│ SMTP email              │      │ cache ringan           │
└────────────────────────┘      └────────────────────────┘
```

## Flow Login

```text
Frontend
-> POST /api/auths/login
-> Auth-Service validasi email/password
-> Auth-Service ambil role dan authority
-> Auth-Service simpan session ke Redis
-> Frontend simpan session ke localStorage
-> Request berikutnya membawa Authorization: Session {sessionId}
-> Kong plugin baca session dari Redis
-> Kong inject X-User-Id, X-User-Roles, X-User-Authorities
-> Backend service validasi authority
```

## Flow Event User

```text
User-Service
-> publish UserCreated/UserProfileUpdated/UserDeleted/UserStatusChanged
-> Redis Stream user:events
-> Course-Service update course_user_cache
-> Auth-Service bisa invalidasi session untuk event tertentu
```

## Flow Assessment dan Nilai

```text
Instruktur buka group
-> buat assessment
-> learner mark done jika assessment offline sudah dikerjakan
-> instruktur input nilai
-> course-service simpan assessment_grades
-> audit log ditulis
-> gradebook dan laporan membaca data nilai
```

## Lapisan Keamanan

```text
1. Frontend
   - hide/show menu dan tombol berdasarkan authority
   - bukan security final

2. Kong
   - route request
   - baca session dari Redis
   - inject konteks user ke backend

3. Backend
   - validasi authority endpoint
   - validasi business rule
   - tulis audit log untuk aksi penting

4. Data
   - soft delete
   - session TTL
   - password bcrypt
   - token aktivasi/reset dengan TTL
```

## Catatan SSOT

SSOT konfigurasi belum sempurna karena Docker, Kong, dan Nginx masih membawa konfigurasi sendiri.

```text
global/public-config.yml
  -> dipakai backend untuk port, database, Redis, stream, URL auth

api-gateway/docker-compose.yml
  -> punya env Kong, volume, Redis container, body size, network mode

api-gateway/kong/kong.yml
  -> punya route, upstream service URL, Redis host/port plugin

reverse-proxy/nginx.dev.conf / nginx.static.conf
  -> punya port :9000, route /api/*, rate limit, dan target frontend
```

Risiko utamanya adalah konfigurasi drift. Contohnya, port service bisa benar di global config, tapi Kong masih menunjuk ke port lama, atau cloudflared masih mengarah ke port Nginx lama. Saat ada perubahan port, Redis prefix, atau route, cek file-file itu sekaligus.
