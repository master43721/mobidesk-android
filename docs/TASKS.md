# MobiDesk — G0–G5 Delivery Plan and Reforecast

**Baseline:** PRD v1.2 dated 3 November 2026  
**Planning model:** one Project Owner/engineer with AI assistance; approximately six focused engineering hours per working day; tasks are sequential where they share one owner. Estimates are planning ranges, not completed work or a promise. Weekend/holiday work is not assumed.

## 1. Gate estimates and forecast

| Gate | Work package | Effort estimate | Dependencies | PRD §14 target | Reforecast | Risk |
|---|---|---:|---|---|---|---|
| G0 — Product/baseline freeze | Repository/stack audit, build/submodule snapshot, PRD-to-repo reconciliation, branch and change-control freeze | 4–6 h | Owner PRD direction; GitHub access | 4 Nov 2026 | 4 Nov 2026 | LOW — stack/repo match verified; visibility/status questions remain |
| G1 — Contract and threat-model freeze | Exact requirement inventory; API/OpenAPI; error schema; security/STRIDE; data/RLS model; UX/technical design; traceability and review | 24–32 h | G0; owner confirmation of unresolved policy choices | Broker design target 6 Nov; API/authorization completion target 8 Nov | 11 Nov 2026 | HIGH — more complete than an API-only draft; security and API decisions must be testable |
| G2 — Broker ready | Session/JWT validation; active session_id check; unique assignment resolution; VM4 exclusion; lifecycle/readiness probe; grants; rate/concurrency; certificate data; WSS relay; periodic revocation; operations metrics | 48–64 h | G1; server-side Supabase access; VM allowlist; stable TLS/WSS route; certificate policy | 8 Nov 2026 | 25 Nov 2026 | HIGH — prototype is not deployed or conforming; stable WSS and session lookup are unresolved |
| G3 — Android native session ready | FreeRDP source/API spikes; SessionCoordinator; CredentialPrompt; protected LocalBridge; RdpSessionActivity; RdpRenderTarget; certificate/NLA callbacks; input model; rotation/cleanup; raw-WSS transport integration | 80–120 h | G1; G2 contract; exact pinned FreeRDP APIs; test VM and certificate | 10 Nov 2026 | 18–24 Dec 2026 | VERY HIGH — 10 Nov is at risk; no native session or protected raw-stream bridge is evidenced |
| G4 — Physical R1 acceptance | RD-1 confirmation; 20 connection attempts; 120-second FPS workload; input latency; 20 cleanup cycles; recovery; security negatives; packet/log/static scan | 36–48 h | G2 and G3; available physical device; controlled Wi-Fi; four approved endpoints | 12 Nov 2026 | 30 Dec 2026–5 Jan 2027 | HIGH — physical RD-1 device availability is UNKNOWN |
| G5 — Release and operations ready | Distribution decision; owner signing/upload key; reproducible artifact; signing/hash/source record; pin rotation runbook; backup/restore; retention; incident/rollback; support readiness | 24–32 h | G4; owner/Lab IT approvals; distribution and data policies | 13 Nov 2026 | 7–15 Jan 2027 | HIGH — channel, key custody, backup/retention and VM lifecycle are open |

**Total estimated engineering effort:** 216–302 hours (midpoint approximately 259 hours), equivalent to approximately 36–51 focused working days at six hours per weekday. Forecast dates are ranges for the one-person Project Owner + AI-assistance model, exclude weekends/holidays, and are not a delivery promise. The upper end pushes G5 to 15 Jan 2027. A failed raw-stream/surface spike, unavailable VM endpoint, missing Windows guest account policy or unavailable physical device extends the dates.

## 2. Task breakdown

| Task | Estimate | Depends on | Owner | Target / evidence |
|---|---:|---|---|---|
| MD-G0-01 Verify repository identity, stack, branch, Gradle, ABIs, manifest and gitlink | 4–6 h | None | Project Owner | DONE 3 Nov 2026; docs/BASELINE_AUDIT.md |
| MD-G0-02 Resolve repo visibility and record PRD status authority | 1–2 h | MD-G0-01 | Project Owner | 5 Nov; owner decision in OPEN_ITEMS.md |
| MD-G1-01 Normalize the PRD requirement inventory without editing PRD | 4–6 h | G0 | Project Owner | 6 Nov; TRACEABILITY_CHECK.md |
| MD-G1-02 Freeze control API, OpenAPI 3.1 and contract-test checklist | 8–12 h | MD-G1-01 | Project Owner | 9 Nov; API_CONTRACT.md / openapi.yaml |
| MD-G1-03 Complete STRIDE threat model and mitigations/tests | 6–8 h | MD-G1-01 | Project Owner | 10 Nov; SECURITY_THREAT_MODEL.md |
| MD-G1-04 Complete low-level technical specification and spike procedures | 8–12 h | MD-G1-02, MD-G1-03 | Project Owner | 11 Nov; TECHNICAL_SPEC.md |
| MD-G1-05 Complete product/UX design and copy matrix | 6–8 h | MD-G1-01 | Project Owner | 11 Nov; DESIGN.md |
| MD-G1-06 Complete test and requirement traceability plans | 6–8 h | MD-G1-02 to MD-G1-05 | Project Owner | 11 Nov; TEST_PLAN.md / TRACEABILITY_CHECK.md |
| MD-G2-01 Prove server-side Supabase JWT + active session_id validation | 8–12 h | G1; owner-approved server credential path | Project Owner | 17 Nov; authorization test record |
| MD-G2-02 Implement unique assignment resolution and read-only lifecycle/status probe | 8–12 h | MD-G2-01; approved VM inventory | Project Owner + Lab IT consulted | 19 Nov; status contract tests |
| MD-G2-03 Implement one-use grant, hash storage, TTL, rate and concurrency limits | 10–14 h | MD-G2-01/02 | Project Owner | 20 Nov; replay/race/rate tests |
| MD-G2-04 Implement WSS binary relay, pin metadata, queue bounds and cleanup | 14–18 h | MD-G2-03; stable WSS/TLS route | Project Owner | 24 Nov; relay integration tests |
| MD-G2-05 Implement active session/assignment recheck and 30-second termination | 8–10 h | MD-G2-04 | Project Owner | 25 Nov; revocation test |
| MD-G3-01 Inspect pinned FreeRDP raw transport, callbacks, renderer and RDPEDISP | 12–18 h | G1; exact v3.32.0 gitlink | Project Owner | 2 Dec; pinned-source spike report |
| MD-G3-02 Prove descriptor/socket bridge and malicious-local-app rejection | 10–14 h | MD-G3-01 | Project Owner | 4 Dec; local-hijack test |
| MD-G3-03 Implement SessionCoordinator, BrokerClient and StatusRepository | 12–16 h | G2; MD-G3-01 | Project Owner | 8 Dec; unit/contract tests |
| MD-G3-04 Implement RdpSessionActivity, RdpRenderTarget and input overlay | 16–24 h | MD-G3-01 | Project Owner | 11 Dec; surface/input instrumentation |
| MD-G3-05 Implement separate credential prompt, NLA/certificate callbacks and cleanup | 12–18 h | MD-G3-02/04 | Project Owner + Lab IT consulted | 15 Dec; credential/cert lifecycle tests |
| MD-G3-06 Connect WSS relay to exact FreeRDP custom stream and prove display/session lifecycle | 18–30 h | MD-G2-04; MD-G3-02/05 | Project Owner | 18–24 Dec forecast; end-to-end RDP proof |
| MD-G4-01 Confirm RD-1 physical device and controlled Wi-Fi baseline | 2–4 h | Owner device access | Project Owner | Before G4; device/build evidence |
| MD-G4-02 Run 20 attempts, FPS, input latency and recovery procedures | 16–22 h | G3; test VM/route | Project Owner | 4 Jan 2027; raw attempt ledger and performance report |
| MD-G4-03 Run 20-cycle cleanup, negative security, log/packet/static scans | 12–16 h | G2/G3 | Project Owner | 5 Jan 2027; signed test evidence |
| MD-G5-01 Decide Play vs private channel and key custody | 2–4 h | Owner decision | Project Owner | 6 Nov decision; see OPERATIONS.md |
| MD-G5-02 Build reproducible signed artifact and record certificate/hash/source | 6–8 h | G4; key custody | Project Owner | 7 Jan 2027; release evidence |
| MD-G5-03 Approve pin rotation, Windows lockout, backup/restore/retention and semester-end policy | 8–12 h | Lab IT/owner | Project Owner + Lab IT consulted | 13 Jan 2027; approved runbooks |
| MD-G5-04 Incident, support, rollback and pilot go/no-go | 8–10 h | G4/G5 evidence | Project Owner | 15 Jan 2027; signed go/no-go record |

## 3. Reforecast of PRD §14

| PRD §14 item | Original target | Current forecast | Reason |
|---|---|---|---|
| PRD and architecture approval | 4 Nov 2026 | 4 Nov 2026 | Owner has declared v1.2 final in the current instruction; PDF cover wording still needs an approval-status record. |
| Repository/commit freeze | 4 Nov 2026 | 4 Nov 2026 | G0 technical audit passed; current main and pinned gitlink recorded. |
| Windows account provisioning/reset | 5 Nov 2026 | 6 Nov 2026 | Lab IT approval and lockout policy are external dependencies. |
| Broker session-active lookup and readiness design | 6 Nov 2026 | 11 Nov 2026 | Requires a validated Supabase session lookup path and bounded RDP-negotiation probe contract. |
| Stable WSS hostname/route | 6 Nov 2026 | 12 Nov 2026 | Owner DNS/TLS/network decision plus handshake/idle test required. |
| Broker API/authorization changes | 8 Nov 2026 | 25 Nov 2026 | Estimated 48–64 hours including session checks, unique assignment, status, grants, relay, rate limits and revocation. |
| Native FreeRDP session integration | 10 Nov 2026 | 18–24 Dec 2026 | **AT RISK.** Estimated 80–120 hours, exact API/surface/bridge support unproven; only five working days remain before 10 Nov. |
| Physical R1 performance/security acceptance | 12 Nov 2026 | 30 Dec 2026–5 Jan 2027 | Depends on broker and native RDP integration plus RD-1 confirmation. |
| Signing and pilot distribution | 13 Nov 2026 | 7–15 Jan 2027 | Depends on G4, distribution choice, key custody and operations policies. |

## 4. Critical path and stop conditions

Critical path: G1 contracts/threat model -> G2 broker session/assignment/readiness/relay -> G3 FreeRDP raw transport + render target + NLA -> G4 physical proof -> G5 release/operations.

Stop dependent implementation if:
- the pinned FreeRDP client cannot safely consume the raw byte-stream relay;
- a socketpair/peer-verified Unix socket or authenticated one-shot loopback bridge cannot be proven;
- the RDP certificate cannot be validated fail-closed;
- the broker cannot verify session_id and assignment revocation;
- VM4 has not been verified but is proposed for any mapping;
- the actual physical RD-1 device is unavailable for performance acceptance.

The estimates do not authorize source, Gradle, submodule, CI, database, server or VM changes in this documentation task.
