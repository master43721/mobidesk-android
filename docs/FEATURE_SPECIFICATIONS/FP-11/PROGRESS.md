# FP-11 Native RDP Session Window and Desktop Rendering — Progress & Iteration Record

**Feature Point:** FP-11 (Native RDP Session Window and Desktop Rendering)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP11-FR-01 (Dedicated Non-Exported Activity)** | PRD §5.3 / §12 / AGENTS.md | `RdpSessionActivity.kt`, `AndroidManifest.xml` (`android:exported="false"`) | `verify_live_drill_gate.py` | **Complete.** Explicitly unexported Activity isolating remote desktop from third-party app intents. |
| **FP11-FR-02 (FLAG_SECURE Pre-Inflation Protection)** | Security Invariant 1 / PRD §9 | `RdpSessionActivity.kt:51-54` | `scan-secrets-logs.sh` | **Complete.** `FLAG_SECURE` enforced before layout inflation or pixel rendering; blocks screenshots and Recent Tasks previews. |
| **FP11-FR-03 (Native View Hierarchy)** | FreeRDP API Audit §2 | `RdpSessionActivity.kt:66-79` (`R.layout.session`) | `verify_rdp_pipeline.py` | **Complete.** Embeds `ScrollView2D`, `SessionView`, `TouchPointerView`, and `ExtendedKeyboardView`. |
| **FP11-FR-04 (Desktop Surface Change & Resize)** | FreeRDP API Audit §2 | `RdpSessionActivity.onSettingsChanged()` | `verify_rdp_pipeline.py` | **Complete.** Dynamically allocates bitmap (ARGB_8888 or RGB_565), attaches to `SessionState`, and notifies `SessionView`. |
| **FP11-FR-05 (Graphics Invalidation & Redraw)** | FreeRDP API Audit §2 | `RdpSessionActivity.onGraphicsUpdate()` | `verify_rdp_pipeline.py` | **Complete.** Copies pixel updates from native engine and schedules invalidation of modified desktop regions. |
| **FP11-FR-06 (System Bars & Immersive Mode)** | UX / Immersive | `RdpSessionActivity.hideSystemBars()` | `RdpSessionActivity.kt` | **Complete.** Hides status and navigation bars; accommodates display cutouts via `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`. |
| **FP11-FR-07 (Input Event Routing)** | PRD §6.5 | `RdpSessionActivity.onKeyDown()` / `onKeyUp()` | `SessionInputManager.java` | **Complete.** Routes hardware and soft keyboard events into `SessionInputManager`. |
| **FP11-FR-08 (Floating Session Toolbar)** | PRD §6.5 | `RdpSessionActivity.floatingToolbar` | `RdpSessionActivity.kt` | **Complete.** Floating toolbar provides quick toggles for touch-pointer mode and soft keyboard. |
