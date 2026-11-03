# MobiDesk R1 open items

Status as of 2026-11-03. The Project Owner is the accountable approver; no approval is inferred from the document being shared.

1. **Approve PRD v1.2 and architecture.** The PDF is explicitly an owner-review draft and says approval is pending. Until recorded, treat it as the proposed target and avoid irreversible production changes.
2. **Freeze source baseline.** Recheck repository branch/commit and pinned FreeRDP gitlink before the G0 freeze; current inspection was `main` at `4986006`.
3. **Reconcile assignment/endpoint discrepancy.** The 2026-10-30 audit reported five student assignments over five Windows VMs. PRD v1.2 says only four endpoints are approved and VM4 remains unverified/excluded. Recheck live Supabase state and identify the exact owner-approved endpoint set. Do not reassign a student or route to VM4 until verified and approved.
4. **Approve active-session/revocation lookup.** Broker must validate the Supabase JWT including `session_id`, then check active session state at issuance and during the relay. Agree on a narrowly scoped server-side mechanism; no service-role key in the app.
5. **Provide a stable HTTPS/WSS route.** A Cloudflare Quick Tunnel is temporary. The Cloudflare account was previously found without an owned DNS zone. Choose an owner-controlled hostname/route; do not expose broker port directly or public RDP.
6. **Review host TCP/3389 and firewall exposure.** Read-only check found host `xrdp` active and bound to a wildcard address. Internet reachability is unknown; UFW status could not be read without sudo. Preserve the GUI path, obtain owner/admin firewall evidence, and ensure no guest RDP is exposed publicly before R1.
7. **Provision Windows lab credentials.** Each pilot user needs a separate least-privilege Windows account and reset/expiry process. Confirm account-lockout and failed-logon monitoring with lab IT. Windows credentials are never Supabase credentials.
8. **Create and verify per-VM RDP certificate pins or approved internal CA.** Define secure generation, independent verification, rotation and rollback. Missing/mismatched pin must fail closed.
9. **Prove pinned FreeRDP transport API.** Inspect exact v3.32.0 Android source for Activity, surface, credentials, certificate callback and protected raw-stream injection; perform the RDPEDISP/surface lifecycle spike. If protected local transport is unsupported, stop rather than ship an unauthenticated localhost bridge.
10. **Confirm physical reference device/network.** PRD names Pixel 6 / Android 16 / API 36; confirm actual device or obtain Owner-approved replacement. Emulator tests are not physical R1 evidence.
11. **Owner-controlled signing and distribution.** Select Play/App Signing or private pilot distribution and provide/store the owner-controlled signing/upload key through a secure owner-managed process, never Git/chat.
12. **Operations/data lifecycle.** Confirm VM disk persistence, backup cadence, restore drill, student-data retention/semester-end deletion, support owner, maintenance window and rollback procedure.

No server, firewall, tunnel, Supabase, VM or signing-key changes have been made in response to these open items.