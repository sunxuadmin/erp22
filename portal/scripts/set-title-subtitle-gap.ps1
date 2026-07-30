param(
  [ValidateRange(-1, 120)]
  [int]$Gap = -1,

  [ValidateRange(0, 120)]
  [int]$V1Gap = 29,

  [ValidateRange(0, 120)]
  [int]$V2Gap = 17,

  [switch]$SkipZip
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Gap -ge 0) {
  $V1Gap = $Gap
  $V2Gap = $Gap
}

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$ReleaseRoot = Join-Path $ProjectRoot "release\art"
$ZipPath = Join-Path $ProjectRoot "release\art-static.zip"
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$StyleId = "art-title-gap-override"
$StylePattern = '(?s)\s*<style\s+id="' + [regex]::Escape($StyleId) + '">.*?</style>'
$StyleBlock = @"
    <style id="$StyleId">
      :root {
        --art-v1-title-subtitle-gap: ${V1Gap}px;
        --art-v2-title-subtitle-gap: ${V2Gap}px;
      }
      .page-version-v1 .hero-subtitle {
        margin-top: var(--art-v1-title-subtitle-gap) !important;
      }
      .page-version-v2 .version-two-heading p {
        margin-top: var(--art-v2-title-subtitle-gap) !important;
      }
    </style>
"@

function Set-TitleGapOverride {
  param([Parameter(Mandatory = $true)][string]$Path)

  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    return $false
  }

  $Content = [System.IO.File]::ReadAllText($Path)
  if ([regex]::IsMatch($Content, $StylePattern)) {
    $Updated = [regex]::Replace($Content, $StylePattern, "`r`n$StyleBlock", 1)
  } else {
    $HeadClose = $Content.IndexOf("</head>", [System.StringComparison]::OrdinalIgnoreCase)
    if ($HeadClose -lt 0) {
      throw "Missing </head> in $Path"
    }
    $Updated = $Content.Insert($HeadClose, "$StyleBlock`r`n  ")
  }

  [System.IO.File]::WriteAllText($Path, $Updated, $Utf8NoBom)
  Write-Host "Updated: $Path"
  return $true
}

$Targets = @(
  (Join-Path $ProjectRoot "index.html"),
  (Join-Path $ProjectRoot "dist\index.html"),
  (Join-Path $ReleaseRoot "index.html")
)

$UpdatedCount = 0
foreach ($Target in $Targets) {
  if (Set-TitleGapOverride -Path $Target) {
    $UpdatedCount += 1
  }
}

if ($UpdatedCount -eq 0) {
  throw "No index.html target was found."
}

if (-not $SkipZip -and (Test-Path -LiteralPath $ReleaseRoot -PathType Container)) {
  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ZipPath) | Out-Null
  if (Test-Path -LiteralPath $ZipPath) {
    Remove-Item -LiteralPath $ZipPath -Force
  }
  Compress-Archive -Path (Join-Path $ReleaseRoot "*") -DestinationPath $ZipPath -Force
  Write-Host "Updated zip: $ZipPath"
}

Write-Host "Title gaps applied: v1=${V1Gap}px, v2=${V2Gap}px"
