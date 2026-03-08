#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEMO_DIR="$ROOT_DIR/.demo"
SESSION_FILE="$DEMO_DIR/session.env"
APP_LOG="$DEMO_DIR/app.log"
NGROK_LOG="$DEMO_DIR/ngrok.log"
POSTGRES_CONTAINER_NAME="club-sla-postgres"

fail() {
  echo "Error: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

port_in_use() {
  lsof -ti "tcp:$1" >/dev/null 2>&1
}

choose_port() {
  local primary="$1"
  local fallback="$2"
  if ! port_in_use "$primary"; then
    echo "$primary"
    return 0
  fi
  if ! port_in_use "$fallback"; then
    echo "$fallback"
    return 0
  fi
  return 1
}

ensure_demo_dir() {
  mkdir -p "$DEMO_DIR"
}

load_demo_state() {
  [[ -f "$SESSION_FILE" ]] || return 1
  set -a
  source "$SESSION_FILE"
  set +a
}

write_demo_state() {
  ensure_demo_dir
  cat >"$SESSION_FILE" <<EOF
APP_PID=$1
NGROK_PID=$2
APP_PORT=$3
DB_PORT=$4
PUBLIC_URL=$5
PUBLIC_HOST=$6
STARTED_AT=$7
EOF
}

load_app_env() {
  local env_file="$ROOT_DIR/.env"
  [[ -f "$env_file" ]] || fail "Missing .env file at $env_file"
  set -a
  source "$env_file"
  set +a

  local required_vars=(
    SPRING_DATASOURCE_USERNAME
    SPRING_DATASOURCE_PASSWORD
    GITHUB_WEBHOOK_SECRET
    GITHUB_APP_APP_ID
    GITHUB_APP_PRIVATE_KEY
    GITHUB_APP_CLIENT_ID
    GITHUB_APP_CLIENT_SECRET
    GITHUB_APP_INSTALL_URL
    APP_SECURITY_ENCRYPTION_SECRET
    OPS_ADMIN_API_KEY
    BETA_ALLOWED_GITHUB_LOGINS
  )

  local missing=()
  local key
  for key in "${required_vars[@]}"; do
    if [[ -z "${!key:-}" ]]; then
      missing+=("$key")
    fi
  done

  if [[ "${#missing[@]}" -gt 0 ]]; then
    fail "Missing required .env values: ${missing[*]}"
  fi
}

require_docker() {
  require_command docker
  docker info >/dev/null 2>&1 || fail "Docker daemon is not running"
}

wait_for_postgres() {
  local attempts=30
  local status=""
  while (( attempts > 0 )); do
    status="$(
      docker inspect \
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
        "$POSTGRES_CONTAINER_NAME" 2>/dev/null || true
    )"
    if [[ "$status" == "healthy" || "$status" == "running" ]]; then
      return 0
    fi
    attempts=$((attempts - 1))
    sleep 2
  done
  return 1
}

wait_for_app_health() {
  local app_port="$1"
  local attempts=60
  local health_url="http://localhost:$app_port/api/health"
  while (( attempts > 0 )); do
    if curl -fsS "$health_url" | grep -q '"status":"UP".*"database":"UP".*"migrations":"UP"'; then
      return 0
    fi
    attempts=$((attempts - 1))
    sleep 2
  done
  return 1
}

wait_for_ngrok_url() {
  local attempts=30
  local public_url=""
  while (( attempts > 0 )); do
    public_url="$(
      curl -fsS http://127.0.0.1:4040/api/tunnels 2>/dev/null \
        | grep -o 'https://[^"]*' \
        | head -n 1 || true
    )"
    if [[ -n "$public_url" ]]; then
      echo "$public_url"
      return 0
    fi
    attempts=$((attempts - 1))
    sleep 2
  done
  return 1
}

stop_pid_if_running() {
  local pid="$1"
  if [[ -n "${pid:-}" ]] && kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    sleep 2
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  fi
}

release_ngrok_web_port() {
  local pid=""
  local command_name=""
  pid="$(lsof -ti "tcp:4040" 2>/dev/null | head -n 1 || true)"
  [[ -n "$pid" ]] || return 0

  command_name="$(ps -p "$pid" -o comm= 2>/dev/null || true)"
  if [[ "$command_name" == *ngrok* ]]; then
    stop_pid_if_running "$pid"
    sleep 1
    return 0
  fi

  fail "Port 4040 is already in use by pid $pid ($command_name). Stop that process first."
}
