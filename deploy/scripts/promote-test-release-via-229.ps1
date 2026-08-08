[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Version,
    [string]$ControlEnvFile,
    [string]$RuntimeEnvFile,
    [switch]$Apply,
    [string]$Confirmation,
    [switch]$VerifyPublicEndpoint
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$deployRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($ControlEnvFile)) { $ControlEnvFile = Join-Path $deployRoot '.env' }
if ([string]::IsNullOrWhiteSpace($RuntimeEnvFile)) { $RuntimeEnvFile = Join-Path $deployRoot 'env\prod.env' }

function Stop-Blocked([string]$Message) { throw "BLOCKED $Message" }
function Read-DotEnv([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { Stop-Blocked "environment file does not exist: $Path" }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) { Stop-Blocked "invalid environment line in $Path" }
        $name = $trimmed.Substring(0, $separator)
        $value = $trimmed.Substring($separator + 1)
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) { $value = $value.Substring(1, $value.Length - 2) }
        $values[$name] = $value
    }
    return $values
}
function Require-Value([hashtable]$Values, [string]$Name) {
    $value = [string]$Values[$Name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value.Contains('replace-with')) { Stop-Blocked "missing or placeholder $Name" }
    return $value
}
function Require-SafeReleaseVersion([string]$Value) {
    if ($Value -notmatch '^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$') { Stop-Blocked 'Version must be a release-style immutable tag' }
}
function Require-Sha256([string]$Value, [string]$Name) {
    if ($Value -notmatch '^[0-9a-f]{64}$') { Stop-Blocked "$Name must be a lowercase SHA-256 value" }
}
function Get-Target([hashtable]$Control, [string]$Prefix) {
    $hostName = Require-Value $Control "${Prefix}_SSH_HOST"
    $portText = Require-Value $Control "${Prefix}_SSH_PORT"
    $userName = Require-Value $Control "${Prefix}_SSH_USER"
    $auth = Require-Value $Control "${Prefix}_SSH_AUTH"
    $keyPath = Require-Value $Control "${Prefix}_SSH_KEY_PATH"
    $fingerprint = Require-Value $Control "${Prefix}_HOST_FINGERPRINT"
    $port = 0
    if ($hostName -notmatch '^[A-Za-z0-9.-]+$' -or $userName -notmatch '^[A-Za-z0-9_.-]+$' -or -not [int]::TryParse($portText, [ref]$port) -or $port -lt 1 -or $port -gt 65535) { Stop-Blocked "invalid ${Prefix} SSH endpoint" }
    if ($auth -ne 'key') { Stop-Blocked "${Prefix}_SSH_AUTH must be key for immutable promotion" }
    if ($fingerprint -notmatch '^SHA256:[A-Za-z0-9+/]{20,}={0,2}$') { Stop-Blocked "${Prefix}_HOST_FINGERPRINT must be a confirmed SHA256 fingerprint" }
    if (-not (Test-Path -LiteralPath $keyPath -PathType Leaf)) { Stop-Blocked "${Prefix} SSH key file is unavailable" }
    return @{ Host = $hostName; Port = $port; User = $userName; KeyPath = (Resolve-Path -LiteralPath $keyPath).Path; Fingerprint = $fingerprint }
}
function Test-ProdRuntime([hashtable]$Runtime) {
    if ((Require-Value $Runtime 'CREHN_RUNTIME_ENV') -ne 'prod' -or (Require-Value $Runtime 'CREHN_COMPOSE_PROJECT_NAME') -ne 'crehn-prod' -or (Require-Value $Runtime 'CREHN_DEPLOY_ROOT') -ne '/srv/crehn-prod') { Stop-Blocked 'production runtime must be prod, crehn-prod, /srv/crehn-prod' }
    $runtimeVersion = Require-Value $Runtime 'CREHN_VERSION'
    Require-SafeReleaseVersion $runtimeVersion
    if ($runtimeVersion -ne $Version) { Stop-Blocked 'production runtime CREHN_VERSION does not equal requested promotion Version' }
    $revision = Require-Value $Runtime 'CREHN_SOURCE_REVISION'
    if ($revision -notmatch '^[0-9a-fA-F]{40,64}$') { Stop-Blocked 'production runtime CREHN_SOURCE_REVISION must be a full revision' }
    return $revision
}
function ConvertTo-SshConfigPath([string]$Path, [string]$Label) {
    if ([string]::IsNullOrWhiteSpace($Path) -or $Path.Contains('"') -or $Path.Contains("`r") -or $Path.Contains("`n")) { Stop-Blocked "$Label contains an unsafe OpenSSH config path" }
    # Win32 OpenSSH accepts absolute C:/ paths. Quote to preserve spaces while
    # rejecting control characters and quotes that could alter ssh_config syntax.
    return '"' + $Path.Replace('\', '/') + '"'
}
function New-SshConfig([hashtable]$TestTarget, [hashtable]$ProdTarget) {
    $directory = Join-Path ([System.IO.Path]::GetTempPath()) ('crehn-promotion-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $directory | Out-Null
    $jumpKnownHosts = Join-Path $directory 'jump_known_hosts'
    $prodKnownHosts = Join-Path $directory 'prod_known_hosts'
    $config = Join-Path $directory 'ssh_config'
    $jumpKnownHostsConfig = ConvertTo-SshConfigPath $jumpKnownHosts 'jump known_hosts path'
    $prodKnownHostsConfig = ConvertTo-SshConfigPath $prodKnownHosts 'production known_hosts path'
    $testKeyConfig = ConvertTo-SshConfigPath $TestTarget.KeyPath 'TEST SSH identity path'
    $prodKeyConfig = ConvertTo-SshConfigPath $ProdTarget.KeyPath 'production SSH identity path'
    @(
        'Host crehn-test-jump',
        "  HostName $($TestTarget.Host)",
        "  Port $($TestTarget.Port)",
        "  User $($TestTarget.User)",
        "  IdentityFile $testKeyConfig",
        '  IdentitiesOnly yes',
        '  BatchMode yes',
        '  StrictHostKeyChecking accept-new',
        "  UserKnownHostsFile $jumpKnownHostsConfig",
        '  GlobalKnownHostsFile NUL',
        '  ConnectTimeout 15',
        'Host crehn-prod-target',
        "  HostName $($ProdTarget.Host)",
        "  Port $($ProdTarget.Port)",
        "  User $($ProdTarget.User)",
        "  IdentityFile $prodKeyConfig",
        '  IdentitiesOnly yes',
        '  BatchMode yes',
        '  StrictHostKeyChecking accept-new',
        "  UserKnownHostsFile $prodKnownHostsConfig",
        '  GlobalKnownHostsFile NUL',
        '  ConnectTimeout 15',
        '  ProxyJump crehn-test-jump'
    ) | Set-Content -LiteralPath $config -Encoding ascii
    return @{ Directory = $directory; JumpKnownHosts = $jumpKnownHosts; ProdKnownHosts = $prodKnownHosts; Config = $config }
}
function Confirm-HostFingerprint([hashtable]$Ssh, [string]$Alias, [string]$Fingerprint) {
    $knownHosts = switch ($Alias) {
        'crehn-test-jump' { $Ssh.JumpKnownHosts; break }
        'crehn-prod-target' { $Ssh.ProdKnownHosts; break }
        default { Stop-Blocked "unknown SSH alias for fingerprint confirmation: $Alias" }
    }
    & ssh -F $Ssh.Config $Alias exit 2>$null
    $fingerprints = if (Test-Path -LiteralPath $knownHosts) { & ssh-keygen -lf $knownHosts -E sha256 } else { @() }
    if ($LASTEXITCODE -ne 0 -and -not ($fingerprints -match [regex]::Escape($Fingerprint))) { Stop-Blocked "unable to verify $Alias host fingerprint" }
    if (-not ($fingerprints -match [regex]::Escape($Fingerprint))) { Stop-Blocked "$Alias host fingerprint does not match the confirmed value" }
}
function Invoke-Checked([string]$Executable, [string[]]$Arguments, [string]$Failure) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "FAILED $Failure (exit=$LASTEXITCODE)" }
}
function Copy-RemoteFile([hashtable]$Ssh, [string]$Source, [string]$Destination) {
    Invoke-Checked 'scp' @('-F', $Ssh.Config, $Source, $Destination) 'controlled SSH copy failed'
}
function Read-PromotionMetadata([string]$Path) {
    $values = Read-DotEnv $Path
    $expected = @('version', 'source_revision', 'architecture', 'manifest_sha256', 'release_sha256', 'deploy_assets_sha256')
    if ($values.Keys.Count -ne $expected.Count -or @($values.Keys | Where-Object { $_ -notin $expected }).Count -ne 0) { Stop-Blocked 'export metadata fields are invalid' }
    Require-SafeReleaseVersion (Require-Value $values 'version')
    if ((Require-Value $values 'source_revision') -notmatch '^[0-9a-fA-F]{40,64}$' -or (Require-Value $values 'architecture') -ne 'x86_64') { Stop-Blocked 'export metadata revision or architecture is invalid' }
    foreach ($name in @('manifest_sha256', 'release_sha256', 'deploy_assets_sha256')) { Require-Sha256 (Require-Value $values $name) $name }
    return $values
}
function Test-DeploymentAssetArchive([string]$Path) {
    if (-not (Get-Command tar -ErrorAction SilentlyContinue)) { Stop-Blocked 'tar is required to validate deployment assets' }
    $entries = & tar -tzf $Path
    if ($LASTEXITCODE -ne 0 -or -not $entries) { Stop-Blocked 'deployment assets archive cannot be read' }
    foreach ($entry in $entries) {
        if ($entry -notmatch '^deploy(/|$)' -or $entry.StartsWith('/') -or $entry.Contains('..') -or $entry.Contains("`r") -or $entry.Contains("`n")) { Stop-Blocked 'deployment assets archive contains an unsafe path' }
    }
}
function Test-PublicEndpoint([hashtable]$Control) {
    $domain = Require-Value $Control 'CREHN_PROD_PUBLIC_DOMAIN'
    $expectedIp = Require-Value $Control 'CREHN_PROD_EXPECTED_IPV4'
    if ($domain -notmatch '^[A-Za-z0-9.-]+$' -or $expectedIp -notmatch '^(?:25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})(?:\.(?:25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})){3}$') { Stop-Blocked 'production public domain or expected IPv4 is invalid' }
    $records = @(Resolve-DnsName -Name $domain -Type A -ErrorAction Stop | Where-Object { $_.IPAddress } | Select-Object -ExpandProperty IPAddress)
    if ($expectedIp -notin $records) { throw "FAILED DNS A record does not include the expected production IPv4 for $domain" }
    $response = Invoke-WebRequest -Uri "https://$domain/healthz" -UseBasicParsing -TimeoutSec 20
    if ($response.StatusCode -ne 200 -or $response.Content.Trim() -ne 'ok') { throw 'FAILED HTTPS /healthz contract did not return 200 ok' }
    Write-Output "[PASS] public_dns_https_contract domain=$domain"
}

Require-SafeReleaseVersion $Version
$control = Read-DotEnv $ControlEnvFile
$runtime = Read-DotEnv $RuntimeEnvFile
$runtimeRevision = Test-ProdRuntime $runtime
$testTarget = Get-Target $control 'CREHN_TEST'
$prodTarget = Get-Target $control 'CREHN_ECS2'
$expectedConfirmation = "PROMOTE:crehn-prod:$Version"
if (-not $Apply) {
    Write-Output "[PLAN] immutable release=$Version source=229-test export gate transport=OpenSSH ProxyJump target=crehn-prod"
    Write-Output '[PLAN] remote writes are blocked; add -Apply with the exact promotion confirmation after separate server authorization'
    if ($VerifyPublicEndpoint) { Write-Output '[PLAN] public DNS/HTTPS validation is deferred until after a successful applied promotion' }
    exit 0
}
if ($Confirmation -ne $expectedConfirmation) { Stop-Blocked "confirmation mismatch; expected $expectedConfirmation" }
foreach ($command in @('ssh', 'scp', 'ssh-keygen', 'tar')) { if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { Stop-Blocked "required command is unavailable: $command" } }

$ssh = New-SshConfig $testTarget $prodTarget
$workspace = Join-Path ([System.IO.Path]::GetTempPath()) ('crehn-promotion-artifacts-' + [Guid]::NewGuid().ToString('N'))
try {
    New-Item -ItemType Directory -Path $workspace | Out-Null
    Confirm-HostFingerprint $ssh 'crehn-test-jump' $testTarget.Fingerprint
    Confirm-HostFingerprint $ssh 'crehn-prod-target' $prodTarget.Fingerprint
    Invoke-Checked 'ssh' @('-F', $ssh.Config, 'crehn-test-jump', 'sudo', '-n', '/usr/local/sbin/crehn-test-release-export-gate', 'export', '--apply', $expectedConfirmation, $Version) 'TEST export gate failed'
    $testExport = "/srv/crehn-test/exports/$Version"
    $localFiles = @{
        'promotion.env' = Join-Path $workspace 'promotion.env'
        "crehn-images-$Version.json" = Join-Path $workspace 'crehn-images.json'
        "crehn-images-$Version.tar" = Join-Path $workspace 'crehn-images.tar'
        "crehn-deploy-$Version.tar.gz" = Join-Path $workspace 'crehn-deploy-assets.tar.gz'
        'SUCCESS' = Join-Path $workspace 'SUCCESS'
    }
    foreach ($name in $localFiles.Keys) { Copy-RemoteFile $ssh "crehn-test-jump:$testExport/$name" $localFiles[$name] }
    if (-not (Test-Path -LiteralPath $localFiles['SUCCESS'] -PathType Leaf)) { Stop-Blocked 'TEST export SUCCESS marker is missing' }
    $metadata = Read-PromotionMetadata $localFiles['promotion.env']
    if ($metadata.version -ne $Version -or $metadata.source_revision -ne $runtimeRevision) { Stop-Blocked 'TEST export version or revision does not match the production runtime' }
    foreach ($pair in @(@($localFiles["crehn-images-$Version.json"], $metadata.manifest_sha256), @($localFiles["crehn-images-$Version.tar"], $metadata.release_sha256), @($localFiles["crehn-deploy-$Version.tar.gz"], $metadata.deploy_assets_sha256))) {
        if ((Get-FileHash -LiteralPath $pair[0] -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pair[1]) { Stop-Blocked 'downloaded promotion artifact SHA-256 mismatch' }
    }
    & (Join-Path $scriptRoot 'verify-release.ps1') -ManifestPath $localFiles["crehn-images-$Version.json"] -ExpectedVersion $Version -ExpectedRevision $runtimeRevision
    Test-DeploymentAssetArchive $localFiles["crehn-deploy-$Version.tar.gz"]

    $prodInbox = "/home/$($prodTarget.User)/crehn-prod-inbox"
    $uploads = @{
        $localFiles['promotion.env'] = "$prodInbox/promotion.env"
        $localFiles["crehn-images-$Version.json"] = "$prodInbox/crehn-images.json"
        $localFiles["crehn-images-$Version.tar"] = "$prodInbox/crehn-images.tar"
        $localFiles["crehn-deploy-$Version.tar.gz"] = "$prodInbox/crehn-deploy-assets.tar.gz"
        (Resolve-Path -LiteralPath $RuntimeEnvFile).Path = "$prodInbox/crehn-prod-runtime.env"
    }
    foreach ($upload in $uploads.GetEnumerator()) { Copy-RemoteFile $ssh $upload.Key "crehn-prod-target:$($upload.Value)" }
    Invoke-Checked 'ssh' @('-F', $ssh.Config, 'crehn-prod-target', 'sudo', '-n', '/usr/local/sbin/crehn-prod-deploy-gate', 'prepare', '--apply', $expectedConfirmation) 'production inbox preparation gate failed'
    Invoke-Checked 'ssh' @('-F', $ssh.Config, 'crehn-prod-target', 'sudo', '-n', '/usr/local/sbin/crehn-prod-deploy-gate', 'stage', '--apply', $expectedConfirmation) 'production staging gate failed'
    Invoke-Checked 'ssh' @('-F', $ssh.Config, 'crehn-prod-target', 'sudo', '-n', '/usr/local/sbin/crehn-prod-deploy-gate', 'deploy', '--apply', $expectedConfirmation) 'production deployment gate failed'
    if ($VerifyPublicEndpoint) { Test-PublicEndpoint $control } else { Write-Output '[NOT_RUN] public DNS/HTTPS verification requires -VerifyPublicEndpoint after independently configured TLS and network exposure' }
    Write-Output "[PASS] immutable promotion completed version=$Version revision=$runtimeRevision"
} finally {
    if ($workspace) { Remove-Item -LiteralPath $workspace -Recurse -Force -ErrorAction SilentlyContinue }
    if ($ssh -and $ssh.Directory) { Remove-Item -LiteralPath $ssh.Directory -Recurse -Force -ErrorAction SilentlyContinue }
}
