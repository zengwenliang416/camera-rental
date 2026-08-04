#!/usr/bin/env bash
set -euo pipefail

deploy_root="${1:?usage: remote-deploy.sh DEPLOY_ROOT RELEASE_SHA SOURCE_BUNDLE}"
release_sha="${2:?usage: remote-deploy.sh DEPLOY_ROOT RELEASE_SHA SOURCE_BUNDLE}"
source_bundle="${3:?usage: remote-deploy.sh DEPLOY_ROOT RELEASE_SHA SOURCE_BUNDLE}"

if [[ ! "${release_sha}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "[woodpecker-remote][error] invalid release SHA" >&2
  exit 1
fi
test -s "${source_bundle}"

cleanup() {
  rm -f \
    "${source_bundle}" \
    /tmp/remote-deploy.sh \
    /tmp/server-build-deploy.sh \
    /tmp/incremental-build-lib.sh \
    /tmp/deployment-runtime-lib.sh \
    /tmp/prepare-production-80.sh
}
trap cleanup EXIT

echo "[woodpecker-remote] source bundle received for ${release_sha}"
export DEPLOY_ROOT="${deploy_root}"
export RELEASE_SHA="${release_sha}"
export REPO_URL="${source_bundle}"
export SOURCE_DIR="${deploy_root}/source"

bash /tmp/prepare-production-80.sh
bash /tmp/server-build-deploy.sh
