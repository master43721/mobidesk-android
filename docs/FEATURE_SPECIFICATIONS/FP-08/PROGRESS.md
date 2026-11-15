# FP-08 Protected FreeRDP Local Transport Bridge — Progress & Iteration Record

**Feature Point:** FP-08 (Protected FreeRDP Local Transport Bridge)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP08-FR-01 (Loopback Ephemeral Binding)** | PRD §5.3 / §6.5 | `LocalRdpBridge.startListener()` (`127.0.0.1:0`) | `LocalRdpBridgeTest.startListener_bindsToLoopbackPort` | **Complete.** Binds exclusively to loopback address on OS-allocated ephemeral port (>0). |
| **FP08-FR-02 (Anti-Hijack Immediate Closure)** | Threat Model §3 / FP-08 | `LocalRdpBridge.acceptAndPump()` lines 65–70 | `LocalRdpBridgeTest.acceptAndPump_acceptsOnePeerAndImmediatelyClosesListener` | **Complete.** Accepts exactly one connection from local FreeRDP and immediately closes `ServerSocket` listener. |
| **FP08-FR-03 (Second Peer Rejection)** | PRD §6.5 | `LocalRdpBridge.acceptAndPump()` | `LocalRdpBridgeTest.acceptAndPump_acceptsOnePeerAndImmediatelyClosesListener` | **Complete.** Verified that rogue second local connection attempt fails with `ConnectException`. |
| **FP08-FR-04 (Pre-Connect Inbound Buffering)** | Robustness / Stream Integrity | `LocalRdpBridge.pendingInboundQueue` | `LocalRdpBridgeTest.writeToFreeRdp_beforePeerConnects_buffersAndFlushesOnAccept` | **Complete.** Incoming WebSocket packets arriving before local socket completes `accept()` are buffered and flushed on connect. |
| **FP08-FR-05 (Bidirectional Byte Pumping)** | PRD §5.3 | `LocalRdpBridge.kt` (pump coroutine + `writeToFreeRdp`) | `LocalRdpBridgeTest.acceptAndPump_acceptsOnePeerAndImmediatelyClosesListener` | **Complete.** Ordered, full-duplex forwarding between FreeRDP and `RdpRelayClient`. |
| **FP08-FR-06 (Deterministic Teardown)** | PRD §9 | `LocalRdpBridge.close()` | `verify_rdp_pipeline.py` | **Complete.** Cancels pump coroutine, drains queues, and closes all sockets. |
