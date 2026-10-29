# Adds the Meet Planner and Places wiring to YOUR project files, without replacing them.
# Safe to run twice. Makes a .bak copy of every file it changes.
# Run from the project root (the folder that has "app" and "gradlew.bat"):
#   powershell -ExecutionPolicy Bypass -File tools\apply_wiring.ps1
$ErrorActionPreference = 'Stop'
$src = Join-Path (Get-Location).Path 'app\src\main\java\com\twinhearts\app'
if (-not (Test-Path $src)) { Write-Host "Run this from the project root folder." -ForegroundColor Red; exit 1 }
$enc = New-Object System.Text.UTF8Encoding($false)
function Read-Text($p) { [IO.File]::ReadAllText($p) }
function Save-Text($p, $t) { Copy-Item $p "$p.bak" -Force; [IO.File]::WriteAllText($p, $t, $enc); Write-Host "  changed: $p" -ForegroundColor Green }

# 1) Routes.kt: add the two route objects
$routes = Get-ChildItem $src -Recurse -Filter Routes.kt | Select-Object -First 1
if (-not $routes) { Write-Host "Routes.kt not found" -ForegroundColor Red } else {
  $t = Read-Text $routes.FullName; $add = ''
  if ($t -notmatch 'data object MeetPlanner') { $add += "    @Serializable data object MeetPlanner`r`n" }
  if ($t -notmatch 'data object Places')      { $add += "    @Serializable data object Places`r`n" }
  if ($add) {
    $rx = New-Object System.Text.RegularExpressions.Regex('(object\s+Routes\s*\{[^\r\n]*\r?\n)')
    $new = $rx.Replace($t, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $m.Groups[1].Value + $add }, 1)
    if ($new -eq $t) { Write-Host "  could not edit Routes.kt - add: @Serializable data object MeetPlanner / Places" -ForegroundColor Yellow } else { Save-Text $routes.FullName $new }
  } else { Write-Host "  Routes.kt already done" }
}

# 2) The file that registers the feature graphs (the line that calls homeGraph(...))
$graphFile = Get-ChildItem $src -Recurse -Filter *.kt | Where-Object {
  $c = Read-Text $_.FullName; ($c -match '(?m)^\s*homeGraph\(\w+\)') -and ($c -notmatch 'fun\s+NavGraphBuilder\.homeGraph')
} | Select-Object -First 1
if (-not $graphFile) { Write-Host "  graph file not found - add: meetPlannerGraph(navController) and placesGraph(navController)" -ForegroundColor Yellow } else {
  $t = Read-Text $graphFile.FullName
  $m = [regex]::Match($t, '(?m)^(\s*)homeGraph\((\w+)\)[^\r\n]*\r?\n')
  $indent = $m.Groups[1].Value; $nav = $m.Groups[2].Value; $add = ''; $imports = ''
  if ($t -notmatch 'meetPlannerGraph') { $add += "${indent}meetPlannerGraph($nav) // Meet Planner`r`n"; $imports += "import com.twinhearts.app.feature.meetplanner.ui.meetPlannerGraph`r`n" }
  if ($t -notmatch 'placesGraph')      { $add += "${indent}placesGraph($nav) // Places`r`n";             $imports += "import com.twinhearts.app.feature.places.ui.placesGraph`r`n" }
  if ($add) {
    $t = $t.Insert($m.Index + $m.Length, $add)
    $rx2 = New-Object System.Text.RegularExpressions.Regex('(?m)^(package [^\r\n]+\r?\n)')
    $t = $rx2.Replace($t, [System.Text.RegularExpressions.MatchEvaluator]{ param($x) $x.Groups[1].Value + "`r`n" + $imports }, 1)
    Save-Text $graphFile.FullName $t
  } else { Write-Host "  graph file already done" }
}

# 3) HomeRoute.kt: the chips open the new screens
$home = Get-ChildItem $src -Recurse -Filter HomeRoute.kt | Select-Object -First 1
if (-not $home) { Write-Host "  HomeRoute.kt not found" -ForegroundColor Yellow } else {
  $t = Read-Text $home.FullName
  $nav = 'navController'; $nm = [regex]::Match($t, '(\w+)\.navigate\(Routes\.'); if ($nm.Success) { $nav = $nm.Groups[1].Value }
  $add = ''
  if ($t -notmatch 'HomeShortcut\.MeetPlanner\s*->') { $add += "                    HomeShortcut.MeetPlanner -> $nav.navigate(Routes.MeetPlanner)`r`n" }
  if ($t -notmatch 'HomeShortcut\.Places\s*->')      { $add += "                    HomeShortcut.Places -> $nav.navigate(Routes.Places)`r`n" }
  if ($add) {
    $em = [regex]::Match($t, '(?m)^[ \t]*else\s*->')
    if (-not $em.Success) { Write-Host "  could not edit HomeRoute.kt - add the two HomeShortcut lines above 'else ->'" -ForegroundColor Yellow }
    else { Save-Text $home.FullName ($t.Insert($em.Index, $add)) }
  } else { Write-Host "  HomeRoute.kt already done" }
}
Write-Host "Done. Now Sync Gradle and build." -ForegroundColor Cyan
