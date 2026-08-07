$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Require-Match {
    param([string]$Text, [string]$Pattern, [string]$Message)
    if ($Text -notmatch $Pattern) { throw "FAILED $Message" }
}

function Require-NoMatch {
    param([string]$Text, [string]$Pattern, [string]$Message)
    if ($Text -match $Pattern) { throw "FAILED $Message" }
}

$sudoers = Get-Content -LiteralPath (Join-Path $root 'deploy\sudoers\crehn-test-deploy-gate') -Raw
Require-Match $sudoers 'Cmnd_Alias CREHN_TEST_DEPLOY_GATE = /usr/local/sbin/crehn-test-deploy-gate stage, /usr/local/sbin/crehn-test-deploy-gate build, /usr/local/sbin/crehn-test-deploy-gate deploy, /usr/local/sbin/crehn-test-deploy-gate verify' 'sudoers must enumerate exactly four gate actions'
Require-NoMatch $sudoers '(?im)(\*|NOPASSWD:\s*ALL\b|\bbash\b|\bdocker\b)' 'sudoers must not grant wildcard, command ALL, bash, or docker access'

$gate = Get-Content -LiteralPath (Join-Path $root 'deploy\linux\crehn-test-deploy-gate.sh') -Raw
Require-Match $gate '\[\[ "\$#" -eq 1 \]\]' 'gate must require one argument'
Require-Match $gate 'readonly DEPLOY_ROOT=/srv/crehn-test' 'gate must bind the TEST root'
Require-Match $gate 'readonly PROJECT=crehn-test' 'gate must bind the TEST project'
Require-Match $gate 'readonly TRUSTED_ROOT=/usr/local/libexec/crehn-test' 'gate must use trusted libexec scripts'
Require-Match $gate 'trusted Compose file is missing or unsafe' 'gate must validate root-owned Compose files'
Require-Match $gate 'readlink -f' 'gate must verify its fixed resolved path'
Require-Match $gate 'safe_root_dir' 'gate must validate trusted parent directories'
Require-NoMatch $gate '(?im)(deploy-prod|database|rollback|backup-maintenance|sudo\s+-n|/srv/crehn-test/source/.+\.sh)' 'gate must not expose prod, database, rollback, cleanup, nested sudo, or source scripts'

$stage = Get-Content -LiteralPath (Join-Path $root 'deploy\linux\crehn-test-stage-candidate.sh') -Raw
foreach ($pattern in @('! -L', '-f', "'600'", "'700'", 'sha256sum', 'source_revision', 'tar -tzf', 'tar -tvzf', 'archive contains a symbolic link, hard link, or special entry')) {
    Require-Match $stage ([regex]::Escape($pattern)) "candidate staging lacks required guard: $pattern"
}

$controllerPath = Join-Path $root 'deploy\scripts\invoke-deployment.ps1'
$tokens = $null; $parseErrors = $null
[void][System.Management.Automation.Language.Parser]::ParseFile($controllerPath, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -ne 0) { throw "FAILED invoke-deployment.ps1 parse errors: $($parseErrors.Message -join '; ')" }
$controller = Get-Content -LiteralPath $controllerPath -Raw
Require-Match $controller "'sudo', '-n', '/usr/local/sbin/crehn-test-deploy-gate', 'stage'" 'controller must stage through the installed gate'
Require-Match $controller ('sudo'', ''-n'', ''/usr/local/sbin/crehn-test-deploy-gate'', \$gateAction') 'controller must invoke exact dynamic gate action'
Require-Match $controller ('@\(''mkdir'', ''-p'', ''--'', \$candidateInbox\)') 'controller must create the fixed candidate inbox idempotently'
Require-Match $controller ('@\(''chmod'', ''700'', ''--'', \$candidateInbox\)') 'controller must reset the fixed candidate inbox mode'
Require-NoMatch $controller ('@\(''mkdir'', ''-m'', ''700'', ''--'', \$candidateInbox\)') 'controller must not fail when the fixed candidate inbox already exists'
Require-NoMatch $controller 'crehn-test-deploy-gate.*(database|rollback|cleanup|prod)' 'controller must map only four TEST gate actions'
Require-NoMatch (Get-Content -LiteralPath (Join-Path $root 'deploy\linux\install-assets.sh') -Raw) 'TRUSTED_ROOT.+crehn-deploy|TRUSTED_ROOT.+compose' 'ordinary stage must not rewrite trusted root assets'
Require-Match $stage 'install -o root -g root -m 0600 "\$\{ARCHIVE\}" "\$\{WORK_ARCHIVE\}"' 'stage must copy archive root-owned before validation'
Require-Match $stage 'safe_root_dir' 'stage must validate root-owned deployment directories'
Require-Match (Get-Content -LiteralPath (Join-Path $root 'deploy\linux\crehn-deploy.sh') -Raw) 'SOURCE_PROJECT_DIR="/srv/crehn-test/source"' 'trusted helper must use the fixed source project directory'
Require-Match (Get-Content -LiteralPath (Join-Path $root 'deploy\linux\install-assets.sh') -Raw) 'TEST staged input must be root-owned non-symlink mode 600' 'TEST installer must recheck root-owned inputs'
$bootstrap = Get-Content -LiteralPath (Join-Path $root 'deploy\linux\install-crehn-test-deploy-gate.sh') -Raw
Require-Match $bootstrap 'sudoers template must be a non-symlink regular file' 'bootstrap must reject symlinked sudoers templates'
Require-Match $bootstrap 'mktemp -d /root/crehn-test-gate-bootstrap\.XXXXXX' 'bootstrap must create a root-owned asset snapshot'
Require-Match $bootstrap 'mktemp /etc/sudoers\.d/\.crehn-test-deploy-gate\.XXXXXX' 'bootstrap must use a sudoers temporary file'
Require-Match $bootstrap 'visudo -cf "\$\{SUDOERS_TEMP\}"' 'bootstrap must validate temporary sudoers before installation'
Require-Match $bootstrap 'mv -f -- "\$\{SUDOERS_TEMP\}" /etc/sudoers\.d/crehn-test-deploy-gate' 'bootstrap must atomically install the validated sudoers file'
if ($bootstrap.IndexOf('visudo -cf "${SUDOERS_TEMP}"') -gt $bootstrap.IndexOf('mv -f -- "${SUDOERS_TEMP}" /etc/sudoers.d/crehn-test-deploy-gate')) {
    throw 'FAILED bootstrap must validate sudoers before installing it'
}

Write-Output '[PASS] OPS-003 TEST root deployment gate static contract passed'
