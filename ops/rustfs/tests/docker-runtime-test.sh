#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
runtime_script="${repo_root}/ops/rustfs/docker-runtime.sh"
test_dir="$(mktemp -d /tmp/camera-rental-rustfs-runtime.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

mock_bin="${test_dir}/bin"
mock_log="${test_dir}/commands.log"
mkdir -p "${mock_bin}"

cat > "${mock_bin}/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'docker %s\n' "$*" >> "${RUSTFS_TEST_LOG}"
case "${1:-}" in
  info) exit 0 ;;
  --version) echo "Docker version test" ;;
  compose)
    if [ "${2:-}" = "version" ]; then
      echo "Docker Compose version test"
    fi
    ;;
esac
EOF
chmod +x "${mock_bin}/docker"

PATH="${mock_bin}:/usr/bin:/bin" \
RUSTFS_TEST_LOG="${mock_log}" \
RUSTFS_SKIP_DOCKER_SERVICE_START=true \
bash -c '
  set -euo pipefail
  source "$1"
  rustfs_ensure_docker
  rustfs_compose pull example/image
' bash "${runtime_script}"

grep -Fq "docker info" "${mock_log}"
grep -Fq "docker compose version" "${mock_log}"
grep -Fq "docker compose pull example/image" "${mock_log}"

compose_marker="${test_dir}/compose-installed"
cat > "${mock_bin}/docker" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'docker %s\n' "$*" >> "${RUSTFS_TEST_LOG}"
case "${1:-}" in
  info) exit 0 ;;
  --version) echo "Docker version test" ;;
  compose)
    if [ ! -f "${RUSTFS_TEST_COMPOSE_MARKER}" ]; then
      exit 1
    fi
    if [ "${2:-}" = "version" ]; then
      echo "Docker Compose version installed by test"
    fi
    ;;
esac
EOF
cat > "${mock_bin}/apt-get" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf 'apt-get %s\n' "$*" >> "${RUSTFS_TEST_LOG}"
case "$*" in
  *docker-compose-v2*)
    touch "${RUSTFS_TEST_COMPOSE_MARKER}"
    ;;
esac
EOF
chmod +x "${mock_bin}/docker" "${mock_bin}/apt-get"
: > "${mock_log}"

PATH="${mock_bin}:/usr/bin:/bin" \
RUSTFS_TEST_LOG="${mock_log}" \
RUSTFS_TEST_COMPOSE_MARKER="${compose_marker}" \
RUSTFS_SKIP_DOCKER_SERVICE_START=true \
bash -c '
  set -euo pipefail
  source "$1"
  rustfs_ensure_docker
' bash "${runtime_script}"

test -f "${compose_marker}"
grep -Fq "apt-get update" "${mock_log}"
grep -Fq "apt-get install -y docker-compose-v2" "${mock_log}"

echo "RustFS Docker runtime tests passed"
