#!/usr/bin/env bash
# MobiDesk Documentation Hygiene Checker (Bash)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"

OUT_FILE="$OUTPUT_DIR/docs-check.json"
PASSED=true

# 1. Case collisions
CASE_COLLISIONS=()
if command -v git >/dev/null 2>&1; then
    while IFS= read -r line; do
        if [ -n "$line" ]; then
            CASE_COLLISIONS+=("$line")
            PASSED=false
        fi
    done < <(git ls-files | sort -f | uniq -di || true)
fi

# 2. Duplicate root plan / TODO files
DUPLICATES=()
for f in plan.md TODO.md; do
    if [ -f "$f" ]; then
        DUPLICATES+=("$f")
        PASSED=false
    fi
done

# 3. Broken links check via python or inline script
BROKEN_LINKS_JSON="[]"
if command -v python3 >/dev/null 2>&1; then
    BROKEN_LINKS_JSON=$(python3 -c '
import os, re, json

broken = []
root = os.getcwd()

for dirpath, _, filenames in os.walk("docs"):
    for f in filenames:
        if f.endswith(".md"):
            filepath = os.path.join(dirpath, f)
            with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
                content = fp.read()
            for m in re.finditer(r"\[([^\]]+)\]\(([^)]+)\)", content):
                link = m.group(2)
                if link.startswith(("http://", "https://", "mailto:", "#")):
                    continue
                link_clean = link.split("#")[0]
                if not link_clean:
                    continue
                resolved = os.path.normpath(os.path.join(dirpath, link_clean))
                if not os.path.exists(resolved):
                    broken.append({"source": os.path.relpath(filepath, root), "link": link})

print(json.dumps(broken))
')
    if [ "$BROKEN_LINKS_JSON" != "[]" ]; then
        PASSED=false
    fi
fi

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
cat <<EOF > "$OUT_FILE"
{
  "timestamp": "$TIMESTAMP",
  "passed": $PASSED,
  "caseCollisions": $(printf '%s\n' "${CASE_COLLISIONS[@]:-}" | jq -R . | jq -s . 2>/dev/null || echo "[]"),
  "duplicatePlanFiles": $(printf '%s\n' "${DUPLICATES[@]:-}" | jq -R . | jq -s . 2>/dev/null || echo "[]"),
  "brokenLinks": $BROKEN_LINKS_JSON
}
EOF

cat "$OUT_FILE"
if [ "$PASSED" = false ]; then
    exit 1
fi
exit 0
