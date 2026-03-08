#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/demo-common.sh"

if load_demo_state; then
  stop_pid_if_running "${NGROK_PID:-}"
  stop_pid_if_running "${APP_PID:-}"
  (
    cd "$ROOT_DIR"
    POSTGRES_PORT="${DB_PORT:-5432}" docker compose down >/dev/null 2>&1 || true
  )
  rm -f "$SESSION_FILE"
  echo "Demo session is down."
  exit 0
fi

(
  cd "$ROOT_DIR"
  docker compose down >/dev/null 2>&1 || true
)
echo "No recorded demo session was found. Local postgres was stopped if it existed."
