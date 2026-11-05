param(
    [string]$Tag = "data-center-all-in-one:3.4.3",
    [string]$DsHome = $env:DS_HOME,
    [string]$StHome = $env:ST_HOME,
    [string]$DataApiJar = $env:DATA_API_JAR
)

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $DsHome) { $DsHome = "D:\HPG\workspace\LHYF\apache-dolphinscheduler-3.4.3-bin" }
if (-not $StHome) { $StHome = "D:\HPG\workspace\LHYF\apache-seatunnel-3.0.0" }
if (-not $DataApiJar) {
    $DataApiJar = Join-Path $Root "dolphinscheduler\dolphinscheduler-data-api\target\dolphinscheduler-data-api-3.4.3.jar"
}
$MagicDir = Join-Path $Root "dolphinscheduler\dolphinscheduler-data-api\data\magic-api"

foreach ($p in @($DsHome, $StHome, $DataApiJar, $MagicDir)) {
    if (-not (Test-Path $p)) { throw "missing artifact: $p" }
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker is not installed or not on PATH"
}

$Build = Join-Path $PSScriptRoot "build"
Remove-Item $Build -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $Build | Out-Null

Write-Host "staging DolphinScheduler..."
Copy-Item $DsHome (Join-Path $Build "ds") -Recurse
Write-Host "staging SeaTunnel..."
Copy-Item $StHome (Join-Path $Build "seatunnel") -Recurse
Write-Host "staging data-api..."
New-Item -ItemType Directory -Path (Join-Path $Build "data-api\data") -Force | Out-Null
Copy-Item $DataApiJar (Join-Path $Build "data-api\dolphinscheduler-data-api.jar")
Copy-Item $MagicDir (Join-Path $Build "data-api\data\magic-api") -Recurse

# DS OpenMetadata task plugin + rebuilt UI (if present)
$PluginJar = Join-Path $Root "dolphinscheduler\dolphinscheduler-task-plugin\dolphinscheduler-task-openmetadata\target\dolphinscheduler-task-openmetadata-3.4.3.jar"
if (Test-Path $PluginJar) {
    New-Item -ItemType Directory -Path (Join-Path $Build "ds\plugins\task-plugins") -Force | Out-Null
    Copy-Item $PluginJar (Join-Path $Build "ds\plugins\task-plugins")
    Write-Host "staged OpenMetadata task plugin"
}
$UiDist = Join-Path $Root "dolphinscheduler\dolphinscheduler-ui\dist"
if (Test-Path $UiDist) {
    Remove-Item (Join-Path $Build "ds\ui") -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item $UiDist (Join-Path $Build "ds\ui") -Recurse
    Write-Host "staged rebuilt DolphinScheduler UI"
}

# runtime logs are not part of the image
Get-ChildItem $Build -Recurse -Directory -Filter logs -ErrorAction SilentlyContinue | ForEach-Object {
    Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}
Get-ChildItem $Build -Recurse -File -Include *.log, gc-*.log -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

Push-Location $PSScriptRoot
try {
    docker build -t $Tag .
    if ($LASTEXITCODE -ne 0) { throw "docker build failed" }
} finally {
    Pop-Location
}

Write-Host "built $Tag"
Write-Host "run: docker compose -f deploy/all-in-one/docker-compose.yml up -d"
