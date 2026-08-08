#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

OPERATOR=""
SOURCE_DEPLOY_DIR=""
SUDOERS_TEMP=""
fail() { printf '[FAILED] %s\n' "$1" >&2; exit 1; }
cleanup() { [[ -n "${SUDOERS_TEMP}" ]] && rm -f -- "${SUDOERS_TEMP}"; }
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
for file in linux/crehn-test-release-export-gate.sh sudoers/crehn-test-release-export-gate; do
  [[ -f "${SOURCE_DEPLOY_DIR}/${file}" && ! -L "${SOURCE_DEPLOY_DIR}/${file}" ]] || fail "missing reviewed bootstrap asset: ${file}"
done
install -d -o root -g root -m 0755 /usr/local/sbin /usr/local/libexec /usr/local/libexec/crehn-test /srv /srv/crehn-test /srv/crehn-test/exports
install -o root -g root -m 0755 "${SOURCE_DEPLOY_DIR}/linux/crehn-test-release-export-gate.sh" /usr/local/libexec/crehn-test/crehn-test-release-export-gate.sh
install -o root -g root -m 0755 "${SOURCE_DEPLOY_DIR}/linux/crehn-test-release-export-gate.sh" /usr/local/sbin/crehn-test-release-export-gate
SUDOERS_TEMP="$(mktemp /etc/sudoers.d/.crehn-test-release-export.XXXXXX)"
chown root:root "${SUDOERS_TEMP}"
chmod 0440 "${SUDOERS_TEMP}"
sed "s/@CREHN_TEST_OPERATOR@/${OPERATOR}/g" "${SOURCE_DEPLOY_DIR}/sudoers/crehn-test-release-export-gate" > "${SUDOERS_TEMP}"
visudo -cf "${SUDOERS_TEMP}"
mv -f -- "${SUDOERS_TEMP}" /etc/sudoers.d/crehn-test-release-export-gate
SUDOERS_TEMP=""
printf '[PASS] TEST immutable-release export gate bootstrap complete.\n'
