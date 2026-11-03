# MobiDesk — Repository Baseline Audit

**Snapshot:** 3 November 2026 · **Scope:** master43721/mobidesk-android only.  
This is an evidence snapshot, not the normative product specification. PRD.md defines desired behaviour.

## Evidence labels
- **VERIFIED:** observed in repository/configuration or executed test at the snapshot.
- **REPORTED:** described in a previous handoff but not re-tested here.
- **PROPOSED:** target design not yet implemented or proven.
- **UNKNOWN:** evidence unavailable; owner or technical check required.
- **BLOCKING:** must be resolved before the dependent pilot gate.

## Verified baseline
- Canonical repository is a native Kotlin + Jetpack Compose Android application.
- Supabase email/password authentication and session restoration are present; student-scoped profile and assignment reads use existing RLS.
- Self-registration is absent; existing roster provisioning remains authoritative.
- Official FreeRDP Android source is included as a pinned git submodule; baseline records v3.32.0.
- Native FreeRDP libraries were packaged into a debug APK in the historical build evidence.
- Existing repository documentation includes build, platform, security, server implementation and Supabase discovery notes.
- The app does not yet complete an end-to-end broker grant, WSS relay, protected local bridge and verified RDP desktop journey.

## Historical evidence and limitations
- A debug APK was built and signature/ABI packaging checked; debug signing is not production release signing.
- JVM build/test evidence exists in prior records. No claim is made here of a live Windows RDP session.
- Historical VM inventory and connectivity checks are snapshots, not a current guarantee.
- Google AI Studio import was a separate static workspace, not a live-synced GitHub source of truth.

## Blocking gaps
1. No proven deployed broker status/grant/revocation service.
2. Exact WSS-to-FreeRDP transport compatibility is not established.
3. No verified RDP certificate trust inventory and rotation procedure.
4. No demonstrated local bridge hijack protection and teardown.
5. No verified Windows guest credential UX or native buffer lifecycle.
6. No physical-device end-to-end RDP evidence.
7. Distribution/signing and VM lifecycle decisions remain open.

## Audit discipline
Record commit SHA, date, test command, device/environment and result when refreshing. Do not copy student records, credentials, endpoint IPs or other secrets into this document. Source code is implementation evidence; it does not supersede an approved PRD requirement.
