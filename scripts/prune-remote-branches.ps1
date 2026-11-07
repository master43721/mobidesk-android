<#
.SYNOPSIS
    MobiDesk Remote Branch Pruning Utility (PowerShell)
    Safely prunes stale and superseded remote branches from GitHub 'origin'.

.DESCRIPTION
    By default, runs in -DryRun mode (preview only).
    Pass -Execute to perform actual git push origin --delete commands.
    In accordance with AGENTS.md, deletions require repository admin rights.
    Before execution, archive tags (archive/<branch>) and a full git bundle
    (mobidesk-all-branches-archive.bundle) MUST be created.

.EXAMPLE
    .\scripts\prune-remote-branches.ps1
    (Dry run preview)

.EXAMPLE
    .\scripts\prune-remote-branches.ps1 -Execute
    (Creates archive bundle, creates archive tags, and deletes stale remote branches on origin)
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
    Write-Host "[MODE: EXECUTE] Repository Admin Execution Mode.`n" -ForegroundColor Red
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

# In execute mode, create git bundle and archive tags first
if ($Execute) {
    $bundleFile = "mobidesk-all-branches-archive.bundle"
    Write-Host "Creating safety archive bundle: $bundleFile..." -ForegroundColor Cyan
    & $gitPath bundle create $bundleFile --all
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create git bundle. Aborting pruning."
        exit 1
    }
    Write-Host "Safety archive bundle created successfully.`n" -ForegroundColor Green

    Write-Host "Creating archive tags for branches before pruning..." -ForegroundColor Cyan
    foreach ($branch in $staleBranches) {
        $tagBranch = $branch -replace '/', '-'
        $tagName = "archive/$tagBranch"
        $sha = (& $gitPath rev-parse "origin/$branch" 2>$null)
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($sha)) {
            & $gitPath tag -f $tagName "origin/$branch"
            Write-Host "Created tag: $tagName -> $sha" -ForegroundColor Gray
        }
    }
    Write-Host "Archive tags created.`n" -ForegroundColor Green
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
    Write-Host "Dry run complete. Stale branches are previewed above." -ForegroundColor Green
    Write-Host "To execute deletion with admin rights (creates bundle + archive tags first):" -ForegroundColor Green
    Write-Host "  .\scripts\prune-remote-branches.ps1 -Execute" -ForegroundColor White
} else {
    Write-Host "Pruning complete. Running remote prune..." -ForegroundColor Green
    & $gitPath remote prune origin
    Write-Host "Remote tracking branches refreshed." -ForegroundColor Green
}
