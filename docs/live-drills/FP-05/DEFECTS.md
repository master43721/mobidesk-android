# FP-05..11 FreeRDP Pipeline Live-Drill Defects Log

**Feature Point:** FP-05 through FP-11  
**Branch:** `fp-05-freerdp-pipeline`  
**Status:** No unresolved fatal defects.  

---

## Defects Table

| Defect ID | Severity | Description | Status | Resolution |
|:---:|:---:|:---|:---:|:---|
| `DEF-FP05-01` | Minor | Early inbound WebSocket packets dropped before local bridge accept | FIXED | Added pending inbound packet queue in `LocalRdpBridge` to buffer and flush packets on connect. |
| `DEF-FP05-02` | Major | `MobiDeskApplication` did not inherit from `GlobalApp`, causing null `sessionMap` | FIXED | Updated `MobiDeskApplication` to extend `GlobalApp` and call `super.onCreate()`. |
| `DEF-FP05-03` | Minor | `BookmarkBase` screen settings and performance flags method mismatch | FIXED | Corrected method calls in `RdpSessionManager`. |
| `DEF-FP05-04` | Minor | Bare LF in HTTP upgrade headers caused `readLine` hang | FIXED | Enhanced `StandardRdpRelayClient.readLine` to tolerate both bare LF and CRLF. |
