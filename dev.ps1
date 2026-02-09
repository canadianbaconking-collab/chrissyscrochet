param(
  [switch]$NoPull,
  [switch]$NoInstall,
  [switch]$NoLaunch,
  [switch]$Logcat,
  [switch]$Clean,
  [string]$Package # optional override
)

$ErrorActionPreference = "Stop"

function Require-RepoRoot {
  if (-not (Test-Path ".\.git")) {
    throw "Not in repo root (no .git found). cd to the project root and try again."
  }
  if (-not (Test-Path ".\gradlew") -and -not (Test-Path ".\gradlew.bat")) {
    throw "No gradlew/gradlew.bat found. Run this from the Android project root."
  }
}

function Resolve-GradleWrapper {
  if (Test-Path ".\gradlew") { return ".\gradlew" }
  if (Test-Path ".\gradlew.bat") { return ".\gradlew.bat" }
  throw "Gradle wrapper not found."
}

function Get-AndroidPackageName {
  param([string]$Override)

  if ($Override -and $Override.Trim().Length -gt 0) {
    return $Override.Trim()
  }

  $pathsToTry = @(
    ".\app\build.gradle.kts",
    ".\app\build.gradle"
  ) | Where-Object { Test-Path $_ }

  # Helper: parse a quoted string after a key
  function Parse-QuotedValueFromFile($filePath, $regex) {
    try {
      $content = Get-Content -Raw -Path $filePath
      $m = [regex]::Match($content, $regex)
      if ($m.Success) { return $m.Groups["v"].Value }
    } catch { }
    return $null
  }

  # 1) Prefer explicit applicationId (most correct for launch)
  foreach ($p in $pathsToTry) {
    $v = Parse-QuotedValueFromFile $p 'applicationId\s*=\s*"(?<v>[^"]+)"'
    if ($v) { return $v }

    # Groovy form: applicationId "com.foo.bar"
    $v = Parse-QuotedValueFromFile $p 'applicationId\s+"(?<v>[^"]+)"'
    if ($v) { return $v }
  }

  # 2) Fallback to namespace (often matches package, but not always)
  foreach ($p in $pathsToTry) {
    $v = Parse-QuotedValueFromFile $p 'namespace\s*=\s*"(?<v>[^"]+)"'
    if ($v) { return $v }

    # Groovy form: namespace "com.foo.bar"
    $v = Parse-QuotedValueFromFile $p 'namespace\s+"(?<v>[^"]+)"'
    if ($v) { return $v }
  }

  # 3) Last resort: parse AndroidManifest package attr (older projects)
  $manifest = ".\app\src\main\AndroidManifest.xml"
  if (Test-Path $manifest) {
    $v = Parse-QuotedValueFromFile $manifest 'package\s*=\s*"(?<v>[^"]+)"'
    if ($v) { return $v }
  }

  return $null
}

function Has-AdbDevice {
  try {
    $out = & adb devices 2>$null
    return ($out | Select-String "`tdevice") -ne $null
  } catch {
    return $false
  }
}

Write-Host "== Chrissy's Crochet: dev v2 =="
Require-RepoRoot
$gradlew = Resolve-GradleWrapper

# git sync
if (-not $NoPull) {
  Write-Host "== git pull --rebase =="
  git pull --rebase
}

if ($Clean) {
  Write-Host "== gradle clean =="
  & $gradlew clean
}

# tests + build
Write-Host "== gradle test =="
& $gradlew test

Write-Host "== gradle assembleDebug =="
& $gradlew assembleDebug

# install
if (-not $NoInstall) {
  Write-Host "== adb devices =="
  if (Has-AdbDevice) {
    Write-Host "== gradle installDebug =="
    & $gradlew installDebug
  } else {
    Write-Host "No device detected. Start an emulator or plug in your phone (USB debugging on)."
  }
}

# launch
if (-not $NoLaunch) {
  $pkg = Get-AndroidPackageName -Override $Package
  if (-not $pkg) {
    Write-Host "Could not detect applicationId/namespace. Launch skipped."
    Write-Host "Tip: run .\dev.ps1 -Package com.your.app"
  } elseif (Has-AdbDevice) {
    Write-Host "== Launching $pkg =="
    try {
      adb shell monkey -p $pkg 1 | Out-Null
    } catch {
      Write-Host "Launch failed. You can still open the app manually from the device."
    }
  } else {
    Write-Host "Launch skipped (no adb device)."
  }
}

# logcat
if ($Logcat) {
  Write-Host "== logcat (Ctrl+C to stop) =="
  adb logcat
}

Write-Host "== Done =="
