# FP-02 Decision Register — Student Dashboard and Workspace

**Version:** 0.1  
**Date:** 4 November 2026

This register distinguishes feature decisions already supported by the current Feature Inventory/parent docs from implementation defaults that still need Owner review. A documented proposal is not silently promoted to an Owner decision.

## 1. Source-confirmed feature decisions

| ID | Topic | Decision / requirement | Source | State |
|---|---|---|---|---|
| FP02-D-01 | Workspace outcome | One clear home screen for the student's identity and Cloud PC | FEATURE_INVENTORY.md FP-02 | SOURCE CONFIRMED |
| FP02-D-02 | Profile content | Student display name and approved profile fields; college label only where available | FEATURE_INVENTORY.md FP-02; DESIGN.md S-02 | SOURCE CONFIRMED |
| FP02-D-03 | Cloud PC cardinality | Display only one valid assigned Cloud PC | FEATURE_INVENTORY.md FP-02; FP-03 acceptance boundary | REQUIRED |
| FP02-D-04 | Data isolation | Only current student's own profile and one valid assigned VM may be shown | FEATURE_INVENTORY.md FP-02; FR-AUTH-02; SECURITY_THREAT_MODEL.md | REQUIRED |
| FP02-D-05 | Infrastructure privacy | No hostname, IP, VM UUID, port, Guacamole routing data or another student's data in the workspace | FEATURE_INVENTORY.md FP-02 | REQUIRED |
| FP02-D-06 | Status semantics | VM lifecycle and RDP transport are separate dimensions; assignment existence is not live status | DESIGN.md §§4.1–4.2; API_CONTRACT.md §4 | REQUIRED |
| FP02-D-07 | Status freshness | Current broker contract marks sample stale after 15 seconds; Connect refreshes before use | API_CONTRACT.md §4; DESIGN.md §4 | REQUIRED |
| FP02-D-08 | Connect condition | Connect only when exact-one assignment is valid and status is fresh with RUNNING + RDP_NEGOTIATION_OK | FEATURE_INVENTORY FP-02; FP-03/FP-04; DESIGN flow | REQUIRED |
| FP02-D-09 | Admin boundary | Student UI has no VM power or assignment controls | DESIGN.md §2 | REQUIRED |
| FP02-D-10 | Dock | R1 displays an informational R2 placeholder only; no Dock streaming controls | FEATURE_INVENTORY FP-02; DESIGN.md S-09 | REQUIRED |
| FP02-D-11 | Account actions | Settings/account entry and sign-out access are present; auth behavior belongs to FP-01 | FEATURE_INVENTORY FP-02; FP-01 | REQUIRED |
| FP02-D-12 | Status API | Consume `GET /v1/vm-status` with current Supabase JWT and no request selector; do not add target selectors | API_CONTRACT.md §4 | REQUIRED |

## 2. Proposed implementation defaults

| ID | Proposed default | Rationale | Status |
|---|---|---|---|
| FP02-D-13 | Preserve current DashboardScreen/Compose visual structure; add status detail consistent with S-03 instead of redesigning the app | Existing DashboardScreen and DESIGN screen inventory already define the desired shape | PROPOSED DEFAULT |
| FP02-D-14 | Load status on workspace entry, on explicit refresh and immediately before Connect; no periodic polling in FP-02 | These triggers satisfy the documented refresh and 15-second freshness behavior without inventing a polling cadence | PROPOSED DEFAULT — OWNER REVIEW |
| FP02-D-15 | Keep the last status visible only as explicitly stale after refresh failure; never let it enable Connect | Prevent stale positive state | PROPOSED DEFAULT |
| FP02-D-16 | Omit a missing/blank college label rather than invent a default or placeholder institution | The feature says "where available" | PROPOSED DEFAULT |
| FP02-D-17 | Use an FP-03-provided safe Cloud PC display label; do not expose raw `vms.name` until Owner/FP-03 confirms it is safe | Current documents do not establish that current VM names are user-safe | BLOCKED BY LABEL REVIEW |
| FP02-D-18 | On Connect, FP-02 performs a fresh status read and only then hands off to FP-05; FP-06/broker revalidates | UI state is not authorization | PROPOSED DEFAULT |
| FP02-D-19 | Use a small screen-facing workspace state and fakeable repository contracts; no new navigation or DI framework | Consistent with current Kotlin/Compose architecture | PROPOSED DEFAULT |
| FP02-D-20 | Status timestamp age uses a testable clock and fails closed when time validity is uncertain | Device/server clock skew may affect `sampledAt` interpretation | NEEDS FP-04 AGREEMENT |

## 3. Required dependency decisions

- **FP-01 integration:** AuthLevel must be fully authenticated; SDK Authenticated alone is not sufficient. Current-main evidence is inconsistent with the Owner's completion statement and is logged in PROGRESS.md.
- **FP-03 contract:** profile and assignment result must express exactly zero/one/invalid assignment and safe display fields.
- **FP-04 contract:** status DTO and broker endpoint must match `docs/API_CONTRACT.md §4`; current endpoint deployment/base URL is not evidenced in this repository snapshot.
- **FP-05 navigation:** the Connect destination is the existing Windows credential prompt, without a VM selector or guest credential passed through the dashboard.

## 4. Decision change control

If an Owner changes the scope, refresh policy, display fields, status logic or downstream handoff, add a new decision ID, record the date/owner, affected requirements/tests, migration or rollback effect, and update all dependent FP-02 documents together.
