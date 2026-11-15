# FP-05 Separate Windows Guest Credentials and NLA — Progress & Iteration Record

**Feature Point:** FP-05 (Separate Windows Guest Credential Prompt and NLA)  
**Branch:** `fp-05-freerdp-pipeline`  
**Status Label:** **IN PROGRESS (UNACCEPTED BASE: Staging candidate waiting for physical Live Drill gate)**  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md` and `AGENTS.md`  

---

## 1. Audit Gap Table (FR -> Implementation -> Tests -> Resolution)

| Functional Requirement (FR) | PRD v1.2 / Spec Ref | Android Implementation | Automated Tests | Gap & Resolution |
|:---|:---|:---|:---|:---|
| **FP05-FR-01 (Separate Lab Account Prompt)** | PRD §5.2 / FP-05 | `WindowsCredentialDialog.kt` | `WindowsGuestCredentialsTest` | **Complete.** Explicit explanation that Windows lab credentials differ from college/Supabase login. |
| **FP05-FR-02 (Memory Zeroization via CharArray)** | Security Invariant 1 / AGENTS.md | `WindowsGuestCredentials.kt` | `WindowsGuestCredentialsTest.clear_zeroesOutAllMemoryAndMarksDestroyed` | **Complete.** Credentials stored in mutable `CharArray` instead of immutable `String`; overwritten with `\u0000` via `clear()`. |
| **FP05-FR-03 (AutoCloseable Scope Protection)** | Security Invariant 1 | `WindowsGuestCredentials.kt:18` | `WindowsGuestCredentialsTest.useBlock_automaticallyClearsMemoryOnExit` | **Complete.** AutoCloseable implementation ensures deterministic memory scrubbing upon scope exit. |
| **FP05-FR-04 (Zero Credential Leakage in Logs / Strings)** | PRD §9 / AGENTS.md §1.3 | `WindowsGuestCredentials.toString()` | `WindowsGuestCredentialsTest.toString_doesNotLeakCredentials` | **Complete.** `toString()` redacts username, domain, and password. |
| **FP05-FR-05 (FreeRDP Hand-off and In-Session Handoff)** | FreeRDP API Audit §3 | `RdpSessionManager.kt:183-198` (`OnAuthenticate`) | `verify_rdp_pipeline.py` | **Complete.** Credentials passed directly to FreeRDP native NLA callback and zeroed immediately after handoff. |
| **FP05-FR-06 (No Intent Extra / Bundle Serialization)** | Security Threat Model §4 | `RdpSessionCarrier.kt` | `WindowsGuestCredentialsTest` | **Complete.** Ephemeral in-memory staging carrier prevents credential exposure in Android Intent extras or task dumps. |
