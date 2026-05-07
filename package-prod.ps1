param(
  [string]$OutputDir = (Join-Path $PSScriptRoot 'deploy'),
  [string]$BundleName = ('upcam-prod-' + (Get-Date -Format 'yyyyMMdd_HHmmss')),
  [switch]$SkipNodeInstall
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = $PSScriptRoot
$stagingRoot = Join-Path $OutputDir 'staging'
$bundleRoot = Join-Path $stagingRoot $BundleName
$zipFile = Join-Path $OutputDir ($BundleName + '.zip')
$isWindowsHost = $env:OS -eq 'Windows_NT'
$mavenCommand = if ($isWindowsHost) { 'mvn.cmd' } else { 'mvn' }
$npmCommand = if ($isWindowsHost) { 'npm.cmd' } else { 'npm' }

$javaFiles = @(
  @{ Source = Join-Path $repoRoot 'target\upcam-client-1.0-jar-with-dependencies.jar'; Target = 'upcam-client-1.0-jar-with-dependencies.jar' },
  @{ Source = Join-Path $repoRoot 'src\main\resources\application.example.properties'; Target = 'application.example.properties' },
  @{ Source = Join-Path $repoRoot 'src\main\resources\upcamclient.example.properties'; Target = 'upcamclient.example.properties' },
  @{ Source = Join-Path $repoRoot 'src\main\resources\log4j2.xml'; Target = 'log4j2.xml' },
  @{ Source = Join-Path $repoRoot 'upcamclient.cmd'; Target = 'upcamclient.cmd' },
  @{ Source = Join-Path $repoRoot 'upcamclient.sh'; Target = 'upcamclient.sh' }
)

$nodeFiles = @(
  @{ Source = Join-Path $repoRoot 'SnapShotter\package.json'; Target = 'SnapShotter\package.json' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\package-lock.json'; Target = 'SnapShotter\package-lock.json' }
)

$nodeSrcFiles = @(
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\SnapShotter.js'; Target = 'SnapShotter\src\SnapShotter.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\config.js'; Target = 'SnapShotter\src\config.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\dataCollector.js'; Target = 'SnapShotter\src\dataCollector.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\logger.js'; Target = 'SnapShotter\src\logger.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\motionDetector.js'; Target = 'SnapShotter\src\motionDetector.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\reolinkSnapshotClient.js'; Target = 'SnapShotter\src\reolinkSnapshotClient.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\runtimeSupervisor.js'; Target = 'SnapShotter\src\runtimeSupervisor.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\serialTaskQueue.js'; Target = 'SnapShotter\src\serialTaskQueue.js' },
  @{ Source = Join-Path $repoRoot 'SnapShotter\src\whatsappSupport.js'; Target = 'SnapShotter\src\whatsappSupport.js' }
)

$runtimeDirs = @(
  'images\received',
  'images\sent',
  'images\noise',
  'logs',
  '.state',
  '.lock'
)

function Assert-CommandExists([string]$Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command not found: $Name"
  }
}

function Remove-IfExists([string]$PathValue) {
  if (Test-Path $PathValue) {
    Remove-Item -Path $PathValue -Recurse -Force
  }
}

function Ensure-ParentDir([string]$PathValue) {
  $parent = Split-Path -Parent $PathValue
  if ($parent) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
  }
}

function Copy-StagedFile([string]$Source, [string]$RelativeTarget) {
  if (-not (Test-Path $Source)) {
    throw "Required source file missing: $Source"
  }
  $target = Join-Path $bundleRoot $RelativeTarget
  Ensure-ParentDir $target
  Copy-Item -Path $Source -Destination $target -Force
}

function New-KeepDirectory([string]$RelativePath) {
  $dir = Join-Path $bundleRoot $RelativePath
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  Set-Content -Path (Join-Path $dir '.keep') -Value '' -Encoding ascii
}

function Write-GeneratedFile([string]$RelativePath, [string]$Content) {
  $target = Join-Path $bundleRoot $RelativePath
  Ensure-ParentDir $target
  Set-Content -Path $target -Value $Content -Encoding ascii
}

Write-Host '[1/6] Running Maven package'
Assert-CommandExists $mavenCommand
Push-Location $repoRoot
try {
  & $mavenCommand clean package
  if ($LASTEXITCODE -ne 0) {
    throw "mvn clean package failed with exit code $LASTEXITCODE"
  }
} finally {
  Pop-Location
}

Write-Host '[2/6] Preparing staging area'
Remove-IfExists $OutputDir
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
New-Item -ItemType Directory -Force -Path $bundleRoot | Out-Null

Write-Host '[3/6] Copying Java runtime files'
foreach ($file in $javaFiles) {
  Copy-StagedFile $file.Source $file.Target
}

Write-Host '[4/6] Copying Node runtime files'
foreach ($file in $nodeFiles) {
  Copy-StagedFile $file.Source $file.Target
}

foreach ($file in $nodeSrcFiles) {
  Copy-StagedFile $file.Source $file.Target
}

foreach ($dir in $runtimeDirs) {
  New-KeepDirectory $dir
}

Write-GeneratedFile 'snapshotter.cmd' @'
@echo off
setlocal
cd /d %~dp0
if "%SNAPSHOTTER_NODE_MAX_OLD_SPACE_MB%"=="" set "SNAPSHOTTER_NODE_MAX_OLD_SPACE_MB=256"
if "%NODE_OPTIONS%"=="" set "NODE_OPTIONS=--max-old-space-size=%SNAPSHOTTER_NODE_MAX_OLD_SPACE_MB%"
node SnapShotter\src\SnapShotter.js %*
'@

Write-GeneratedFile 'snapshotter.sh' @'
#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
: "${SNAPSHOTTER_NODE_MAX_OLD_SPACE_MB:=256}"
if [[ " ${NODE_OPTIONS:-} " != *" --max-old-space-size="* ]]; then
  export NODE_OPTIONS="${NODE_OPTIONS:-} --max-old-space-size=${SNAPSHOTTER_NODE_MAX_OLD_SPACE_MB}"
fi
node SnapShotter/src/SnapShotter.js "$@"
'@

Write-GeneratedFile 'bundle-manifest.txt' @"
Bundle root: $BundleName

This bundle contains:
- Java ingest runtime
- Java config templates and logging config
- Java launchers
- Node motion runtime source (core files only)
- Node package manifests
- Empty runtime directories
- Generated snapshotter launchers

Notes:
- The Node dependency tree is platform-specific because of native modules such as sharp.
- Build this bundle on the same OS/architecture as the production target if you include node_modules.
- Copy application.example.properties to application.properties (or upcamclient.example.properties to upcamclient.properties) and set credentials on target host.
- Start Java with upcamclient.cmd/sh (prefers application.properties, then upcamclient.properties).
- Start Node with snapshotter.cmd/sh from the bundle root.
"@

if (-not $SkipNodeInstall) {
  Write-Host '[5/6] Installing Node runtime dependencies into staged bundle'
  Assert-CommandExists $npmCommand
  Push-Location (Join-Path $bundleRoot 'SnapShotter')
  try {
    & $npmCommand ci --omit=dev --no-fund --no-audit
    if ($LASTEXITCODE -ne 0) {
      throw "npm ci failed with exit code $LASTEXITCODE"
    }
  } finally {
    Pop-Location
  }
} else {
  Write-Host '[5/6] Skipping Node dependency installation as requested'
}

Write-Host '[6/6] Creating zip archive'
Compress-Archive -Path $bundleRoot -DestinationPath $zipFile -CompressionLevel Optimal
Remove-IfExists $stagingRoot

Write-Host ''
Write-Host 'Bundle created:'
Write-Host "  $zipFile"
