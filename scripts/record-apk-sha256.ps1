<#
.SYNOPSIS
    MobiDesk APK SHA-256 and Integrity Recorder
.DESCRIPTION
    Computes the SHA-256 hash and size of the built APK.
    Outputs machine-readable JSON to build/reports/apk-sha256.json.
#>
[CmdletBinding()]
param (
    [string]$ApkPath = "app/build/outputs/apk/debug/app-debug.apk",
    [string]$OutputDir = "build/reports"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ApkPath)) {
    Write-Warning "APK not found at $ApkPath. Marking record as NOT_BUILT."
    $report = [ordered]@{
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        apkFound = $false
        apkPath = $ApkPath
        status = "NOT_BUILT"
    }
} else {
    $item = Get-Item $ApkPath
    $hash = (Get-FileHash -Path $ApkPath -Algorithm SHA256).Hash.ToLower()
    $report = [ordered]@{
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
        apkFound = $true
        apkPath = $ApkPath
        sha256 = $hash
        sizeBytes = $item.Length
        status = "RECORDED"
    }
}

$jsonOutput = $report | ConvertTo-Json -Depth 3
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$outFile = Join-Path $OutputDir "apk-sha256.json"
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8

Write-Host $jsonOutput
exit 0
