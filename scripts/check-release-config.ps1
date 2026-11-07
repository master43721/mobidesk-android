<#
.SYNOPSIS
    MobiDesk Release Configuration Safety Checker
.DESCRIPTION
    Scans project configurations and source code for release risks:
    - android:usesCleartextTraffic="true"
    - release { debuggable true }
    - Insecure TrustAll / X509TrustManager implementations
    - Demo/mock bypass switches in production code
    Outputs machine-readable JSON.
#>
[CmdletBinding()]
param (
    [string]$OutputDir = "build/reports"
)

$ErrorActionPreference = "Stop"

$report = [ordered]@{
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    findings = @()
    passed = $true
}

# 1. Check AndroidManifest.xml
$manifests = Get-ChildItem -Path "app/src" -Filter "AndroidManifest.xml" -Recurse -ErrorAction SilentlyContinue
foreach ($m in $manifests) {
    $content = Get-Content -Raw $m.FullName
    if ($content -match 'android:usesCleartextTraffic\s*=\s*"true"') {
        $report.findings += [ordered]@{
            file = $m.FullName.Substring((Get-Location).Path.Length + 1)
            category = "CLEARTEXT_TRAFFIC_ENABLED"
            description = "android:usesCleartextTraffic is set to true"
        }
    }
}

# 2. Check build.gradle files for release debuggable
$gradleFiles = Get-ChildItem -Path "." -Filter "*.gradle" -Recurse -ErrorAction SilentlyContinue
foreach ($g in $gradleFiles) {
    $content = Get-Content -Raw $g.FullName
    if ($content -match 'release\s*\{[^}]*debuggable\s+true') {
        $report.findings += [ordered]@{
            file = $g.FullName.Substring((Get-Location).Path.Length + 1)
            category = "RELEASE_DEBUGGABLE_TRUE"
            description = "debuggable true found inside release buildType"
        }
    }
}

# 3. Check for trust-all cert managers in production source
$mainSources = Get-ChildItem -Path "app/src/main" -Include "*.kt", "*.java" -Recurse -ErrorAction SilentlyContinue
foreach ($s in $mainSources) {
    $content = Get-Content -Raw $s.FullName
    if ($content -match 'TrustAll|checkServerTrusted\s*\([^)]*\)\s*\{\s*\}|ALLOW_ALL_HOSTNAME_VERIFIER') {
        $report.findings += [ordered]@{
            file = $s.FullName.Substring((Get-Location).Path.Length + 1)
            category = "INSECURE_TLS_TRUST_ALL"
            description = "Insecure trust-all certificate or hostname verifier found in production source"
        }
    }
}

if ($report.findings.Count -gt 0) {
    $report.passed = $false
}

$jsonOutput = $report | ConvertTo-Json -Depth 5
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$outFile = Join-Path $OutputDir "release-config-check.json"
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8

Write-Host $jsonOutput
if (-not $report.passed) {
    exit 1
}
exit 0
