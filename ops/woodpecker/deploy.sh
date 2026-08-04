#!/usr/bin/env bash
set -euo pipefail

expected_repo="${EXPECTED_REPO:-zengwenliang416/camera-rental}"
release_sha="${CI_COMMIT_SHA:?CI_COMMIT_SHA is required}"
deploy_host="${DEPLOY_HOST:?DEPLOY_HOST is required}"
deploy_port="${DEPLOY_PORT:?DEPLOY_PORT is required}"
deploy_user="${DEPLOY_USER:?DEPLOY_USER is required}"
deploy_root="${DEPLOY_ROOT:?DEPLOY_ROOT is required}"
private_key="${DEPLOY_SSH_PRIVATE_KEY:?DEPLOY_SSH_PRIVATE_KEY is required}"
known_hosts="${DEPLOY_SSH_KNOWN_HOSTS:?DEPLOY_SSH_KNOWN_HOSTS is required}"

if [ "${CI_REPO:-}" != "${expected_repo}" ]; then
  echo "[woodpecker-deploy][error] unexpected repository: ${CI_REPO:-missing}" >&2
  exit 1
fi
if [[ ! "${release_sha}" =~ ^[0-9a-f]{40}$ ]]; then
  echo "[woodpecker-deploy][error] invalid release SHA" >&2
  exit 1
fi

temp_dir="$(mktemp -d /tmp/camera-rental-woodpecker.XXXXXX)"
key_file="${temp_dir}/deploy_key"
known_hosts_file="${temp_dir}/known_hosts"
bundle_file="${temp_dir}/camera-rental-source.bundle"
remote_bundle="/tmp/camera-rental-source-${release_sha}.bundle"
control_path="${temp_dir}/ssh-%C"

cleanup() {
  rm -rf "${temp_dir}"
}
trap cleanup EXIT

printf '%s\n' "${private_key}" > "${key_file}"
printf '%s\n' "${known_hosts}" > "${known_hosts_file}"
chmod 600 "${key_file}" "${known_hosts_file}"

ssh_opts=(
  -i "${key_file}"
  -p "${deploy_port}"
  -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=yes
  -o UserKnownHostsFile="${known_hosts_file}"
  -o ServerAliveInterval=30
  -o ServerAliveCountMax=20
  -o ControlMaster=auto
  -o ControlPersist=15m
  -o ControlPath="${control_path}"
)
scp_opts=(
  -i "${key_file}"
  -P "${deploy_port}"
  -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=yes
  -o UserKnownHostsFile="${known_hosts_file}"
  -o ServerAliveInterval=30
  -o ServerAliveCountMax=20
  -o ControlMaster=auto
  -o ControlPersist=15m
  -o ControlPath="${control_path}"
)

printf -v root_q '%q' "${deploy_root}"
remote_source_sha="$(
  ssh "${ssh_opts[@]}" "${deploy_user}@${deploy_host}" \
    "git -C ${root_q}/source rev-parse HEAD 2>/dev/null || true"
)"

bash ops/github-deploy/create-source-bundle.sh \
  "${remote_source_sha}" "${release_sha}" "${bundle_file}"

scp "${scp_opts[@]}" \
  ops/github-deploy/server-build-deploy.sh \
  ops/github-deploy/incremental-build-lib.sh \
  ops/github-deploy/deployment-runtime-lib.sh \
  ops/github-deploy/prepare-production-80.sh \
  ops/woodpecker/remote-deploy.sh \
  "${deploy_user}@${deploy_host}:/tmp/"
scp "${scp_opts[@]}" "${bundle_file}" \
  "${deploy_user}@${deploy_host}:${remote_bundle}"

printf -v sha_q '%q' "${release_sha}"
printf -v bundle_q '%q' "${remote_bundle}"
ssh "${ssh_opts[@]}" "${deploy_user}@${deploy_host}" \
  "bash /tmp/remote-deploy.sh ${root_q} ${sha_q} ${bundle_q}"

echo "[woodpecker-deploy] release ${release_sha} deployed"
