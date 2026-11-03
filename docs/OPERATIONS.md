# MobiDesk — Pilot Operations Runbook

**Version:** 1.2 · **Date:** 3 November 2026 · **Status:** Proposed; not a deployed runbook.

## Before opening a pilot
- Confirm only the intended cohort is provisioned and each student has exactly one active assignment.
- Confirm guest account creation, reset, lockout, expiry and offboarding with Lab IT.
- Confirm broker deployment, TLS certificates, private VM route, firewall and server-owned endpoint allowlist.
- Verify public TCP/3389 is closed from the internet.
- Load per-VM RDP certificate pins or approved internal CA roots; verify rotation owner.
- Confirm secrets are server-side and never embedded in the APK.
- Run smoke tests for identity, assignment, readiness, grant, relay, NLA, disconnect and cleanup.
- Confirm status probes and relays have timeouts, rate limits and resource caps.
- Confirm rollback build and operator contact path.

## Daily checks
- Broker liveness and readiness; certificate expiry/rotation.
- Grant issue/deny counts and safe reason codes.
- Active relay count, duration, disconnect reasons and stale sessions.
- Private endpoint reachability without exposing addresses in the app.
- Rate-limit/abuse events, unexpected concurrency and resource utilisation.
- Controlled verification that session revocation and assignment changes close relays.

## Incident response
1. Stop new grants if cross-student access, destination substitution or grant replay is suspected.
2. Revoke grants and close active relays.
3. Isolate affected VM if certificate mismatch or guest compromise is suspected.
4. Preserve safe audit evidence; do not copy tokens, passwords, screen data or student PII into chat/logs.
5. Notify owner/Lab IT using approved channels.
6. Restore last reviewed version only after authorization, assignment and certificate controls pass.
7. Record cause, scope, containment, evidence and corrective actions.

## Rollback triggers
Cross-student access; RDP certificate mismatch; public RDP exposure; inability to terminate a revoked session within 30 seconds; local bridge hijack; unbounded relay/resource use; credential leakage; repeated crashes or incomplete cleanup.

## Safe maintenance
No VM power action, student reassignment, guest password reset, firewall change or host modification is implied by the student client. Those operations require owner/Lab IT approval and an auditable procedure. The app must not display a Start button unless an approved control API and authorization model exist.
