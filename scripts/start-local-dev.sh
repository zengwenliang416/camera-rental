#!/usr/bin/env bash
# Start backend + admin in two background jobs (logs under /tmp).
# Usage:
#   ./scripts/start-local-dev.sh          # both
#   ./scripts/start-local-dev.sh server   # backend only
#   ./scripts/start-local-dev.sh admin    # admin only
#   ./scripts/start-local-dev.sh setup    # DB bootstrap only
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVER="$ROOT/camera-rental-server"
ADMIN="$ROOT/camera-rental-admin"
MODE="${1:-both}"

case "$MODE" in
  setup)
    exec "$SERVER/scripts/setup-local.sh"
    ;;
  server)
    exec "$SERVER/scripts/start-local.sh"
    ;;
  admin)
    exec "$ADMIN/scripts/start-local.sh"
    ;;
  both)
    echo "[dev] bootstrapping DB if needed..."
    "$SERVER/scripts/setup-local.sh" || true

    SERVER_LOG="/tmp/camera-rental-server.log"
    ADMIN_LOG="/tmp/camera-rental-admin.log"

    echo "[dev] starting backend → $SERVER_LOG"
    nohup "$SERVER/scripts/start-local.sh" >"$SERVER_LOG" 2>&1 &
    echo $! > /tmp/camera-rental-server.pid
    echo "[dev] server pid=$(cat /tmp/camera-rental-server.pid)"

    echo "[dev] starting admin → $ADMIN_LOG"
    nohup "$ADMIN/scripts/start-local.sh" >"$ADMIN_LOG" 2>&1 &
    echo $! > /tmp/camera-rental-admin.pid
    echo "[dev] admin pid=$(cat /tmp/camera-rental-admin.pid)"

    echo
    echo "  Backend:  http://127.0.0.1:${SERVER_PORT:-48080}  (log: $SERVER_LOG)"
    echo "  Admin:    http://127.0.0.1:${VITE_PORT:-5173}   (log: $ADMIN_LOG)"
    echo "  Login:    admin / admin123  tenant: 芋道源码"
    echo "  Rental:   http://127.0.0.1:${VITE_PORT:-5173}/rental/xianyu"
    echo
    echo "  Stop: kill \$(cat /tmp/camera-rental-server.pid /tmp/camera-rental-admin.pid)"
    echo "  Tail: tail -f $SERVER_LOG"
    ;;
  *)
    echo "usage: $0 [both|server|admin|setup]" >&2
    exit 1
    ;;
esac
