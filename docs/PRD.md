# MobiDesk — Product Requirements Document v1.2

**Normative source:** [MobiDesk_PRD_v1.2.pdf](MobiDesk_PRD_v1.2.pdf), the checked-in 23-page PDF.  
**Document role:** navigational index and requirement identifier inventory only. This Markdown page is **not** a line-by-line transcription and does not supersede the PDF. The PDF is not modified by this engineering suite.

## 1. Product baseline

The Project Owner's instruction for this work declares PRD v1.2 the final decided product baseline. The printed cover in the PDF still contains an owner-review status note; that documentary mismatch is listed in [OPEN_ITEMS.md](OPEN_ITEMS.md). No v1.3 proposal in this suite silently changes the normative v1.2 requirements.

## 2. Section map

| PDF section | Subject | Engineering companion |
|---|---|---|
| §1–2 | Purpose, outcomes and scope | [DESIGN.md](DESIGN.md) |
| §3 | Goals / non-goals and product principles | [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) |
| §4 | User journeys and R1/R2 boundary | [DESIGN.md](DESIGN.md) |
| §5 | UX flow and application states | [DESIGN.md](DESIGN.md), [TEST_PLAN.md](TEST_PLAN.md) |
| §6.1–6.8 | Android architecture, APIs, guest credentials, broker, bridge, certificates, rendering and transport | [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md), [API_CONTRACT.md](API_CONTRACT.md), [openapi.yaml](openapi.yaml) |
| §7 | Data and authorization rules | [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md), [SECURITY_THREAT_MODEL.md](SECURITY_THREAT_MODEL.md) |
| §8 | Non-functional and performance requirements | [PERFORMANCE_PLAN.md](PERFORMANCE_PLAN.md), [TEST_PLAN.md](TEST_PLAN.md) |
| §9 | Security requirements and threat controls | [SECURITY_THREAT_MODEL.md](SECURITY_THREAT_MODEL.md) |
| §10–11 | VM eligibility, pilot boundaries and success measures | [OPERATIONS.md](OPERATIONS.md), [TEST_PLAN.md](TEST_PLAN.md) |
| §12 | User-facing error catalogue | [API_CONTRACT.md](API_CONTRACT.md), [DESIGN.md](DESIGN.md), [TEST_PLAN.md](TEST_PLAN.md) |
| §13 | G0–G5 release gates | [TASKS.md](TASKS.md), [PROGRESS.md](PROGRESS.md), [TRACEABILITY_CHECK.md](TRACEABILITY_CHECK.md) |
| §14 | Owners, milestones and dates | [TASKS.md](TASKS.md) |
| §15–16 | Risks, dependencies and supporting notes | [OPEN_ITEMS.md](OPEN_ITEMS.md), [OPERATIONS.md](OPERATIONS.md) |

## 3. Requirement identifiers from the PDF

The following identifiers are transcribed from PRD v1.2. Their acceptance criteria and gates are mapped in [TRACEABILITY_CHECK.md](TRACEABILITY_CHECK.md). A requirement marked NOT MET in the baseline audit is not made complete by having a design document.

### Functional requirements

- **Authentication:** FR-AUTH-01, FR-AUTH-02.
- **VM assignment and readiness:** FR-VM-01 through FR-VM-05.
- **Session authorization and relay:** FR-SES-01 through FR-SES-04.
- **Windows guest authentication:** FR-GUEST-01, FR-GUEST-02.
- **Native RDP client/rendering:** FR-RDP-01 through FR-RDP-05.
- **Touch, pointer and keyboard:** FR-INPUT-01 through FR-INPUT-03.
- **Lifecycle and cleanup:** FR-LIFE-01 through FR-LIFE-04.
- **Telemetry:** FR-OBS-01.
- **Device integrity:** FR-SEC-04.
- **Release integrity:** FR-REL-01, FR-REL-02.
- **Dock (R2):** FR-DOCK-01, FR-DOCK-02.

**Source numbering gap:** FR-SEC-01, FR-SEC-02 and FR-SEC-03 are not defined in the v1.2 PDF, while FR-SEC-04 is present. This is a source gap, not permission to invent normative IDs. Proposed wording is isolated in [PRD_V1_3_CHANGES.md](PRD_V1_3_CHANGES.md); the suite maps the underlying §9 security controls separately.

### Non-functional requirements

NFR-COMP-01; NFR-PERF-01 through NFR-PERF-06; NFR-SEC-01; NFR-PRIV-01.

## 4. Error catalogue in §12

The exact English message text and retry instructions are defined in the PDF. The companion UX/API/test documents map each code without redefining it:

AUTH-01, ASSIGN-01, ASSIGN-02, ASSIGN-03, VM-01, VM-02, VM-03, GRANT-01, GRANT-02, TUNNEL-01, RDP-CERT-01, RDP-AUTH-01, RDP-01, SESSION-01, SESSION-02, SESSION-03.

The English user copy in the engineering suite must match the normative PDF. Telugu draft translations are marked for native-speaker review and are not release-approved.

## 5. Compatibility and reference device

The PDF §8.1 nominates RD-1 as Pixel 6 / Android 16 / API 36 / arm64. Its physical availability and actual test results remain UNKNOWN until the Project Owner verifies the physical device. The repository minimum SDK is API 29; compile/target and ABI preservation is governed by NFR-COMP-01. See [BASELINE_AUDIT.md](BASELINE_AUDIT.md) and [OPEN_ITEMS.md](OPEN_ITEMS.md).

## 6. Change control

- Do not edit this index to redefine a requirement. Update it only when a new approved PRD revision is adopted.
- Any proposed v1.3 wording stays in [PRD_V1_3_CHANGES.md](PRD_V1_3_CHANGES.md) until explicitly approved.
- Existing code and audit evidence do not override the normative PDF.
