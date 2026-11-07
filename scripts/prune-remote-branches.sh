#!/usr/bin/env bash
# MobiDesk Remote Branch Pruning Utility
# Safe script to delete stale and superseded remote branches from GitHub 'origin'.
#
# Usage:
#   ./scripts/prune-remote-branches.sh             # Dry run (safe preview)
#   ./scripts/prune-remote-branches.sh --execute   # Actually execute remote branch deletions (admin only)

set -euo pipefail

EXECUTE=false
if [[ "${1:-}" == "--execute" ]]; then
    EXECUTE=true
fi

# Branches that are stale or superseded by trunk-based main/fp-* branches:
STALE_BRANCHES=(
    "docs/college-admin-student-account-model"
    "docs/fp-01-current-main-handoff"
    "docs/fp-02-student-dashboard-workspace"
    "docs/fp-02-v0.2-corrected-codespaces"
    "docs/fp-02-v0.2-final"
    "docs/mandatory-live-drill-gate"
    "docs/mobidesk-prd-v1.2"
    "docs/prd-v1.2-suite"
    "feature/fp-01-authentication-draft"
    "feature/fp-01-auth-hardening"
    "step-by-step-reorganization-and-hardening-3977521026406886263"
    "step/fp-02-4999127741281951902"
)

echo "=== MobiDesk Remote Branch Prune Utility ==="
if [ "$EXECUTE" = false ]; then
    echo "[MODE: DRY RUN] No changes will be made. Pass --execute to delete remote branches."
else
    echo "[MODE: EXECUTE] Repository Admin Execution Mode."
    echo ""
    echo "Creating safety archive bundle: mobidesk-all-branches-archive.bundle..."
    git bundle create mobidesk-all-branches-archive.bundle --all
    echo "Safety archive bundle created successfully."
    echo ""
    echo "Creating archive tags for branches before pruning..."
    for branch in "${STALE_BRANCHES[@]}"; do
        tag_branch="${branch//\//-}"
        tag_name="archive/${tag_branch}"
        if sha=$(git rev-parse "origin/$branch" 2>/dev/null); then
            git tag -f "$tag_name" "origin/$branch"
            echo "Created tag: $tag_name -> $sha"
        fi
    done
    echo "Archive tags created."
fi
echo ""

# Verify git remote origin exists
if ! git remote get-url origin >/dev/null 2>&1; then
    echo "ERROR: Remote 'origin' is not configured in this repository." >&2
    exit 1
fi

for branch in "${STALE_BRANCHES[@]}"; do
    if git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
        if [ "$EXECUTE" = true ]; then
            echo "Deleting remote branch: origin/$branch..."
            git push origin --delete "$branch"
        else
            echo "Would delete remote branch: origin/$branch"
        fi
    else
        echo "Remote branch does not exist (already deleted): origin/$branch"
    fi
done

echo ""
if [ "$EXECUTE" = false ]; then
    echo "Dry run complete. To execute deletion with admin rights, run:"
    echo "  ./scripts/prune-remote-branches.sh --execute"
else
    echo "Pruning complete. Running remote prune..."
    git remote prune origin
    echo "Remote tracking branches refreshed."
fi
