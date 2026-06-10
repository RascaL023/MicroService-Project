#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN_DIR="$ROOT_DIR/.assets/run"
LOG_DIR="$RUN_DIR/logs"
COMPOSE_FILE="$ROOT_DIR/api-gateway/docker-compose.yml"
NGINX_CONF="${DEV_TOGGLE_NGINX_CONF:-$ROOT_DIR/reverse-proxy/nginx.conf}"
NGINX_PREFIX="$RUN_DIR/nginx"
NGINX_PID_FILE="$RUN_DIR/proxy.pid"

mkdir -p "$RUN_DIR" "$LOG_DIR" "$NGINX_PREFIX/logs"

TARGETS=(compose auth user course notification frontend proxy)
LOCAL_TARGETS=(auth user course notification frontend)

usage() {
  cat <<'EOF'
Usage:
  ./.assets/scripts/toggle.sh <command> [target]

Commands:
  up|start [target]       Start target. Default target: all
  down|stop [target]      Stop target. Default target: all
  restart [target]        Restart target. Default target: all
  toggle [target]         Start target if stopped, stop it if running. Default target: all
  status [target]         Show status. Default target: all
  logs <target>           Tail logs for target

Targets:
  all                     compose + auth + user + course + notification + frontend + proxy
  compose|gateway|infra   api-gateway/docker-compose.yml
  auth                    backend/auth-service
  user                    backend/user-service
  course                  backend/course-service
  notification            backend/notification-service
  frontend                frontend
  proxy|nginx             nginx reverse proxy on :9000

Examples:
  ./.assets/scripts/toggle.sh toggle compose
  ./.assets/scripts/toggle.sh up all
  ./.assets/scripts/toggle.sh status
  ./.assets/scripts/toggle.sh logs user
  ./.assets/scripts/toggle.sh toggle proxy

Docker:
  Compose commands use sudo docker compose by default.
  Set DEV_TOGGLE_DOCKER_SUDO=0 to run docker without sudo.

Nginx:
  Proxy uses sudo nginx by default.
  Set DEV_TOGGLE_NGINX_CONF=/path/to/nginx.conf to override config.
  Set DEV_TOGGLE_NGINX_SUDO=0 to run nginx without sudo.
EOF
}

die() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

info() {
  printf '[dev-toggle] %s\n' "$*"
}

has_command() {
  command -v "$1" >/dev/null 2>&1
}

port_listening() {
  local port="$1"

  if has_command ss; then
    ss -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "(:|\\])$port$"
  elif has_command lsof; then
    lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
  else
    return 1
  fi
}

docker_compose_cmd() {
  local sudo_prefix=""

  if [[ "${DEV_TOGGLE_DOCKER_SUDO:-1}" != "0" && "$(id -u)" != "0" ]]; then
    has_command sudo || die "sudo tidak ditemukan, padahal Docker diset memakai sudo"
    sudo_prefix="sudo "
  fi

  if ${sudo_prefix}docker compose version >/dev/null 2>&1; then
    printf '%sdocker compose' "$sudo_prefix"
  elif ${sudo_prefix}docker-compose version >/dev/null 2>&1; then
    printf '%sdocker-compose' "$sudo_prefix"
  else
    die "docker compose/docker-compose tidak ditemukan atau tidak bisa diakses"
  fi
}

normalize_target() {
  case "${1:-all}" in
    all) printf 'all' ;;
    compose|gateway|infra|docker) printf 'compose' ;;
    auth|auth-service) printf 'auth' ;;
    user|user-service) printf 'user' ;;
    course|course-service) printf 'course' ;;
    notification|notification-service|notif) printf 'notification' ;;
    frontend|front|web) printf 'frontend' ;;
    proxy|nginx|reverse-proxy) printf 'proxy' ;;
    *) die "target tidak dikenal: $1" ;;
  esac
}

pid_file() {
  printf '%s/%s.pid' "$RUN_DIR" "$1"
}

log_file() {
  printf '%s/%s.log' "$LOG_DIR" "$1"
}

is_pid_running() {
  local pid="${1:-}"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

target_pid() {
  local file
  file="$(pid_file "$1")"
  if [[ -f "$file" ]]; then
    sed -n '1p' "$file"
  fi
  return 0
}

is_local_running() {
  local pid
  pid="$(target_pid "$1")"
  is_pid_running "$pid"
}

target_workdir() {
  case "$1" in
    auth) printf '%s/backend/auth-service' "$ROOT_DIR" ;;
    user) printf '%s/backend/user-service' "$ROOT_DIR" ;;
    course) printf '%s/backend/course-service' "$ROOT_DIR" ;;
    notification) printf '%s/backend/notification-service' "$ROOT_DIR" ;;
    frontend) printf '%s/frontend' "$ROOT_DIR" ;;
    *) die "target lokal tidak dikenal: $1" ;;
  esac
}

target_command() {
  case "$1" in
    auth) printf './cmd/bin/main' ;;
    user) printf './mvnw spring-boot:run -DskipTests' ;;
    course) printf './mvnw spring-boot:run -DskipTests' ;;
    notification) printf './mvnw spring-boot:run -DskipTests' ;;
    frontend) printf 'npm run dev -- --host 0.0.0.0' ;;
    *) die "command target lokal tidak dikenal: $1" ;;
  esac
}

preflight_local() {
  local target="$1"
  local workdir
  workdir="$(target_workdir "$target")"

  [[ -d "$workdir" ]] || die "folder tidak ditemukan: $workdir"

  case "$target" in
    auth)
      has_command go || die "go tidak ditemukan"
      [[ -f "$workdir/go.mod" ]] || die "go.mod tidak ditemukan di $workdir"
      ;;
    course|user|notification)
      [[ -x "$workdir/mvnw" ]] || die "mvnw tidak executable/tidak ditemukan di $workdir"
      find "$workdir/src/main/java" -type f -name '*.java' -print -quit 2>/dev/null | grep -q . ||
        die "source Java belum ditemukan di $workdir/src/main/java"
      ;;
    frontend)
      has_command npm || die "npm tidak ditemukan"
      [[ -f "$workdir/package.json" ]] || die "package.json tidak ditemukan di $workdir"
      ;;
  esac
}

is_local_available() {
  local target="$1"
  local workdir
  workdir="$(target_workdir "$target")"

  [[ -d "$workdir" ]] || return 1

  case "$target" in
    auth)
      has_command go && [[ -f "$workdir/go.mod" ]]
      ;;
    course|user|notification)
      [[ -x "$workdir/mvnw" ]] && find "$workdir/src/main/java" -type f -name '*.java' -print -quit 2>/dev/null | grep -q .
      ;;
    frontend)
      has_command npm && [[ -f "$workdir/package.json" ]]
      ;;
    *)
      return 1
      ;;
  esac
}

compose_up() {
  [[ -f "$COMPOSE_FILE" ]] || die "compose file tidak ditemukan: $COMPOSE_FILE"
  local dc
  dc="$(docker_compose_cmd)"
  info "starting compose stack"
  (cd "$(dirname "$COMPOSE_FILE")" && $dc up -d)
}

compose_down() {
  [[ -f "$COMPOSE_FILE" ]] || die "compose file tidak ditemukan: $COMPOSE_FILE"
  local dc
  dc="$(docker_compose_cmd)"
  info "stopping compose stack"
  (cd "$(dirname "$COMPOSE_FILE")" && $dc down)
}

compose_status() {
  [[ -f "$COMPOSE_FILE" ]] || die "compose file tidak ditemukan: $COMPOSE_FILE"
  local dc
  dc="$(docker_compose_cmd)"
  (cd "$(dirname "$COMPOSE_FILE")" && $dc ps)
}

compose_logs() {
  [[ -f "$COMPOSE_FILE" ]] || die "compose file tidak ditemukan: $COMPOSE_FILE"
  local dc
  dc="$(docker_compose_cmd)"
  (cd "$(dirname "$COMPOSE_FILE")" && $dc logs -f)
}

start_local() {
  local target="$1"
  local workdir command pidfile logfile

  if is_local_running "$target"; then
    info "$target sudah running (pid $(target_pid "$target"))"
    return 0
  fi

  preflight_local "$target"
  workdir="$(target_workdir "$target")"
  command="$(target_command "$target")"
  pidfile="$(pid_file "$target")"
  logfile="$(log_file "$target")"

  info "starting $target"
  info "log: $logfile"

  (
    cd "$workdir"
    exec bash -lc "$command"
  ) >"$logfile" 2>&1 &

  printf '%s\n' "$!" >"$pidfile"
  sleep 0.5

  if is_local_running "$target"; then
    info "$target running (pid $(target_pid "$target"))"
  else
    rm -f "$pidfile"
    die "$target gagal start, cek log: $logfile"
  fi
}

stop_local() {
  local target="$1"
  local pid pidfile
  pidfile="$(pid_file "$target")"
  pid="$(target_pid "$target")"

  if ! is_pid_running "$pid"; then
    rm -f "$pidfile"
    info "$target tidak running"
    return 0
  fi

  info "stopping $target (pid $pid)"
  kill "$pid" >/dev/null 2>&1 || true

  for _ in {1..20}; do
    if ! is_pid_running "$pid"; then
      rm -f "$pidfile"
      info "$target stopped"
      return 0
    fi
    sleep 0.25
  done

  info "$target belum berhenti, kirim SIGKILL"
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$pidfile"
}

status_local() {
  local target="$1"
  local pid
  pid="$(target_pid "$target")"
  if is_pid_running "$pid"; then
    printf '%-14s running pid=%s log=%s\n' "$target" "$pid" "$(log_file "$target")"
  else
    printf '%-14s stopped log=%s\n' "$target" "$(log_file "$target")"
  fi
}

logs_local() {
  local target="$1"
  local logfile
  logfile="$(log_file "$target")"
  [[ -f "$logfile" ]] || die "log belum ada untuk $target: $logfile"
  tail -f "$logfile"
}

nginx_cmd() {
  local sudo_prefix=""

  has_command nginx || die "nginx tidak ditemukan"
  if [[ "${DEV_TOGGLE_NGINX_SUDO:-1}" != "0" && "$(id -u)" != "0" ]]; then
    has_command sudo || die "sudo tidak ditemukan, padahal nginx diset memakai sudo"
    sudo_prefix="sudo "
  fi

  printf '%snginx' "$sudo_prefix"
}

is_proxy_running() {
  local pid=""
  [[ -f "$NGINX_PID_FILE" ]] && pid="$(sed -n '1p' "$NGINX_PID_FILE")"
  is_pid_running "$pid"
}

start_proxy() {
  [[ -f "$NGINX_CONF" ]] || die "nginx config tidak ditemukan: $NGINX_CONF"
  if is_proxy_running; then
    info "proxy sudah running (pid $(sed -n '1p' "$NGINX_PID_FILE"))"
    return 0
  fi
  if port_listening 9000; then
    die "port 9000 sudah dipakai proses luar; hentikan proses itu dulu, lalu start proxy lewat script agar managed"
  fi

  local nginx logfile
  nginx="$(nginx_cmd)"
  logfile="$(log_file proxy)"

  info "starting proxy with nginx config: $NGINX_CONF"
  info "log: $logfile"

  (
    exec $nginx -p "$NGINX_PREFIX" -c "$NGINX_CONF" \
      -g "daemon off; pid $NGINX_PREFIX/logs/nginx.pid; error_log $LOG_DIR/proxy-error.log;"
  ) >"$logfile" 2>&1 &

  printf '%s\n' "$!" >"$NGINX_PID_FILE"
  sleep 0.5

  if is_proxy_running; then
    info "proxy running (pid $(sed -n '1p' "$NGINX_PID_FILE"))"
  else
    rm -f "$NGINX_PID_FILE"
    die "proxy gagal start atau pid file tidak dibuat: $NGINX_PID_FILE"
  fi
}

stop_proxy() {
  if ! is_proxy_running; then
    rm -f "$NGINX_PID_FILE"
    info "proxy tidak running"
    return 0
  fi

  local pid
  pid="$(sed -n '1p' "$NGINX_PID_FILE")"
  info "stopping proxy (pid $pid)"
  kill "$pid" >/dev/null 2>&1 || true

  for _ in {1..20}; do
    if ! is_pid_running "$pid"; then
      rm -f "$NGINX_PID_FILE"
      info "proxy stopped"
      return 0
    fi
    sleep 0.25
  done

  info "proxy belum berhenti, kirim SIGKILL"
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$NGINX_PID_FILE"
}

status_proxy() {
  if is_proxy_running; then
    printf '%-14s running pid=%s log=%s config=%s\n' "proxy" "$(sed -n '1p' "$NGINX_PID_FILE")" "$(log_file proxy)" "$NGINX_CONF"
  else
    printf '%-14s stopped log=%s config=%s\n' "proxy" "$(log_file proxy)" "$NGINX_CONF"
  fi
}

logs_proxy() {
  local logfile
  logfile="$(log_file proxy)"
  [[ -f "$logfile" ]] || die "log proxy belum ada: $logfile"
  tail -f "$logfile"
}

for_each_target() {
  local action="$1"
  local target="$2"

  if [[ "$target" == "all" ]]; then
    for item in "${TARGETS[@]}"; do
      "$action" "$item"
    done
  else
    "$action" "$target"
  fi
}

start_target() {
  case "$1" in
    compose) compose_up ;;
    course|auth|user|notification|frontend) start_local "$1" ;;
    proxy) start_proxy ;;
    all)
      compose_up
      for item in "${LOCAL_TARGETS[@]}"; do
        if is_local_available "$item"; then
          start_local "$item"
        else
          info "skip $item: belum runnable atau tool belum tersedia"
        fi
      done
      start_proxy
      ;;
    *) die "target start tidak dikenal: $1" ;;
  esac
}

stop_target() {
  case "$1" in
    compose) compose_down ;;
    course|auth|user|notification|frontend) stop_local "$1" ;;
    proxy) stop_proxy ;;
    all)
      for item in proxy frontend notification course user auth compose; do
        stop_target "$item"
      done
      ;;
    *) die "target stop tidak dikenal: $1" ;;
  esac
}

status_target() {
  case "$1" in
    compose) compose_status ;;
    course|auth|user|notification|frontend) status_local "$1" ;;
    proxy) status_proxy ;;
    all)
      status_local auth
      status_local user
      status_local course
      status_local notification
      status_local frontend
      status_proxy
      compose_status
      ;;
    *) die "target status tidak dikenal: $1" ;;
  esac
}

logs_target() {
  case "$1" in
    compose) compose_logs ;;
    course|auth|user|notification|frontend) logs_local "$1" ;;
    proxy) logs_proxy ;;
    all) die "logs perlu target spesifik: compose/auth/user/course/notification/frontend/proxy" ;;
    *) die "target logs tidak dikenal: $1" ;;
  esac
}

toggle_target() {
  case "$1" in
    all)
      local any_running="false"
      for item in "${LOCAL_TARGETS[@]}"; do
        if is_local_running "$item"; then
          any_running="true"
          break
        fi
      done
      if [[ "$any_running" == "true" ]]; then
        stop_target all
      else
        start_target all
      fi
      ;;
    compose)
      local dc
      dc="$(docker_compose_cmd)"
      if (cd "$(dirname "$COMPOSE_FILE")" && $dc ps --status running --services | grep -q .); then
        compose_down
      else
        compose_up
      fi
      ;;
    proxy)
      if is_proxy_running; then
        stop_proxy
      else
        start_proxy
      fi
      ;;
    course|auth|user|notification|frontend)
      if is_local_running "$1"; then
        stop_local "$1"
      else
        start_local "$1"
      fi
      ;;
    *) die "target toggle tidak dikenal: $1" ;;
  esac
}

main() {
  local command="${1:-help}"
  local target
  target="$(normalize_target "${2:-all}")"

  case "$command" in
    up|start) start_target "$target" ;;
    down|stop) stop_target "$target" ;;
    restart)
      stop_target "$target"
      start_target "$target"
      ;;
    toggle) toggle_target "$target" ;;
    status|ps) status_target "$target" ;;
    logs|log) logs_target "$target" ;;
    help|-h|--help) usage ;;
    *) usage; die "command tidak dikenal: $command" ;;
  esac
}

main "$@"
