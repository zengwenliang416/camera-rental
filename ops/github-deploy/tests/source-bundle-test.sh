#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-source-bundle.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

source_repo="${test_dir}/source"
mkdir -p "${source_repo}"
git -C "${source_repo}" init --quiet
git -C "${source_repo}" config user.name "Bundle Test"
git -C "${source_repo}" config user.email "bundle-test@example.invalid"

printf 'one\n' > "${source_repo}/example.txt"
git -C "${source_repo}" add example.txt
git -C "${source_repo}" commit --quiet -m one
baseline_sha="$(git -C "${source_repo}" rev-parse HEAD)"

printf 'two\n' >> "${source_repo}/example.txt"
git -C "${source_repo}" commit --quiet -am two
printf 'three\n' >> "${source_repo}/example.txt"
git -C "${source_repo}" commit --quiet -am three
release_sha="$(git -C "${source_repo}" rev-parse HEAD)"

incremental_bundle="${test_dir}/incremental.bundle"
bash "${repo_root}/ops/github-deploy/create-source-bundle.sh" \
  "${baseline_sha}" "${release_sha}" "${incremental_bundle}" "${source_repo}"

target_repo="${test_dir}/target"
git clone --quiet --no-hardlinks "${source_repo}" "${target_repo}"
git -C "${target_repo}" checkout --quiet "${baseline_sha}"
git -C "${target_repo}" fetch --quiet "${incremental_bundle}" "${release_sha}"
test "$(git -C "${target_repo}" rev-parse FETCH_HEAD)" = "${release_sha}"

complete_bundle="${test_dir}/complete.bundle"
bash "${repo_root}/ops/github-deploy/create-source-bundle.sh" \
  "" "${release_sha}" "${complete_bundle}" "${source_repo}"
fresh_repo="${test_dir}/fresh"
git clone --quiet "${complete_bundle}" "${fresh_repo}"
test "$(git -C "${fresh_repo}" rev-parse HEAD)" = "${release_sha}"

same_head_bundle="${test_dir}/same-head.bundle"
bash "${repo_root}/ops/github-deploy/create-source-bundle.sh" \
  "${release_sha}" "${release_sha}" "${same_head_bundle}" "${source_repo}"
git -C "${target_repo}" fetch --quiet "${same_head_bundle}" "${release_sha}"
test "$(git -C "${target_repo}" rev-parse FETCH_HEAD)" = "${release_sha}"

echo "source bundle tests passed"
