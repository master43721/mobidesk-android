<#
.SYNOPSIS
    MobiDesk Remote Branch Pruning Utility (PowerShell)
    Safely prunes stale and superseded remote branches from GitHub 'origin'.

.DESCRIPTION
    By default, runs in -DryRun mode (preview only).
    Pass -Execute to perform actual git push origin --delete commands.

.EXAMPLE
    .\scripts\prune-remote-branches.ps1
    (Dry run preview)

.EXAMPLE
    .\scripts\prune-remote-branches.ps1 -Execute
    (Deletes stale remote branches on origin)
#>

[CmdletBinding()]
param (
    [switch]$Execute
)

$ErrorActionPreference = "Stop"

$staleBranches = @(
    "docs/college-admin-student-account-model",
    "docs/fp-01-current-main-handoff",
    "docs/fp-02-student-dashboard-workspace",
    "docs/fp-02-v0.2-corrected-codespaces",
    "docs/fp-02-v0.2-final",
    "docs/mandatory-live-drill-gate",
    "docs/mobidesk-prd-v1.2",
    "docs/prd-v1.2-suite",
    "feature/fp-01-authentication-draft",
    "feature/fp-01-auth-hardening",
    "step-by-step-reorganization-and-hardening-3977521026406886263",
    "step/fp-02-4999127741281951902"
)

Write-Host "=== MobiDesk Remote Branch Prune Utility ===" -ForegroundColor Cyan
if (-not $Execute) {
    Write-Host "[MODE: DRY RUN] No changes will be made. Pass -Execute to delete remote branches.`n" -ForegroundColor Yellow
} else {
    Write-Host "[MODE: EXECUTE] Deleting stale branches from remote 'origin'...`n" -ForegroundColor Red
}

$gitPath = "C:\Program Files\Git\cmd\git.exe"
if (-not (Test-Path $gitPath)) {
    $cmd = Get-Command "git" -ErrorAction SilentlyContinue
    if ($cmd) {
        $gitPath = $cmd.Source
    } else {
        Write-Error "Git executable not found in PATH or standard Git directories."
    }
}

foreach ($branch in $staleBranches) {
    $remoteCheck = & $gitPath ls-remote --exit-code --heads origin $branch 2>$null
    if ($LASTEXITCODE -eq 0) {
        if ($Execute) {
            Write-Host "Deleting remote branch: origin/$branch..." -ForegroundColor Magenta
            & $gitPath push origin --delete $branch
        } else {
            Write-Host "Would delete remote branch: origin/$branch" -ForegroundColor Gray
        }
    } else {
        Write-Host "Remote branch does not exist (already deleted): origin/$branch" -ForegroundColor DarkGray
    }
}

Write-Host ""
if (-not $Execute) {
    Write-Host "Dry run complete. To execute deletion, run:" -ForegroundColor Green
    Write-Host "  .\scripts\prune-remote-branches.ps1 -Execute" -ForegroundColor White
} else {
    Write-Host "Pruning complete. Running remote prune..." -ForegroundColor Green
    & $gitPath remote prune origin
    Write-Host "Remote tracking branches refreshed." -ForegroundColor Green
}
