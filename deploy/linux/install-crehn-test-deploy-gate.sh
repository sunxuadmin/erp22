#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

OPERATOR=""
SOURCE_DEPLOY_DIR=""
SUDOERS_TEMP=""
SNAPSHOT_DIR=""
fail() { printf '[FAILED] %s\n' "$1" >&2; exit 1; }
cleanup_sudoers_temp() {
  [[ -n "${SUDOERS_TEMP}" ]] && rm -f -- "${SUDOERS_TEMP}"
  [[ -n "${SNAPSHOT_DIR}" ]] && rm -rf -- "${SNAPSHOT_DIR}"
}
trap cleanup_sudoers_temp EXIT
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --operator) OPERATOR="${2:-}"; shift 2 ;;
    --source-deploy-dir) SOURCE_DEPLOY_DIR="${2:-}"; shift 2 ;;
    *) fail "Unknown bootstrap argument: $1" ;;
  esac
done
[[ "$(id -u)" -eq 0 ]] || fail 'bootstrap must be run by an administrator as root'
[[ "${OPERATOR}" =~ ^[A-Za-z_][A-Za-z0-9_-]*$ ]] || fail 'operator is invalid'
[[ -d "/home/${OPERATOR}" && -n "${SOURCE_DEPLOY_DIR}" && ! -L "${SOURCE_DEPLOY_DIR}" ]] || fail 'operator home or reviewed source deploy directory is missing'
SNAPSHOT_DIR="$(mktemp -d /root/crehn-test-gate-bootstrap.XXXXXX)"
chown root:root "${SNAPSHOT_DIR}"
chmod 0700 "${SNAPSHOT_DIR}"
for path in linux compose.yml compose.test.yml sudoers; do cp -a -- "${SOURCE_DEPLOY_DIR}/${path}" "${SNAPSHOT_DIR}/"; done
SOURCE_DEPLOY_DIR="${SNAPSHOT_DIR}"
for file in linux/crehn-test-deploy-gate.sh linux/crehn-test-stage-candidate.sh linux/install-assets.sh linux/crehn-deploy.sh compose.yml compose.test.yml; do
  [[ -f "${SOURCE_DEPLOY_DIR}/${file}" && ! -L "${SOURCE_DEPLOY_DIR}/${file}" ]] || fail "missing reviewed bootstrap asset: ${file}"
done
SUDOERS_TEMPLATE="${SOURCE_DEPLOY_DIR}/sudoers/crehn-test-deploy-gate"
[[ -f "${SUDOERS_TEMPLATE}" && ! -L "${SUDOERS_TEMPLATE}" ]] || fail 'sudoers template must be a non-symlink regular file'
install -d -o root -g root -m 0755 /usr/local/sbin /usr/local/libexec /usr/local/libexec/crehn-test /srv /srv/crehn-test /srv/crehn-test/runtime /srv/crehn-test/state
for script in crehn-test-deploy-gate.sh crehn-test-stage-candidate.sh install-assets.sh crehn-deploy.sh; do
  install -o root -g root -m 0755 "${SOURCE_DEPLOY_DIR}/linux/${script}" "/usr/local/libexec/crehn-test/${script}"
done
install -o root -g root -m 0644 "${SOURCE_DEPLOY_DIR}/compose.yml" /usr/local/libexec/crehn-test/compose.yml
install -o root -g root -m 0644 "${SOURCE_DEPLOY_DIR}/compose.test.yml" /usr/local/libexec/crehn-test/compose.test.yml
install -o root -g root -m 0755 "${SOURCE_DEPLOY_DIR}/linux/crehn-test-deploy-gate.sh" /usr/local/sbin/crehn-test-deploy-gate
printf 'CREHN_TEST_OPERATOR=%q\n' "${OPERATOR}" > /etc/crehn-test-deploy-gate.conf
chown root:root /etc/crehn-test-deploy-gate.conf
chmod 0600 /etc/crehn-test-deploy-gate.conf
install -d -o "${OPERATOR}" -g "${OPERATOR}" -m 0700 "/home/${OPERATOR}/crehn-test-inbox"
SUDOERS_TEMP="$(mktemp /etc/sudoers.d/.crehn-test-deploy-gate.XXXXXX)"
chown root:root "${SUDOERS_TEMP}"
chmod 0440 "${SUDOERS_TEMP}"
sed "s/@CREHN_TEST_OPERATOR@/${OPERATOR}/g" "${SUDOERS_TEMPLATE}" > "${SUDOERS_TEMP}"
visudo -cf "${SUDOERS_TEMP}"
mv -f -- "${SUDOERS_TEMP}" /etc/sudoers.d/crehn-test-deploy-gate
SUDOERS_TEMP=""
printf '[PASS] TEST root gate bootstrap complete; SSH authentication and sudo authorization remain separate.\n'
