# MobiDesk — Architecture and Product Decisions

**Version:** 1.2 · **Date:** 3 November 2026  
**Status:** Recommendations for Project Owner ratification. A recommendation is not an implemented fact.

| ID | Decision | Recommendation | Reason / consequence | Status |
|---|---|---|---|---|
| DEC-01 | Canonical source | Use only master43721/mobidesk-android | Native Kotlin + Jetpack Compose and pinned FreeRDP are the target implementation | Recommended |
| DEC-02 | Client stack | Kotlin + Jetpack Compose with a native RdpSessionActivity | Matches repository architecture; FreeRDP surface remains native | Recommended |
| DEC-03 | Assignment authority | Broker derives VM from authenticated Supabase identity and trusted assignment query | Client must never select its destination VM | Required |
| DEC-04 | Windows authentication | Student enters separate Windows credentials each session for pilot | Lowest operational complexity; broker does not mint/store guest password | Recommended |
| DEC-05 | Session grant | One-use random grant, 60-second TTL, hash at rest, WSS relay | Limits replay and separates control-plane authorization from RDP data | Required |
| DEC-06 | Revocation | Check auth.sessions at grant issue; re-check session and assignment at <=15s intervals; close within 30s | JWT expiry alone is not immediate revocation | Required |
| DEC-07 | RDP server identity | Internal CA or per-VM SHA-256 pins; no accept-all fallback | Prevents transparent server substitution | Required |
| DEC-08 | Local bridge | Prefer socketpair/connected descriptor/Unix socket; strict one-use authenticated loopback fallback | Prevents local port hijack | Required |
| DEC-09 | Dock architecture | Run a comparative spike before choosing | Phone transcode path vs Pi-native FreeRDP have different power/performance trade-offs | Open for R2 |
| DEC-10 | AOA trust | Device strings are discovery only; pair using key-backed challenge-response | USB descriptors are spoofable | Required for R2 |
| DEC-11 | VM status | Lifecycle and RDP readiness are separate values | TCP reachability is not proof of successful NLA | Required |
| DEC-12 | Session backgrounding | Foreground-only session in R1; pause/close on background or screen lock | Avoids premature foreground-service assumptions | Recommended |
| DEC-13 | Sensitive UI | FLAG_SECURE on RDP session; best-effort rooted-device block for pilot | Reduces screenshot leakage and documents rooted-device policy | Required |
| DEC-14 | Release distribution | Do not create production signing/release promise until channel and key custody approved | Play Store, institutional MDM and direct distribution have different compliance/key workflows | Owner decision |
| DEC-15 | VM lifecycle | Prefer persistent per-student VM with admin-managed semester reset; student has no power controls | Operationally predictable pilot; must be approved by owner/Lab IT | Proposed |

## Owner approvals still required
- Confirm VM physical host/location and whether student VMs are persistent or ephemeral.
- Approve Windows guest-account creation, reset, lockout, expiry and disablement policy.
- Select physical reference Android device and Android OS/API version.
- Select distribution channel and owner-controlled signing-key custody.
- Approve telemetry retention, support access and privacy notice.
- Approve production broker hostname, private network route and operational owner.

## Change control
Use DEC-XX identifiers in this document. Every changed decision must include date, owner, reason, impacted requirements and migration/rollback notes. Requirements remain normative in PRD.md; decisions explain the design choice and do not silently weaken requirements.
