# Independent Review Prompt — FP-02 Student Dashboard and Workspace

You are an independent senior Android/Kotlin/Compose and application-security reviewer. Review the completed FP-02 implementation in `master43721/mobidesk-android`.

**Do not edit code during this review.** Verify the implementation against the latest current main, normative PRD v1.2, parent engineering docs and every FP-02 feature document.

## 1. Required documents and evidence

Read:
- `docs/MobiDesk_PRD_v1.2.pdf`
- `docs/FEATURE_INVENTORY.md`
- `docs/DESIGN.md`
- `docs/API_CONTRACT.md`
- `docs/TECHNICAL_SPEC.md`
- `docs/SECURITY_THREAT_MODEL.md`
- `docs/TEST_PLAN.md`
- `docs/TRACEABILITY_CHECK.md`
- every file under `docs/FEATURE_SPECIFICATIONS/FP-02/`
- related FP-01/FP-03/FP-04/FP-05/FP-06 contracts and progress documents
- complete implementation diff, tests, reports, manifest/build/dependency changes.

## 2. Ownership and scope checks

1. FP-01 is the only authority for AuthLevel/sign-in/session restore/sign-out. Confirm FP-02 enters only after `FULLY_AUTHENTICATED`.
2. FP-03 owns current-user profile/assignment authorization and exactly-one semantics. Confirm FP-02 does not query/select/alter assignments itself.
3. FP-04 owns broker VM status/probe. Confirm FP-02 consumes the approved `GET /v1/vm-status` contract.
4. FP-05 owns Windows credential prompt/NLA. Confirm FP-02 only hands off Connect.
5. FP-06+ own grant/relay/FreeRDP. Confirm no session/grant/RDP logic leaked into FP-02.
6. FP-23..FP-28 own admin workspace/operations. Confirm no admin controls are visible.
7. Dock is R2. Confirm the R1 placeholder has no action.

## 3. Requirement audit

For every FP02-FR-001..FP02-FR-018, provide:
- exact requirement;
- source file/class/function/line evidence;
- test ID and actual result;
- PASS/PARTIAL/FAIL/BLOCKED/NOT TESTED;
- remaining gap.

Check parent FR-AUTH-02/FR-VM-01/FR-VM-04/FR-VM-05 mapping. Do not claim the dashboard proves server-side RLS, assignment enforcement or broker readiness.

## 4. Critical behavior/security review

Independently exercise:
- FP-01 initialization, signed-out, recovery-only and fully-authenticated transitions.
- User A vs user B profile and assignment.
- No profile / no assignment / exactly one / duplicate / inactive / unmapped assignment.
- Label missing/unsafe handling.
- Every lifecycle state: STOPPED, STARTING, RUNNING, STOPPING, ERROR, UNKNOWN.
- Every transport state: NOT_CHECKED, TCP_UNREACHABLE, TCP_REACHABLE, RDP_NEGOTIATION_OK, UNKNOWN.
- Missing/invalid status fields and malformed/unrecognized enum values.
- 15-second staleness boundary.
- Refresh failure after previous Ready result.
- Connect enabled/disabled truth table for all input combinations.
- Stale Connect tap performs fresh refresh before FP-05 handoff.
- Fresh status becomes not-ready and prevents handoff.
- Duplicate taps/parallel refresh.
- Sign-out/session expiry/account switch clears prior student data.
- Settings/sign-out delegation.
- Dock placeholder only.

Verify a WorkspaceRepository and StatusRepository fake test seam. Prove that a status/assignment response cannot cause the UI to choose a VM target, expose endpoint metadata or bypass server authorization.

## 5. API and privacy review

- Status call must use current Supabase access JWT.
- No request body, query or VM/endpoint selector.
- Verify DTO contains only lifecycle, transport, sampledAt, probeDurationMs, staleAfterSeconds and requestId.
- Verify status response/UI do not contain VM ID, display name, endpoint, hostname or port.
- Profile/assignment display model contains only approved user-facing fields and a safe Cloud PC label.
- Verify no other-student data, roll number, Guac fields, hostnames, IPs, port, guest credential, JWT, grant, Authorization header or raw exception in UI/state/navigation/SavedState/logs/crash/analytics.
- Verify RLS is actually proven by FP-03 integration/negative tests; do not infer this from a green Compose test.
- Verify Connect is not an authorization control and broker rechecks at grant time.

## 6. Exact status and message review

Confirm:
- RUNNING alone does not enable Connect.
- TCP_REACHABLE never enables Connect.
- Only RUNNING + RDP_NEGOTIATION_OK + fresh sample + one valid assignment + FP-01 fully authenticated enables Connect.
- Status is stale at the contract boundary; previous Ready state cannot remain actionable after refresh failure.
- Exact PRD English copy is used for ASSIGN-01/02/03, VM-01/02/03 and AUTH-01.
- Retry policy and no-retry policy match API_CONTRACT.md §7.

## 7. Tests, accessibility and build

Run all FP02-CORE-01..30 tests and relevant current baseline tests. Inspect the actual report, not just Gradle exit status. Record emulator/physical device, Android/API, branch/base SHA and app commit.

Verify:
- TalkBack labels/order/announcements.
- 1.3x and 1.5x font scaling.
- status not communicated through color alone.
- disabled-state semantics for Connect.
- no clipped status/time/error copy.
- current min/compile/target SDK, Java/Kotlin target and ABI baseline preserved.
- no unrelated Gradle/dependency/manifest change.
- no new unapproved endpoint or hard-coded server address.

## 8. External gates

A fake StatusRepository does not prove the broker endpoint is deployed. A reachable SSH host does not prove a stable HTTPS status route. A ready dashboard does not prove Windows NLA or RDP connection. Mark missing broker/FP-03/FP-01 integration, physical device or server evidence BLOCKED/NOT TESTED.

## 9. Required review output

1. Verdict: APPROVE / REQUEST CHANGES / BLOCKED / NOT READY.
2. Summary of what is implemented vs mocked/documented.
3. P0–P3 findings with file/line, requirement, impact, reproduction and recommended fix.
4. FP02-FR-001..018 traceability matrix.
5. CORE-01..30 test matrix with command/result/report/environment.
6. Connect eligibility truth-table evidence.
7. API/RLS/privacy/security audit.
8. Accessibility review.
9. Cross-feature ownership and PRD consistency report.
10. Diff/build/dependency/base-branch report.
11. External dependency/release-gate matrix.
12. Merge recommendation and exact remaining closure evidence.

Be strict: compilation is not acceptance; a fake status test is not broker verification; a Ready button is not RDP success; no requirement may be marked PASS without inspectable evidence.
