$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..\..\..')).Path
$validator = Get-Content -LiteralPath (Join-Path $repoRoot 'backend\ruoyi-modules\ruoyi-crehn\src\main\java\org\dromara\crehn\cms\service\impl\PortalHomeConfigurationValidator.java') -Raw
$portalMain = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\main.ts') -Raw
$canvas = Get-Content -LiteralPath (Join-Path $repoRoot 'portal\src\components\competition-home\CompetitionHomeCanvas.vue') -Raw
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
}

if ($portalMain -notmatch 'homeComponents: Array\.isArray\(homePayload\.data\.components\)') {
    throw 'published component snapshot is not wired to portal runtime config'
}
if ($validator -notmatch 'COMPETITION_CONFIG_KEYS' -or $validator -notmatch '布局组件标识重复') {
    throw 'server-side component field or duplicate-key validation is missing'
}
if ($cmsIndex -notmatch 'const selectLayout = async' -or $cmsIndex -notmatch 'if \(await applyLayoutComponents\(layout\)\) selectedLayout\.value = layout;') {
    throw 'CMS layout selection must load its component snapshot before changing the selected layout'
}

Write-Output "PASS: PORTAL-005 registered $($requiredTypes.Count) component types across backend, CMS, and portal runtime."
