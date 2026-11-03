# MobiDesk — PRD v1.2 Traceability Check

**Date:** 3 November 2026  
**Normative source:** MobiDesk PRD v1.2 PDF, 23 pages, current main branch.  
**Scope:** Cross-check of PRD requirement IDs, §9 security controls, §12 errors and G0–G5 gates against the documentation suite.  
**Result:** Requirement coverage is mapped. Three PRD security identifiers are missing from the source and remain a formal numbering gap; they are not invented or silently repaired.

## 1. Coverage summary

| PRD area | Coverage in suite | Status |
|---|---|---|
| FR-AUTH-01 / 02 | TECHNICAL_SPEC §10; DESIGN §§2–5; TEST_PLAN §2.1 | MAPPED |
| FR-VM-01–05 | API_CONTRACT §§4, 8; TECHNICAL_SPEC §§4, 10; DESIGN §4; TEST_PLAN §2.1 | MAPPED |
| FR-SES-01–04 | API_CONTRACT §§5–8; TECHNICAL_SPEC §§4–5, 10; DESIGN §§3, 5; TEST_PLAN §2.1 | MAPPED |
| FR-GUEST-01 / 02 | TECHNICAL_SPEC §8; DESIGN §5.4; SECURITY_THREAT_MODEL §5 SEC-08; TEST_PLAN §2.1 | MAPPED |
| FR-RDP-01–05 | TECHNICAL_SPEC §§6–7; DESIGN §§5.5–5.6; SECURITY_THREAT_MODEL §§5–6; TEST_PLAN §2.1 | MAPPED; implementation spikes BLOCKING |
| FR-INPUT-01–03 | DESIGN §6; TECHNICAL_SPEC §9; TEST_PLAN §2.1 | MAPPED |
| FR-LIFE-01–04 | TECHNICAL_SPEC §5; DESIGN §7; TEST_PLAN §2.1 | MAPPED |
| FR-OBS-01 | TECHNICAL_SPEC §11; SECURITY_THREAT_MODEL §5 SEC-13; TEST_PLAN §2.1 | MAPPED |
| FR-SEC-01–03 | No source definitions in PRD v1.2 | GAP — see §3 |
| FR-SEC-04 | TECHNICAL_SPEC §9; SECURITY_THREAT_MODEL §5 SEC-09; TEST_PLAN §2.1 | MAPPED |
| FR-REL-01 / 02 | TECHNICAL_SPEC §12; OPERATIONS §§2–4; TEST_PLAN §2.1 | MAPPED |
| FR-DOCK-01 / 02 | DESIGN §11; TECHNICAL_SPEC §13; SECURITY_THREAT_MODEL §5 SEC-10; TEST_PLAN §2.1 | MAPPED — R2 only |
| NFR-PERF-01–06 | PERFORMANCE requirements in TEST_PLAN §6; OPS run conditions in DESIGN/OPERATIONS | MAPPED |
| NFR-SEC-01 | TECHNICAL_SPEC §§6, 10; SECURITY_THREAT_MODEL §§5–6; TEST_PLAN §2.3 | MAPPED |
| NFR-PRIV-01 | TECHNICAL_SPEC §11; SECURITY_THREAT_MODEL §5 SEC-13; TEST_PLAN §7 | MAPPED |
| NFR-COMP-01 | BASELINE_AUDIT §3; TECHNICAL_SPEC §12; OPERATIONS §4; TEST_PLAN §2.2 | MAPPED; baseline min29/compile37/target37 |

**Meaning of MAPPED:** the suite identifies a design statement and test ID for the requirement. It does not mean the implementation has passed the test or that its release gate is complete.

## 2. Functional requirement matrix

| PRD requirement | Design location | Test IDs | Gate | Coverage result |
|---|---|---|---|---|
| FR-AUTH-01 | TECHNICAL_SPEC §§3, 4, 10; DESIGN §§2, 3, 5.1, 5.7 | T-AUTH-01–03; E-01 | G1, G4 | MAPPED |
| FR-AUTH-02 | TECHNICAL_SPEC §10.2; SECURITY_THREAT_MODEL §§2, 5 SEC-01; DESIGN §5.2 | T-AUTH-04/05; T-NEG-04 | G1, G2 | MAPPED |
| FR-VM-01 | API_CONTRACT §4; TECHNICAL_SPEC §§4, 10.2; DESIGN §§3, 4 | T-VM-01–03; E-02–04 | G1, G2 | MAPPED |
| FR-VM-02 | API_CONTRACT §§5, 8; TECHNICAL_SPEC §10.1; SECURITY_THREAT_MODEL §5 SEC-01 | T-VM-04/05; T-NEG-02/03 | G2 | MAPPED |
| FR-VM-03 | API_CONTRACT §§2, 5; TECHNICAL_SPEC §§4, 10; SECURITY_THREAT_MODEL §5 SEC-01 | T-VM-06/07; T-NEG-02/03 | G2 | MAPPED |
| FR-VM-04 | API_CONTRACT §4; TECHNICAL_SPEC §§4, 10.1; DESIGN §4.2 | T-VM-08/09; E-05–07 | G2, G4 | MAPPED |
| FR-VM-05 | API_CONTRACT §4; TECHNICAL_SPEC §§4, 5, 10; DESIGN §§3, 4 | T-VM-10/11; T-NEG-03 | G2, G4 | MAPPED |
| FR-SES-01 | API_CONTRACT §5; TECHNICAL_SPEC §§5, 10; DESIGN §§3, 5.5 | T-SES-01/02 | G2 | MAPPED |
| FR-SES-02 | API_CONTRACT §§5–6; TECHNICAL_SPEC §§5, 10; SECURITY_THREAT_MODEL §5 SEC-03 | T-SES-03–05; T-NEG-07 | G2 | MAPPED |
| FR-SES-03 | API_CONTRACT §§2, 6; TECHNICAL_SPEC §§5, 10.1; SECURITY_THREAT_MODEL §5 SEC-04 | T-SES-06/07; T-NEG-05/06 | G2, G4 | MAPPED |
| FR-SES-04 | API_CONTRACT §§6, 8; TECHNICAL_SPEC §§5, 6.3, 10.1; SECURITY_THREAT_MODEL §5 SEC-06 | T-SES-08/09; T-NEG-08 | G2, G4 | MAPPED |
| FR-GUEST-01 | TECHNICAL_SPEC §§4, 8; DESIGN §5.4; SECURITY_THREAT_MODEL §5 SEC-08 | T-GUEST-01–03; E-12 | G3, G4 | MAPPED |
| FR-GUEST-02 | TECHNICAL_SPEC §8; SECURITY_THREAT_MODEL §5 SEC-08; DESIGN §5.4 | T-GUEST-04/05; T-PRIV-01 | G3, G4 | MAPPED |
| FR-RDP-01 | TECHNICAL_SPEC §§6–7; SECURITY_THREAT_MODEL §§5 SEC-11/12, 6 | T-RDP-01/02; T-NEG-17/18 | G3, G4, G5 | MAPPED — TS-RDP-02 is blocking |
| FR-RDP-02 | API_CONTRACT §5; TECHNICAL_SPEC §§6.2, 7.2, 7.3; SECURITY_THREAT_MODEL §5 SEC-07 | T-RDP-03–05; T-NEG-11 | G2, G3, G4 | MAPPED — certificate callback spike blocking |
| FR-RDP-03 | TECHNICAL_SPEC §§3–4, 5, 7.2, 9; DESIGN §§2, 5.5–5.6 | T-RDP-06/07 | G3, G4 | MAPPED — renderer integration unproven |
| FR-RDP-04 | TECHNICAL_SPEC §§6.2, 7.2, 7.3; DESIGN §§7, 12 | T-RDP-08–10 | G3, G4 | MAPPED — RDPEDISP/surface spike blocking |
| FR-RDP-05 | API_CONTRACT §6; TECHNICAL_SPEC §6; SECURITY_THREAT_MODEL §5 SEC-02 | T-BRIDGE-01–07; T-NEG-12–14 | G2, G3 | MAPPED — raw-stream injection unproven |
| FR-INPUT-01 | DESIGN §6.1; TECHNICAL_SPEC §4, 7.2 | T-INPUT-01/02 | G4 | MAPPED |
| FR-INPUT-02 | DESIGN §6.2–6.3; TECHNICAL_SPEC §4 | T-INPUT-03/04 | G4 | MAPPED |
| FR-INPUT-03 | DESIGN §§5.6, 9; TECHNICAL_SPEC §9; SECURITY_THREAT_MODEL §5 SEC-09 | T-INPUT-05; T-NEG-20 | G4, G5 | MAPPED |
| FR-LIFE-01 | TECHNICAL_SPEC §§4–5; DESIGN §§3, 5.5–5.6 | T-LIFE-01/02 | G3, G4 | MAPPED |
| FR-LIFE-02 | TECHNICAL_SPEC §§5, 9; DESIGN §7 | T-LIFE-03/04 | G3, G4 | MAPPED |
| FR-LIFE-03 | API_CONTRACT §§6–7; TECHNICAL_SPEC §§5, 6.3, 10; DESIGN §7 | T-LIFE-05/06; T-NEG-14 | G3, G4 | MAPPED |
| FR-LIFE-04 | TECHNICAL_SPEC §5; SECURITY_THREAT_MODEL §5 SEC-06; DESIGN §7 | T-LIFE-07; T-PERF-05 | G3, G4 | MAPPED |
| FR-OBS-01 | API_CONTRACT §9; TECHNICAL_SPEC §11; SECURITY_THREAT_MODEL §5 SEC-13 | T-OBS-01/02; T-PRIV-01/02 | G4, G5 | MAPPED |
| FR-SEC-01 | No definition exists in PRD v1.2 | T-SEC-01/02 are candidate checks for §9 items 1–2 | G1, G2 | GAP — do not mark a requirement ID closed |
| FR-SEC-02 | No definition exists in PRD v1.2 | T-SEC-03/04 are candidate checks for §9 items 3–5 | G1, G2 | GAP — do not mark a requirement ID closed |
| FR-SEC-03 | No definition exists in PRD v1.2 | T-SEC-05/06 are candidate checks for §9 items 6–8 | G1, G2, G3 | GAP — do not mark a requirement ID closed |
| FR-SEC-04 | TECHNICAL_SPEC §9; SECURITY_THREAT_MODEL §5 SEC-09 | T-SEC-07/08; T-NEG-16 | G3, G4 | MAPPED |
| FR-REL-01 | TECHNICAL_SPEC §12; OPERATIONS §§2–4; SECURITY_THREAT_MODEL §5 SEC-12 | T-REL-01/02; T-COMP-02 | G5 | MAPPED |
| FR-REL-02 | TECHNICAL_SPEC §§8, 12; OPERATIONS §§3–4; SECURITY_THREAT_MODEL §5 SEC-11/13 | T-REL-03–05; T-PRIV-01/02; T-NEG-19 | G5 | MAPPED |
| FR-DOCK-01 | DESIGN §11; TECHNICAL_SPEC §13 | T-DOCK-01/02 | R2 | MAPPED — not applicable to R1 |
| FR-DOCK-02 | DESIGN §11; TECHNICAL_SPEC §13; SECURITY_THREAT_MODEL §5 SEC-10 | T-DOCK-03–05 | R2 | MAPPED — not applicable to R1 |

## 3. FR-SEC-01..03 numbering gap

The v1.2 document uses FR-SEC-04, but no definitions for FR-SEC-01, FR-SEC-02 or FR-SEC-03 are present. PRD §9 contains numbered security controls and supplies sufficient control content to create candidate tests, but it does not supply those missing FR definitions.

The suite therefore:
- maps §9 items 1–14 to named security controls in SECURITY_THREAT_MODEL.md;
- assigns T-SEC-01 onward to tests for those controls;
- maps candidate FR-SEC-01..03 references in TEST_PLAN.md but marks them as a source numbering gap;
- does **not** assert that the candidate test grouping is the official FR definition;
- proposes exact wording in PRD_V1_3_CHANGES.md for owner approval.

**Traceability result for these three identifiers: GAP / OWNER DECISION REQUIRED.** They cannot be signed off as requirement IDs until the PRD is formally amended. The underlying §9 security controls remain testable and are traced in §4 below.

## 4. PRD §9 security control traceability

| PRD §9 item | Design location | Test IDs | Gate | Status |
|---:|---|---|---|---|
| 1 | TECHNICAL_SPEC §§4, 10; API_CONTRACT §§2, 5 | T-SEC-01; T-NEG-01 | G1, G2 | MAPPED |
| 2 | TECHNICAL_SPEC §§4, 10.1; SECURITY_THREAT_MODEL §5 SEC-01 | T-SEC-02; T-NEG-01/02 | G2 | MAPPED |
| 3 | API_CONTRACT §2; TECHNICAL_SPEC §§5, 10.1; SECURITY_THREAT_MODEL §5 SEC-04 | T-SEC-03; T-SES-06/07; T-NEG-05/06 | G2 | MAPPED |
| 4 | API_CONTRACT §§5, 8; TECHNICAL_SPEC §§5, 10.1; SECURITY_THREAT_MODEL §5 SEC-03 | T-SEC-04; T-SES-03–05; T-NEG-07/08 | G2 | MAPPED |
| 5 | API_CONTRACT §§6, 8; TECHNICAL_SPEC §10.1; SECURITY_THREAT_MODEL §5 SEC-06 | T-SEC-05; T-NEG-09/10 | G2, G5 | MAPPED |
| 6 | TECHNICAL_SPEC §6; SECURITY_THREAT_MODEL §5 SEC-02 | T-SEC-06; T-BRIDGE-01–07; T-NEG-12/14 | G3 | MAPPED |
| 7 | API_CONTRACT §5; TECHNICAL_SPEC §§6.2, 7.2–7.3; SECURITY_THREAT_MODEL §5 SEC-07 | T-RDP-03–05; T-NEG-11 | G2, G3 | MAPPED |
| 8 | TECHNICAL_SPEC §§8, 11; SECURITY_THREAT_MODEL §5 SEC-08/13 | T-GUEST-04/05; T-PRIV-01/02; T-NEG-19 | G3, G5 | MAPPED |
| 9 | TECHNICAL_SPEC §9; DESIGN §§5.6, 9; SECURITY_THREAT_MODEL §5 SEC-09 | T-INPUT-05; T-NEG-20 | G4 | MAPPED |
| 10 | TECHNICAL_SPEC §9; SECURITY_THREAT_MODEL §5 SEC-09 | T-SEC-07/08; T-NEG-16 | G4 | MAPPED |
| 11 | TECHNICAL_SPEC §§6.2, 12; OPERATIONS §4 | T-SEC-10; T-NEG-10; T-REL-03 | G2, G5 | MAPPED |
| 12 | API_CONTRACT §9; TECHNICAL_SPEC §10.3; SECURITY_THREAT_MODEL §5 SEC-13 | T-OBS-01/02; T-PRIV-01/02; T-NEG-19 | G2, G5 | MAPPED |
| 13 | TECHNICAL_SPEC §8; OPERATIONS §7; SECURITY_THREAT_MODEL §5 SEC-08 | T-GUEST-06; T-SEC-11; E-12 | G3, G5 | MAPPED |
| 14 | TECHNICAL_SPEC §§7, 12; SECURITY_THREAT_MODEL §5 SEC-12; OPERATIONS §4 | T-SEC-12; T-NEG-18; T-REL-02 | G5 | MAPPED |

## 5. PRD §12 error-code traceability

The exact English strings are in DESIGN §8 and TEST_PLAN §2.4; API_CONTRACT §7 uses the same catalogue.

| Error code | Design location | Test ID | Gate |
|---|---|---|---|
| AUTH-01 | DESIGN §5.7; TECHNICAL_SPEC §10 | E-01; T-AUTH-03 | G1, G4 |
| ASSIGN-01 | DESIGN §§4, 5.7; TECHNICAL_SPEC §10 | E-02; T-VM-01 | G1, G2 |
| ASSIGN-02 | DESIGN §§4, 5.7; TECHNICAL_SPEC §10 | E-03; T-VM-08/09 | G2 |
| ASSIGN-03 | DESIGN §§4, 5.7; TECHNICAL_SPEC §10 | E-04; T-VM-02/03 | G2 |
| VM-01 | DESIGN §4.1; TECHNICAL_SPEC §4 | E-05; T-VM-08 | G2 |
| VM-02 | DESIGN §4.1; TECHNICAL_SPEC §4 | E-06; T-VM-08/09 | G2 |
| VM-03 | DESIGN §4.1; API_CONTRACT §4 | E-07; T-VM-09 | G2 |
| GRANT-01 | DESIGN §§5.5, 8; API_CONTRACT §§5, 7 | E-08; T-SES-01/02 | G2 |
| GRANT-02 | DESIGN §§5.5, 8; API_CONTRACT §§5–7 | E-09; T-SES-03/04 | G2 |
| TUNNEL-01 | DESIGN §§5.5, 8; API_CONTRACT §6 | E-10; T-BRIDGE-01/07 | G2, G3 |
| RDP-CERT-01 | DESIGN §§5.5, 8; TECHNICAL_SPEC §7.2 | E-11; T-RDP-03–05 | G3 |
| RDP-AUTH-01 | DESIGN §§5.4, 8; TECHNICAL_SPEC §8 | E-12; T-GUEST-01–06 | G3 |
| RDP-01 | DESIGN §§5.5, 8; TECHNICAL_SPEC §7 | E-13; T-RDP-01/02 | G3 |
| SESSION-01 | DESIGN §§5.6, 7–8; TECHNICAL_SPEC §5 | E-14; T-LIFE-05/06; T-PERF-06 | G3, G4 |
| SESSION-02 | DESIGN §§5.6, 7–8; API_CONTRACT §6 | E-15; T-SES-06/07 | G2, G4 |
| SESSION-03 | DESIGN §§5.5–5.6, 7–8; TECHNICAL_SPEC §5 | E-16; T-LIFE-07; T-PERF-05 | G3, G4 |

**Required UX test assertion:** English strings must match PRD v1.2 exactly. Telugu copy is a draft and is not release-approved until reviewed by a native Telugu speaker.

## 6. Non-functional requirement traceability

| PRD requirement | Design location | Test ID | Gate |
|---|---|---|---|
| NFR-PERF-01 | TEST_PLAN §6; DESIGN §5.5 | T-PERF-01 | G4 |
| NFR-PERF-02 | TEST_PLAN §6; DESIGN §§5.6, 7 | T-PERF-02 | G4 |
| NFR-PERF-03 | TEST_PLAN §6; TECHNICAL_SPEC §11 | T-PERF-03 | G4 |
| NFR-PERF-04 | TEST_PLAN §6; API_CONTRACT §8 | T-PERF-04 | G4 |
| NFR-PERF-05 | TEST_PLAN §§6, 7; TECHNICAL_SPEC §5 | T-PERF-05 | G4 |
| NFR-PERF-06 | TEST_PLAN §6; DESIGN §7; API_CONTRACT §6 | T-PERF-06 | G4 |
| NFR-SEC-01 | SECURITY_THREAT_MODEL §§5–6; TECHNICAL_SPEC §§6, 10 | T-NEG-01–20; T-SEC-09 | G2, G4, G5 |
| NFR-PRIV-01 | SECURITY_THREAT_MODEL §5 SEC-13; TECHNICAL_SPEC §11 | T-PRIV-01/02 | G4, G5 |
| NFR-COMP-01 | BASELINE_AUDIT §3; TECHNICAL_SPEC §12; OPERATIONS §4 | T-COMP-01/02 | G0, G5 |

## 7. Release gate traceability

| Gate | Design inputs | Mandatory test IDs | Gate owner / decision |
|---|---|---|---|
| G0 | BASELINE_AUDIT; OPEN_ITEMS; TASKS; TECHNICAL_SPEC §2 | T-COMP-01/02; repository/stack checks in audit | Project Owner |
| G1 | API_CONTRACT; openapi.yaml; SECURITY_THREAT_MODEL; TECHNICAL_SPEC; DESIGN; PRD_V1_3_CHANGES | T-AUTH-01–05; contract and schema tests; T-SEC-01–04; error tests E-01–E-16 | Project Owner |
| G2 | API_CONTRACT; openapi.yaml; TECHNICAL_SPEC §§6, 10; SECURITY_THREAT_MODEL | T-VM-01–11; T-SES-01–09; T-SEC-01–06,09–11; T-NEG-01–10 | Project Owner; Lab IT consulted |
| G3 | TECHNICAL_SPEC §§5–9; SECURITY_THREAT_MODEL; OPERATIONS | T-GUEST-01–06; T-RDP-01–10; T-BRIDGE-01–07; T-SEC-06/07/08 | Project Owner; Lab IT consulted |
| G4 | DESIGN §§4–9; TEST_PLAN §§5–7; PERFORMANCE_PLAN (if used as supplemental plan) | T-INPUT-01–05; T-LIFE-01–07; T-PERF-01–06; T-NEG-11–20 | Project Owner; RD-1 physical device required |
| G5 | OPERATIONS; SECURITY_THREAT_MODEL §6; TECHNICAL_SPEC §12; PRD_V1_3_CHANGES | T-REL-01–05; T-PRIV-01/02; T-SEC-10/12; T-COMP-01/02 | Project Owner; Lab IT consulted |

Gates are conjunctive: all required tests and approvals for a gate must pass. A document being committed does not close an implementation or physical-device gate.

## 8. Unmapped or unresolved items

| Gap | Current handling | Closure evidence |
|---|---|---|
| FR-SEC-01..03 identifiers do not exist in v1.2 | Candidate tests are linked to §9 controls, but these FR IDs remain GAP | Owner approves a future PRD revision defining IDs and final wording |
| RD-1 physical device is not verified | Pixel 6 remains the PRD nomination; no physical-device result claimed | Photograph or Settings/build evidence held outside Git, model/OS/API/ABI recorded in private test report |
| FreeRDP raw custom transport and descriptor injection not proven | TS-RDP-02 and bridge tests are blocking | Byte-fidelity test and proof of authenticated descriptor/socket path |
| Pinned FreeRDP certificate-ignore/accept-all behavior not proven safe | No release allowed until strict pin callback is proven | Valid pin passes; wrong/missing/expired pin fails; trust-all paths unreachable |
| RdpSessionActivity/RdpRenderTarget integration not implemented | Source map exists; actual rebind behavior UNKNOWN | Activity and render lifecycle instrumentation; recreate/resize/orientation report |
| MS-RDPEDISP guest negotiation unproven | DISP source path exists; actual Windows test required | Two-resolution test with recorded guest/client dimensions and resize results |
| Session_id verification source/permissions not implemented | Broker-side design only | Approved query adapter and test showing <=30s relay termination |
| Assignment uniqueness/active-row policy unknown | RLS schema facts historical; no claim about a unique active constraint | Live schema review and zero/duplicate assignment test |
| Play/private/direct distribution unresolved | OPERATIONS presents options; no selection | Owner decision and key custody record |
| Backup/restore/retention/semester-end policy unknown | OPERATIONS marks UNKNOWN | Approved policy and successful restore drill |
| Repository visibility conflict | Baseline records public metadata vs private README wording | Owner confirms intended setting and acceptable public contents |
| PRD approval label conflict | Owner says v1.2 final; PDF still says approval pending | Owner records approval decision and future status text action |

## 9. Overall consistency result

- **Mapped:** all explicit FR families except the three missing FR-SEC identifiers; all NFR families; all §9 security control items; all §12 error codes; all G0–G5 gates.
- **Source gaps:** FR-SEC-01..03 missing from the normative PRD; physical RD-1 not confirmed.
- **Implementation blockers:** FreeRDP raw-stream/socket injection, strict certificate callback, session/renderer lifecycle, live MS-RDPEDISP behavior, active session_id revocation adapter and broker deployment.
- **Policy blockers:** owner confirmation of repository visibility, distribution/signing, Windows guest account lockout, VM lifecycle and backup/retention.
- **No PRD change is applied by this suite.** PRD v1.2 remains normative until a later approved revision.

**Sign-off readiness:** documentation traceability is complete subject to the FR-SEC numbering gap being accepted as open. G2–G5 are not implementation-passed and must not be marked complete without evidence.
