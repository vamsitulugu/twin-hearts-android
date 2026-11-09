# Copies the web app's built cinema (us-app/public/cinema) into this Android project (app/src/main/assets/cinema).
# Run it from the Android project root after ANY change to the cinema:
#     powershell -ExecutionPolicy Bypass -File tools\sync-cinema.ps1
#     powershell -ExecutionPolicy Bypass -File tools\sync-cinema.ps1 -WebRepo C:\path\to\us-app
param(
    [string]$WebRepo = (Join-Path (Split-Path -Parent $PSScriptRoot) '..\us-app'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$WebRepo = (Resolve-Path $WebRepo).Path
$src = Join-Path $WebRepo 'public\cinema'
$dst = Join-Path (Split-Path -Parent $PSScriptRoot) 'app\src\main\assets\cinema'

if (-not $SkipBuild) {
    Write-Host "Building the cinema in $WebRepo ..."
    Push-Location $WebRepo
    try { npm run build:cinema; if ($LASTEXITCODE -ne 0) { throw 'npm run build:cinema failed' } } finally { Pop-Location }
}
if (-not (Test-Path (Join-Path $src 'index.html'))) { throw "No built cinema at $src (run npm run build:cinema in the web repo)" }

New-Item -ItemType Directory -Force -Path $dst | Out-Null
# /MIR makes the copy identical (removes old hashed files); ads.txt is for the website only.
robocopy $src $dst /MIR /XF ads.txt /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy failed (code $LASTEXITCODE)" }
$mb = [math]::Round(((Get-ChildItem $dst -Recurse -File | Measure-Object Length -Sum).Sum) / 1MB, 1)
Write-Host "Done: cinema copied to app\src\main\assets\cinema ($mb MB). Rebuild the app in Android Studio."
