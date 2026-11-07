<#
.SYNOPSIS
    MobiDesk Documentation Hygiene Checker
.DESCRIPTION
    Checks for:
    1. Case-colliding file names in git tracking
    2. Broken relative markdown links across docs/, AGENTS.md, README.md
    3. Duplicate root plan/TODO files
    Outputs machine-readable JSON.
#>
[CmdletBinding()]
param (
    [string]$OutputDir = "build/reports"
)

$ErrorActionPreference = "Stop"

$report = [ordered]@{
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    caseCollisions = @()
    brokenLinks = @()
    duplicatePlanFiles = @()
    passed = $true
}

# 1. Case collisions
$gitPath = "C:\Program Files\Git\cmd\git.exe"
if (-not (Test-Path $gitPath)) {
    $cmd = Get-Command "git" -ErrorAction SilentlyContinue
    if ($cmd) { $gitPath = $cmd.Source }
}

if ($gitPath -and (Test-Path $gitPath)) {
    $trackedFiles = & $gitPath ls-files
    $grouped = $trackedFiles | Group-Object { $_.ToLower() } | Where-Object { $_.Count -gt 1 }
    foreach ($grp in $grouped) {
        $report.caseCollisions += @($grp.Group)
    }
}

# 2. Duplicate root plan / TODO files
$rootPlanFiles = Get-ChildItem -Path "." -Filter "*plan*.md" -ErrorAction SilentlyContinue
$rootTodoFiles = Get-ChildItem -Path "." -Filter "*todo*.md" -ErrorAction SilentlyContinue
foreach ($f in ($rootPlanFiles + $rootTodoFiles)) {
    if ($f.Name -match "^(plan|TODO)\.md$") {
        $report.duplicatePlanFiles += $f.Name
    }
}

# 3. Broken markdown links
$mdFiles = Get-ChildItem -Path "docs", "AGENTS.md", "README.md" -Filter "*.md" -Recurse -ErrorAction SilentlyContinue
foreach ($file in $mdFiles) {
    $content = Get-Content -Raw $file.FullName
    $matches = [regex]::Matches($content, '\[([^\]]+)\]\(([^)]+)\)')
    foreach ($m in $matches) {
        $link = $m.Groups[2].Value
        if ($link -match '^https?://' -or $link -match '^mailto:' -or $link -match '^#') { continue }
        $linkPath = $link -replace '#.*$', ''
        if ([string]::IsNullOrWhiteSpace($linkPath)) { continue }
        $resolved = Join-Path $file.DirectoryName $linkPath
        if (-not (Test-Path $resolved)) {
            $report.brokenLinks += [ordered]@{
                source = $file.FullName.Substring((Get-Location).Path.Length + 1)
                link = $link
                resolved = $resolved
            }
        }
    }
}

if ($report.caseCollisions.Count -gt 0 -or $report.brokenLinks.Count -gt 0 -or $report.duplicatePlanFiles.Count -gt 0) {
    $report.passed = $false
}

$jsonOutput = $report | ConvertTo-Json -Depth 5
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$outFile = Join-Path $OutputDir "docs-check.json"
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8

Write-Host $jsonOutput
if (-not $report.passed) {
    exit 1
}
exit 0
