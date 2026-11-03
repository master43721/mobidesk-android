# MobiDesk — System Architecture (R1)

**Version:** 1.2 · **Date:** 3 November 2026 · **Status:** Target design, not deployed-state evidence.

## Architecture decision
R1 is a native Kotlin/Jetpack Compose Android client using the pinned FreeRDP Android engine. A dedicated MobiDesk-owned native RDP Activity owns desktop rendering and controls. A trusted session broker authorizes the student's current assignment and relays RDP bytes over WSS to a server-side allowlisted private endpoint.

The Android UI does not connect to VM IPs, public TCP/3389 or hypervisor APIs, and does not request VM power control. Supabase is the identity/profile/assignment system; it is not a live hypervisor status monitor.

## Logical components
| Component | Responsibility | Trust boundary |
|---|---|---|
| Compose screens/ViewModels | Login, profile, assigned-PC card, status and safe errors | Untrusted client |
| Supabase Auth | Student identity and session | Identity authority |
| Supabase Postgres/RLS | Own student profile and assignment records | Data authority |
| Broker API client | User-token HTTPS status/grant requests | Untrusted transport |
| Session coordinator | Guest prompt, grant, WSS, local bridge and cleanup | Client process |
| MobiDesk RdpSessionActivity | Native FreeRDP surface, input and session UI | Client UI |
| FreeRDP v3.32.0 | RDP, NLA, certificate callback, display and input | Protocol engine |
| Local transport adapter | Protected stream between FreeRDP and WSS | Same-app IPC |
| Session broker | JWT/session validation, assignment, grant, WSS relay, limits and audit | Trusted session boundary |
| KVM/libvirt host | VM lifecycle and private endpoint inventory | Trusted infrastructure |
| Windows guest | RDP service, NLA account and desktop workload | Guest identity boundary |
| Future dock | Authenticated external display/input | Untrusted until paired |

## R1 sequence
1. Student signs in through Supabase and restores a valid session.
2. App loads own profile and assignment through RLS.
3. App calls GET /v1/vm-status with the bearer token and no VM ID.
4. Broker verifies caller/session, resolves exactly one assignment and returns lifecycle/readiness without a private endpoint.
5. Student enters separate Windows guest credentials.
6. App calls POST /v1/session-grants with no destination selector.
7. Broker revalidates session/assignment and issues one-use 60-second grant.
8. App opens WSS relay; broker consumes grant and connects to private allowlisted VM TCP/3389.
9. FreeRDP validates the server identity, performs NLA and renders the desktop in the native Activity.
10. Disconnect, revocation or failure closes the relay and releases client/native resources.

## Network and trust boundaries
- HTTPS for control, WSS for relay, private TCP/3389 only from broker to allowlisted guest.
- RDP certificate validation is separate from WSS TLS.
- WSS is a proposed custom binary RDP relay, not an RD Gateway.
- Verify exact pinned FreeRDP v3.32.0 transport APIs; do not assume arbitrary WebSocket support.
- The local FreeRDP bridge prefers socketpair/connected descriptor, then app-private Unix socket. Any TCP fallback needs one-use authentication and one-connection listener shutdown.
- No public RDP or client-supplied target route.
- Future dock is R2 only; choose architecture after power/performance/latency test.

## Required design spikes
1. FreeRDP raw/custom transport API and WSS framing.
2. Native surface attach/detach, rotation and resize; verify MS-RDPEDISP support.
3. Certificate callback and fail-closed policy in pinned client.
4. Protected local transport and local hijack test.
5. RDP readiness probe stronger than a TCP port check.
6. Supabase session revocation query and <=30s relay closure.
