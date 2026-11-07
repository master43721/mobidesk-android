#!/usr/bin/env bash
# MobiDesk Documentation Hygiene Checker (Bash)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/docs-check.json"

REPORT_JSON="{}"
if command -v python3 >/dev/null 2>&1; then
    REPORT_JSON=$(python3 -c '
import os, re, json, subprocess, datetime

root = os.getcwd()

# 1. Case collisions
case_collisions = []
if os.path.exists(os.path.join(root, ".git")):
    try:
        proc = subprocess.run(["git", "ls-files"], capture_output=True, text=True, check=True)
        files = [line.strip() for line in proc.stdout.splitlines() if line.strip()]
        seen = {}
        for f in files:
            lower = f.lower()
            seen.setdefault(lower, []).append(f)
        for lower, group in seen.items():
            if len(group) > 1:
                case_collisions.extend(group)
    except Exception:
        pass

# 2. Duplicate root plan / TODO files
duplicates = []
for f in ["plan.md", "TODO.md"]:
    if os.path.exists(os.path.join(root, f)):
        duplicates.append(f)

# 3. Broken links check across docs/, AGENTS.md, README.md
broken_links = []
md_files = []
for target in ["docs", "AGENTS.md", "README.md"]:
    full_target = os.path.join(root, target)
    if os.path.isdir(full_target):
        for dirpath, _, filenames in os.walk(full_target):
            for f in filenames:
                if f.endswith(".md"):
                    md_files.append(os.path.join(dirpath, f))
    elif os.path.isfile(full_target) and full_target.endswith(".md"):
        md_files.append(full_target)

for filepath in md_files:
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
            content = fp.read()
        dirpath = os.path.dirname(filepath)
        for m in re.finditer(r"\[([^\]]+)\]\(([^)]+)\)", content):
            link = m.group(2)
            if link.startswith(("http://", "https://", "mailto:", "#")):
                continue
            link_clean = link.split("#")[0]
            if not link_clean:
                continue
            resolved = os.path.normpath(os.path.join(dirpath, link_clean))
            if not os.path.exists(resolved):
                broken_links.append({"source": os.path.relpath(filepath, root), "link": link})
    except Exception:
        pass

passed = (len(case_collisions) == 0 and len(duplicates) == 0 and len(broken_links) == 0)

report = {
    "timestamp": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "passed": passed,
    "caseCollisions": case_collisions,
    "duplicatePlanFiles": duplicates,
    "brokenLinks": broken_links
}

print(json.dumps(report, indent=2))
')
fi

echo "$REPORT_JSON" > "$OUT_FILE"
cat "$OUT_FILE"

if echo "$REPORT_JSON" | grep -q '"passed": false'; then
    exit 1
fi
exit 0
