#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

readonly CONFIG=/etc/crehn-prod-deploy-gate.conf
readonly TRUSTED_ROOT=/usr/local/libexec/crehn-prod
readonly DEPLOY_ROOT=/srv/crehn-prod
readonly PROJECT=crehn-prod

blocked() { printf '[BLOCKED] %s\n' "$1" >&2; exit 3; }
safe_root_dir() {
  local path="$1" mode
  [[ -d "${path}" && ! -L "${path}" && "$(stat -c '%U:%G' "${path}")" == 'root:root' ]] || blocked "unsafe root directory: ${path}"
  mode="$(stat -c '%a' "${path}")"
  (( (8#${mode} & 8#022) == 0 )) || blocked "root directory is group/world writable: ${path}"
}
candidate_version() {
  local file="${DEPLOY_ROOT}/runtime/prod-candidate.env"
  [[ -f "${file}" && ! -L "${file}" && "$(stat -c '%U:%G:%a' "${file}")" == 'root:root:600' ]] || blocked 'no root-owned staged production candidate is available'
  # shellcheck disable=SC1090
  source "${file}"
  [[ "${version:-}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] || blocked 'staged production version is invalid'
  printf '%s' "${version}"
}
inbox_version() {
  local file="/home/${CREHN_PROD_OPERATOR}/crehn-prod-inbox/promotion.env" version
  [[ -f "${file}" && ! -L "${file}" && "$(stat -c '%U' "${file}")" == "${CREHN_PROD_OPERATOR}" ]] || blocked 'promotion metadata is missing or unsafe'
  version="$(grep -m1 '^version=' "${file}" | cut -d= -f2- | tr -d '\r')"
  [[ "${version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] || blocked 'promotion metadata version is invalid'
  printf '%s' "${version}"
}
require_promotion_confirmation() {
  local version="$1"
  [[ "${CONFIRMATION}" == "PROMOTE:crehn-prod:${version}" ]] || blocked 'production promotion confirmation is invalid'
}

[[ "$(id -u)" -eq 0 ]] || blocked 'gate must run through sudo'
[[ "$#" -eq 3 && "$2" == '--apply' ]] || blocked 'gate accepts only ACTION --apply CONFIRMATION'
readonly ACTION="$1"
readonly CONFIRMATION="$3"
[[ "$(readlink -f "$0")" == '/usr/local/sbin/crehn-prod-deploy-gate' ]] || blocked 'gate path is not the fixed installed path'
for dir in /usr /usr/local /usr/local/sbin /usr/local/libexec "${TRUSTED_ROOT}" /srv "${DEPLOY_ROOT}" "${DEPLOY_ROOT}/runtime" "${DEPLOY_ROOT}/releases" "${DEPLOY_ROOT}/state"; do safe_root_dir "${dir}"; done
[[ -f "${CONFIG}" && ! -L "${CONFIG}" && "$(stat -c '%U:%G:%a' "${CONFIG}")" == 'root:root:600' ]] || blocked 'production gate configuration is missing or unsafe'
# shellcheck disable=SC1090
source "${CONFIG}"
[[ "${SUDO_USER:-}" == "${CREHN_PROD_OPERATOR:-}" ]] || blocked 'sudo caller is not the configured production operator'
for trusted in "${TRUSTED_ROOT}/crehn-prod-stage-release.sh" "${TRUSTED_ROOT}/crehn-deploy.sh" "${TRUSTED_ROOT}/install-assets.sh" "${TRUSTED_ROOT}/validate-crehn-prod-runtime-env.sh"; do
  [[ -f "${trusted}" && ! -L "${trusted}" && "$(stat -c '%U:%G:%a' "${trusted}")" =~ ^root:root:(700|750|755)$ ]] || blocked 'trusted production script is missing or unsafe'
done
for compose_file in "${TRUSTED_ROOT}/compose.yml" "${TRUSTED_ROOT}/compose.prod.yml"; do [[ -f "${compose_file}" && ! -L "${compose_file}" && "$(stat -c '%U:%G:%a' "${compose_file}")" == root:root:644 ]] || blocked 'trusted production Compose file is missing or unsafe'; done

case "${ACTION}" in
prepare)
    inbox="/home/${CREHN_PROD_OPERATOR}/crehn-prod-inbox"
    require_promotion_confirmation "$(inbox_version)"
    for file in promotion.env crehn-images.json crehn-images.tar crehn-deploy-assets.tar.gz crehn-prod-runtime.env; do
      [[ -f "${inbox}/${file}" && ! -L "${inbox}/${file}" && "$(stat -c '%U' "${inbox}/${file}")" == "${CREHN_PROD_OPERATOR}" ]] || blocked 'production inbox does not contain the exact operator-owned promotion inputs'
      chmod 0600 "${inbox}/${file}"
    done
    printf '[PASS] production inbox fixed inputs prepared\n'
    ;;
  stage)
    require_promotion_confirmation "$(inbox_version)"
    exec "${TRUSTED_ROOT}/crehn-prod-stage-release.sh"
    ;;
  deploy)
    version="$(candidate_version)"
    require_promotion_confirmation "${version}"
    exec "${TRUSTED_ROOT}/crehn-deploy.sh" deploy-prod --env-file "${DEPLOY_ROOT}/runtime/prod-${version}.env" --release-manifest "${DEPLOY_ROOT}/releases/${version}/crehn-images-${version}.json" --apply --confirmation "DEPLOY:${PROJECT}:${version}"
    ;;
  *) blocked 'only prepare, stage and deploy are accepted' ;;
esac
