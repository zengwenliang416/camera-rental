#!/usr/bin/env bash
# Start yudao-server with local profile + XianGuanJia + Redis password.
# Prefer fat jar (fast). Fall back to mvn spring-boot:run if jar missing.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

load_env() {
  local f="$1"
  if [[ -f "$f" ]]; then
    set -a
    set -f # disable glob so cron like 0 * * * * ? is not expanded
    # shellcheck disable=SC1090
    source "$f"
    set +f
    set +a
    # strip accidental surrounding quotes from cron values
    XGJ_JOB_SHOP_CRON="${XGJ_JOB_SHOP_CRON//\"/}"
    XGJ_JOB_ORDER_CRON="${XGJ_JOB_ORDER_CRON//\"/}"
    export XGJ_JOB_SHOP_CRON XGJ_JOB_ORDER_CRON
    if [[ -n "${XGJ_JOB_REGISTER_INFRA_JOBS:-}" ]]; then
      export XGJ_JOB_REGISTER_INFRA="$XGJ_JOB_REGISTER_INFRA_JOBS"
    fi
    echo "[start-local] loaded $(basename "$f")"
  fi
}

load_env "$ROOT/.env.local"
load_env "$ROOT/.env.xianyu"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
if [[ -n "${REDIS_PASSWORD:-}" ]]; then
  export SPRING_DATA_REDIS_PASSWORD="$REDIS_PASSWORD"
  export REDIS_PASSWORD
fi

JAR="$ROOT/yudao-server/target/yudao-server.jar"
MODE="${START_MODE:-auto}" # auto | jar | mvn

echo "[start-local] profile=${SPRING_PROFILES_ACTIVE}  port=${SERVER_PORT:-48080}"
echo "[start-local] XGJ_ENABLED=${XGJ_ENABLED:-false}"
echo "[start-local] XGJ_WRITE_ENABLED=${XGJ_WRITE_ENABLED:-false}"
echo "[start-local] MySQL=ruoyi-vue-pro  Redis password set=${REDIS_PASSWORD:+yes}"

use_jar() {
  [[ -f "$JAR" ]]
}

if [[ "$MODE" == "mvn" ]] || { [[ "$MODE" == "auto" ]] && ! use_jar; }; then
  if ! use_jar && [[ "$MODE" == "auto" ]]; then
    echo "[start-local] no jar yet — packaging (skip tests)..."
    mvn -pl yudao-server -am package -DskipTests
  fi
fi

if [[ "$MODE" == "mvn" ]]; then
  echo "[start-local] mvn spring-boot:run"
  exec mvn -pl yudao-server -am org.springframework.boot:spring-boot-maven-plugin:run
fi

if use_jar; then
  echo "[start-local] java -jar $JAR"
  exec java -jar "$JAR"
fi

echo "[start-local] ERROR: jar missing and package failed" >&2
exit 1
