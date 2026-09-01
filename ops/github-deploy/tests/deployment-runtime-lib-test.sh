#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test_dir="$(mktemp -d /tmp/camera-rental-runtime-health.XXXXXX)"
trap 'rm -rf "${test_dir}"' EXIT

bin_dir="${test_dir}/bin"
mkdir -p "${bin_dir}"

cat > "${bin_dir}/systemctl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [ -n "${FAKE_SYSTEMCTL_LOG:-}" ]; then
  printf '%s\n' "$*" >> "${FAKE_SYSTEMCTL_LOG}"
fi
if [ "${1:-}" = "is-active" ]; then
  [ "${FAKE_SERVICE_ACTIVE:-true}" = "true" ]
  exit
fi
if [ "${1:-}" = "list-unit-files" ] \
  && [ "${2:-}" = "web.service" ] \
  && [ "${FAKE_WEB_SERVICE_EXISTS:-true}" != "true" ]; then
  exit 1
fi
exit 0
EOF

cat > "${bin_dir}/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
output=""
url=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    -o)
      output="$2"
      shift 2
      ;;
    -w|--max-time|--resolve|-H|-X|--data)
      shift 2
      ;;
    -*)
      shift
      ;;
    *)
      url="$1"
      shift
      ;;
  esac
done
if [ -n "${output}" ] && [ "${output}" != "/dev/null" ] && [[ "${url}" == */admin/ ]]; then
  cat > "${output}" <<'HTML'
<title>相机租赁管理后台</title>
<script type="module" src="/admin/assets/0123456789ab/index.js"></script>
<img src="/admin/logo.gif" alt="Logo" />
HTML
fi
printf '%s' "${FAKE_HTTP_STATUS:-200}"
EOF

cat > "${bin_dir}/journalctl" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF

chmod +x "${bin_dir}/systemctl" "${bin_dir}/curl" "${bin_dir}/journalctl"

# shellcheck source=../deployment-runtime-lib.sh
source "${repo_root}/ops/github-deploy/deployment-runtime-lib.sh"

release_test_root="${test_dir}/release-activation"
previous_release_dir="${release_test_root}/releases/previous"
next_release_dir="${release_test_root}/releases/next"
current_release_link="${release_test_root}/current"
activation_log="${release_test_root}/activation.log"
rollback_log="${release_test_root}/rollback.log"
systemctl_log="${release_test_root}/systemctl.log"
mkdir -p "${previous_release_dir}" "${next_release_dir}"
previous_release_dir="$(cd "${previous_release_dir}" && pwd -P)"
next_release_dir="$(cd "${next_release_dir}" && pwd -P)"
ln -s "${previous_release_dir}" "${current_release_link}"

activation_succeeds() {
  printf 'activated\n' >> "${activation_log}"
}

activation_fails() {
  return 23
}

rollback_must_not_run() {
  printf 'unexpected\n' >> "${rollback_log}"
  return 1
}

rollback_restarts_previous_release() {
  printf 'rollback\n' >> "${rollback_log}"
  restart_release_services \
    "${previous_release_dir}" \
    backend.service \
    web.service \
    false
}

run_release_activation_with_rollback \
  "${current_release_link}" \
  "${previous_release_dir}" \
  "${next_release_dir}" \
  activation_succeeds \
  rollback_must_not_run
[ "$(release_link_target "${current_release_link}")" = "${next_release_dir}" ]
[ ! -e "${rollback_log}" ]

atomic_switch_release_link "${current_release_link}" "${previous_release_dir}"
activation_status=0
PATH="${bin_dir}:${PATH}" FAKE_SYSTEMCTL_LOG="${systemctl_log}" \
  run_release_activation_with_rollback \
    "${current_release_link}" \
    "${previous_release_dir}" \
    "${next_release_dir}" \
    activation_fails \
    rollback_restarts_previous_release || activation_status=$?
[ "${activation_status}" -eq 23 ]
[ "$(release_link_target "${current_release_link}")" = "${previous_release_dir}" ]
grep -Fxq 'restart backend.service' "${systemctl_log}"
grep -Fxq 'rollback' "${rollback_log}"

atomic_switch_release_link "${current_release_link}" "${previous_release_dir}"
invalid_previous_status=0
run_release_activation_with_rollback \
  "${current_release_link}" \
  "${release_test_root}/releases/missing" \
  "${next_release_dir}" \
  activation_fails \
  rollback_must_not_run >/dev/null 2>&1 || invalid_previous_status=$?
[ "${invalid_previous_status}" -eq 23 ]
[ "$(release_link_target "${current_release_link}")" = "${next_release_dir}" ]

return_class_path="cn/iocoder/yudao/module/rental/controller/app/returnregistration/AppReturnRegistrationController.class"
remark_ai_class_path="cn/iocoder/yudao/module/rental/service/SellerRemarkAiFallbackService.class"
ai_controller_class_path="cn/iocoder/yudao/module/ai/controller/admin/model/AiApiKeyController.class"
rental_module_root="${test_dir}/rental-module"
ai_module_root="${test_dir}/ai-module"
outer_root="${test_dir}/outer"
rental_jar="${outer_root}/BOOT-INF/lib/yudao-module-rental-biz-test.jar"
ai_jar="${outer_root}/BOOT-INF/lib/yudao-module-ai-test.jar"
mkdir -p \
  "${rental_module_root}/$(dirname "${return_class_path}")" \
  "${rental_module_root}/$(dirname "${remark_ai_class_path}")" \
  "${ai_module_root}/$(dirname "${ai_controller_class_path}")" \
  "${outer_root}/BOOT-INF/lib"
touch \
  "${rental_module_root}/${return_class_path}" \
  "${rental_module_root}/${remark_ai_class_path}" \
  "${ai_module_root}/${ai_controller_class_path}"
(
  cd "${rental_module_root}"
  jar cf "${rental_jar}" .
)
(
  cd "${ai_module_root}"
  jar cf "${ai_jar}" .
)
(
  cd "${outer_root}"
  zip -q -0 -r "${test_dir}/yudao-server.jar" BOOT-INF
)
verify_return_registration_artifact "${test_dir}/yudao-server.jar"

compressed_dir="${test_dir}/compressed"
mkdir -p "${compressed_dir}/BOOT-INF/lib"
cp "${rental_jar}" "${compressed_dir}/BOOT-INF/lib/yudao-module-rental-biz-test.jar"
cp "${ai_jar}" "${compressed_dir}/BOOT-INF/lib/yudao-module-ai-test.jar"
(
  cd "${compressed_dir}"
  zip -q -0 "${test_dir}/compressed-yudao-server.jar" \
    BOOT-INF/lib/yudao-module-ai-test.jar
  zip -q -9 "${test_dir}/compressed-yudao-server.jar" \
    BOOT-INF/lib/yudao-module-rental-biz-test.jar
)
if verify_return_registration_artifact "${test_dir}/compressed-yudao-server.jar" \
  >/dev/null 2>&1; then
  echo "expected compressed nested rental JAR verification to fail" >&2
  exit 1
fi

mkdir -p "${test_dir}/missing/BOOT-INF/lib" "${test_dir}/empty-module"
(
  cd "${test_dir}/empty-module"
  jar cf "${test_dir}/missing/BOOT-INF/lib/yudao-module-rental-biz-test.jar" .
)
cp "${ai_jar}" "${test_dir}/missing/BOOT-INF/lib/yudao-module-ai-test.jar"
(
  cd "${test_dir}/missing"
  jar cf "${test_dir}/missing-yudao-server.jar" .
)
if verify_return_registration_artifact "${test_dir}/missing-yudao-server.jar" \
  >/dev/null 2>&1; then
  echo "FAIL: backend artifact without the return controller must be rejected" >&2
  exit 1
fi

PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=401 \
  wait_for_service_http backend.service http://backend.test reachable 1 0 0
PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=200 \
  wait_for_service_http web.service http://web.test success 1 0 0
PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=200 \
  verify_static_frontend_route https://rental.test/admin/ rental.test:443:127.0.0.1
PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=200 \
  verify_admin_frontend_route https://rental.test/admin/ rental.test:443:127.0.0.1

if PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=500 \
  wait_for_service_http backend.service http://backend.test reachable 1 0 0 \
  >/dev/null 2>&1; then
  echo "FAIL: backend HTTP 500 must fail the deployment health check" >&2
  exit 1
fi

if PATH="${bin_dir}:${PATH}" FAKE_SERVICE_ACTIVE=false \
  wait_for_service_http web.service http://web.test success 1 0 0 \
  >/dev/null 2>&1; then
  echo "FAIL: inactive service must fail the deployment health check" >&2
  exit 1
fi

if PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=403 \
  verify_static_frontend_route https://rental.test/admin/ rental.test:443:127.0.0.1 \
  >/dev/null 2>&1; then
  echo "FAIL: admin HTTP 403 must fail the deployment health check" >&2
  exit 1
fi

if PATH="${bin_dir}:${PATH}" FAKE_HTTP_STATUS=404 \
  verify_admin_frontend_route https://rental.test/admin/ rental.test:443:127.0.0.1 \
  >/dev/null 2>&1; then
  echo "FAIL: missing admin entry assets must fail the deployment health check" >&2
  exit 1
fi

grep -q '20260803_041_rental_device_short_codes.sql' \
  "${repo_root}/ops/github-deploy/server-deploy.sh"

echo "deployment runtime health tests passed"
