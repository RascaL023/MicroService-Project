# Terakhir Dikerjakan

## Frontend

- UI dan animasi dibuat lebih rapi.
- Toast dibuat lebih stabil dan tidak ikut posisi parent layout.
- Guard login/logout diperbaiki.

## Keamanan

- Kong punya request audit sederhana via stdout.
- Reverse proxy disiapkan untuk rate limiting dasar.

## Course-Service

- Error query log/audit diperbaiki.
- Gradebook dan call frontend dirapikan agar tidak terlalu boros.

## Tertunda

- User-service: fitur foto profil user.
- Keamanan: load balancer di API Gateway masih ditunda.

## Catatan

- Respon deleted user pada member group perlu diputuskan: ditandai deleted atau tetap tampil sebagai data historis.
- SSOT konfigurasi belum sempurna karena Docker/Kong masih menyimpan konfigurasi sendiri di luar `global/public-config.yml`.
