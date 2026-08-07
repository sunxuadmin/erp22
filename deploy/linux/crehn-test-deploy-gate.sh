#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

readonly CONFIG=/etc/crehn-test-deploy-gate.conf
readonly TRUSTED_ROOT=/usr/local/libexec/crehn-test
readonly DEPLOY_ROOT=/srv/crehn-test
readonly PROJECT=crehn-test

blocked() { printf '[BLOCKED] %s\n' "$1" >&2; exit 3; }
safe_root_dir() {
  local path="$1" mode
  [[ -d "${path}" && ! -L "${path}" && "$(stat -c '%U:%G' "${path}")" == 'root:root' ]] || blocked "unsafe root directory: ${path}"
  mode="$(stat -c '%a' "${path}")"
  (( (8#${mode} & 8#022) == 0 )) || blocked "root directory is group/world writable: ${path}"
}
[[ "$(id -u)" -eq 0 ]] || blocked 'gate must run through sudo'
[[ "$#" -eq 1 ]] || blocked 'gate accepts exactly one action'
[[ "$(readlink -f "$0")" == '/usr/local/sbin/crehn-test-deploy-gate' ]] || blocked 'gate path is not the fixed installed path'
for dir in /usr /usr/local /usr/local/sbin /usr/local/libexec /usr/local/libexec/crehn-test /srv /srv/crehn-test /srv/crehn-test/runtime /srv/crehn-test/state; do safe_root_dir "${dir}"; done
[[ -f "${CONFIG}" && ! -L "${CONFIG}" && "$(stat -c '%U:%G:%a' "${CONFIG}")" == 'root:root:600' ]] ||
  blocked 'gate configuration is missing or unsafe'
# shellcheck disable=SC1090
source "${CONFIG}"
[[ "${SUDO_USER:-}" == "${CREHN_TEST_OPERATOR:-}" ]] || blocked 'sudo caller is not the configured TEST operator'
for trusted in "${TRUSTED_ROOT}/crehn-test-stage-candidate.sh" "${TRUSTED_ROOT}/crehn-deploy.sh" "${TRUSTED_ROOT}/install-assets.sh"; do
  [[ -f "${trusted}" && ! -L "${trusted}" ]] || blocked "trusted script is missing: ${trusted}"
  [[ "$(stat -c '%U:%G:%a' "${trusted}")" =~ ^root:root:(700|750|755)$ ]] || blocked "trusted script permissions are unsafe: ${trusted}"
done
for compose_file in "${TRUSTED_ROOT}/compose.yml" "${TRUSTED_ROOT}/compose.test.yml" "${TRUSTED_ROOT}/compose.test-gate.yml"; do
  [[ -f "${compose_file}" && ! -L "${compose_file}" && "$(stat -c '%U:%G:%a' "${compose_file}")" == 'root:root:644' ]] ||
    blocked "trusted Compose file is missing or unsafe: ${compose_file}"
done

candidate_version() {
  local file="${DEPLOY_ROOT}/runtime/test-candidate.env"
  [[ -f "${file}" && ! -L "${file}" && "$(stat -c '%U:%G:%a' "${file}")" == 'root:root:600' ]] ||
    blocked 'no root-owned staged TEST candidate is available'
  # shellcheck disable=SC1090
  source "${file}"
  [[ "${version:-}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] || blocked 'staged version is invalid'
  printf '%s' "${version}"
}

case "$1" in
  stage)
    exec "${TRUSTED_ROOT}/crehn-test-stage-candidate.sh"
    ;;
  build)
    version="$(candidate_version)"
    exec "${TRUSTED_ROOT}/crehn-deploy.sh" build-local --env-file "${DEPLOY_ROOT}/runtime/test-${version}.env" --apply --confirmation "BUILD:${PROJECT}:${version}"
    ;;
  deploy)
    version="$(candidate_version)"
    exec "${TRUSTED_ROOT}/crehn-deploy.sh" deploy-local --env-file "${DEPLOY_ROOT}/runtime/test-${version}.env" --release-manifest "${DEPLOY_ROOT}/releases/${version}/crehn-images-${version}.json" --apply --confirmation "DEPLOY:${PROJECT}:${version}"
    ;;
  verify)
    exec "${TRUSTED_ROOT}/crehn-deploy.sh" verify-local --env-file "${DEPLOY_ROOT}/runtime/test.env"
    ;;
  *) blocked 'only stage, build, deploy and verify are accepted' ;;
esac
