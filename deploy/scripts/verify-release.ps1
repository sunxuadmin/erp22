param(
    [Parameter(Mandatory = $true)]
    [string]$ManifestPath,
    [string]$ExpectedVersion,
    [string]$ExpectedRevision
)
$ErrorActionPreference = 'Stop'
$manifestFile = (Resolve-Path -LiteralPath $ManifestPath).Path
$manifest = Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json
$required = @('schemaVersion', 'version', 'sourceRevision', 'architecture', 'archive', 'sha256', 'images')
foreach ($property in $required) {
    if ($null -eq $manifest.$property -or [string]::IsNullOrWhiteSpace([string]$manifest.$property)) {
        throw "发布清单缺少字段：$property"
    }
}
if ($manifest.schemaVersion -ne 1) { throw "不支持的发布清单版本：$($manifest.schemaVersion)" }
if ($manifest.version -notmatch '^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$') {
    throw "版本标签无效：$($manifest.version)"
}
if ($manifest.sourceRevision -notmatch '^[0-9a-fA-F]{40,64}$') {
    throw "源码修订号无效：$($manifest.sourceRevision)"
}
if ($manifest.architecture -notin @('x86_64', 'aarch64')) {
    throw "不支持的镜像架构：$($manifest.architecture)"
}
if ($manifest.archive -notmatch '^crehn-images-[0-9A-Za-z.-]+\.tar$') {
    throw "归档文件名无效：$($manifest.archive)"
}
if ($manifest.sha256 -notmatch '^[0-9a-f]{64}$') { throw '清单中的 SHA-256 格式无效' }
if ($ExpectedVersion -and $manifest.version -ne $ExpectedVersion) {
    throw "版本不一致：expected=$ExpectedVersion, actual=$($manifest.version)"
}
if ($ExpectedRevision -and $manifest.sourceRevision -ne $ExpectedRevision) {
    throw "修订号不一致：expected=$ExpectedRevision, actual=$($manifest.sourceRevision)"
}
$archive = Join-Path (Split-Path $manifestFile -Parent) $manifest.archive
if (-not (Test-Path -LiteralPath $archive)) { throw "镜像归档不存在：$archive" }
$actual = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $manifest.sha256) { throw "SHA-256 不一致：expected=$($manifest.sha256), actual=$actual" }
foreach ($image in $manifest.images) {
    if ([string]::IsNullOrWhiteSpace([string]$image.reference) -or
        [string]::IsNullOrWhiteSpace([string]$image.id) -or
        $image.id -notmatch '^sha256:[0-9a-f]{64}$') {
        throw '镜像清单包含无效的 reference 或 image ID'
    }
}
Write-Output "[PASS] VERIFIED version=$($manifest.version) revision=$($manifest.sourceRevision) arch=$($manifest.architecture) sha256=$actual"
