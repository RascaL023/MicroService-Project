# Indeks Dokumentasi Lengkap

Dokumen ini membantu memilih file dokumentasi yang tepat.

## File Dokumentasi

| File | Fokus |
|---|---|
| `README.md` | Panduan utama project, arsitektur, script dev, Nginx, SQL |
| `SUMMARY.md` | Ringkasan cepat sistem |
| `CODEBASE_OVERVIEW.md` | Referensi teknis service, endpoint, integrasi |
| `ARCHITECTURE.md` | Diagram dan flow arsitektur |
| `README_DOCUMENTATION.md` | Indeks dokumentasi singkat |
| `NGINX_GUIDE.md` | Panduan Nginx, rate limit, cloudflared, script dev |
| `NGINX_QUICK_REFERENCE.md` | Cheat sheet Nginx dan script dev |

## Rekomendasi Baca

Setup project di Linux:

1. baca `README.md`;
2. jika ingin artifact terbaru, jalankan `./.assets/scripts/toggle.sh build all`;
3. jalankan `./.assets/scripts/toggle.sh up all`;
4. buka `http://localhost:9000`;
5. cek log dengan `./.assets/scripts/toggle.sh logs <target>`.

Menjalankan Nginx:

1. baca `NGINX_QUICK_REFERENCE.md`;
2. gunakan `./.assets/scripts/toggle.sh up proxy`;
3. jangan jalankan `sudo nginx -c ...` manual kecuali sedang debug config.

Testing online dengan cloudflared:

1. jalankan stack lokal dengan `./.assets/scripts/toggle.sh up all`;
2. pastikan `http://localhost:9000` hidup;
3. jalankan `cloudflared tunnel run lms-dev`;
4. arahkan tunnel ke `http://localhost:9000`.

Mengubah rate limit:

1. buka `reverse-proxy/nginx.dev.conf` atau `reverse-proxy/nginx.static.conf`;
2. ubah `limit_req_zone` atau `limit_req`;
3. jalankan `./.assets/scripts/toggle.sh restart proxy`.

## Port

| Komponen | Port |
|---|---|
| Nginx reverse proxy | `9000` |
| Kong proxy | `8000` |
| Kong admin | `8001` |
| Auth-service | sesuai `global/public-config.yml`, saat ini `8081` |
| User-service | sesuai `global/public-config.yml`, saat ini `8082` |
| Notification-service | sesuai `global/public-config.yml`, saat ini `8083` |
| Course-service | sesuai `global/public-config.yml`, saat ini `8084` |
| Frontend dev server | `5173` |
| Redis | `6379` |
| PostgreSQL | sesuai config, saat ini `5555` di `global/public-config.yml` |

## Path Penting

```text
.assets/scripts/toggle.sh
.assets/scripts/dev-toggle.sh
reverse-proxy/nginx.dev.conf
reverse-proxy/nginx.static.conf
api-gateway/docker-compose.yml
api-gateway/kong/kong.yml
global/public-config.yml
global/private-config.yml
.assets/run/logs/
```

## Catatan SSOT

SSOT konfigurasi belum sempurna. Backend membaca `global/public-config.yml` dan `global/private-config.yml`, tetapi Docker Compose, Kong, dan Nginx masih menyimpan sebagian konfigurasi sendiri.

Saat mengubah host, port, Redis prefix, route, atau domain testing, cek:

- `global/public-config.yml`;
- `api-gateway/docker-compose.yml`;
- `api-gateway/kong/kong.yml`;
- `reverse-proxy/nginx.dev.conf`;
- `reverse-proxy/nginx.static.conf`;
- config cloudflared lokal.

## Checklist Development

- [ ] PostgreSQL dan Redis siap.
- [ ] Custom library Java sudah di-install jika dibutuhkan.
- [ ] `./.assets/scripts/toggle.sh up all` berhasil.
- [ ] `http://localhost:9000` bisa dibuka.
- [ ] `./.assets/scripts/toggle.sh status` menunjukkan service yang dibutuhkan running.
- [ ] Log dicek dari `.assets/run/logs/` atau `toggle.sh logs`.
