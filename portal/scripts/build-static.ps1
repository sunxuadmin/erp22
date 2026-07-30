param(
  [string]$OutputDir = "release\art",
  [string]$ZipPath = "release\art-static.zip",
  [switch]$SkipZip
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$ReleaseRoot = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot "release"))
$OutputRoot = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot $OutputDir))
$DistRoot = Join-Path $ProjectRoot "dist"
$ReleaseRootPrefix = $ReleaseRoot.TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) + [System.IO.Path]::DirectorySeparatorChar

if ($OutputRoot -eq $ReleaseRoot -or -not $OutputRoot.StartsWith($ReleaseRootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "OutputDir must stay inside release directory: $OutputRoot"
}

if (-not (Test-Path -LiteralPath (Join-Path $DistRoot "index.html"))) {
  throw "Missing Vite build output: $DistRoot. Run npm run build first."
}

if (-not (Test-Path -LiteralPath (Join-Path $DistRoot "assets"))) {
  throw "Missing Vite assets directory: $DistRoot\assets"
}

if (-not (Test-Path -LiteralPath (Join-Path $DistRoot "config.js"))) {
  throw "Missing runtime config: $DistRoot\config.js"
}

$LegacyAssetConfig = Join-Path $DistRoot "assets\config.js"
if (Test-Path -LiteralPath $LegacyAssetConfig) {
  throw "Duplicate runtime config detected: $LegacyAssetConfig. Keep public/config.js as the only runtime config source."
}

if (Test-Path -LiteralPath $OutputRoot) {
  Remove-Item -LiteralPath $OutputRoot -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
Copy-Item -LiteralPath (Join-Path $DistRoot "index.html") -Destination $OutputRoot
Copy-Item -LiteralPath (Join-Path $DistRoot "config.js") -Destination $OutputRoot
Copy-Item -LiteralPath (Join-Path $DistRoot "assets") -Destination $OutputRoot -Recurse

$RuntimeConfigFiles = @(Get-ChildItem -LiteralPath $OutputRoot -Recurse -File -Filter "config.js")
if ($RuntimeConfigFiles.Count -ne 1 -or $RuntimeConfigFiles[0].FullName -ne (Join-Path $OutputRoot "config.js")) {
  throw "Static output must contain exactly one runtime config at $OutputRoot\config.js"
}

if (-not $SkipZip) {
  $ZipFullPath = [System.IO.Path]::GetFullPath((Join-Path $ProjectRoot $ZipPath))
  if (-not $ZipFullPath.StartsWith($ReleaseRootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "ZipPath must stay inside release directory: $ZipFullPath"
  }
  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ZipFullPath) | Out-Null
  if (Test-Path -LiteralPath $ZipFullPath) {
    Remove-Item -LiteralPath $ZipFullPath -Force
  }
  Compress-Archive -Path (Join-Path $OutputRoot "*") -DestinationPath $ZipFullPath -Force
  Write-Host "Static zip: $ZipFullPath"
}

Write-Host "Static art output: $OutputRoot"
