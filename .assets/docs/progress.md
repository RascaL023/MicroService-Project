# Terakhir Dikerjakan

## Frontend

- UI dan animasi dibuat lebih rapi.
- Toast dibuat lebih stabil dan tidak ikut posisi parent layout.
- Guard login/logout diperbaiki.

## Keamanan

- Kong punya request audit sederhana via stdout.
- Reverse proxy disiapkan untuk rate limiting dasar.
- Nginx dipakai sebagai entry point lokal `:9000` dan target cloudflared untuk testing online.

## Course-Service

- Error query log/audit diperbaiki.
- Gradebook dan call frontend dirapikan agar tidak terlalu boros.

## Tertunda

- User-service: fitur foto profil user, bingung taruh gambar dimana :v
- Keamanan: load balancer di API Gateway masih ditunda, ribet urus instance :v

## Catatan

- Respon deleted user pada member group perlu diputuskan: ditandai deleted atau tetap tampil sebagai data historis.
- SSOT konfigurasi belum sempurna karena Docker/Kong masih menyimpan konfigurasi sendiri di luar `global/public-config.yml`.
- Cara jalan paling nyaman di Linux adalah lewat `./.assets/scripts/toggle.sh up all`.
