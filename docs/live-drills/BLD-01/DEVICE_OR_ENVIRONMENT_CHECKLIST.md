# BLD-01 Broker Live-Drill Environment Checklist

**Component:** BLD-01 — Session Broker  
**LIVE_DRILL_STATUS:** READY FOR OWNER  

## Preflight Environment

| Field | Requirement | Verified |
|:---|:---|:---:|
| Host Machine | Linux / macOS / BSD host with Python 3.12+ | [ ] |
| Staging Supabase | Active staging instance with test student | [ ] |
| Windows Target VM | Port 3389 open and reachable from broker host | [ ] |
| Endpoint JSON | Valid JSON mapping VM UUID to IP without leaking to repo | [ ] |

## Scenarios

| # | Step | Expected | Status |
|---|---|---|---|
| 1 | `GET /health` | 200 ok | NOT RUN |
| 2 | `GET /ready` | 200 ready | NOT RUN |
| 3 | `GET /v1/vm-status` | 200 with schema, budget <= 1500ms | NOT RUN |
| 4 | RDP Probe | Valid X.224 negotiation | NOT RUN |
| 5 | `POST /v1/session-grants` | 201 grant + certificate pin, no VM info | NOT RUN |
| 6 | Body rejection | 400 on non-empty body | NOT RUN |
| 7 | Rate limits | 429 on rapid requests | NOT RUN |
| 8 | Relay upgrade | WSS binary stream to TCP 3389 | NOT RUN |
| 9 | Revocation | Close 4403 within 30s | NOT RUN |
| 10 | Redaction check | Clean audit log | NOT RUN |
