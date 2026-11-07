<#
.SYNOPSIS
    MobiDesk Traceability Matrix Verification Script
.DESCRIPTION
    Parses TRACEABILITY.md for FP-01 and FP-02.
    Verifies:
    1. Every FR (Functional Requirement) maps to at least one test ID
    2. Test IDs referenced exist in test files or feature test plans
    Outputs machine-readable JSON.
#>
[CmdletBinding()]
param (
    [string]$OutputDir = "build/reports"
)

$ErrorActionPreference = "Stop"

$report = [ordered]@{
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    featurePoints = @()
    unmappedRequirements = @()
    missingTests = @()
    passed = $true
}

# Collect all test source content
$testFiles = Get-ChildItem -Path "app/src/test", "docs/FEATURE_SPECIFICATIONS" -Include "*.kt", "*.java", "*.md" -Recurse -ErrorAction SilentlyContinue
$allTestText = ($testFiles | ForEach-Object { Get-Content -Raw $_.FullName }) -join "`n"

$traceabilityFiles = @(
    "docs/FEATURE_SPECIFICATIONS/FP-01/TRACEABILITY.md",
    "docs/FEATURE_SPECIFICATIONS/FP-02/TRACEABILITY.md"
)

foreach ($tracePath in $traceabilityFiles) {
    if (-not (Test-Path $tracePath)) {
        $report.unmappedRequirements += "Missing traceability file: $tracePath"
        continue
    }

    $fpName = (Split-Path (Split-Path $tracePath -Parent) -Leaf)
    $fpReport = [ordered]@{
        featurePoint = $fpName
        totalRequirements = 0
        mappedRequirements = 0
        requirements = @()
    }

    $lines = Get-Content $tracePath
    foreach ($line in $lines) {
        if ($line -match '\|\s*(FP\d{2}-FR-\d{3}|FP\d{2}-FR-\d{2}|FR-[A-Z]+-\d{2})\s*\|\s*([^\|]+)\|') {
            $frId = $matches[1].Trim()
            $testMapping = $matches[2].Trim()
            $fpReport.totalRequirements++

            if ([string]::IsNullOrWhiteSpace($testMapping) -or $testMapping -eq "-") {
                $report.unmappedRequirements += "$($fpName): $frId has no mapped tests"
            } else {
                $fpReport.mappedRequirements++
                $testIds = [regex]::Matches($testMapping, '(FP01-CORE-\d{2}|FP01-BASE-\d{2}|CORE-\d{2}|T-[A-Z]+-\d{2})') | ForEach-Object { $_.Value }
                foreach ($tId in $testIds) {
                    if ($allTestText -notmatch [regex]::Escape($tId)) {
                        $report.missingTests += "$($fpName): Test $tId for $frId not found in test suite or specs"
                    }
                }
            }

            $fpReport.requirements += [ordered]@{
                id = $frId
                mapping = $testMapping
            }
        }
    }

    $report.featurePoints += $fpReport
}

if ($report.unmappedRequirements.Count -gt 0 -or $report.missingTests.Count -gt 0) {
    $report.passed = $false
}

$jsonOutput = $report | ConvertTo-Json -Depth 5
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$outFile = Join-Path $OutputDir "traceability-check.json"
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8

Write-Host $jsonOutput
if (-not $report.passed) {
    exit 1
}
exit 0
