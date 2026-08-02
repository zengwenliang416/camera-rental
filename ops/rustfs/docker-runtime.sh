#!/usr/bin/env bash

rustfs_compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi
  echo "[rustfs][error] Docker Compose is unavailable" >&2
  return 1
}

rustfs_install_compose_apt() {
  if DEBIAN_FRONTEND=noninteractive apt-get install -y docker-compose-v2; then
    return
  fi
  if DEBIAN_FRONTEND=noninteractive apt-get install -y docker-compose-plugin; then
    return
  fi
  DEBIAN_FRONTEND=noninteractive apt-get install -y docker-compose
}

rustfs_install_docker_apt() {
  echo "[rustfs] install Docker with apt"
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io
  rustfs_install_compose_apt
}

rustfs_install_docker() {
  if command -v apt-get >/dev/null 2>&1; then
    rustfs_install_docker_apt
    return
  fi
  if command -v dnf >/dev/null 2>&1; then
    echo "[rustfs] install Docker with dnf"
    dnf install -y docker docker-compose-plugin
    return
  fi
  if command -v yum >/dev/null 2>&1; then
    echo "[rustfs] install Docker with yum"
    yum install -y docker docker-compose-plugin
    return
  fi
  echo "[rustfs][error] no supported package manager found for Docker installation" >&2
  return 1
}

rustfs_install_compose() {
  if command -v apt-get >/dev/null 2>&1; then
    echo "[rustfs] install Docker Compose with apt"
    apt-get update
    rustfs_install_compose_apt
    return
  fi
  if command -v dnf >/dev/null 2>&1; then
    echo "[rustfs] install Docker Compose with dnf"
    dnf install -y docker-compose-plugin
    return
  fi
  if command -v yum >/dev/null 2>&1; then
    echo "[rustfs] install Docker Compose with yum"
    yum install -y docker-compose-plugin
    return
  fi
  echo "[rustfs][error] no supported package manager found for Compose installation" >&2
  return 1
}

rustfs_ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    rustfs_install_docker
  fi

  if ! docker compose version >/dev/null 2>&1 \
    && ! command -v docker-compose >/dev/null 2>&1; then
    rustfs_install_compose
  fi

  if command -v systemctl >/dev/null 2>&1 \
    && [ "${RUSTFS_SKIP_DOCKER_SERVICE_START:-false}" != "true" ]; then
    systemctl enable --now docker
  fi

  if ! docker info >/dev/null 2>&1; then
    echo "[rustfs][error] Docker daemon is unavailable" >&2
    return 1
  fi
  if ! docker compose version >/dev/null 2>&1 \
    && ! command -v docker-compose >/dev/null 2>&1; then
    echo "[rustfs][error] Docker is installed but Compose is unavailable" >&2
    return 1
  fi

  echo "[rustfs] Docker runtime is ready: $(docker --version)"
  rustfs_compose version
}
