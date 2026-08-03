[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        'PreflightLocal',
        'StageTestAssets',
        'BuildLocal',
        'DeployLocal',
        'VerifyLocal',
        'PreflightProd',
        'StageProdAssets',
        'DeployProd',
        'DatabasePlan',
        'DatabaseInitialize',
        'Rollback',
        'BackupMaintenance'
    )]
    [string]$Action,

    [string]$ControlEnvFile,
    [string]$RuntimeEnvFile,
    [string]$ReleaseManifest,
    [string]$BackupId,
    [ValidateSet('Test', 'Prod')]
    [string]$Environment = 'Test',
    [switch]$Apply,
    [string]$Confirmation
)

$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptRoot '..\..')).Path
$deployRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($ControlEnvFile)) {
    $ControlEnvFile = Join-Path $deployRoot '.env'
}
$linuxDeployScript = Join-Path $deployRoot 'linux\crehn-deploy.sh'
$linuxInstallScript = Join-Path $deployRoot 'linux\install-assets.sh'

function Write-Status {
    param([string]$Level, [string]$Message)
    Write-Output "[$Level] $Message"
}

function Read-DotEnv {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "BLOCKED environment file does not exist: $Path"
    }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) {
            continue
        }
        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            throw "BLOCKED invalid environment line in $Path"
        }
        $name = $trimmed.Substring(0, $separator)
        $value = $trimmed.Substring($separator + 1)
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$name] = $value
    }
    return $values
}

function Require-Value {
    param(
        [hashtable]$Values,
        [string]$Name,
        [switch]$AllowPlaceholder
    )
    $value = [string]$Values[$Name]
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "BLOCKED missing $Name"
    }
    if (-not $AllowPlaceholder -and $value.Contains('replace-with')) {
        throw "BLOCKED placeholder remains in $Name"
    }
    return $value
}

function Test-ReleaseStyleVersion {
    param([string]$Version)
    if ($Version -notmatch '^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$') {
        throw 'BLOCKED CREHN_VERSION is not a release-style immutable tag'
    }
}

function Resolve-TargetContext {
    param([bool]$Production)

    $control = Read-DotEnv -Path $ControlEnvFile
    $prefix = if ($Production) { 'CREHN_ECS2' } else { 'CREHN_TEST' }
    $hostName = Require-Value $control "${prefix}_SSH_HOST"
    $portText = Require-Value $control "${prefix}_SSH_PORT"
    $userName = Require-Value $control "${prefix}_SSH_USER"
    $auth = Require-Value $control "${prefix}_SSH_AUTH"
    $fingerprint = Require-Value $control "${prefix}_HOST_FINGERPRINT"
    $keyPath = [string]$control["${prefix}_SSH_KEY_PATH"]
    $password = [string]$control["${prefix}_SSH_PASSWORD"]

    $port = 0
    if (-not [int]::TryParse($portText, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
        throw "BLOCKED invalid ${prefix}_SSH_PORT"
    }
    if ($hostName -notmatch '^[A-Za-z0-9.-]+$' -or $userName -notmatch '^[A-Za-z0-9_.-]+$') {
        throw 'BLOCKED SSH host or user contains unsupported characters'
    }
    if ($fingerprint -notmatch '^SHA256:[A-Za-z0-9+/]{20,}={0,2}$') {
        throw "BLOCKED ${prefix}_HOST_FINGERPRINT must be a confirmed SHA256 fingerprint"
    }
    if ($auth -notin @('key', 'password')) {
        throw "BLOCKED ${prefix}_SSH_AUTH must be key or password"
    }
    if ($auth -eq 'key') {
        if ([string]::IsNullOrWhiteSpace($keyPath) -or -not (Test-Path -LiteralPath $keyPath -PathType Leaf)) {
            throw "BLOCKED SSH key does not exist: $keyPath"
        }
        $keyPath = (Resolve-Path -LiteralPath $keyPath).Path
    } elseif ([string]::IsNullOrEmpty($password)) {
        throw "BLOCKED ${prefix}_SSH_PASSWORD is empty"
    }

    return @{
        Host = $hostName
        Port = $port
        User = $userName
        Auth = $auth
        KeyPath = $keyPath
        Password = $password
        Fingerprint = $fingerprint
    }
}

function New-SshSessionArguments {
    param([hashtable]$Target)

    foreach ($commandName in @('ssh', 'scp', 'ssh-keygen')) {
        if (-not (Get-Command $commandName -ErrorAction SilentlyContinue)) {
            throw "BLOCKED required OpenSSH command is unavailable: $commandName"
        }
    }

    $knownHosts = Join-Path ([System.IO.Path]::GetTempPath()) ("crehn-known-hosts-" + [Guid]::NewGuid().ToString('N'))
    $probeArgs = @(
        '-p', [string]$Target.Port,
        '-o', 'IdentitiesOnly=yes',
        '-o', 'StrictHostKeyChecking=accept-new',
        '-o', "UserKnownHostsFile=$knownHosts",
        '-o', 'GlobalKnownHostsFile=NUL',
        '-o', 'ConnectTimeout=10',
        '-o', 'BatchMode=yes',
        '-o', 'PreferredAuthentications=none',
        '-o', 'PubkeyAuthentication=no',
        '-o', 'PasswordAuthentication=no',
        '-o', 'KbdInteractiveAuthentication=no',
        "$($Target.User)@$($Target.Host)",
        'exit'
    )
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # The unauthenticated probe is expected to write diagnostics and exit nonzero.
        $ErrorActionPreference = 'Continue'
        $null = & ssh @probeArgs 2>$null
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if (-not (Test-Path -LiteralPath $knownHosts) -or
        (Get-Item -LiteralPath $knownHosts).Length -eq 0) {
        Remove-Item -LiteralPath $knownHosts -Force -ErrorAction SilentlyContinue
        throw "BLOCKED unable to read SSH host keys from $($Target.Host):$($Target.Port)"
    }
    $fingerprints = & ssh-keygen -lf $knownHosts -E sha256
    if ($LASTEXITCODE -ne 0 -or -not ($fingerprints -match [regex]::Escape($Target.Fingerprint))) {
        Remove-Item -LiteralPath $knownHosts -Force -ErrorAction SilentlyContinue
        throw "BLOCKED SSH host fingerprint does not match the confirmed value"
    }

    $common = @(
        '-o', 'IdentitiesOnly=yes',
        '-o', 'StrictHostKeyChecking=yes',
        '-o', "UserKnownHostsFile=$knownHosts",
        '-o', 'ConnectTimeout=10'
    )
    if ($Target.Auth -eq 'key') {
        $common += @('-o', 'BatchMode=yes', '-i', $Target.KeyPath)
    } else {
        $common += @(
            '-o', 'BatchMode=no',
            '-o', 'PreferredAuthentications=password',
            '-o', 'PubkeyAuthentication=no'
        )
    }
    return @{
        KnownHosts = $knownHosts
        Common = $common
    }
}

function Invoke-WithSshAuth {
    param(
        [hashtable]$Target,
        [scriptblock]$Operation
    )

    $oldAskPass = $env:SSH_ASKPASS
    $oldAskPassRequire = $env:SSH_ASKPASS_REQUIRE
    $oldDisplay = $env:DISPLAY
    $oldPassword = $env:CREHN_SSH_PASSWORD
    $askPassFile = $null
    try {
        if ($Target.Auth -eq 'password') {
            $askPassFile = Join-Path ([System.IO.Path]::GetTempPath()) ("crehn-askpass-" + [Guid]::NewGuid().ToString('N') + '.cmd')
            @(
                '@echo off',
                'powershell.exe -NoProfile -Command "[Console]::Out.Write($env:CREHN_SSH_PASSWORD)"'
            ) | Set-Content -LiteralPath $askPassFile -Encoding Ascii
            $env:CREHN_SSH_PASSWORD = $Target.Password
            $env:SSH_ASKPASS = $askPassFile
            $env:SSH_ASKPASS_REQUIRE = 'force'
            $env:DISPLAY = 'crehn-ssh'
        }
        & $Operation
    } finally {
        if ($askPassFile) {
            Remove-Item -LiteralPath $askPassFile -Force -ErrorAction SilentlyContinue
        }
        $env:SSH_ASKPASS = $oldAskPass
        $env:SSH_ASKPASS_REQUIRE = $oldAskPassRequire
        $env:DISPLAY = $oldDisplay
        $env:CREHN_SSH_PASSWORD = $oldPassword
    }
}

function Invoke-SshCommand {
    param(
        [hashtable]$Target,
        [hashtable]$Session,
        [string[]]$RemoteArguments,
        [string]$StandardInputFile
    )
    $sshArgs = @('-p', [string]$Target.Port) + $Session.Common +
        @("$($Target.User)@$($Target.Host)") + $RemoteArguments
    Invoke-WithSshAuth -Target $Target -Operation {
        $sshExitCode = $null
        if ($StandardInputFile) {
            $standardInput = [System.IO.File]::ReadAllText($StandardInputFile).Replace("`r`n", "`n")
            if ($standardInput.Length -gt 0 -and $standardInput[0] -eq [char]0xFEFF) {
                $standardInput = $standardInput.Substring(1)
            }
            $standardInput = $standardInput.TrimEnd([char[]]"`r`n") + "`n#"
            $standardInputTemp = Join-Path ([System.IO.Path]::GetTempPath()) ("crehn-ssh-stdin-" + [Guid]::NewGuid().ToString('N') + '.sh')
            try {
                [System.IO.File]::WriteAllText($standardInputTemp, $standardInput, [System.Text.UTF8Encoding]::new($false))
                $sshProcess = Start-Process -FilePath 'ssh' -ArgumentList $sshArgs -RedirectStandardInput $standardInputTemp -NoNewWindow -PassThru -Wait
                $sshExitCode = $sshProcess.ExitCode
            } finally {
                Remove-Item -LiteralPath $standardInputTemp -Force -ErrorAction SilentlyContinue
            }
        } else {
            & ssh @sshArgs
            $sshExitCode = $LASTEXITCODE
        }
        if ($sshExitCode -ne 0) {
            throw "FAILED remote command exited with code $sshExitCode"
        }
    }
}

function Copy-SshFile {
    param(
        [hashtable]$Target,
        [hashtable]$Session,
        [string]$LocalPath,
        [string]$RemotePath
    )
    $scpArgs = @('-P', [string]$Target.Port) + $Session.Common +
        @($LocalPath, "$($Target.User)@$($Target.Host):$RemotePath")
    Invoke-WithSshAuth -Target $Target -Operation {
        & scp @scpArgs
        if ($LASTEXITCODE -ne 0) {
            throw "FAILED scp exited with code $LASTEXITCODE"
        }
    }
}

function Require-ApplyConfirmation {
    param([string]$Expected)
    if (-not $Apply) {
        throw "BLOCKED this action changes remote state; add -Apply after separate authorization"
    }
    if ($Confirmation -ne $Expected) {
        throw "BLOCKED confirmation mismatch; expected $Expected"
    }
}

function Get-RuntimeContext {
    param([bool]$Production)
    if ([string]::IsNullOrWhiteSpace($RuntimeEnvFile)) {
        $relativeEnvPath = if ($Production) { 'env\prod.env' } else { 'env\test.env' }
        $script:RuntimeEnvFile = Join-Path $deployRoot $relativeEnvPath
    }
    $resolved = (Resolve-Path -LiteralPath $RuntimeEnvFile).Path
    $values = Read-DotEnv -Path $resolved
    $expectedEnvironment = if ($Production) { 'prod' } else { 'test' }
    if ((Require-Value $values 'CREHN_RUNTIME_ENV') -ne $expectedEnvironment) {
        throw "BLOCKED runtime environment must be $expectedEnvironment"
    }
    $project = Require-Value $values 'CREHN_COMPOSE_PROJECT_NAME'
    $remoteDeployRoot = Require-Value $values 'CREHN_DEPLOY_ROOT'
    $version = Require-Value $values 'CREHN_VERSION'
    $revision = Require-Value $values 'CREHN_SOURCE_REVISION'
    if ($project -notmatch '^crehn-(test|prod|stage)$' -or
        $remoteDeployRoot -notmatch '^/srv/crehn-(test|prod|stage)$') {
        throw 'BLOCKED runtime project name or deployment root is outside the CREHN boundary'
    }
    Test-ReleaseStyleVersion $version
    $requiresImmutableRevision = $Action -notin @('PreflightLocal', 'PreflightProd')
    if ($requiresImmutableRevision -and $revision -notmatch '^[0-9a-fA-F]{40,64}$') {
        throw 'BLOCKED CREHN_SOURCE_REVISION must be a full Git revision'
    }
    return @{
        Path = $resolved
        Values = $values
        Project = $project
        DeployRoot = $remoteDeployRoot
        Version = $version
        Revision = $revision
        StagedRemoteEnv = "$remoteDeployRoot/runtime/$expectedEnvironment-$version.env"
        ActiveRemoteEnv = "$remoteDeployRoot/runtime/$expectedEnvironment.env"
        RemoteScript = "$remoteDeployRoot/source/deploy/linux/crehn-deploy.sh"
    }
}

function Invoke-Preflight {
    param(
        [bool]$Production,
        [hashtable]$Target,
        [hashtable]$Session,
        [hashtable]$Runtime
    )
    $values = $Runtime.Values
    $ports = if ($Production) {
        Require-Value $values 'CREHN_HTTP_PORT'
    } else {
        @(
            Require-Value $values 'CREHN_HTTP_PORT'
            Require-Value $values 'CREHN_MINIO_API_PORT'
            Require-Value $values 'CREHN_MINIO_CONSOLE_PORT'
        ) -join ','
    }
    $remoteAction = if ($Production) { 'preflight-prod' } else { 'preflight-local' }
    $arguments = @(
        'bash', '-s', '--', $remoteAction,
        '--project-name', $Runtime.Project,
        '--deploy-root', $Runtime.DeployRoot,
        '--ports', $ports
    )
    if (-not $Production) {
        $arguments += @(
            '--protected-project', (Require-Value $values 'CREHN_PROTECTED_COMPOSE_PROJECT'),
            '--protected-web-url', (Require-Value $values 'CREHN_PROTECTED_WEB_URL'),
            '--protected-storage-url', (Require-Value $values 'CREHN_PROTECTED_STORAGE_HEALTH_URL')
        )
    }
    Invoke-SshCommand -Target $Target -Session $Session -RemoteArguments $arguments -StandardInputFile $linuxDeployScript
}

function New-SourceArchive {
    param(
        [bool]$Production,
        [string]$Revision
    )
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw 'BLOCKED Git is required to stage an exact committed source revision'
    }
    $insideWorkTree = & git -C $projectRoot rev-parse --is-inside-work-tree 2>$null
    if ($LASTEXITCODE -ne 0 -or $insideWorkTree -ne 'true') {
        throw 'BLOCKED CREHN is not a Git work tree; an immutable source revision cannot be staged'
    }
    $resolvedRevision = & git -C $projectRoot rev-parse "$Revision^{commit}" 2>$null
    if ($LASTEXITCODE -ne 0 -or $resolvedRevision -notmatch '^[0-9a-fA-F]{40,64}$') {
        throw "BLOCKED source revision is not present in the CREHN repository: $Revision"
    }
    if ($resolvedRevision.ToLowerInvariant() -ne $Revision.ToLowerInvariant()) {
        throw 'BLOCKED runtime CREHN_SOURCE_REVISION must equal the full committed revision'
    }
    foreach ($secretPath in @('deploy/.env', 'deploy/env/test.env', 'deploy/env/prod.env')) {
        & git -C $projectRoot ls-files --error-unmatch -- $secretPath 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            throw "BLOCKED secret environment file is tracked by Git: $secretPath"
        }
    }

    $archive = Join-Path ([System.IO.Path]::GetTempPath()) ("crehn-assets-" + [Guid]::NewGuid().ToString('N') + '.tar.gz')
    $paths = if ($Production) {
        @('deploy')
    } else {
        @('backend', 'frontend', 'portal', 'deploy', '.dockerignore')
    }
    $gitOutput = & git -c core.autocrlf=false -C $projectRoot archive --format=tar.gz --output=$archive $resolvedRevision -- @paths 2>&1
    if ($LASTEXITCODE -ne 0) {
        Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue
        throw "FAILED git archive could not stage revision $resolvedRevision`: $gitOutput"
    }
    return $archive
}

function Stage-Assets {
    param(
        [bool]$Production,
        [hashtable]$Target,
        [hashtable]$Session,
        [hashtable]$Runtime
    )
    Require-ApplyConfirmation "STAGE:$($Runtime.Project):$($Runtime.Revision)"
    $archive = New-SourceArchive -Production $Production -Revision $Runtime.Revision
    $remoteTemp = "/tmp/crehn-stage-$([Guid]::NewGuid().ToString('N'))"
    $manifestPath = $null
    $releaseArchivePath = $null
    try {
        Invoke-SshCommand -Target $Target -Session $Session -RemoteArguments @(
            'mkdir', '-m', '700', '--', $remoteTemp
        )
        Copy-SshFile -Target $Target -Session $Session -LocalPath $archive -RemotePath "$remoteTemp/assets.tar.gz"
        Copy-SshFile -Target $Target -Session $Session -LocalPath $Runtime.Path -RemotePath "$remoteTemp/runtime.env"

        $installArguments = @(
            'bash', '-s', '--',
            '--mode', $(if ($Production) { 'prod-release' } else { 'test-source' }),
            '--deploy-root', $Runtime.DeployRoot,
            '--archive', "$remoteTemp/assets.tar.gz",
            '--runtime-env', "$remoteTemp/runtime.env",
            '--version', $Runtime.Version,
            '--source-revision', $Runtime.Revision
        )
        if ($Production) {
            if ([string]::IsNullOrWhiteSpace($ReleaseManifest)) {
                throw 'BLOCKED -ReleaseManifest is required for production staging'
            }
            $manifestPath = (Resolve-Path -LiteralPath $ReleaseManifest).Path
            $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
            if ($manifest.version -ne $Runtime.Version -or $manifest.sourceRevision -ne $Runtime.Revision) {
                throw 'BLOCKED release manifest does not match production runtime version/revision'
            }
            & (Join-Path $scriptRoot 'verify-release.ps1') `
                -ManifestPath $manifestPath `
                -ExpectedVersion $Runtime.Version `
                -ExpectedRevision $Runtime.Revision
            if ($manifest.architecture -ne 'x86_64') {
                throw 'BLOCKED release architecture does not match the required ECS 2 x86_64 architecture'
            }
            $releaseArchivePath = Join-Path (Split-Path $manifestPath -Parent) $manifest.archive
            if (-not (Test-Path -LiteralPath $releaseArchivePath -PathType Leaf)) {
                throw "BLOCKED release archive does not exist: $releaseArchivePath"
            }
            $actualHash = (Get-FileHash -LiteralPath $releaseArchivePath -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($actualHash -ne $manifest.sha256) {
                throw 'BLOCKED release archive SHA-256 does not match the manifest'
            }
            Copy-SshFile -Target $Target -Session $Session -LocalPath $manifestPath -RemotePath "$remoteTemp/release.json"
            Copy-SshFile -Target $Target -Session $Session -LocalPath $releaseArchivePath -RemotePath "$remoteTemp/release.tar"
            $installArguments += @(
                '--release-manifest', "$remoteTemp/release.json",
                '--release-archive', "$remoteTemp/release.tar"
            )
        }
        $remoteInstallArguments = if ($Production) {
            $installArguments
        } else {
            @('sudo', '-n') + $installArguments
        }
        Invoke-SshCommand -Target $Target -Session $Session -RemoteArguments $remoteInstallArguments -StandardInputFile $linuxInstallScript
        Write-Status PASS "assets staged for $($Runtime.Project); no build, container, or SQL action was implied"
    } finally {
        Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue
        try {
            Invoke-SshCommand -Target $Target -Session $Session -RemoteArguments @(
                'rm', '-rf', '--', $remoteTemp
            )
        } catch {
            Write-Status WARN "remote temporary directory may require manual cleanup: $remoteTemp"
        }
    }
}

function Invoke-InstalledAction {
    param(
        [hashtable]$Target,
        [hashtable]$Session,
        [hashtable]$Runtime,
        [string]$RemoteAction
    )
    $usesStagedEnvironment = $RemoteAction -in @('build-local', 'deploy-local', 'deploy-prod') -or
        ($RemoteAction -eq 'database' -and $Action -in @('DatabasePlan', 'DatabaseInitialize'))
    $remoteEnvironmentFile = if ($usesStagedEnvironment) {
        $Runtime.StagedRemoteEnv
    } else {
        $Runtime.ActiveRemoteEnv
    }
    $arguments = @($Runtime.RemoteScript, $RemoteAction, '--env-file', $remoteEnvironmentFile)
    if ($Apply) {
        $arguments += '--apply'
    }
    if (-not [string]::IsNullOrWhiteSpace($Confirmation)) {
        $arguments += @('--confirmation', $Confirmation)
    }
    $needsRelease = $RemoteAction -in @('deploy-local', 'deploy-prod') -or
        ($RemoteAction -eq 'database' -and $Action -eq 'DatabaseInitialize' -and $Runtime.Values['CREHN_RUNTIME_ENV'] -eq 'prod')
    if (-not [string]::IsNullOrWhiteSpace($ReleaseManifest) -or $needsRelease) {
        if (-not [string]::IsNullOrWhiteSpace($ReleaseManifest) -and $ReleaseManifest -match '^/') {
            $remoteManifest = $ReleaseManifest
        } else {
            $remoteManifest = "$($Runtime.DeployRoot)/releases/$($Runtime.Version)/crehn-images-$($Runtime.Version).json"
        }
        $arguments += @('--release-manifest', $remoteManifest)
    }
    if (-not [string]::IsNullOrWhiteSpace($BackupId)) {
        $arguments += @('--backup-id', $BackupId)
    }
    if ($Action -eq 'DatabaseInitialize') {
        $arguments += @('--database-operation', 'initialize')
    } elseif ($Action -eq 'DatabasePlan') {
        $arguments += @('--database-operation', 'plan')
    }
    if (-not $production -and $RemoteAction -in @('build-local', 'deploy-local')) {
        $arguments = @('sudo', '-n') + $arguments
    }
    Invoke-SshCommand -Target $Target -Session $Session -RemoteArguments $arguments
}

$production = $Action -in @('PreflightProd', 'StageProdAssets', 'DeployProd') -or $Environment -eq 'Prod'
if ($Action -in @('PreflightLocal', 'StageTestAssets', 'BuildLocal', 'DeployLocal', 'VerifyLocal') -and $production) {
    throw "BLOCKED action $Action is TEST-only"
}
if ($Action -in @('PreflightProd', 'StageProdAssets', 'DeployProd') -and -not $production) {
    throw "BLOCKED action $Action is PROD-only"
}
$target = Resolve-TargetContext -Production $production
$runtime = Get-RuntimeContext -Production $production
$session = New-SshSessionArguments -Target $target
try {
    switch ($Action) {
        'PreflightLocal' {
            Invoke-Preflight -Production $false -Target $target -Session $session -Runtime $runtime
        }
        'PreflightProd' {
            Invoke-Preflight -Production $true -Target $target -Session $session -Runtime $runtime
        }
        'StageTestAssets' {
            Stage-Assets -Production $false -Target $target -Session $session -Runtime $runtime
        }
        'StageProdAssets' {
            Stage-Assets -Production $true -Target $target -Session $session -Runtime $runtime
        }
        'BuildLocal' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'build-local'
        }
        'DeployLocal' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'deploy-local'
        }
        'VerifyLocal' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'verify-local'
        }
        'DeployProd' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'deploy-prod'
        }
        'DatabasePlan' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'database'
        }
        'DatabaseInitialize' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'database'
        }
        'Rollback' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'rollback'
        }
        'BackupMaintenance' {
            Invoke-InstalledAction -Target $target -Session $session -Runtime $runtime -RemoteAction 'backup-maintenance'
        }
    }
    Write-Status PASS "action=$Action completed"
} catch {
    Write-Status FAILED $_.Exception.Message
    throw
} finally {
    Remove-Item -LiteralPath $session.KnownHosts -Force -ErrorAction SilentlyContinue
}
