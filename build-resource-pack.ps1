# Zip the resource pack (run from project root)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$zipName = "Dead-Air-Pip-Boy-Radio-Conversion-1.0.0.zip"
$zipPath = Join-Path $outDir $zipName
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

$stage = Join-Path $env:TEMP ("dead_air_pip_pack_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $stage | Out-Null
try {
  Copy-Item (Join-Path $root "pack.mcmeta") $stage -Force
  Copy-Item (Join-Path $root "assets") $stage -Recurse -Force
  if (Test-Path (Join-Path $root "pack.png")) {
    Copy-Item (Join-Path $root "pack.png") $stage -Force
  }
  Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $zipPath -Force
  Write-Host "Wrote $zipPath"
} finally {
  Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
}

# Copy into common CurseForge resourcepacks folders when present
$targets = @(
  "$env:USERPROFILE\curseforge\minecraft\Instances\C.Ideas\resourcepacks",
  "$env:USERPROFILE\curseforge\minecraft\Instances\dead air tests\resourcepacks"
)
foreach ($t in $targets) {
  if (Test-Path $t) {
    Copy-Item $zipPath (Join-Path $t $zipName) -Force
    Write-Host "Copied to $t"
  }
}
