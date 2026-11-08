# BLD-01 Broker Live-Drill Execution Report & Kit

**Feature Package:** BLD-01 — MobiDesk Session Broker Deployment & Relay  
**Component:** Session Broker Service (`master43721/session-broker`)  
**Target Host:** Loopback on host designated by Project Owner (`127.0.0.1:8766`)  
**Current Live-Drill Status:** `LIVE_DRILL_STATUS: READY FOR OWNER`  
**Owner Sign-off Status:** UNACCEPTED (Awaiting physical demonstration)  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  

---

## 1. Executive Summary

BLD-01 verifies the MobiDesk Session Broker running against staging Supabase Auth and a real Windows VM target. It validates the complete Contract v1.1 protocol: `GET /health`, `GET /ready`, `GET /v1/vm-status` (with RDP X.224 negotiation probe and opaque assignmentTag), `POST /v1/session-grants` (single-use, 60s TTL, certificate fingerprint pin, body rejection), WebSocket `/v1/rdp` binary byte relay, rate limiting, and 15-second revocation watchdog closing with code 4403.

Per the Terminal Acceptance Invariant, the Session Broker cannot be accepted until the Project Owner executes this live drill and signs off.

---

## 2. Preconditions & Prerequisites (Ordered)

1. **Staging Supabase Project (not production):**
   - Staging Supabase project URL and publishable key configured.
   - Test student account provisioned with exactly one assigned VM in `student_assignments`.
2. **Throwaway Windows VM:**
   - Real Windows VM (e.g. win-01 on 10.x/172.x/192.168.x private IP or loopback port-forward) with RDP enabled on TCP port 3389.
   - Dedicated guest credentials for interactive sign-in test.
   - Known X.509 leaf certificate fingerprint.
3. **Execution Host:** Loopback environment on host designated by Project Owner.

---

## 3. Exact Deployment & Run Steps

```bash
# 1. Clone or checkout session-broker branch broker/catch-up-v1.1
cd /path/to/session-broker
git checkout broker/catch-up-v1.1
git rev-parse HEAD

# 2. Configure environment (no secrets committed)
export SUPABASE_URL="https://<staging-project>.supabase.co"
export SUPABASE_PUBLISHABLE_KEY="<staging-publishable-key>"
export MOBIDESK_VM_ENDPOINTS_JSON='{"<vm-uuid>": "<private-ip-or-127.0.0.1>"}'
export MOBIDESK_BIND_HOST="127.0.0.1"
export MOBIDESK_PORT=8766

# 3. Launch the broker service
python3 -m mobidesk_broker.app

# 4. Verify process liveness and readiness
curl -s http://127.0.0.1:8766/health | jq .
curl -s http://127.0.0.1:8766/ready | jq .
```

---

## 4. Numbered Scenarios & Acceptance Criteria

| Step # | Scenario & Action | Acceptance Criteria (Expected Result) | Result |
|:---:|:---|:---|:---:|
| **1** | Process Health (`GET /health`) | Returns HTTP 200 `{"status": "ok", "timestamp": "..."}`. Contains NO hostnames, IPs, or secrets. | NOT RUN |
| **2** | Service Readiness (`GET /ready`) | Returns HTTP 200 `{"status": "ready", "timestamp": "..."}`. Returns 503 if backing dependencies unavailable. | NOT RUN |
| **3** | VM Status Probe (`GET /v1/vm-status`) | Call with valid student JWT. Returns HTTP 200 with `lifecycle`, `rdpTransport`, `sampledAt`, `probeDurationMs`, `staleAfterSeconds`, `requestId`, and `assignmentTag`. Never leaks VM ID, name, IP, or port. Complete within 1,500 ms budget. | NOT RUN |
| **4** | RDP Transport Probe Accuracy | With Windows VM TCP/3389 reachable and accepting X.224 Connection Request, reports `RDP_NEGOTIATION_OK`. With port closed, reports `TCP_UNREACHABLE`. | NOT RUN |
| **5** | Session Grant Issuance (`POST /v1/session-grants`) | Send POST with empty body `{}` and JWT. Returns HTTP 201 with `grant`, `expiresAt`, `relayPath`, `rdpCertificate`, and `requestId`. Does NOT contain `vm` object. | NOT RUN |
| **6** | Body Destination Rejection | Send POST to `/v1/session-grants` with non-empty body `{"vm": "..."}`. Rejects immediately with HTTP 400 `client_destination_prohibited`. | NOT RUN |
| **7** | Rate Limiting Enforcement | Exceed 5 grants/min or 30 status/min for same user. Returns HTTP 429 with standards-compliant `Retry-After` header. | NOT RUN |
| **8** | Binary Relay & One-Use Consume | Connect WebSocket to `/v1/rdp` with `Authorization: Bearer <grant>`. Handshake succeeds. Attempting to connect a second WebSocket with same grant fails (HTTP/WSS rejection). | NOT RUN |
| **9** | Mid-Session Revocation Watchdog | While relay is active, revoke student session or remove VM assignment in Supabase. Broker rechecker detects revocation within 15s; closes WebSocket with application code 4403 within 30s. | NOT RUN |
| **10** | Redaction & Audit Scan | Inspect broker stdout/stderr logs. Proves JSON audit events (`GRANT_ISSUED`, `RELAY_OPENED`, `RELAY_CLOSED`, `SESSION_REVOKED`) contain zero JWTs, raw grants, passwords, student PII, VM IPs, or hostnames. | NOT RUN |

---

## 5. Evidence Checklist & Redaction Protocol

- **Evidence Files:**
  - `<date>-bld01-step01-health.json`
  - `<date>-bld01-step03-status.json`
  - `<date>-bld01-step05-grant.json`
  - `<date>-bld01-step08-relay.log`
  - `<date>-bld01-step09-revocation.log`
  - `<date>-bld01-step10-audit.json`
- **SHA-256 Manifest:** Recorded in `docs/live-drills/BLD-01/evidence/manifest.sha256`.
- **Redaction Checklist:**
  - [ ] No Supabase JWTs or refresh tokens in logs or captures.
  - [ ] No Windows VM private IP addresses or hostnames.
  - [ ] No raw student credentials or grant tokens.

---

## 6. Abort & Rollback Procedure

If the broker fails or encounters an unhandled exception:
1. Terminate the broker process (`Ctrl+C` or `kill -TERM`).
2. Log defect details in `docs/live-drills/BLD-01/DEFECTS.md`.
3. Transition `LIVE_DRILL_STATUS` to `FAIL` or `BLOCKED`.

---

## 7. Owner Sign-Off Block

```text
================================================================================
                    PROJECT OWNER LIVE-DRILL SIGN-OFF
================================================================================
Component:               BLD-01 — MobiDesk Session Broker Service
Commit SHA:              [Recorded by Owner from session-broker HEAD]
Deployment Host:         [Recorded by Owner]
All Scenarios Passed:    [YES / NO]
Evidence Manifest Valid: [YES / NO]
Owner Decision:          [ ACCEPTED / REJECTED ]

Signed by:               __________________________________
Date:                    __________________________________
================================================================================
```
