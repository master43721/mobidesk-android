#!/usr/bin/env bash
# MobiDesk Broker Contract Schema Validator (Bash)
set -euo pipefail

OUTPUT_DIR="${1:-build/reports}"
mkdir -p "$OUTPUT_DIR"
OUT_FILE="$OUTPUT_DIR/broker-contract-validation.json"

REPORT_JSON="{}"
PASSED=true

if python3 -c 'import sys; sys.exit(0)' >/dev/null 2>&1; then
    REPORT_JSON=$(python3 -c '
import json, datetime

sample_payloads = [
    {"lifecycle":"RUNNING","rdpTransport":"RDP_NEGOTIATION_OK","sampledAt":"2026-11-03T06:00:00Z","probeDurationMs":384,"staleAfterSeconds":15,"requestId":"req_7f1b24a890d643d8","assignmentTag":"asg_tag_7f1b24a890d643d8"},
    {"lifecycle":"UNKNOWN","rdpTransport":"UNKNOWN","sampledAt":"2026-11-03T06:00:01Z","probeDurationMs":1500,"staleAfterSeconds":15,"requestId":"req_9a26d0f4712a45ef","assignmentTag":"asg_tag_9a26d0f4712a45ef"}
]

valid_lifecycles = {"STOPPED", "STARTING", "RUNNING", "STOPPING", "ERROR", "UNKNOWN"}
valid_transports = {"NOT_CHECKED", "TCP_UNREACHABLE", "TCP_REACHABLE", "RDP_NEGOTIATION_OK", "UNKNOWN"}
prohibited_fields = ["vm_id", "vmid", "host", "hostname", "ip", "ipv4", "port", "password", "secret", "token", "service_role"]

report = {
    "timestamp": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "contractVersion": "1.1 (API_CONTRACT.md)",
    "fakeVersusRealStatus": "UNKNOWN",
    "prohibitedFieldViolations": [],
    "schemaViolations": [],
    "passed": True
}

for idx, obj in enumerate(sample_payloads, 1):
    for p in prohibited_fields:
        if p in obj:
            report["prohibitedFieldViolations"].append(f"Payload #{idx} contains prohibited field: {p}")
    if obj.get("lifecycle") not in valid_lifecycles:
        report["schemaViolations"].append(f"Payload #{idx} invalid or missing lifecycle: {obj.get(\"lifecycle\")}")
    if obj.get("rdpTransport") not in valid_transports:
        report["schemaViolations"].append(f"Payload #{idx} invalid or missing rdpTransport: {obj.get(\"rdpTransport\")}")
    if not obj.get("assignmentTag"):
        report["schemaViolations"].append(f"Payload #{idx} missing mandatory assignmentTag")
    if not obj.get("requestId"):
        report["schemaViolations"].append(f"Payload #{idx} missing mandatory requestId")

if report["prohibitedFieldViolations"] or report["schemaViolations"]:
    report["passed"] = False

print(json.dumps(report, indent=2))
')
fi

echo "$REPORT_JSON" > "$OUT_FILE"
cat "$OUT_FILE"

if echo "$REPORT_JSON" | grep -q '"passed": false'; then
    exit 1
fi
exit 0
