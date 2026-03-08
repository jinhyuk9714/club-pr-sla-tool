#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/demo-common.sh"

if ! load_demo_state; then
  echo "No active demo session state found."
  exit 1
fi

APP_STATUS="stopped"
if kill -0 "${APP_PID:-0}" >/dev/null 2>&1; then
  APP_STATUS="running"
fi

NGROK_STATUS="stopped"
if kill -0 "${NGROK_PID:-0}" >/dev/null 2>&1; then
  NGROK_STATUS="running"
fi

POSTGRES_STATUS="$(
  docker ps --filter "name=^/${POSTGRES_CONTAINER_NAME}$" --format '{{.Status}}' | head -n 1
)"
if [[ -z "$POSTGRES_STATUS" ]]; then
  POSTGRES_STATUS="stopped"
fi

HEALTH="unavailable"
if [[ "$APP_STATUS" == "running" ]]; then
  HEALTH="$(curl -fsS "http://localhost:$APP_PORT/api/health" 2>/dev/null || echo unavailable)"
fi

cat <<EOF
Demo status
Started at: $STARTED_AT
App port: $APP_PORT ($APP_STATUS, pid=$APP_PID)
DB port: $DB_PORT ($POSTGRES_STATUS)
Ngrok: $PUBLIC_URL ($NGROK_STATUS, pid=$NGROK_PID)
Health: $HEALTH
App log: $APP_LOG
Ngrok log: $NGROK_LOG
EOF
