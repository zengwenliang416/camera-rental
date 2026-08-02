#!/usr/bin/env bash
set -euo pipefail

baseline_sha="${1:-}"
release_sha="${2:?usage: create-source-bundle.sh BASELINE_SHA RELEASE_SHA OUTPUT [REPO]}"
output="${3:?usage: create-source-bundle.sh BASELINE_SHA RELEASE_SHA OUTPUT [REPO]}"
repo="${4:-.}"

head_sha="$(git -C "${repo}" rev-parse HEAD)"
if [ "${head_sha}" != "${release_sha}" ]; then
  echo "[source-bundle][error] checkout HEAD ${head_sha} does not match ${release_sha}" >&2
  exit 1
fi

bundle_base=""
if [[ "${baseline_sha}" =~ ^[0-9a-f]{40}$ ]] \
  && git -C "${repo}" cat-file -e "${baseline_sha}^{commit}" 2>/dev/null \
  && git -C "${repo}" merge-base --is-ancestor "${baseline_sha}" "${release_sha}"; then
  bundle_base="${baseline_sha}"
  if [ "${baseline_sha}" = "${release_sha}" ]; then
    bundle_base="$(git -C "${repo}" rev-parse "${release_sha}^")"
  fi
fi

if [ -n "${bundle_base}" ]; then
  echo "[source-bundle] create incremental bundle from ${bundle_base} to ${release_sha}"
  git -C "${repo}" bundle create "${output}" "${bundle_base}..HEAD"
else
  echo "[source-bundle] create complete bundle for ${release_sha}"
  git -C "${repo}" bundle create "${output}" HEAD
fi

git -C "${repo}" bundle verify "${output}"
