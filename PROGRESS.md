# MobiDesk R1 progress

**Last updated:** 2026-11-03. **Target:** PRD v1.2, which is still an owner-review draft. No release gate is passed solely because a build succeeds.

## Verified baseline

- Canonical private GitHub repo was clean on `main` at `4986006` during inspection; FreeRDP v3.32.0 is pinned at `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`.
- Native Kotlin/Jetpack Compose shell, Supabase Auth/recovery, student/profile/assigned-VM reads, and truthful empty/error states exist. No RDP session launch, broker status UI, or working desktop connection is implemented.
- The historical 2026-10-27 debug APK and build evidence are described in `docs/ANDROID_BUILD_REPORT.md`; it is debug-signed and not the final R1 artifact.
- A separate out-of-repo broker prototype has 23 local unit tests reported passing. It is not deployed and has material gaps against PRD v1.2; see `docs/PRD_TRACEABILITY.md`.
- Tailscale SSH identity to the server was verified. A read-only inventory confirmed five Windows VMs running and two other configured guests shut off. The broker is not deployed. A host xrdp listener binds wildcard TCP/3389; outside reachability and firewall state remain unverified. No server configuration was changed.

## Gates

| Gate | Status | Evidence / blocker |
|---|---|---|
| G0 Product/baseline freeze | **BLOCKED** | PRD owner approval and final endpoint/repo freeze pending. |
| G1 Contract/threat-model freeze | **NOT STARTED** | API contract, active-session lookup, revocation, RDP pin and local-bridge design/tests incomplete. |
| G2 Broker ready | **NOT STARTED** | No broker deployed; status API, session revocation, stable TLS/WSS route and VM allowlist reconciliation incomplete. |
| G3 Android session ready | **NOT STARTED** | No MobiDesk native session Activity, protected local bridge, credential prompt or certificate callback. |
| G4 Physical R1 acceptance | **NOT STARTED** | No physical RDP attempt, performance, negative authorization or cleanup evidence. |
| G5 Release/operations ready | **NOT STARTED** | No owner-signed artifact or approved operations/backup/retention/rollback evidence. |

The detailed checklist is `TODO.md`; acceptance methods are in `TEST_PLAN.md`; blockers are in `OPEN_ITEMS.md`. Update this file only when evidence is actually collected.