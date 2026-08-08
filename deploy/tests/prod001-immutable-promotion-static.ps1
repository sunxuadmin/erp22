$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$promotion = Join-Path $projectRoot 'deploy\scripts\promote-test-release-via-229.ps1'
$testGate = Join-Path $projectRoot 'deploy\linux\crehn-test-release-export-gate.sh'
$prodGate = Join-Path $projectRoot 'deploy\linux\crehn-prod-deploy-gate.sh'
$prodStage = Join-Path $projectRoot 'deploy\linux\crehn-prod-stage-release.sh'
$runtimeValidator = Join-Path $projectRoot 'deploy\linux\validate-crehn-prod-runtime-env.sh'
$sudoers = Join-Path $projectRoot 'deploy\sudoers\crehn-prod-deploy-gate'

function Require-Match([string]$Content, [string]$Pattern, [string]$Message) {
    if ($Content -notmatch $Pattern) { throw "FAILED $Message" }
}
function Require-NoMatch([string]$Content, [string]$Pattern, [string]$Message) {
    if ($Content -match $Pattern) { throw "FAILED $Message" }
}
function Require-Exit([scriptblock]$Operation, [string]$Expected) {
    $output = & $Operation 2>&1
    if ($LASTEXITCODE -eq 0 -or (($output | Out-String) -notmatch [regex]::Escape($Expected))) { throw "FAILED expected blocked result: $Expected; actual=$($output | Out-String)" }
}

$promotionContent = Get-Content -LiteralPath $promotion -Raw
$testGateContent = Get-Content -LiteralPath $testGate -Raw
$prodGateContent = Get-Content -LiteralPath $prodGate -Raw
$prodStageContent = Get-Content -LiteralPath $prodStage -Raw
$runtimeValidatorContent = Get-Content -LiteralPath $runtimeValidator -Raw
$sudoersContent = Get-Content -LiteralPath $sudoers -Raw

Require-Match $promotionContent 'if \(-not \$Apply\)' 'promotion must default to a no-write plan'
Require-Match $promotionContent 'PROMOTE:crehn-prod:' 'promotion must require an exact confirmation'
Require-Match $promotionContent 'ProxyJump crehn-test-jump' 'production SSH must use the 229 jump host'
Require-Match $promotionContent 'Confirm-HostFingerprint' 'both SSH endpoints must be fingerprint checked'
Require-Match $promotionContent 'jump_known_hosts' 'jump host must use a dedicated known_hosts file'
Require-Match $promotionContent 'prod_known_hosts' 'production host must use a dedicated known_hosts file'
Require-Match $promotionContent 'JumpKnownHosts' 'fingerprint confirmation must bind the jump alias to its own known_hosts file'
Require-Match $promotionContent 'ProdKnownHosts' 'fingerprint confirmation must bind the production alias to its own known_hosts file'
Require-Match $promotionContent 'ConvertTo-SshConfigPath' 'Windows OpenSSH config paths must be normalized and quoted'
Require-Match $promotionContent 'UserKnownHostsFile \$jumpKnownHostsConfig' 'jump SSH config must reference only its dedicated known_hosts file'
Require-Match $promotionContent 'UserKnownHostsFile \$prodKnownHostsConfig' 'production SSH config must reference only its dedicated known_hosts file'
Require-Match $promotionContent "\.Replace\('\\', '/'\)" 'Windows OpenSSH paths must be normalized to forward slashes'
Require-Match $promotionContent 'crehn-test-release-export-gate' 'TEST artifacts must come from the fixed export gate'
Require-Match $promotionContent 'crehn-prod-deploy-gate' 'production writes must use the fixed deployment gate'
Require-Match $promotionContent 'Test-PublicEndpoint' 'DNS and HTTPS health verification contract is required'
Require-NoMatch $promotionContent '(?im)\bdocker\s+(build|pull|system\s+prune)\b|\bsql\b|Invoke-RestMethod.*dns' 'promotion entry must not build, pull, prune, execute SQL, or call a DNS API'
Require-Match $testGateContent 'SUCCESS, manifest and archive are required' 'TEST gate must require complete immutable release markers'
Require-Match $testGateContent 'manifest_architecture.*x86_64' 'TEST gate must require x86_64'
Require-Match $testGateContent 'manifest image IDs are missing or invalid' 'TEST gate must validate image IDs'
Require-Match $testGateContent 'candidate metadata differs from active runtime' 'TEST export must refuse deployment assets from a different candidate revision'
Require-Match $testGateContent 'release-export\.lock' 'TEST export must take a fixed lock'
Require-Match $prodStageContent 'release archive SHA-256 mismatch' 'production stage must validate the archive hash'
Require-Match $prodStageContent 'CREHN_RUNTIME_ENV.*prod' 'production stage must require prod runtime'
Require-Match $prodStageContent 'CREHN_COMPOSE_PROJECT_NAME.*crehn-prod' 'production stage must require the crehn-prod project'
Require-Match $prodStageContent 'CREHN_DEPLOY_ROOT.*srv/crehn-prod' 'production stage must require the fixed root'
Require-Match $prodStageContent 'validate-crehn-prod-runtime-env' 'production stage must validate dotenv without sourcing it'
Require-Match $prodStageContent 'promotion-stage\.lock' 'production stage must take a fixed lock'
Require-Match $prodStageContent 'inbox=cleared' 'production stage must clear fixed inbox inputs after success'
Require-NoMatch $runtimeValidatorContent '(?im)\bsource\b|\beval\b' 'runtime validator must not source or evaluate operator input'
Require-Match $prodGateContent 'only prepare, stage and deploy are accepted' 'production gate must expose only fixed actions'
Require-Match $prodGateContent 'ACTION --apply CONFIRMATION' 'production gate must require apply and an exact confirmation'
Require-NoMatch $prodGateContent '(?im)docker\s+(build|pull|system\s+prune)|\bsql\b|bash\s+-c' 'production gate must not expose build, pull, prune, SQL, or arbitrary bash'
Require-Match $sudoersContent 'crehn-prod-deploy-gate prepare --apply' 'sudoers must limit production actions to the gate'

$fixture = Join-Path ([System.IO.Path]::GetTempPath()) ('crehn-prod001-test-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixture | Out-Null
try {
    $testKey = Join-Path $fixture 'test.key'
    $prodKey = Join-Path $fixture 'prod.key'
    Set-Content -LiteralPath $testKey -Value 'fixture-only' -NoNewline
    Set-Content -LiteralPath $prodKey -Value 'fixture-only' -NoNewline
    $control = Join-Path $fixture 'control.env'
    @(
        'CREHN_TEST_SSH_HOST=192.168.2.229', 'CREHN_TEST_SSH_PORT=22', 'CREHN_TEST_SSH_USER=fixture_test', 'CREHN_TEST_SSH_AUTH=key', "CREHN_TEST_SSH_KEY_PATH=$testKey", 'CREHN_TEST_HOST_FINGERPRINT=SHA256:AAAAAAAAAAAAAAAAAAAAAAAA',
        'CREHN_ECS2_SSH_HOST=118.190.150.161', 'CREHN_ECS2_SSH_PORT=22', 'CREHN_ECS2_SSH_USER=fixture_prod', 'CREHN_ECS2_SSH_AUTH=key', "CREHN_ECS2_SSH_KEY_PATH=$prodKey", 'CREHN_ECS2_HOST_FINGERPRINT=SHA256:BBBBBBBBBBBBBBBBBBBBBBBB',
        'CREHN_PROD_PUBLIC_DOMAIN=cre.blog56.top', 'CREHN_PROD_EXPECTED_IPV4=118.190.150.161'
    ) | Set-Content -LiteralPath $control -Encoding ascii
    $runtime = Join-Path $fixture 'prod.env'
    @('CREHN_VERSION=1.2.3', 'CREHN_SOURCE_REVISION=0123456789abcdef0123456789abcdef01234567', 'CREHN_RUNTIME_ENV=prod', 'CREHN_COMPOSE_PROJECT_NAME=crehn-prod', 'CREHN_DEPLOY_ROOT=/srv/crehn-prod') | Set-Content -LiteralPath $runtime -Encoding ascii
    $plan = & pwsh -NoProfile -File $promotion -Version '1.2.3' -ControlEnvFile $control -RuntimeEnvFile $runtime 2>&1
    if ($LASTEXITCODE -ne 0 -or ($plan | Out-String) -notmatch '\[PLAN\] immutable release=1\.2\.3') { throw "FAILED no-Apply plan did not pass: $($plan | Out-String)" }
    Require-Exit { & pwsh -NoProfile -File $promotion -Version '1.2.3' -ControlEnvFile $control -RuntimeEnvFile $runtime -Apply -Confirmation 'WRONG' } 'confirmation mismatch'
    Set-Content -LiteralPath $runtime -Value @('CREHN_VERSION=1.2.3', 'CREHN_SOURCE_REVISION=bad', 'CREHN_RUNTIME_ENV=prod', 'CREHN_COMPOSE_PROJECT_NAME=crehn-prod', 'CREHN_DEPLOY_ROOT=/srv/crehn-prod') -Encoding ascii
    Require-Exit { & pwsh -NoProfile -File $promotion -Version '1.2.3' -ControlEnvFile $control -RuntimeEnvFile $runtime } 'CREHN_SOURCE_REVISION must be a full revision'

    $validDotEnv = Join-Path $fixture 'safe-runtime.env'
    $bcryptFixture = '$2b$12$' + ('a' * 53)
    @('CREHN_RUNTIME_ENV=prod', 'CREHN_JWT_SECRET=Aa0+/=:_@%,-', 'ALIYUN_OSS_ENDPOINT=oss-cn-test.aliyuncs.com', "CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH=$bcryptFixture") | Set-Content -LiteralPath $validDotEnv -Encoding ascii
    $validOutput = & 'C:\Program Files\Git\bin\bash.exe' $runtimeValidator ($validDotEnv -replace '\\', '/') 2>&1
    if ($LASTEXITCODE -ne 0 -or ($validOutput | Out-String) -notmatch 'dotenv syntax is safe') { throw "FAILED strict dotenv validator rejected a safe fixture: $($validOutput | Out-String)" }
    $maliciousDotEnv = Join-Path $fixture 'malicious-runtime.env'
    @('CREHN_RUNTIME_ENV=prod', 'CREHN_JWT_SECRET=$(id)', 'ALIYUN_OSS_ENDPOINT=oss-cn-test.aliyuncs.com') | Set-Content -LiteralPath $maliciousDotEnv -Encoding ascii
    $maliciousOutput = & 'C:\Program Files\Git\bin\bash.exe' $runtimeValidator ($maliciousDotEnv -replace '\\', '/') 2>&1
    if ($LASTEXITCODE -eq 0 -or ($maliciousOutput | Out-String) -notmatch 'shell metacharacter') { throw "FAILED strict dotenv validator accepted CREHN_JWT_SECRET=$(id): $($maliciousOutput | Out-String)" }
    @('CREHN_RUNTIME_ENV=prod', 'CREHN_DB_PASSWORD=abc;touch', 'ALIYUN_OSS_ENDPOINT=oss-cn-test.aliyuncs.com') | Set-Content -LiteralPath $maliciousDotEnv -Encoding ascii
    $maliciousOutput = & 'C:\Program Files\Git\bin\bash.exe' $runtimeValidator ($maliciousDotEnv -replace '\\', '/') 2>&1
    if ($LASTEXITCODE -eq 0 -or ($maliciousOutput | Out-String) -notmatch 'shell metacharacter') { throw "FAILED strict dotenv validator accepted semicolon payload: $($maliciousOutput | Out-String)" }
    @('CREHN_RUNTIME_ENV=prod', 'CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH=replace-with-a-valid-bcrypt-hash-before-start', 'ALIYUN_OSS_ENDPOINT=oss-cn-test.aliyuncs.com') | Set-Content -LiteralPath $maliciousDotEnv -Encoding ascii
    $maliciousOutput = & 'C:\Program Files\Git\bin\bash.exe' $runtimeValidator ($maliciousDotEnv -replace '\\', '/') 2>&1
    if ($LASTEXITCODE -eq 0 -or ($maliciousOutput | Out-String) -notmatch 'complete bcrypt') { throw "FAILED strict dotenv validator accepted bcrypt placeholder: $($maliciousOutput | Out-String)" }
    @('CREHN_RUNTIME_ENV=prod', 'CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH=not-a-bcrypt-hash', 'ALIYUN_OSS_ENDPOINT=oss-cn-test.aliyuncs.com') | Set-Content -LiteralPath $maliciousDotEnv -Encoding ascii
    $maliciousOutput = & 'C:\Program Files\Git\bin\bash.exe' $runtimeValidator ($maliciousDotEnv -replace '\\', '/') 2>&1
    if ($LASTEXITCODE -eq 0 -or ($maliciousOutput | Out-String) -notmatch 'complete bcrypt') { throw "FAILED strict dotenv validator accepted invalid bcrypt hash: $($maliciousOutput | Out-String)" }
} finally {
    Remove-Item -LiteralPath $fixture -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Output '[PASS] PROD-001 immutable-promotion static gates verified'
