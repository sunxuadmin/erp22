#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

OPERATOR=""
SOURCE_DEPLOY_DIR=""
SUDOERS_TEMP=""
SNAPSHOT_DIR=""
fail() { printf '[FAILED] %s\n' "$1" >&2; exit 1; }
cleanup() {
  [[ -n "${SUDOERS_TEMP}" ]] && rm -f -- "${SUDOERS_TEMP}"
  [[ -n "${SNAPSHOT_DIR}" ]] && rm -rf -- "${SNAPSHOT_DIR}"
}
trap cleanup EXIT
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --operator) OPERATOR="${2:-}"; shift 2 ;;
    --source-deploy-dir) SOURCE_DEPLOY_DIR="${2:-}"; shift 2 ;;
    *) fail "Unknown bootstrap argument: $1" ;;
  esac
done
[[ "$(id -u)" -eq 0 ]] || fail 'bootstrap must be run by root'
[[ "${OPERATOR}" =~ ^[A-Za-z_][A-Za-z0-9_-]*$ && -d "/home/${OPERATOR}" ]] || fail 'operator is invalid or its home is missing'
[[ -d "${SOURCE_DEPLOY_DIR}" && ! -L "${SOURCE_DEPLOY_DIR}" ]] || fail 'reviewed source deploy directory is missing'
SNAPSHOT_DIR="$(mktemp -d /root/crehn-prod-gate-bootstrap.XXXXXX)"
chown root:root "${SNAPSHOT_DIR}"
chmod 0700 "${SNAPSHOT_DIR}"
for path in linux compose.yml compose.prod.yml sudoers; do cp -a -- "${SOURCE_DEPLOY_DIR}/${path}" "${SNAPSHOT_DIR}/"; done
SOURCE_DEPLOY_DIR="${SNAPSHOT_DIR}"
for file in linux/crehn-prod-stage-release.sh linux/crehn-prod-deploy-gate.sh linux/install-assets.sh linux/crehn-deploy.sh linux/validate-crehn-prod-runtime-env.sh compose.yml compose.prod.yml sudoers/crehn-prod-deploy-gate; do
  [[ -f "${SOURCE_DEPLOY_DIR}/${file}" && ! -L "${SOURCE_DEPLOY_DIR}/${file}" ]] || fail "missing reviewed bootstrap asset: ${file}"
done
install -d -o root -g root -m 0755 /usr/local/sbin /usr/local/libexec /usr/local/libexec/crehn-prod /srv /srv/crehn-prod /srv/crehn-prod/runtime /srv/crehn-prod/releases /srv/crehn-prod/state
for script in crehn-prod-stage-release.sh crehn-prod-deploy-gate.sh install-assets.sh crehn-deploy.sh validate-crehn-prod-runtime-env.sh; do
  install -o root -g root -m 0755 "${SOURCE_DEPLOY_DIR}/linux/${script}" "/usr/local/libexec/crehn-prod/${script}"
done
install -o root -g root -m 0644 "${SOURCE_DEPLOY_DIR}/compose.yml" /usr/local/libexec/crehn-prod/compose.yml
install -o root -g root -m 0644 "${SOURCE_DEPLOY_DIR}/compose.prod.yml" /usr/local/libexec/crehn-prod/compose.prod.yml
install -o root -g root -m 0755 "${SOURCE_DEPLOY_DIR}/linux/crehn-prod-deploy-gate.sh" /usr/local/sbin/crehn-prod-deploy-gate
printf 'CREHN_PROD_OPERATOR=%q\n' "${OPERATOR}" >/etc/crehn-prod-deploy-gate.conf
chown root:root /etc/crehn-prod-deploy-gate.conf
chmod 0600 /etc/crehn-prod-deploy-gate.conf
install -d -o "${OPERATOR}" -g "${OPERATOR}" -m 0700 "/home/${OPERATOR}/crehn-prod-inbox"
SUDOERS_TEMP="$(mktemp /etc/sudoers.d/.crehn-prod-deploy-gate.XXXXXX)"
chown root:root "${SUDOERS_TEMP}"
chmod 0440 "${SUDOERS_TEMP}"
sed "s/@CREHN_PROD_OPERATOR@/${OPERATOR}/g" "${SOURCE_DEPLOY_DIR}/sudoers/crehn-prod-deploy-gate" >"${SUDOERS_TEMP}"
visudo -cf "${SUDOERS_TEMP}"
mv -f -- "${SUDOERS_TEMP}" /etc/sudoers.d/crehn-prod-deploy-gate
SUDOERS_TEMP=""
printf '[PASS] production deployment gate bootstrap complete; database initialization, TLS and network exposure remain separate.\n'
