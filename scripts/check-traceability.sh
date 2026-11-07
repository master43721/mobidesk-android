#!/usr/bin/env bash
# MobiDesk Traceability Matrix Verification Script (Bash)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/traceability-check.json"

REPORT_JSON="{}"
PASSED=true

if command -v python3 >/dev/null 2>&1; then
    REPORT_JSON=$(python3 -c '
import os, re, json

root = os.getcwd()
test_text = ""
for search_dir in ["app/src/test", "docs/FEATURE_SPECIFICATIONS"]:
    full_search = os.path.join(root, search_dir)
    if os.path.exists(full_search):
        for dirpath, _, filenames in os.walk(full_search):
            for f in filenames:
                if f.endswith((".kt", ".java", ".md")):
                    with open(os.path.join(dirpath, f), "r", encoding="utf-8", errors="ignore") as fp:
                        test_text += fp.read() + "\n"

trace_files = [
    "docs/FEATURE_SPECIFICATIONS/FP-01/TRACEABILITY.md",
    "docs/FEATURE_SPECIFICATIONS/FP-02/TRACEABILITY.md"
]

report = {
    "featurePoints": [],
    "unmappedRequirements": [],
    "missingTests": [],
    "passed": True
}

for tf in trace_files:
    full_tf = os.path.join(root, tf)
    if not os.path.exists(full_tf):
        report["unmappedRequirements"].append("Missing: " + tf)
        continue
    fp_name = os.path.basename(os.path.dirname(full_tf))
    fp_rep = {"featurePoint": fp_name, "totalRequirements": 0, "mappedRequirements": 0, "requirements": []}
    with open(full_tf, "r", encoding="utf-8", errors="ignore") as fp:
        for line in fp:
            m = re.search(r"\|\s*(FP\d{2}-FR-\d{2,3}|FR-[A-Z]+-\d{2})\s*\|\s*([^\|]+)\|", line)
            if m:
                fr_id = m.group(1).strip()
                test_map = m.group(2).strip()
                fp_rep["totalRequirements"] += 1
                if not test_map or test_map == "-":
                    report["unmappedRequirements"].append(f"{fp_name}: {fr_id} has no mapped tests")
                else:
                    fp_rep["mappedRequirements"] += 1
                    t_ids = re.findall(r"(FP01-CORE-\d{2}|FP01-BASE-\d{2}|CORE-\d{2}|T-[A-Z]+-\d{2})", test_map)
                    for tid in t_ids:
                        if tid not in test_text:
                            report["missingTests"].append(f"{fp_name}: Test {tid} for {fr_id} not found")
                fp_rep["requirements"].append({"id": fr_id, "mapping": test_map})
    report["featurePoints"].append(fp_rep)

if report["unmappedRequirements"] or report["missingTests"]:
    report["passed"] = False

print(json.dumps(report))
')
fi

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
echo "$REPORT_JSON" > "$OUT_FILE"
cat "$OUT_FILE"

if echo "$REPORT_JSON" | grep -q '"passed": false'; then
    exit 1
fi
exit 0
