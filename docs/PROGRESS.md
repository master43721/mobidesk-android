# MobiDesk — Progress Snapshot

**Version:** 1.2 · **Date:** 3 November 2026  
**Canonical repository:** master43721/mobidesk-android  
**Base branch inspected:** main at 4986006a5e6823206f2c179bfe8f24d342ede5eb.  
**Repository scope:** native Kotlin + Jetpack Compose Android app and pinned FreeRDP submodule only.

## Current verified baseline
- Existing repository docs include Android build report, platform notes, security notes, server implementation plan and Supabase discovery.
- Existing app authenticates through Supabase and reads student-scoped profile/assignment data under RLS.
- FreeRDP Android core is pinned as a submodule and native libraries were included in a debug APK build.
- Existing app does not yet provide an end-to-end broker grant, WSS relay, secure local transport bridge or verified live RDP session.
- An APK compile/build result is not proof of an authenticated Windows desktop session.
- No production broker deployment, physical-device end-to-end RDP verification or release-signed artifact is claimed here.

## This documentation release
- PRD v1.2 separates normative desired behaviour from repository audit evidence.
- Decision register covers identity, assignment, guest credentials, session grants, revocation, certificate trust, bridge, Activity boundary, backgrounding and Dock R2 spike.
- Engineering plans include API contract, security threat model, performance plan, testable task list, operations and open decisions.
- Existing docs are retained; this release adds/updates only documentation.

## Critical blockers
1. Confirm VM host/location and persistent vs ephemeral lifecycle.
2. Approve Windows guest-account lifecycle with Lab IT.
3. Prove FreeRDP v3.32.0 can use the proposed WSS/raw relay transport.
4. Implement server-side assignment/grant/revocation and private relay.
5. Resolve RDP certificate inventory and certificate rotation.
6. Select and test a physical Android reference device.
7. Select distribution and signing-key custody.

## Next sequence
1. Owner review and ratification of PRD/decisions.
2. Close platform and operational owner decisions.
3. Prove FreeRDP transport and native rendering spikes.
4. Implement broker status and grant/revocation controls.
5. Implement RDP Activity, Windows credentials and bridge.
6. Execute tests/performance gates, then owner go/no-go.

**Evidence rule:** statuses are not inferred from plans. Each task needs a direct test/build/deployment record before it can be marked PASS.
