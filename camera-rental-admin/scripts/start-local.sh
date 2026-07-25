#!/usr/bin/env bash
# Start admin Vite dev server (mode env.local → .env.env.local)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f "$ROOT/.env.env.local" ]]; then
  if [[ -f "$ROOT/.env.env.local.example" ]]; then
    cp "$ROOT/.env.env.local.example" "$ROOT/.env.env.local"
    echo "[start-local] created .env.env.local from example"
  else
    echo "[start-local] missing .env.env.local — create it first" >&2
    exit 1
  fi
fi

if [[ ! -d node_modules ]]; then
  echo "[start-local] pnpm install..."
  pnpm install
fi

export VITE_PORT="${VITE_PORT:-5173}"
export VITE_BASE_URL="${VITE_BASE_URL:-http://127.0.0.1:48080}"

echo "[start-local] http://127.0.0.1:${VITE_PORT}  →  API ${VITE_BASE_URL}/admin-api"
exec pnpm dev -- --port "$VITE_PORT" --strictPort
