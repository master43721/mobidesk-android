# FP-02 Physical Device & Environment Live-Drill Checklist

**Feature Package:** FP-02 — Student Dashboard and Workspace
**Target Application:** `com.mobidesk.android`
**LIVE_DRILL_STATUS:** NOT REQUESTED
**Operator:** Master43721 (Owner)
**Verification Agent:** Antigravity

---

## Live-Drill Protocol & Stop Rules

1. Each scenario requires physical touch and real network interaction on a physical Android device.
2. Emulators, unit tests, and mock HTTP engines do not satisfy the FP-02 live drill.
3. Verification must be performed against the approved staging Session Broker.
4. Stop Rule: If a status probe leaks private IP/UUID, if Connect is enabled during an unready state, or if the UI crashes/glitches, halt execution and record in `DEFECTS.md`.

---

## Scenarios Matrix (Draft Scenarios for Future Execution)

| Scenario # | Scenario Description | Required Input / Action | Expected Result | Status |
|:---:|:---|:---|:---|:---:|
| **1** | Authenticated Workspace Entry | Launch app with valid student session from FP-01. | Displays Dashboard; shows student name and institution label; no route flash. | NOT RUN |
| **2** | Single Assigned Cloud PC Display | Observe main workspace area. | Renders exactly one Cloud PC card with safe student-facing label; no VM UUID or internal hostnames visible. | NOT RUN |
| **3** | Stopped VM Status Rendering | Staging broker reports VM STOPPED. | Displays exact VM-01 copy; Connect button is disabled. | NOT RUN |
| **4** | Starting VM Status Rendering | Staging broker reports VM STARTING. | Displays exact VM-02 copy; Connect button is disabled. | NOT RUN |
| **5** | Port Open but RDP Not Ready | Broker reports TCP_REACHABLE but NOT RDP_NEGOTIATION_OK. | Connect button remains disabled; status indicates RDP negotiation pending. | NOT RUN |
| **6** | Fully Ready State & Connect Gating | Broker reports RUNNING + RDP_NEGOTIATION_OK + fresh sample (<15s). | Status renders "Ready"; Connect button is enabled. | NOT RUN |
| **7** | Status Staleness Transition | Allow 15 seconds to elapse without fresh status sample. | Status transitions to stale; Connect button disables or triggers mandatory fresh probe before handoff. | NOT RUN |
| **8** | Manual Status Refresh | Tap "Refresh" action on dashboard. | Loading indicator displays during probe; updates status cleanly without UI jitter. | NOT RUN |
| **9** | Unassigned Student Experience | Log in with provisioned student account that has no assigned VM. | Displays exact ASSIGN-01 copy; Cloud PC card omitted; no error crash. | NOT RUN |
| **10** | Informational Dock Area | Inspect Dock card/section. | Renders informational R2 placeholder only; no interactive actions or error triggers. | NOT RUN |
| **11** | Sign-Out from Settings | Tap Settings / Sign-Out from Dashboard. | Wipes cached student workspace data and signs out session; returns to Sign-In. | NOT RUN |
| **12** | Privacy & Log Audit | Inspect logcat during all dashboard operations. | Zero VM UUIDs, broker auth secrets, internal hostnames, or student PII emitted in logcat. | NOT RUN |
