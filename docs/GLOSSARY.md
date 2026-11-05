# MobiDesk Glossary

**Updated:** 5 November 2026  
**Scope:** Shared product, Android, broker and Codespaces vocabulary.

| Term | Meaning |
|---|---|
| FP | Functional Point: one independently scoped product capability with requirements, dependencies, tests and acceptance evidence. |
| FP-01 | Authentication, session lifecycle, recovery and sign-out owner. |
| FP-02 | Student Dashboard and Workspace; owns the Android StatusRepository, UI composition, freshness, refresh and Connect orchestration. |
| FP-03 | Student profile and authoritative exactly-one assignment result, safe Cloud PC label and assignmentTag source. |
| FP-04 | Broker status endpoint, VM lifecycle/RDP probe, request limits and assignmentTag in status response. |
| FP-05 | Windows guest credential prompt and Network Level Authentication (NLA) handoff. |
| FP-06 | Session grant, secure WebSocket relay and native FreeRDP session path. |
| FULLY_AUTHENTICATED | FP-01 application-level authenticated state that permits protected workspace operations; not interchangeable with the raw Supabase SDK Authenticated event. |
| RLS | Row-Level Security: database policies limiting each user to authorized rows. |
| VM lifecycle | Power/management state: STOPPED, STARTING, RUNNING, STOPPING, ERROR or UNKNOWN. |
| RDP transport | Remote Desktop reachability/negotiation state: NOT_CHECKED, TCP_UNREACHABLE, TCP_REACHABLE, RDP_NEGOTIATION_OK or UNKNOWN. TCP reachability is not RDP negotiation or NLA success. |
| sampledAt | Server UTC timestamp for display/diagnostics. The Android freshness gate does not compare it with handset wall time. |
| Monotonic clock | Elapsed-time source that does not jump when the user/device changes calendar time. Android uses SystemClock.elapsedRealtime() for freshness and cooldown. |
| Receipt-age upper-bound | Conservative status age estimate: request round-trip duration plus monotonic time since full response receipt. Requires the broker to return an observation sampled after the current request started. |
| assignmentTag | Opaque equality-only marker tying the FP-03 displayed assignment to the FP-04 status sample for the same assignment revision. Not an ID, secret, credential or target selector. |
| StatusRepository | Android FP-02 client adapter for authenticated GET /v1/vm-status; server implementation is owned by FP-04. |
| Connect affordance | UI action letting a valid student request a fresh status check. It is not authorization and never hands off from cached state. |
| Fresh known-negative | A still-fresh status sample that is definitively not connectable (e.g. STOPPED, STARTING, ERROR, TCP_UNREACHABLE or TCP_REACHABLE). Once stale, it becomes indeterminate. |
| Retry-After | HTTP response header that tells the client when a rate-limited request may be retried. MobiDesk status uses delta seconds plus optional safe retryAfterSeconds. |
| Codespace | Cloud development environment attached to a GitHub repository/branch. Git history and CI remain the source of truth; uncommitted workspace files must be preserved. | 
| MobiLab host | Owner-reported HP PC running Ubuntu Server 24.04 LTS and KVM/QEMU/libvirt; persistent VM/runtime environment distinct from Codespaces. | 
| Tailscale SSH | Authenticated SSH path over the private Tailscale network; configured tagged access does not by itself prove a local `ssh` alias resolves. | 
| Guacamole | Browser-based remote desktop gateway; last reported local listener was loopback-only on port 8090. | 
| Cloudflare Quick Tunnel | Temporary Cloudflare tunnel URL forwarding to a local service; do not use it as a stable production broker/WSS hostname. |
| Devcontainer | Versioned .devcontainer configuration used to create a reproducible development environment. |
| FreeRDP gitlink | Pinned third-party FreeRDP source revision referenced by Git submodule; do not update it casually. |
| NLA | Windows Network Level Authentication; guest credentials are entered separately and are not sent to the broker. |
| WSS | WebSocket over TLS used for the broker relay control/data path. |
| R1 / R2 | Product release scope: R1 student Cloud PC pilot; R2 Dock/spatial-computing features. |

## Status vocabulary

- **VERIFIED:** directly observed in repository, tool result or test artifact.
- **OWNER-REPORTED:** communicated by the Project Owner but not independently verified in the checked branch.
- **PROPOSED:** design recommendation awaiting explicit approval.
- **BLOCKED:** cannot proceed safely until an upstream contract or resource is available.
- **NOT RUN:** test/build/device result not yet executed; never a synonym for PASS.
