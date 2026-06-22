# Indeks Dokumentasi Proyek

File ini menjadi pintu masuk untuk membaca dokumentasi project.

## File Dokumentasi

| File | Isi |
|---|---|
| `README.md` | Panduan utama project, cara jalan, arsitektur, dan catatan SSOT |
| `SUMMARY.md` | Ringkasan cepat untuk memahami sistem |
| `CODEBASE_OVERVIEW.md` | Referensi teknis per service, endpoint, dan integrasi |
| `ARCHITECTURE.md` | Diagram arsitektur dan flow data |

## Rekomendasi Baca

Kalau baru masuk ke project:

1. baca `SUMMARY.md`;
2. lanjut ke `README.md`;
3. buka `ARCHITECTURE.md` untuk melihat bentuk sistem;
4. pakai `CODEBASE_OVERVIEW.md` saat mulai mengubah service tertentu.

Kalau mau mengerjakan backend:

1. baca bagian service terkait di `CODEBASE_OVERVIEW.md`;
2. cek SQL di folder `.assets` service;
3. cek authority yang dipakai endpoint;
4. cek event Redis jika perubahan menyentuh data lintas service.

Kalau mau mengerjakan frontend:

1. pahami pembagian `/app/*` dan `/admin/*`;
2. cek helper session dan authority;
3. pastikan tombol/menu mengikuti authority;
4. tetap anggap backend sebagai validasi final.

Kalau mau mengerjakan deployment/infrastruktur:

1. baca bagian SSOT di `README.md`;
2. cek `global/public-config.yml`;
3. cek `api-gateway/docker-compose.yml`;
4. cek `api-gateway/kong/kong.yml`;
5. pastikan port, host, Redis key, dan route tidak drift.

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
├── README.md
├── SUMMARY.md
├── CODEBASE_OVERVIEW.md
├── ARCHITECTURE.md
└── README_DOCUMENTATION.md
```

## Ringkasan Service

| Service | Bahasa | Peran |
|---|---|---|
| Auth-Service | Go | Auth, session, role, authority |
| User-Service | Java | Profil user, batch, jurusan, import |
| Course-Service | Java | Akademik, group, jadwal, nilai, laporan |
| Notification-Service | Java | Email aktivasi dan reset password |
| Frontend | TypeScript | Portal web |
| API Gateway | Kong | Routing API dan session injector |

## Catatan SSOT

Konfigurasi belum sepenuhnya punya satu sumber kebenaran. `global/public-config.yml` sudah menjadi pusat konfigurasi aplikasi, tetapi Docker dan Kong masih menyimpan beberapa nilai sendiri.

Yang perlu dicek saat mengubah konfigurasi:

- port service di `global/public-config.yml`;
- upstream URL di `api-gateway/kong/kong.yml`;
- Redis config di plugin Kong;
- body size dan mode network di `api-gateway/docker-compose.yml`;
- env frontend bila base URL gateway diatur dari env.

Sasaran idealnya: Docker Compose, Kong, backend, dan frontend mengambil nilai dari sumber env/config yang sama.

## Pertanyaan Umum

**Bagaimana service saling komunikasi?**

REST untuk request langsung, Redis Streams untuk event async.

**Di mana session disimpan?**

Session disimpan di Redis. Frontend hanya menyimpan session id dan data ringkas di `localStorage`.

**Apakah validasi frontend cukup?**

Tidak. Frontend hanya mengatur UX. Backend tetap wajib memvalidasi authority.

**Service mana yang menyimpan audit log?**

Saat ini audit domain-level sederhana disimpan di `course-service`.

**Apakah notification-service punya database?**

Tidak. Service ini memakai Redis Stream dan SMTP.

## Update

Tanggal dokumentasi: 22 Juni 2026.
