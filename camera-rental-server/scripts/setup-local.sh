#!/usr/bin/env bash
# One-shot local bootstrap: MySQL DB + base SQL + rental migrations + env files.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ruoyi-vue-pro}"
MYSQL_CLI="${MYSQL_CLI:-$(command -v mysql 2>/dev/null || true)}"

if [[ -f "$ROOT/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env.local"
  set +a
fi

if [[ -z "$MYSQL_CLI" && -x /usr/local/mysql/bin/mysql ]]; then
  MYSQL_CLI=/usr/local/mysql/bin/mysql
fi
if [[ -z "$MYSQL_CLI" ]]; then
  echo "[setup-local] ERROR: mysql client not found. Set MYSQL_CLI to its absolute path." >&2
  exit 1
fi

mysql_cmd() {
  if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
    MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_CLI" --default-character-set=utf8mb4 \
      -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" "$@"
  else
    "$MYSQL_CLI" --default-character-set=utf8mb4 \
      -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" "$@"
  fi
}

run_sql_file() {
  local file="$1"
  local allow_already_applied="${2:-false}"
  local error_file
  error_file="$(mktemp "${TMPDIR:-/tmp}/camera-rental-sql.XXXXXX")"
  if mysql_cmd "$MYSQL_DATABASE" < "$file" 2>"$error_file"; then
    rm -f "$error_file"
    return 0
  fi
  if [[ "$allow_already_applied" == "true" ]]; then
    local mysql_errors unexpected_errors
    mysql_errors="$(grep -Ei '^ERROR [0-9]+' "$error_file" || true)"
    unexpected_errors="$(
      printf '%s\n' "$mysql_errors" |
        grep -Evi 'already exists|Duplicate column|Duplicate key name' || true
    )"
    if [[ -n "$mysql_errors" && -z "$unexpected_errors" ]]; then
      echo "     (already applied, ok)"
      rm -f "$error_file"
      return 0
    fi
  fi
  echo "[setup-local] ERROR: SQL failed: $file" >&2
  cat "$error_file" >&2
  rm -f "$error_file"
  return 1
}

file_sha256() {
  if command -v sha256sum &>/dev/null; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

echo "[setup-local] checking MySQL ${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT} ..."
if ! mysql_cmd -e "SELECT 1" &>/dev/null; then
  echo "[setup-local] ERROR: cannot connect to MySQL. Fix MYSQL_* in .env.local" >&2
  exit 1
fi
echo "[setup-local] MySQL OK"

# Redis probe (optional password)
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
if command -v redis-cli &>/dev/null; then
  if [[ -n "$REDIS_PASSWORD" ]]; then
    if redis-cli -a "$REDIS_PASSWORD" --no-auth-warning ping 2>/dev/null | grep -q PONG; then
      echo "[setup-local] Redis OK (auth)"
    else
      echo "[setup-local] WARNING: Redis auth failed with REDIS_PASSWORD — check .env.local" >&2
    fi
  else
    if redis-cli ping 2>/dev/null | grep -q PONG; then
      echo "[setup-local] Redis OK (no auth)"
    else
      echo "[setup-local] WARNING: Redis needs a password. Set REDIS_PASSWORD in .env.local" >&2
    fi
  fi
else
  echo "[setup-local] WARNING: redis-cli not found"
fi

# Ensure env files
if [[ ! -f "$ROOT/.env.local" ]]; then
  cp "$ROOT/.env.local.example" "$ROOT/.env.local"
  echo "[setup-local] created .env.local from example — edit passwords if needed"
fi
echo "[setup-local] ensuring database \`${MYSQL_DATABASE}\` ..."
mysql_cmd -e "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

TABLE_COUNT="$(mysql_cmd -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}'" 2>/dev/null || echo 0)"
echo "[setup-local] current tables: ${TABLE_COUNT}"

if [[ "${TABLE_COUNT}" -lt 10 ]]; then
  BASE_SQL="$ROOT/sql/mysql/ruoyi-vue-pro.sql"
  QUARTZ_SQL="$ROOT/sql/mysql/quartz.sql"
  if [[ ! -f "$BASE_SQL" ]]; then
    echo "[setup-local] ERROR: missing $BASE_SQL" >&2
    exit 1
  fi
  echo "[setup-local] importing base schema (may take ~30s) ..."
  mysql_cmd "$MYSQL_DATABASE" < "$BASE_SQL"
  if [[ -f "$QUARTZ_SQL" ]]; then
    echo "[setup-local] importing quartz.sql ..."
    run_sql_file "$QUARTZ_SQL" true
  fi
  echo "[setup-local] base import done"
else
  echo "[setup-local] base schema already present — skip full import"
fi

MIG_DIR="$ROOT/sql/mysql/migrations"
if [[ -d "$MIG_DIR" ]]; then
  mysql_cmd "$MYSQL_DATABASE" -e "
    CREATE TABLE IF NOT EXISTS camera_rental_schema_migration (
      version varchar(128) NOT NULL,
      checksum char(64) NOT NULL,
      applied_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  "
  echo "[setup-local] applying rental migrations ..."
  # shellcheck disable=SC2012
  for f in $(ls "$MIG_DIR"/*.sql 2>/dev/null | sort); do
    name="$(basename "$f")"
    checksum="$(file_sha256 "$f")"
    escaped_name="${name//\'/\'\'}"
    applied_checksum="$(
      mysql_cmd "$MYSQL_DATABASE" -N -B -e \
        "SELECT checksum FROM camera_rental_schema_migration WHERE version = '${escaped_name}'" \
        2>/dev/null || true
    )"
    if [[ -n "$applied_checksum" ]]; then
      if [[ "$applied_checksum" != "$checksum" ]]; then
        echo "[setup-local] ERROR: applied migration changed: $name" >&2
        echo "[setup-local] expected checksum: $applied_checksum" >&2
        echo "[setup-local] current checksum:  $checksum" >&2
        exit 1
      fi
      echo "  -> $name (already recorded, skip)"
      continue
    fi
    echo "  -> $name"
    # First adoption of an existing local DB may encounter previously applied objects.
    run_sql_file "$f" true
    mysql_cmd "$MYSQL_DATABASE" -e "
      INSERT INTO camera_rental_schema_migration (version, checksum)
      VALUES ('${escaped_name}', '${checksum}');
    "
  done
fi

# Smoke: rental tables
echo "[setup-local] rental-related tables:"
mysql_cmd -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}' AND table_name LIKE 'rental_%' OR (table_schema='${MYSQL_DATABASE}' AND table_name LIKE 'xianyu_%') ORDER BY 1;" 2>/dev/null | sed 's/^/  /' || true

echo
echo "[setup-local] done."
echo "  Start backend:  $ROOT/scripts/start-local.sh"
echo "  Start admin:    (workspace)/camera-rental-admin/scripts/start-local.sh"
echo "  Or both:        (workspace)/scripts/start-local-dev.sh"
