# FP-09 Native FreeRDP Engine and Protocol Integration — Progress & Iteration Record

**Feature Point:** FP-09 (Native FreeRDP Engine and RDP Protocol Integration)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP09-FR-01 (Pinned FreeRDP Core Linkage)** | AGENTS.md §1.2 | `MobiDeskApplication.kt` (`GlobalApp`), `app/build.gradle` | `verify_rdp_pipeline.py` | **Complete.** `MobiDeskApplication` extends `GlobalApp`, properly initializing session map, preferences, and event listeners. |
| **FP09-FR-02 (Session Orchestration Pipeline)** | PRD §5.3 / §6 | `RdpSessionManager.startSession()` | `verify_rdp_pipeline.py` | **Complete.** Sequentially initializes bridge, establishes WSS relay, configures `BookmarkBase`, and triggers FreeRDP connect worker. |
| **FP09-FR-03 (NLA Callback Authentication)** | PRD §5.2 | `RdpSessionManager.kt:183-198` (`OnAuthenticate`) | `verify_rdp_pipeline.py` | **Complete.** Provides entered username, domain, and password directly to `LibFreeRDP.UIEventListener` and zeros memory upon return. |
| **FP09-FR-04 (Bookmark Screen Configuration)** | FreeRDP API Audit §2 | `RdpSessionManager.kt:152-170` | `verify_rdp_pipeline.py` | **Complete.** Configures `screenSettings` (CUSTOM resolution, width/height) and `performanceFlags` (GFX enabled, RemoteFX disabled). |
| **FP09-FR-05 (Synchronous Graphics Frame Update)** | FreeRDP API Audit §2 | `RdpSessionManager.kt:241-248` | `verify_rdp_pipeline.py` | **Complete.** Dispatches graphics updates and settings change events cleanly to UI listener. |
| **FP09-FR-06 (Intent Bundle Leakage Shield)** | Security Threat Model §4 | `RdpSessionCarrier.kt` | `WindowsGuestCredentialsTest` | **Complete.** Ephemeral in-memory staging carrier eliminates all Intent bundle serialization of credentials and grant tokens. |
| **FP09-FR-07 (Clean Pipeline Teardown)** | PRD §9 | `RdpSessionManager.disconnect()` | `verify_rdp_pipeline.py` | **Complete.** Closes FreeRDP session instance, shuts down bridge socket, terminates relay stream, and zeroes held credentials. |
