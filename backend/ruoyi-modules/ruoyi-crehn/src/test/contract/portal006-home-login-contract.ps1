$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..\..\..')).Path
$canvas = Get-Content -LiteralPath (Join-Path $repositoryRoot 'portal\src\components\competition-home\CompetitionHomeCanvas.vue') -Raw
$login = Get-Content -LiteralPath (Join-Path $repositoryRoot 'frontend\src\views\login.vue') -Raw
$config = Get-Content -LiteralPath (Join-Path $repositoryRoot 'frontend\src\config\loginPage.ts') -Raw

function Assert-Contains([string]$content, [string]$expected, [string]$label) {
  if (-not $content.Contains($expected)) {
    throw "${label}: expected content was not found: $expected"
  }
}

function Assert-NotContains([string]$content, [string]$unexpected, [string]$label) {
  if ($content.Contains($unexpected)) {
    throw "${label}: obsolete content is still reachable: $unexpected"
  }
}

Assert-Contains $canvas 'function goToLogin()' 'portal login action'
Assert-Contains $canvas '/admin/login?redirect=/index&homeVersion=${props.version}' 'portal login action'
Assert-Contains $canvas '@click="goToLogin"' 'portal login button'

Assert-Contains $login "query.homeVersion === 'v2' ? 'v2' : 'v1'" 'home version allowlist'
Assert-Contains $login '.login-page--v1' 'v1 visual theme'
Assert-Contains $login '.login-page--v2' 'v2 visual theme'
Assert-Contains $login '@media (prefers-reduced-motion: reduce)' 'reduced motion fallback'
Assert-Contains $login '<span class="brand-mark" aria-hidden="true">CH</span>' 'code-native brand mark'
Assert-NotContains $login '@/assets/logo/logo.png' 'legacy login logo'
Assert-NotContains $login '.brand-logo' 'legacy login logo styles'
Assert-NotContains $login ':global(html.dark)' 'legacy dark theme override'
Assert-Contains $login 'window.location.href = `${portalHomePath}?version=${homeVersion.value}`' 'portal return action'
Assert-Contains $login 'const safeInternalRedirect' 'redirect validation'
Assert-Contains $login "value.startsWith('//')" 'redirect validation'
Assert-Contains $login 'await router.push(redirect.value)' 'safe redirect use'
Assert-Contains $login 'v-if="!showRegisterCard"' 'direct login form'
Assert-NotContains $login 'selectedEntry' 'login entry selector'
Assert-NotContains $login 'entry-picker' 'login entry selector'
Assert-NotContains $login "portalHomePath = '/portal/'" 'portal return path'

Assert-Contains $config "brandName: '创意河南'" 'login brand'
Assert-Contains $config "subtitle: '设计创新·赋能河南'" 'login slogan'
Assert-Contains $config "description: '全省高等学校第六届“创意河南”艺术设计大赛'" 'login event name'
Assert-NotContains $config 'entryCards' 'login entry selector configuration'
Assert-NotContains $config '上报入口' 'login entry selector configuration'

Write-Host 'PASS: PORTAL-006 home-to-login contract'
