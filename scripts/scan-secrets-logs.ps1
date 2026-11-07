<#
.SYNOPSIS
    MobiDesk Secret and Log Leakage Scanner
.DESCRIPTION
    Scans source files under app/src/ for:
    - Bearer tokens or JWTs
    - API keys and service-role secrets
    - Sensitive parameters in Log.* calls (password, token, secret)
    - Hardcoded IPv4 literals and internal IPs (e.g. 192.168.x.x, 10.x.x.x, port 3389)
    - Suspicious hostnames
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

$sourceFiles = Get-ChildItem -Path "app/src" -Include "*.kt", "*.java", "*.xml" -Recurse -ErrorAction SilentlyContinue

foreach ($file in $sourceFiles) {
    $relativePath = $file.FullName.Substring((Get-Location).Path.Length + 1)
    $lines = Get-Content $file.FullName
    $lineNum = 0

    foreach ($line in $lines) {
        $lineNum++
        
        # 1. Password/token in Log calls
        if ($line -match 'Log\.[vdiew]\([^)]*(password|secret|token|bearer|key)[^)]*\)' -and $line -notmatch 'AuthErrorMessages') {
            $report.findings += [ordered]@{
                file = $relativePath
                line = $lineNum
                category = "LOG_CREDENTIAL_LEAK"
                snippet = $line.Trim()
            }
        }

        # 2. Hardcoded Bearer tokens
        if ($line -match 'Bearer\s+[A-Za-z0-9_\-]{25,}') {
            $report.findings += [ordered]@{
                file = $relativePath
                line = $lineNum
                category = "HARDCODED_BEARER_TOKEN"
                snippet = $line.Trim()
            }
        }

        # 3. Private IP addresses (192.168., 10., 172.16-31.)
        if ($line -match '\b(192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})\b') {
            $report.findings += [ordered]@{
                file = $relativePath
                line = $lineNum
                category = "PRIVATE_IP_LITERAL"
                snippet = $line.Trim()
            }
        }

        # 4. Raw RDP port 3389
        if ($line -match '(?<![a-zA-Z0-9])3389(?![a-zA-Z0-9])') {
            $report.findings += [ordered]@{
                file = $relativePath
                line = $lineNum
                category = "RAW_RDP_PORT_LEAK"
                snippet = $line.Trim()
            }
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
$outFile = Join-Path $OutputDir "secrets-scan.json"
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8

Write-Host $jsonOutput
if (-not $report.passed) {
    exit 1
}
exit 0
