#!/usr/bin/env bash
# MobiDesk Secret and Log Leakage Scanner (Bash)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/secrets-scan.json"

FINDINGS_JSON="[]"
PASSED=true

if command -v python3 >/dev/null 2>&1; then
    FINDINGS_JSON=$(python3 -c '
import os, re, json

findings = []
root = os.getcwd()
src_dir = os.path.join(root, "app", "src")

for dirpath, _, filenames in os.walk(src_dir):
    for f in filenames:
        if f.endswith((".kt", ".java", ".xml")):
            filepath = os.path.join(dirpath, f)
            relpath = os.path.relpath(filepath, root)
            try:
                with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
                    for line_num, line in enumerate(fp, 1):
                        # 1. Credentials in Log calls
                        if re.search(r"Log\.[vdiew]\([^)]*(password|secret|token|bearer|key)[^)]*\)", line) and "AuthErrorMessages" not in line:
                            findings.append({"file": relpath, "line": line_num, "category": "LOG_CREDENTIAL_LEAK", "snippet": line.strip()})
                        # 2. Hardcoded Bearer
                        if re.search(r"Bearer\s+[A-Za-z0-9_\-]{25,}", line):
                            findings.append({"file": relpath, "line": line_num, "category": "HARDCODED_BEARER_TOKEN", "snippet": line.strip()})
                        # 3. Private IP
                        if re.search(r"\b(192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3})\b", line):
                            findings.append({"file": relpath, "line": line_num, "category": "PRIVATE_IP_LITERAL", "snippet": line.strip()})
                        # 4. Raw 3389
                        if re.search(r"(?<![a-zA-Z0-9])3389(?![a-zA-Z0-9])", line):
                            findings.append({"file": relpath, "line": line_num, "category": "RAW_RDP_PORT_LEAK", "snippet": line.strip()})
            except Exception:
                pass

print(json.dumps(findings))
')
    if [ "$FINDINGS_JSON" != "[]" ]; then
        PASSED=false
    fi
fi

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
cat <<EOF > "$OUT_FILE"
{
  "timestamp": "$TIMESTAMP",
  "passed": $PASSED,
  "findings": $FINDINGS_JSON
}
EOF

cat "$OUT_FILE"
if [ "$PASSED" = false ]; then
    exit 1
fi
exit 0
