#!/usr/bin/env bash
# Start yudao-server with local profile + Redis password.
# Prefer an existing fat jar for speed; START_MODE=mvn always rebuilds current sources.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

load_env() {
  local f="$1"
  if [[ -f "$f" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$f"
    set +a
    echo "[start-local] loaded $(basename "$f")"
  fi
}

load_env "$ROOT/.env.local"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
if [[ -z "${MYBATIS_PLUS_ENCRYPTOR_PASSWORD:-}" ]] && command -v security >/dev/null 2>&1; then
  MYBATIS_PLUS_ENCRYPTOR_PASSWORD="$(
    security find-generic-password \
      -a "${USER:-$(id -un)}" \
      -s camera-rental-mybatis-encryptor \
      -w 2>/dev/null || true
  )"
  export MYBATIS_PLUS_ENCRYPTOR_PASSWORD
fi
if [[ -z "${MYBATIS_PLUS_ENCRYPTOR_PASSWORD:-}" ]]; then
  echo "[start-local] ERROR: MYBATIS_PLUS_ENCRYPTOR_PASSWORD is required." >&2
  echo "[start-local] Set it in .env.local or macOS Keychain service camera-rental-mybatis-encryptor." >&2
  exit 1
fi
if [[ -n "${REDIS_PASSWORD:-}" ]]; then
  export SPRING_DATA_REDIS_PASSWORD="$REDIS_PASSWORD"
  export REDIS_PASSWORD
fi

JAR="$ROOT/yudao-server/target/yudao-server.jar"
MODE="${START_MODE:-auto}" # auto | jar | mvn

echo "[start-local] profile=${SPRING_PROFILES_ACTIVE}  port=${SERVER_PORT:-48080}"
echo "[start-local] MySQL=ruoyi-vue-pro  Redis password set=${REDIS_PASSWORD:+yes}"

use_jar() {
  [[ -f "$JAR" ]]
}

if [[ "$MODE" == "mvn" ]]; then
  echo "[start-local] packaging current sources (skip tests)..."
  mvn -pl yudao-server -am package -DskipTests
elif [[ "$MODE" == "auto" ]] && ! use_jar; then
  echo "[start-local] no jar yet — packaging (skip tests)..."
  mvn -pl yudao-server -am package -DskipTests
fi

if use_jar; then
  echo "[start-local] java -jar $JAR"
  exec java -jar "$JAR"
fi

echo "[start-local] ERROR: jar missing and package failed" >&2
exit 1
