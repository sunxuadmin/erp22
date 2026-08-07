$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..\..\..')).Path
$validator = Get-Content -LiteralPath (Join-Path $repoRoot 'backend\ruoyi-modules\ruoyi-crehn\src\main\java\org\dromara\crehn\cms\service\impl\PortalHomeConfigurationValidator.java') -Raw
$portalMain = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\main.ts') -Raw
$canvas = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\components\competition-home\CompetitionHomeCanvas.vue') -Raw
$canvasCss = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\components\competition-home\CompetitionHomeCanvas.css') -Raw
$page = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\components\competition-home\CompetitionHomePage.vue') -Raw
$fallback = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\components\competition-home\competitionHomeFallback.ts') -Raw
$modules = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\config\competitionHomeModules.ts') -Raw
$registry = Get-Content -LiteralPath (Join-Path $repoRoot 'frontend\src\views\crehn\cms\competitionHomeRegistry.ts') -Raw
$cmsIndex = Get-Content -LiteralPath (Join-Path $repoRoot 'frontend\src\views\crehn\cms\index.vue') -Raw

$requiredTypes = @(
    'competition-nav', 'competition-hero', 'competition-key-facts', 'competition-tracks',
    'competition-art-tech', 'competition-journey', 'competition-file-specs', 'competition-timeline',
    'competition-notice-downloads', 'competition-contact', 'competition-footer'
)

foreach ($type in $requiredTypes) {
    if ($validator -notmatch [regex]::Escape('"' + $type + '"')) { throw "validator missing $type" }
    if ($registry -notmatch [regex]::Escape("type: '$type'")) { throw "registry missing $type" }
    if ($canvas -notmatch [regex]::Escape("module.componentType === '$type'")) { throw "renderer missing $type" }
    if ($fallback -notmatch [regex]::Escape('"' + $type + '"')) { throw "fallback missing $type" }
}

$requiredFields = @(
    'navItems', 'loginLabel', 'menuOpenLabel', 'portalModalEyebrow', 'portalModalPrompt', 'noticeModalRules',
    'artText', 'yearText', 'editionText', 'competitionTitle', 'trackCardValue', 'statusLabel', 'axisStart', 'axisTech',
    'documentTitle', 'deadlineAt', 'invalidCountdownValue', 'countdownLabel', 'fact1Label', 'trackAMark', 'trackAEnglish', 'trackAQuota', 'trackADirections', 'trackBDirections',
    'deskTitle', 'deskSummary', 'stepPrefix', 'axisLabels', 'noticeActionLabel', 'noticeArtMark', 'noticeEyebrow', 'organizers', 'brandMark', 'signature', 'versionV1Label', 'versionV2Label', 'backTopLabel'
)

foreach ($field in $requiredFields) {
    if ($registry -notmatch [regex]::Escape("'$field'")) { throw "registry field missing $field" }
    if ($validator -notmatch [regex]::Escape('"' + $field + '"')) { throw "validator field missing $field" }
}

if ($canvas -match 'v-html') { throw 'competition renderer must not use v-html' }
if ($canvas -notmatch 'if \(configured === undefined\) return fallback') { throw 'renderer must distinguish missing fields from explicit empty strings' }
if ($canvas -notmatch 'componentType\?: string' -or $canvas -notmatch '"competition-nav"' -or $canvas -notmatch "value\(navModule, 'portalModalTitle', '', 'competition-nav'\)") { throw 'missing nav component must resolve controlled fallback dialog copy' }
if ($canvas -notmatch '@click="scrollTo\(''journey''\)"' -or $canvas -notmatch 'version === ''v2'' \? scrollTo\(''tracks''\) : scrollTo\(''notice''\)' -or $canvas -notmatch 'scrollTo\(version === ''v1'' \? ''timeline'' : ''tracks''\)') { throw 'reference hero and fact actions are not version-correct' }
if ($canvas -notmatch 'emit\(''change-version'', ''v1''\)' -or $canvas -notmatch 'versionV1Label' -or $canvas -notmatch 'versionV2Label' -or $canvas -notmatch 'v-if="value\(module, ''brandMark''\)"' -or $canvasCss -notmatch 'grid-template-columns: 1fr auto auto') { throw 'footer version switch, empty mark semantics, or layout is missing' }
if ($canvas -match "competition-art-tech' && version === 'v2'" -or $canvas -notmatch 'art-tech-section--\$\{version\}') { throw 'explicitly enabled v1 art-tech must remain renderable' }
if ($canvas -notmatch 'handleDialogKeydown' -or $canvas -notmatch 'dialogCloseButton' -or $canvas -notmatch 'previousFocusedElement' -or $canvas -notmatch 'portalDialogAction' -or $canvas -notmatch 'scrollTo\("contact"\)' -or $canvas -notmatch 'v-if="value\(module, ''noticeArtMark''\)"') { throw 'dialog accessibility, portal follow-up action, or notice empty semantics is missing' }
if ($canvasCss -notmatch 'competition-canvas--v2 \.hero-artwork \{ transform: translate\(-50%, -50%\) rotate\(-2deg\); \}') { throw 'reduced-motion baseline must preserve the v2 artwork transform' }
if ($portalMain -notmatch 'homeComponents: Array\.isArray\(homePayload\.data\.components\)') {
    throw 'published component snapshot is not wired to portal runtime config'
}
if ($cmsIndex -notmatch 'const selectLayout = async' -or $cmsIndex -notmatch 'if \(await applyLayoutComponents\(layout\)\) selectedLayout\.value = layout;') {
    throw 'CMS layout selection must load its component snapshot before changing the selected layout'
}
if ($page -notmatch 'hasPublishedCompetitionHomeSnapshot \? competitionHomeModules : createCompetitionHomeFallback' -or $modules -notmatch 'export const hasPublishedCompetitionHomeSnapshot = Array\.isArray\(runtimeComponents\)') {
    throw 'published empty or all-disabled snapshots must not fall back to the default page'
}
if ($canvas -notmatch 'gridColumn: `\$\{module\.gridX \+ 1\} / span \$\{module\.gridW\}`' -or $canvas -notmatch 'gridRow: `\$\{module\.gridY \+ 1\} / span \$\{module\.gridH\}`' -or $canvasCss -notmatch 'grid-auto-rows') { throw 'portal renderer must consume gridX gridY gridW and gridH' }
if ($registry -notmatch 'versionDefaultEnabled: \{ v1: v1Enabled, v2: v2Enabled \}' -or $registry -notmatch 'versioned\(v1ArtTech, v2ArtTech, false, true\)' -or $fallback -notmatch 'componentType !== "competition-art-tech"') { throw 'v1 art-tech enablement must match the reference package' }
if ($registry -notmatch "countdownLabel: 'DAYS'" -or $registry -notmatch "stepPrefix: 'STEP'" -or $registry -notmatch "noticeArtMark: 'CH\\n2026'" -or $registry -notmatch "brandMark: 'CH'" -or $fallback -notmatch 'stepPrefix: isV2 \? "STEP" : ""' -or $fallback -notmatch 'noticeArtMark: isV2 \? "CH\\n2026" : ""' -or $fallback -notmatch 'brandMark: "CH"') { throw 'version-specific visible text must remain configuration driven in presets and fallback' }
if ($canvas -notmatch 'deadlineAt' -or $canvas -notmatch 'Math\.ceil\(\(deadline - Date\.now\(\)\) / 86_400_000\)' -or $registry -notmatch '2026-10-01T00:00:00\+08:00') { throw 'controlled deadline countdown is missing' }
if ($cmsIndex -notmatch 'versionDefaultConfigs\[componentFormVersion\.value\]' -or $cmsIndex -notmatch 'label="门户显示"') { throw 'CMS must expose enabled state and version-specific type defaults' }
if ($canvasCss -notmatch 'competition-canvas--v1' -or $canvasCss -notmatch 'competition-canvas--v2') {
    throw 'both reference visual renderers are not present'
}
if ($canvasCss -notmatch '@supports not' -or $canvasCss -notmatch 'prefers-reduced-motion') {
    throw 'backdrop-filter fallback or reduced-motion support is missing'
}

Add-Type -AssemblyName System.Drawing
$images = @{
    'creative-henan-hero.png' = @{ Width = 1448; Height = 1086 }
    'arttech-hero.png' = @{ Width = 1584; Height = 990 }
}
foreach ($name in $images.Keys) {
    $path = Join-Path $repoRoot "portal\public\$name"
    if (-not (Test-Path -LiteralPath $path)) { throw "missing portal image $name" }
    $image = [System.Drawing.Image]::FromFile($path)
    try {
        if ($image.Width -ne $images[$name].Width -or $image.Height -ne $images[$name].Height) {
            throw "unexpected dimensions for ${name}: $($image.Width)x$($image.Height)"
        }
    } finally {
        $image.Dispose()
    }
}

Write-Output "PASS: PORTAL-005 keeps $($requiredTypes.Count) components, synchronized editable-field guards, one shared renderer, and both verified reference images."
