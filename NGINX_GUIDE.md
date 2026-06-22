# Panduan Nginx dan Cloudflared

Nginx di project ini dipakai sebagai reverse proxy lokal untuk development/testing. Cloudflared boleh diarahkan ke Nginx agar aplikasi lokal bisa diakses online sementara.

## Arsitektur

```text
Browser lokal
atau
Cloudflare Tunnel
        |
        v
Nginx :9000
        |
        +-- /api/* -> Kong :8000 -> backend services
        |
        +-- /*     -> Vite :5173 pada mode dev
                 atau frontend/build pada mode static
```

Nginx menangani:

- satu origin untuk frontend dan API;
- rate limiting dasar di `/api/*`;
- proxy header ke upstream;
- WebSocket untuk Vite HMR;
- static serving untuk hasil build frontend;
- log akses lokal.

## File

```text
reverse-proxy/nginx.dev.conf
reverse-proxy/nginx.static.conf
reverse-proxy/mime.types
.assets/scripts/toggle.sh
.assets/scripts/dev-toggle.sh
```

`toggle.sh` hanya wrapper ke `dev-toggle.sh`.

## Konfigurasi Development

`reverse-proxy/nginx.dev.conf`:

```text
/api/* -> http://127.0.0.1:8000
/*     -> http://127.0.0.1:5173
```

Mode ini cocok untuk development harian karena Vite HMR tetap jalan.

Jalankan:

```bash
./.assets/scripts/toggle.sh up all
```

Cek:

```bash
./.assets/scripts/toggle.sh status
curl http://localhost:9000
curl http://localhost:9000/api/auths
```

## Konfigurasi Static

`reverse-proxy/nginx.static.conf`:

```text
/api/* -> http://127.0.0.1:8000
/*     -> /home/rascal/!Project!/frontend/build
```

Mode ini cocok untuk testing build frontend tanpa Vite.

Jalankan:

```bash
./.assets/scripts/toggle.sh build frontend
DEV_TOGGLE_RUN_MODE=build DEV_TOGGLE_PROXY_MODE=static ./.assets/scripts/toggle.sh up all
```

## Script Dev

Untuk Linux, cara paling enak menjalankan project adalah dari script:

```bash
./.assets/scripts/toggle.sh up all
```

Script ini mengatur:

- Docker Compose untuk Kong dan Redis;
- auth-service;
- user-service;
- course-service;
- notification-service;
- frontend dev server;
- Nginx reverse proxy.

Perintah umum:

```bash
./.assets/scripts/toggle.sh status
./.assets/scripts/toggle.sh restart all
./.assets/scripts/toggle.sh down all
./.assets/scripts/toggle.sh logs proxy
./.assets/scripts/toggle.sh logs compose
./.assets/scripts/toggle.sh logs course
```

Target yang tersedia:

```text
all
compose
auth
user
course
notification
frontend
proxy
```

Env yang berguna:

```bash
DEV_TOGGLE_PROXY_MODE=dev
DEV_TOGGLE_PROXY_MODE=static
DEV_TOGGLE_RUN_MODE=source
DEV_TOGGLE_RUN_MODE=build
DEV_TOGGLE_NGINX_CONF=/path/to/nginx.conf
DEV_TOGGLE_NGINX_SUDO=0
DEV_TOGGLE_DOCKER_SUDO=0
```

Default:

- Docker memakai `sudo docker compose`;
- Nginx memakai `sudo nginx`;
- proxy mode adalah `dev`;
- port Nginx adalah `9000`.

## Rate Limiting

Konfigurasi saat ini:

```nginx
limit_req_status 429;
limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=10r/s;
```

Di `/api/*`:

```nginx
limit_req zone=api_per_ip burst=20 nodelay;
```

Artinya:

- limit dihitung per IP;
- rata-rata `10 request/detik`;
- burst sampai `20`;
- jika melebihi limit, response `429`.

Ini rate limiting sederhana di layer pertama yang disentuh client. Kong tetap bisa punya policy sendiri nanti, tapi untuk testing lokal Nginx sudah cukup.

## Upload Limit

Nginx membatasi request body:

```nginx
client_max_body_size 10m;
```

Kalau upload frontend/backend butuh lebih besar, ubah di:

```text
reverse-proxy/nginx.dev.conf
reverse-proxy/nginx.static.conf
```

Setelah itu restart proxy:

```bash
./.assets/scripts/toggle.sh restart proxy
```

## Cloudflared

Cloudflared dipakai untuk expose `localhost:9000` ke domain testing, misalnya `dev.rascal.my.id`.

Contoh config:

```yaml
tunnel: lms-dev
credentials-file: /home/rascal/.cloudflared/<tunnel-id>.json

ingress:
  - hostname: dev.rascal.my.id
    service: http://localhost:9000
  - service: http_status:404
```

Urutan jalan:

```bash
./.assets/scripts/toggle.sh up all
cloudflared tunnel run lms-dev
```

Alurnya:

```text
Internet
-> Cloudflare
-> cloudflared lokal
-> localhost:9000
-> Nginx
-> Kong/frontend
```

Catatan penting:

- ini untuk testing online, bukan deployment final;
- jangan expose config sensitif;
- pastikan SMTP/activation URL mengikuti domain yang sedang dipakai;
- kalau pakai mode static, build frontend dulu.

## Log dan Status

Status:

```bash
./.assets/scripts/toggle.sh status
```

Log proxy:

```bash
./.assets/scripts/toggle.sh logs proxy
```

File log:

```text
.assets/run/logs/proxy-access.log
.assets/run/logs/proxy-error.log
.assets/run/logs/proxy.log
```

Log service lain:

```bash
./.assets/scripts/toggle.sh logs auth
./.assets/scripts/toggle.sh logs user
./.assets/scripts/toggle.sh logs course
./.assets/scripts/toggle.sh logs notification
./.assets/scripts/toggle.sh logs frontend
./.assets/scripts/toggle.sh logs compose
```

## Troubleshooting

Port `9000` sudah dipakai:

```bash
lsof -i :9000
./.assets/scripts/toggle.sh status proxy
```

Nginx butuh password sudo:

```bash
sudo -v
./.assets/scripts/toggle.sh restart proxy
```

Kong belum jalan:

```bash
./.assets/scripts/toggle.sh status compose
./.assets/scripts/toggle.sh logs compose
```

Frontend tidak tampil pada mode dev:

```bash
./.assets/scripts/toggle.sh status frontend
./.assets/scripts/toggle.sh logs frontend
```

Frontend tidak tampil pada mode static:

```bash
./.assets/scripts/toggle.sh build frontend
DEV_TOGGLE_PROXY_MODE=static ./.assets/scripts/toggle.sh restart proxy
```

Sering dapat `429`:

- rate limit Nginx sedang bekerja;
- naikkan `rate=10r/s` atau `burst=20` jika perlu untuk testing;
- setelah edit config, restart proxy lewat script.
