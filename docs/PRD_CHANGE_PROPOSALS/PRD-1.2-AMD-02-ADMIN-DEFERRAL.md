# Proposed Amendment AMD-02 — College Administrator Workspace (FP-23..FP-28) Deferral to Supabase Dashboard & CLI Scripts for Pilot

**Parent:** MobiDesk PRD v1.2 (dated 3 November 2026)  
**Proposed version:** v1.2.1 clarification or successor v1.3  
**Date:** 8 November 2026  
**Owner decision:** APPROVED FOR PILOT (Owner directive 2026-11-08) — Pilot admin managed via Supabase dashboard / CLI scripts; native in-app admin workspace deferred.

---

## 1. Problem and Context

MobiDesk PRD v1.2 §4 and §10, alongside `FEATURE_INVENTORY.md` §11–§16, specified native Android College Administrator workspace features:
- **FP-23**: College Administrator Authentication and Role-Based Workspace
- **FP-24**: Student Account Provisioning and Invitation Lifecycle
- **FP-25**: Student Directory and VM Assignment Management
- **FP-26**: Server and VM Fleet Management
- **FP-27**: Student Connection Monitoring and Session Control
- **FP-28**: Administrator Audit and Operational Activity

Building and verifying 6 full-featured native Android administrator UI modules ahead of the student RDP core introduces extensive administrative surface area, role-switching complexity, and substantial delivery risk. For the 4-student pilot (win-01, win-02, win-03, win-05; win-04 excluded), college administration tasks are infrequent, managed directly by Lab IT, and can be reliably and securely performed using standard Supabase management mechanisms.

---

## 2. Proposed Normative Wording

### 2.1 Scope of Pilot Release (R1)
> For the initial R1 pilot release, all College Administrator management capabilities (FP-23 through FP-28) are deferred from the native Android application. The MobiDesk Android client application is exclusively a student Cloud PC client.

### 2.2 Administrative Control Plane for Pilot
> All administrative provisioning and management operations during the R1 pilot MUST be conducted through:
> 1. The **Supabase Web Dashboard** (restricted to authorized Lab IT administrators via MFA).
> 2. The **Supabase CLI** and approved server-side administration scripts executing in an authenticated administrative environment.
>
> Specifically:
> - Student account creation, email invitation, and password resets are performed via Supabase Auth admin APIs.
> - VM mapping and assignment records (`student_assignments`) are provisioned via direct SQL migrations or authenticated administrative scripts using row-level security policies.
> - Session termination and fleet state inspection are executed by administrator scripts or broker control endpoints, never through Android client admin credentials.
> - The Android client application MUST NOT contain administrator views, role-switching routes, administrator credentials, or Supabase `service_role` keys.

---

## 3. Impact Analysis

| Feature Point | Original Scope in PRD v1.2 | Revised Scope for R1 Pilot | Post-Pilot / R2 Horizon |
|:---|:---|:---|:---|
| **FP-23** Admin Auth & Workspace | Native Android admin sign-in & navigation shell | Deferred; student-only app shell | Native admin workspace evaluated for institutional rollout |
| **FP-24** Provisioning & Invitation | Native UI for student invites & CSV bulk upload | Supabase Dashboard / CLI scripts | In-app self-service administration |
| **FP-25** Directory & VM Assignment | In-app assignment mapping table & editor | Direct SQL / CLI script provisioning | Native management console |
| **FP-26** Fleet Management | Host/VM hypervisor status & control screen | Hypervisor console / broker status | Fleet orchestration dashboard |
| **FP-27** Connection Monitoring | Real-time active student session monitoring | Broker audit log / server queries | Live administrator control room |
| **FP-28** Administrator Audit | In-app tamper-evident audit log browser | Supabase audit log / broker JSON logs | Native audit trail viewer |

---

## 4. Security Invariants Preserved

1. **Zero Client Privilege Elevation:** The Android client remains strictly bounded to student privileges. It never holds or requests administrator tokens, hypervisor keys, or Supabase service-role access.
2. **Server-Enforced Authorization:** The session broker continues to validate student assignments independently from the database using student JWTs. Admin changes to assignments immediately reflect on the next 15-second status or relay recheck.
3. **No Attack Surface from Unfinished Admin Screens:** Eliminates incomplete admin screens and routes from student-facing builds.

---

## 5. Requirements and Documentation Traceability

- Parent PRD v1.2 text is preserved; this amendment records the approved pilot deferral.
- `FEATURE_INVENTORY.md` updated to mark FP-23..FP-28 as `DEFERRED FOR R1 PILOT (AMD-02)`.
- `NEGOTIABLE_REGISTER.md` records owner decision on NEG-07 as `ACCEPTED` (2026-11-08).
