#!/usr/bin/env bash
set -euo pipefail

: "${GITHUB_TOKEN:?GITHUB_TOKEN is required}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${GITHUB_RUN_ID:?GITHUB_RUN_ID is required}"

RELEASE_SHA="${RELEASE_SHA:-manual-$(date +%Y%m%d%H%M%S)}"
ARTIFACT_NAMES="${ARTIFACT_NAMES:-server admin staff web}"
API_URL="https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}/artifacts?per_page=100"

workdir="$(mktemp -d /tmp/camera-rental-artifacts.XXXXXX)"
artifact_index="${workdir}/artifacts.json"
release_dir="${workdir}/release"
release_archive="/tmp/camera-rental-${RELEASE_SHA}.tgz"

cleanup() {
  rm -rf "${workdir}"
}
trap cleanup EXIT

mkdir -p "${release_dir}"

echo "[deploy] fetch artifact index for run ${GITHUB_RUN_ID}"
curl -fsSL \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "${API_URL}" \
  -o "${artifact_index}"

for artifact_name in ${ARTIFACT_NAMES}; do
  artifact_url="$(python3 - "${artifact_index}" "${artifact_name}" <<'PY'
import json
import sys

index_path, wanted = sys.argv[1], sys.argv[2]
with open(index_path, "r", encoding="utf-8") as f:
    payload = json.load(f)

for artifact in payload.get("artifacts", []):
    if artifact.get("name") == wanted:
        print(artifact["archive_download_url"])
        break
else:
    raise SystemExit(f"artifact not found: {wanted}")
PY
)"

  echo "[deploy] download artifact ${artifact_name}"
  curl -fL --retry 5 --retry-delay 3 \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "${artifact_url}" \
    -o "${workdir}/${artifact_name}.zip"

  mkdir -p "${release_dir}/${artifact_name}"
  unzip -q "${workdir}/${artifact_name}.zip" -d "${release_dir}/${artifact_name}"
done

echo "[deploy] pack release archive ${release_archive}"
rm -f "${release_archive}"
tar -czf "${release_archive}" -C "${release_dir}" .

unset GITHUB_TOKEN
exec bash /tmp/camera-rental-server-deploy.sh "${release_archive}"
