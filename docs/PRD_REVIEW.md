# MobiDesk PRD v1.2 — Critique Traceability

**Date:** 3 November 2026.

v1.2 incorporates the critique that earlier drafts read as a wishlist or forensic audit rather than a buildable product specification.

| Critique | v1.2 response |
|---|---|
| Code treated as source of truth | PRD is normative; code/audits are evidence only |
| Client-supplied VM ID could be trusted | Client sends no destination; broker derives exactly one assignment |
| Windows guest authentication unclear | Separate Windows NLA credential prompt; per-session manual credentials recommended for pilot |
| JWT expiry mistaken for immediate revocation | Check auth.sessions at grant; re-check session and assignment during relay; <=30s teardown |
| RDP server identity undefined | Internal CA or per-VM fingerprint pin; no accept-any option |
| Local bridge could be hijacked | Prefer descriptor/socketpair/Unix socket; one-use authenticated loopback fallback and hijack test |
| AOA strings treated as trusted dock identity | R2 pairing with explicit confirmation and dock-key challenge-response |
| Broker abuse controls absent | Entropy, one-use, rate, concurrency, bounds, enumeration and DoS controls required |
| WSS assumed equivalent to FreeRDP raw transport | Explicit pinned-version API/framing spike required |
| Android rendering/activity boundary vague | Native RdpSessionActivity and FreeRDP surface selected as R1 target |
| Dynamic display output overpromised | Session/decoder separated from render surface; validate RDPEDISP/resize, reconnect if unsupported |
| Dock transcoding assumed without evidence | R2 comparative phone-vs-Pi benchmark before decision |
| FGS/background lifecycle unspecified | Foreground-only R1 session; platform FGS not assumed |
| NFRs vague or vanity metrics | Concrete reliability, FPS, frame latency, input latency, cleanup and network-recovery gates |
| Owners and dates invented | Project Owner accountable; Lab IT consulted; approvals explicitly open |

## Known unresolved issues
Physical reference device, VM host and persistence model, guest-account operations, distribution/signing, telemetry retention and broker deployment endpoint remain owner decisions. See OPEN_ITEMS.md.
