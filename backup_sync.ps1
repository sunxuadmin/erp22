[CmdletBinding()]
param(
    [ValidateSet('origin', 'github')]
    [string[]]$Remote = @(),

    [switch]$Apply,

    [switch]$IncludeTags,

    [string]$Confirmation
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoPath = [System.IO.Path]::GetFullPath($PSScriptRoot)
$script:logFile = $null

function Invoke-GitChecked {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& git -C $repoPath @Arguments 2>&1)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) {
        throw "Git command failed (exit=$exitCode): git $($Arguments -join ' ')"
    }
    return @($output | ForEach-Object { "$_" })
}

function Write-BackupLog {
    param([Parameter(Mandatory)][string]$Message)

    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    if ($null -ne $script:logFile) {
        Add-Content -LiteralPath $script:logFile -Value $line -Encoding utf8
    }
    Write-Output $line
}

if ($Remote.Count -eq 0) {
    throw 'At least one explicit -Remote value is required: origin or github.'
}

$selectedRemotes = @($Remote | Sort-Object -Unique)
$configuredRemotes = @(Invoke-GitChecked -Arguments @('remote'))
foreach ($remoteName in $selectedRemotes) {
    if ($configuredRemotes -notcontains $remoteName) {
        throw "Configured Git remote not found: $remoteName"
    }
}

$currentBranch = (Invoke-GitChecked -Arguments @('branch', '--show-current') | Select-Object -First 1).Trim()
if ($currentBranch -ne 'main') {
    throw "Backup push is allowed only from main; current branch is $currentBranch"
}

$headRevision = (Invoke-GitChecked -Arguments @('rev-parse', 'HEAD') | Select-Object -First 1).Trim()
$tagSuffix = if ($IncludeTags) { ':tags' } else { '' }
$expectedConfirmation = "PUSH:CREHN:main:$($selectedRemotes -join ',')$tagSuffix"

if (-not $Apply) {
    foreach ($remoteName in $selectedRemotes) {
        Write-Output "[PLAN] remote=$remoteName refspec=main:main include_tags=$($IncludeTags.IsPresent) revision=$headRevision"
    }
    Write-Output "[PLAN] rerun with -Apply -Confirmation '$expectedConfirmation' after separate push authorization"
    return
}

if ($Confirmation -cne $expectedConfirmation) {
    throw "Confirmation mismatch. Expected: $expectedConfirmation"
}

$worktreeChanges = @(Invoke-GitChecked -Arguments @('status', '--porcelain'))
if ($worktreeChanges.Count -ne 0) {
    throw 'Working tree must be clean before backup push.'
}

$logDir = Join-Path $repoPath 'backup_logs'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$script:logFile = Join-Path $logDir ("backup-{0}.log" -f (Get-Date -Format 'yyyy-MM-dd'))
Write-BackupLog '===== CREHN controlled Git synchronization started ====='

foreach ($remoteName in $selectedRemotes) {
    $remoteLines = @(Invoke-GitChecked -Arguments @('ls-remote', $remoteName, 'refs/heads/main'))
    $remoteLine = $remoteLines | Where-Object { $_ -match '\srefs/heads/main$' } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($remoteLine)) {
        throw "Remote main was not found: $remoteName"
    }
    $remoteRevision = ($remoteLine -split '\s+')[0]

    Invoke-GitChecked -Arguments @('cat-file', '-e', ("{0}^{{commit}}" -f $remoteRevision)) | Out-Null
    Invoke-GitChecked -Arguments @('merge-base', '--is-ancestor', $remoteRevision, $headRevision) | Out-Null

    Write-BackupLog "[$remoteName] pushing fast-forward main update"
    Invoke-GitChecked -Arguments @('push', $remoteName, 'main:main') |
        ForEach-Object { Write-BackupLog "[$remoteName] $_" }

    if ($IncludeTags) {
        Write-BackupLog "[$remoteName] pushing explicitly authorized tags"
        Invoke-GitChecked -Arguments @('push', $remoteName, '--tags') |
            ForEach-Object { Write-BackupLog "[$remoteName] $_" }
    }

    $readbackLines = @(Invoke-GitChecked -Arguments @('ls-remote', $remoteName, 'refs/heads/main'))
    $readbackLine = $readbackLines | Where-Object { $_ -match '\srefs/heads/main$' } | Select-Object -First 1
    $readbackRevision = if ($null -eq $readbackLine) { '' } else { ($readbackLine -split '\s+')[0] }
    if ($readbackRevision -ne $headRevision) {
        throw "Remote readback mismatch after push: $remoteName"
    }
    Write-BackupLog "[$remoteName] main readback verified revision=$headRevision"
}

Write-BackupLog '===== CREHN controlled Git synchronization completed ====='
