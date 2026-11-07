<#
.SYNOPSIS
    MobiDesk Verification Script (Toolchain check, JVM tests, lint, assemble)
.DESCRIPTION
    1. Checks host toolchain (JDK, Gradle/gradlew, Android SDK)
    2. If toolchain is missing, records structured status and exits cleanly
    3. If toolchain is present, executes:
       ./gradlew testDebugUnitTest lintDebug assembleDebug
    4. Summarizes JUnit XML results into machine-readable JSON.
#>
[CmdletBinding()]
param (
    [string]$OutputDir = "build/reports"
)

$ErrorActionPreference = "Stop"

$report = [ordered]@{
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    toolchain = [ordered]@{
        jdkAvailable = $false
        jdkVersion = $null
        androidSdkAvailable = $false
        androidHome = $env:ANDROID_HOME
        gradlewAvailable = (Test-Path "./gradlew")
    }
    verification = [ordered]@{
        status = "NOT_RUN"
        totalTests = 0
        passed = 0
        failed = 0
        skipped = 0
        apkBuilt = $false
    }
}

# 1. Check JDK
$javaCmd = Get-Command "java" -ErrorAction SilentlyContinue
if ($javaCmd) {
    $report.toolchain.jdkAvailable = $true
    $report.toolchain.jdkVersion = (& java -version 2>&1 | Select-Object -First 1)
}

# 2. Check Android SDK
if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $report.toolchain.androidSdkAvailable = $true
} elseif ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) {
    $report.toolchain.androidSdkAvailable = $true
    $report.toolchain.androidHome = $env:ANDROID_SDK_ROOT
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$outFile = Join-Path $OutputDir "verification-report.json"

if (-not $report.toolchain.jdkAvailable -or -not $report.toolchain.androidSdkAvailable) {
    Write-Warning "Toolchain prerequisite missing on this host (JDK: $($report.toolchain.jdkAvailable), Android SDK: $($report.toolchain.androidSdkAvailable))."
    $report.verification.status = "BLOCKED_MISSING_TOOLCHAIN"
    $jsonOutput = $report | ConvertTo-Json -Depth 4
    $jsonOutput | Out-File -FilePath $outFile -Encoding utf8
    Write-Host $jsonOutput
    exit 1
}

Write-Host "Toolchain verified. Executing build verification..." -ForegroundColor Cyan
& ./gradlew --no-daemon --stacktrace :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
$buildExit = $LASTEXITCODE

# Parse JUnit results
$xmlFiles = Get-ChildItem -Path "app/build/test-results/testDebugUnitTest" -Filter "*.xml" -ErrorAction SilentlyContinue
foreach ($xml in $xmlFiles) {
    [xml]$x = Get-Content $xml.FullName
    $suite = $x.testsuite
    if ($suite) {
        $report.verification.totalTests += [int]$suite.tests
        $report.verification.failed += [int]$suite.failures + [int]$suite.errors
        $report.verification.skipped += [int]$suite.skipped
    }
}
$report.verification.passed = $report.verification.totalTests - $report.verification.failed - $report.verification.skipped
$report.verification.apkBuilt = (Test-Path "app/build/outputs/apk/debug/app-debug.apk")

if ($buildExit -eq 0 -and $report.verification.failed -eq 0) {
    $report.verification.status = "SUCCESS"
} else {
    $report.verification.status = "FAILED"
}

$jsonOutput = $report | ConvertTo-Json -Depth 4
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8
Write-Host $jsonOutput

if ($report.verification.status -ne "SUCCESS") {
    exit 1
}
exit 0
