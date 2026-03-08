#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/demo-common.sh"

ensure_demo_dir
require_docker
require_command ngrok
require_command curl
load_app_env

if [[ -f "$SESSION_FILE" ]]; then
  "$ROOT_DIR/scripts/demo-down.sh" >/dev/null 2>&1 || true
fi

DB_PORT="$(choose_port 5432 5433)" || fail "Both postgres ports 5432 and 5433 are busy"
APP_PORT="$(choose_port 8080 8081)" || fail "Both app ports 8080 and 8081 are busy"
release_ngrok_web_port

echo "Starting local postgres on port $DB_PORT..."
(
  cd "$ROOT_DIR"
  POSTGRES_PORT="$DB_PORT" docker compose up -d postgres
)
wait_for_postgres || fail "Postgres did not become healthy. Check docker logs for $POSTGRES_CONTAINER_NAME."

echo "Building runnable application jar..."
: >"$APP_LOG"
(
  cd "$ROOT_DIR"
  ./gradlew bootJar >>"$APP_LOG" 2>&1
)
APP_JAR="$(
  find "$ROOT_DIR/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1
)"
[[ -n "$APP_JAR" ]] || fail "Could not find runnable jar under build/libs"

echo "Starting application on port $APP_PORT..."
nohup env \
  SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:$DB_PORT/club_sla" \
  SERVER_PORT="$APP_PORT" \
  java -jar "$APP_JAR" >"$APP_LOG" 2>&1 < /dev/null &
APP_PID=$!

if ! wait_for_app_health "$APP_PORT"; then
  stop_pid_if_running "$APP_PID"
  tail -n 80 "$APP_LOG" || true
  fail "Application health check did not turn UP"
fi
APP_PID="$(lsof -ti "tcp:$APP_PORT" 2>/dev/null | head -n 1 || echo "$APP_PID")"

echo "Starting ngrok tunnel for port $APP_PORT..."
: >"$NGROK_LOG"
nohup sh -c "exec ngrok http '$APP_PORT'" >"$NGROK_LOG" 2>&1 < /dev/null &
NGROK_PID=$!

PUBLIC_URL="$(wait_for_ngrok_url)" || {
  stop_pid_if_running "$NGROK_PID"
  stop_pid_if_running "$APP_PID"
  fail "ngrok did not publish a public URL"
}
NGROK_PID="$(lsof -ti tcp:4040 2>/dev/null | head -n 1 || echo "$NGROK_PID")"
PUBLIC_HOST="${PUBLIC_URL#https://}"
STARTED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
write_demo_state "$APP_PID" "$NGROK_PID" "$APP_PORT" "$DB_PORT" "$PUBLIC_URL" "$PUBLIC_HOST" "$STARTED_AT"

cat <<EOF
Demo session is up.

Home URL: $PUBLIC_URL/
Ops URL: http://localhost:$APP_PORT/ops/login
Local Health: http://localhost:$APP_PORT/api/health
Ngrok Host: $PUBLIC_HOST

GitHub App URLs:
Homepage URL: $PUBLIC_URL/
Callback URL: $PUBLIC_URL/auth/github/callback
Setup URL: $PUBLIC_URL/app/installations/setup
Webhook URL: $PUBLIC_URL/api/webhooks/github

Next steps:
1. Update the GitHub App URLs above.
2. Open $PUBLIC_URL/
3. Install the app, save the Discord webhook, then move a draft PR to Ready for review.

Shutdown:
./scripts/demo-down.sh
EOF
