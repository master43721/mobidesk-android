# MobiDesk PRD v1.2 — Superseded Intake Note

**Status:** This legacy note is superseded by the complete engineering documentation suite linked below.  
**Normative source:** [MobiDesk_PRD_v1.2.pdf](MobiDesk_PRD_v1.2.pdf). The PDF is unchanged and remains authoritative.  
**Current repository:** master43721/mobidesk-android.

## Current references

- [PRD.md](PRD.md) — navigational section and requirement index; not a replacement for the PDF.
- [BASELINE_AUDIT.md](BASELINE_AUDIT.md) — read-only G0 source/stack/SDK/FreeRDP audit, current commit and branch drift.
- [API_CONTRACT.md](API_CONTRACT.md) and [openapi.yaml](openapi.yaml) — control-plane and WSS contracts.
- [SECURITY_THREAT_MODEL.md](SECURITY_THREAT_MODEL.md) — STRIDE and abuse-case controls.
- [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) — low-level Android, broker and FreeRDP implementation specification.
- [DESIGN.md](DESIGN.md) — UX screen/flow, input and error-copy specification.
- [TEST_PLAN.md](TEST_PLAN.md) — requirement/test/gate mapping and device procedures.
- [TRACEABILITY_CHECK.md](TRACEABILITY_CHECK.md) — traceability gaps and PRD error-code coverage.
- [OPEN_ITEMS.md](OPEN_ITEMS.md), [TASKS.md](TASKS.md), [PROGRESS.md](PROGRESS.md), [OPERATIONS.md](OPERATIONS.md) — owners, estimates, status and operations.

## Evidence limitation

Earlier snapshots of the broker prototype, Supabase assignment counts and lab infrastructure are historical evidence only. They are not current assignment authorization or live VM status. This note intentionally does not reproduce hostnames, IP addresses, account identifiers, student records, credentials or server topology. The broker is not claimed deployed; no end-to-end RDP success is claimed by the presence of these documents.

## Status

The detailed requirement mapping in [TRACEABILITY_CHECK.md](TRACEABILITY_CHECK.md) is the current source for requirement-to-design-to-test-to-gate coverage. Missing PRD identifiers FR-SEC-01 through FR-SEC-03 remain explicitly open; candidate numbering is not treated as normative until the Project Owner approves a PRD revision.
