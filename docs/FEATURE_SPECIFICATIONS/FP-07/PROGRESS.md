# FP-07 Secure WSS RDP Byte-Stream Relay — Progress & Iteration Record

**Feature Point:** FP-07 (Secure WSS RDP Byte-Stream Relay)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP07-FR-01 (RFC 6455 Binary WebSocket Transport)** | PRD §5.3 / API Contract v1.1 §6 | `StandardRdpRelayClient.kt` | `StandardRdpRelayClientTest.connect_sendsUpgradeAndBearerGrant_andExchangesBinaryFrames` | **Complete.** Full RFC 6455 WebSocket client using pure Android/Java sockets without unvetted external dependencies. |
| **FP07-FR-02 (Text Frames Strictly Prohibited)** | API Contract v1.1 §6 | `StandardRdpRelayClient.kt:150-155` | `StandardRdpRelayClientTest.textFrame_causesProtocolError_andCloses` | **Complete.** Immediately aborts and disconnects with close code 1002 if text frame (opcode 0x01) is encountered. |
| **FP07-FR-03 (Binary Frame & Continuation Support)** | RFC 6455 §5.4 / Contract v1.1 §6 | `StandardRdpRelayClient.kt:156-159` | `StandardRdpRelayClientTest.continuationFrame_deliversPayload` | **Complete.** Handles both 0x02 (Binary) and 0x00 (Continuation) frames for ordered stream reassembly. |
| **FP07-FR-04 (64 KiB Maximum Frame Cap)** | API Contract v1.1 §6 | `StandardRdpRelayClient.kt:127-132` | `StandardRdpRelayClientTest` | **Complete.** Enforces 65,536-byte frame cap; aborts with code 1009 if oversized frame is received. |
| **FP07-FR-05 (Client-Side Masking)** | RFC 6455 §5.3 | `StandardRdpRelayClient.sendFrame()` | `StandardRdpRelayClientTest` (server verifies mask bit) | **Complete.** Client masks every outbound frame using `SecureRandom` 4-byte mask keys. |
| **FP07-FR-06 (Single-Use Grant in Bearer Header)** | API Contract v1.1 §6 | `StandardRdpRelayClient.kt:80-82` | `StandardRdpRelayClientTest` | **Complete.** Transmits `Authorization: Bearer <grant>` exclusively in HTTP upgrade headers; never in URL query string. |
| **FP07-FR-07 (Robust Line-Ending Tolerance)** | RFC 7230 §2.2 / RFC 9112 | `StandardRdpRelayClient.readLine()` | `StandardRdpRelayClientTest` | **Complete.** Handles both CRLF (`\r\n`) and bare LF (`\n`) safely without socket stalling. |
