#!/usr/bin/env bash
# MobiDesk Release Configuration Safety Checker (Bash)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/release-config-check.json"

FINDINGS_JSON="[]"
PASSED=true

if python3 -c 'import sys; sys.exit(0)' >/dev/null 2>&1; then
    FINDINGS_JSON=$(python3 -c '
import os, re, json

findings = []
root = os.getcwd()

# 1. AndroidManifest
for dirpath, _, filenames in os.walk(os.path.join(root, "app", "src")):
    for f in filenames:
        if f == "AndroidManifest.xml":
            filepath = os.path.join(dirpath, f)
            with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
                content = fp.read()
            if "android:usesCleartextTraffic=\"true\"" in content:
                findings.append({"file": os.path.relpath(filepath, root), "category": "CLEARTEXT_TRAFFIC_ENABLED", "description": "android:usesCleartextTraffic is set to true"})

# 2. build.gradle release debuggable
for dirpath, _, filenames in os.walk(root):
    for f in filenames:
        if f.endswith(".gradle"):
            filepath = os.path.join(dirpath, f)
            with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
                content = fp.read()
            if re.search(r"release\s*\{[^}]*debuggable\s+true", content):
                findings.append({"file": os.path.relpath(filepath, root), "category": "RELEASE_DEBUGGABLE_TRUE", "description": "debuggable true found inside release buildType"})

# 3. Trust-all in main
main_src = os.path.join(root, "app", "src", "main")
for dirpath, _, filenames in os.walk(main_src):
    for f in filenames:
        if f.endswith((".kt", ".java")):
            filepath = os.path.join(dirpath, f)
            with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
                content = fp.read()
            if re.search(r"TrustAll|checkServerTrusted\s*\([^)]*\)\s*\{\s*\}|ALLOW_ALL_HOSTNAME_VERIFIER", content):
                findings.append({"file": os.path.relpath(filepath, root), "category": "INSECURE_TLS_TRUST_ALL", "description": "Insecure trust-all certificate or hostname verifier found in production source"})

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
