# SPK-01 Environment Checklist

**Spike:** SPK-01 — FreeRDP Transport  
**LIVE_DRILL_STATUS:** READY FOR OWNER  

## Preflight Environment

| Field | Requirement | Verified |
|---|---|:---:|
| Target Phone | Physical phone with USB debugging | [ ] |
| Windows VM | Port 3389 with NLA enabled | [ ] |
| Guest Credentials | Separate test account | [ ] |
| Broker Relay | WSS /v1/rdp reachable | [ ] |

## Scenarios

| # | Step | Expected | Status |
|---|---|---|---|
| 1 | Bridge socket | Socketpair established | NOT RUN |
| 2 | Relay attach | Bytes piped bidirectionally | NOT RUN |
| 3 | Strict cert pin | Matching hash accepts; mismatch rejects | NOT RUN |
| 4 | NLA prompt | User enters credentials in session | NOT RUN |
| 5 | Interactive session | Remote desktop renders | NOT RUN |
| 6 | Surface lifecycle | Clean rotation/resize | NOT RUN |
| 7 | Teardown | Zero leaked threads/fds | NOT RUN |
| 8 | Redaction check | Clean logcat | NOT RUN |
