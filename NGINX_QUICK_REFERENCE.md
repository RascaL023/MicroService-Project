# Referensi Cepat Nginx

Dokumen ini menjelaskan setup Nginx yang dipakai di project ini sekarang. Nginx dipakai sebagai reverse proxy lokal di port `9000`, sekaligus titik yang bisa diekspos oleh `cloudflared` untuk testing online.

## Fungsi Nginx

Nginx di project ini bukan pengganti Kong. Alurnya:

```text
Browser / Cloudflared
-> Nginx :9000
-> /api/* diteruskan ke Kong :8000
-> route frontend diteruskan ke Vite :5173 atau frontend/build
```

Yang ditangani Nginx:

- satu entry point lokal: `http://localhost:9000`;
- mengurangi masalah CORS karena frontend dan API terlihat satu origin;
- rate limiting dasar untuk `/api/*`;
- proxy header `X-Real-IP`, `X-Forwarded-For`, dan `X-Forwarded-Proto`;
- WebSocket untuk Vite HMR pada mode development;
- static serving untuk `frontend/build` pada mode static.

## File Konfigurasi

| File | Fungsi |
|---|---|
| `reverse-proxy/nginx.dev.conf` | `/api/*` ke Kong, route lain ke Vite dev server |
| `reverse-proxy/nginx.static.conf` | `/api/*` ke Kong, route lain ke `frontend/build` |
| `reverse-proxy/mime.types` | MIME type untuk static file |

Konfigurasi penting yang aktif:

```nginx
listen 9000;
client_max_body_size 10m;
limit_req_status 429;
limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=10r/s;
```

Untuk API:

```nginx
location /api/ {
    limit_req zone=api_per_ip burst=20 nodelay;
    proxy_pass http://127.0.0.1:8000;
}
```

## Cara Jalan Paling Enak di Linux

Gunakan script dev:

```bash
./.assets/scripts/toggle.sh up all
```

Perintah yang sering dipakai:

```bash
./.assets/scripts/toggle.sh status
./.assets/scripts/toggle.sh logs proxy
./.assets/scripts/toggle.sh logs user
./.assets/scripts/toggle.sh restart proxy
./.assets/scripts/toggle.sh down all
```

Alias target:

```text
compose      -> Kong + Redis dari api-gateway/docker-compose.yml
auth         -> backend/auth-service
user         -> backend/user-service
course       -> backend/course-service
notification -> backend/notification-service
frontend     -> Vite dev server
proxy        -> Nginx :9000
all          -> semua target di atas
```

Default proxy mode adalah `dev`, artinya Nginx meneruskan frontend ke Vite `:5173`.

Untuk mode static:

```bash
./.assets/scripts/toggle.sh build frontend
DEV_TOGGLE_RUN_MODE=build DEV_TOGGLE_PROXY_MODE=static ./.assets/scripts/toggle.sh up all
```

Pada mode static, script tidak menyalakan frontend dev server karena Nginx langsung melayani `frontend/build`.

## Cloudflared

`cloudflared` dipakai hanya untuk testing agar aplikasi lokal bisa diakses dari internet.

Target tunnel:

```yaml
ingress:
  - hostname: dev.rascal.my.id
    service: http://localhost:9000
  - service: http_status:404
```

Jalankan stack lokal dulu:

```bash
./.assets/scripts/toggle.sh up all
```

Lalu jalankan tunnel:

```bash
cloudflared tunnel run lms-dev
```

Alurnya:

```text
https://dev.rascal.my.id
-> Cloudflare Tunnel
-> localhost:9000
-> Nginx
-> Kong/Vite/static frontend
```

Catatan: ini untuk exposure testing/dev, bukan pola produksi final.

## Log

Log Nginx diarahkan ke:

```text
.assets/run/logs/proxy-access.log
.assets/run/logs/proxy-error.log
.assets/run/logs/proxy.log
```

Melihat log:

```bash
./.assets/scripts/toggle.sh logs proxy
tail -f .assets/run/logs/proxy-access.log
```

## Troubleshooting Cepat

Port `9000` sudah dipakai:

```bash
lsof -i :9000
./.assets/scripts/toggle.sh status proxy
./.assets/scripts/toggle.sh restart proxy
```

Nginx butuh sudo:

```bash
sudo -v
./.assets/scripts/toggle.sh restart proxy
```

Kalau environment tidak perlu sudo untuk Nginx:

```bash
DEV_TOGGLE_NGINX_SUDO=0 ./.assets/scripts/toggle.sh up proxy
```

Kena `429`:

- rate limit API aktif;
- default `10r/s` dengan burst `20`;
- ubah `limit_req_zone` dan reload proxy kalau butuh.

API `502` atau tidak nyambung:

```bash
./.assets/scripts/toggle.sh status compose
curl http://localhost:8000
./.assets/scripts/toggle.sh logs compose
```
