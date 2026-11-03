# MobiDesk — Operations, Distribution and Release Runbook

**Version:** 1.0 · **Date:** 3 November 2026  
**Normative source:** MobiDesk PRD v1.2, §§8–10, 13–16.  
**Status:** Proposed pilot runbook. Current backup, retention, guest-account and production broker state are explicitly UNKNOWN unless tagged otherwise.

## 1. Operational ownership

| Responsibility | Accountable / consulted |
|---|---|
| Product go/no-go, distribution, signing custody, privacy and incident decisions | Project Owner |
| Windows guest account policy, lockout, VM health, certificates, host firewall and backup/restore | Project Owner accountable; Lab IT consulted |
| Android artifact build and verification | Project Owner |
| Broker deployment, allowlist and grant/relay operations | Project Owner |
| R1 user support and identity provisioning | Project Owner; Lab IT consulted |
| R2 dock pairing and device key lifecycle | Project Owner; Lab IT consulted |

No role, hostname, private IP, service secret, student credential or VM endpoint belongs in this document.

## 2. Distribution choice

The Project Owner must choose one channel before G5. Do not create a production release signing artifact before the decision.

### 2.1 Option A — Public Google Play

- Public listing and eligibility/review obligations apply.
- Use Android App Bundle (AAB) and enroll in Play App Signing.
- Google Play protects/manages the app-signing key; the publisher retains the separate upload key used to sign the bundle submitted to Play.
- The upload key is security-sensitive; store it in owner-controlled encrypted key storage with offline recovery and audited access.
- New apps and app updates submitted to Google Play must target Android 16 / API 36 or higher from 31 August 2026.
- **[VERIFIED: app/build.gradle at main@a6c7282b4652b10b561ca0578c006c6d4b71cbc3]** MobiDesk currently configures targetSdk 37, which is above the API 36 submission threshold. minSdk 29 is a different setting and does not determine Play target API compliance.
- Before release, verify current Play Console app declarations, data safety, privacy policy, support contact, permissions, test access and any current SDK/policy requirements.

Sources:
- [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Android Developers — target SDK requirement](https://developer.android.com/google/play/requirements/target-sdk)
- [Use Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?rd=1)

### 2.2 Option B — Permanently private / organization-only Google Play

Google's target API policy documents an exception for permanently private apps restricted to users in a specific organization and intended only for internal distribution. Confirm that the selected Managed Google Play configuration satisfies the exact policy definition; do not assume any unlisted app or closed testing track qualifies.

- Use organization-restricted distribution and verify that only approved organization accounts can install/update.
- Decide whether to use Play App Signing; if used, retain the separate owner-controlled upload key.
- Do not treat “unlisted”, “closed test” or “internal test” as automatically equivalent to the permanently private exception without checking the current Play Console policy.
- Device/account management and offboarding must be defined by the institution.

### 2.3 Option C — Owner-managed APK / MDM distribution

- The owner holds and operates the release signing key. The same package must remain signed with the same key for normal in-place updates unless an approved key rotation/signing lineage is used.
- Use an institutional MDM/managed deployment system if available; direct sideloading is a controlled fallback, not the default recommendation.
- Restrict APK distribution to the approved cohort; record release version, artifact SHA-256, signer certificate fingerprint, source commit and FreeRDP gitlink.
- Plan revocation/removal of lost devices, account offboarding and update delivery.
- No Play target API waiver is needed merely because an APK is privately distributed outside Play; Android platform compatibility and security requirements still apply.

### 2.4 Decision record

**Current choice: UNKNOWN / OWNER DECISION.** Record the selected option, visibility, upload/signing key owner, access cohort and release date in DECISIONS.md and close OI-08. This runbook does not choose the channel on the owner's behalf.

## 3. Signing and key custody

### 3.1 Current evidence

- **[VERIFIED: app/build.gradle at main@a6c7282b4652b10b561ca0578c006c6d4b71cbc3]** No explicit owner-controlled release signingConfig is present in the inspected Gradle release block.
- **[REPORTED: README.md and docs/ANDROID_BUILD_REPORT.md, historical 27 Oct 2026]** A debug-signed APK was produced. This is not production release signing.
- **[UNKNOWN]** Release/upload key creation, owner custody, backup, rotation and recovery have not been approved.

### 3.2 Required procedure

1. Owner selects Play App Signing or owner-managed signing based on distribution.
2. Generate signing/upload key offline in an owner-controlled environment; use a strong unique keystore password and private-key password.
3. Store the keystore and recovery copy in separate access-controlled encrypted storage. Never put them in Git, chat, project files, ordinary CI variables or APK assets.
4. Limit key access to the Project Owner and an explicitly approved custodian. Record access and backup checks.
5. For Play App Signing, upload key is owner-held; Google-managed app-signing key signs the APKs delivered by Play. Register the correct public certificate fingerprint with any external API provider that requires it.
6. For direct APK/MDM release, owner-held release key signs the distributed package. Preserve the signing certificate across updates.
7. Build reproducibly from a tagged/recorded commit and exact submodule SHA; capture SHA-256, signer fingerprint, package/version, ABI inventory and build environment.
8. Test install, update, rollback eligibility and uninstall/reinstall behavior on RD-1 before pilot.
9. If key loss/compromise is suspected, stop release, follow Play upload-key reset or owner-signed key rotation plan, verify package continuity and notify operators. Never put a replacement keystore into the repository.

## 4. Release build and target SDK

- **[VERIFIED: app/build.gradle and build.gradle at main@a6c7282...]** Current settings: minSdk 29, compileSdk 37, targetSdk 37, Java/Kotlin target 11, ABIs arm64-v8a and x86_64.
- **[VERIFIED: .github/workflows/android-ci.yml at main@a6c7282...]** Current CI uses Java 21 to build/test with Gradle and produces a debug artifact; it is not an owner-signed production release pipeline.
- Preserve min/compile/target/ABI baseline under NFR-COMP-01 unless an approved PRD change records the reason, compatibility impact and test evidence.
- Current targetSdk 37 exceeds Google Play's API 36 minimum for new apps and updates as of 31 Aug 2026. If Play is selected, recheck policy at actual submission time.
- Do not confuse minSdk29 with the Play target API threshold. Raising minSdk would remove supported older Android devices; it is not required by the cited Play policy.
- Build and verify both required ABIs. CI and emulator pass do not substitute for physical-device RDP acceptance.
- Release gate requires an owner-controlled signature and exact artifact/source provenance.

## 5. RDP certificate inventory and rotation

### 5.1 Provisioning model

Preferred model is an institution-controlled internal CA for Windows RDP server certificates. A per-VM leaf certificate pin is an acceptable pilot alternative if certificate issuance/renewal is controlled.

- Lab IT creates/renews the guest certificate and private key on the Windows VM using an approved provisioning method.
- Private key remains on the Windows guest and is protected by its certificate store/OS permissions.
- Verify the presented certificate over a trusted administrative channel independent of the client network path.
- Calculate SHA-256 over the DER-encoded leaf or approved CA certificate as defined by the broker contract.
- Store only the approved certificate fingerprint, mode, validity dates and rotation metadata in server-controlled inventory; do not store guest private keys in broker config.
- Bind the grant's certificate policy to the same assignment/version as the session grant.
- Missing fingerprint, invalid trust chain, expired certificate or mismatch is a hard failure (RDP-CERT-01). There is no “accept anyway”.

### 5.2 Rotation procedure

The current API contract returns one active fingerprint/trust mode per grant. A dual-pin overlap is not assumed by the R1 schema. Use a coordinated maintenance rotation unless a reviewed API extension explicitly supports multiple pins.

1. Schedule maintenance; stop new grants and close active RDP sessions.
2. Confirm owner/Lab IT approval, certificate subject/SAN and expiration; record a non-secret change ID.
3. Install the new guest certificate and private key using approved guest administration.
4. Verify the new leaf/CA fingerprint from an independent trusted administrative path.
5. Update broker inventory and client grant policy in one controlled maintenance window.
6. Test the intended assigned guest: valid new pin succeeds; old pin, wrong pin, expired pin and missing pin fail closed.
7. Re-open grants only after the valid/invalid tests pass.
8. Retain the prior certificate and pin in secured rollback storage only for the approved rollback window. Never ship both as accepted trust unless the API/PRD contract explicitly allows it.
9. If validation fails, stop new grants, restore the old guest certificate and old broker pin together, retest, and record the incident.

### 5.3 Trust separation

- WSS HTTPS certificate validates Android-to-broker TLS.
- RDP certificate pin/CA validates FreeRDP-to-Windows guest RDP TLS through the byte relay.
- These are separate certificates and separate trust decisions.
- The broker must not terminate/replace RDP TLS unless the PRD and threat model are explicitly revised.

## 6. Broker deployment and network runbook

### 6.1 Preconditions

- Approved stable HTTPS/WSS hostname, DNS and TLS certificate.
- Validated Supabase JWT/session_id verification path and least-privilege server credential.
- Owner-approved server-side VM allowlist for only the four approved R1 endpoints. VM4 remains excluded.
- Certificate inventory for each enabled VM.
- Firewall rule set with no public inbound TCP/3389.
- Broker status/grant/relay contract tests and rollback artifact.
- Monitoring for grant denials, relay open/close, revocation, rate limit and cleanup failures.

**[UNKNOWN]** Stable production broker hostname/route and current deployment are not established; no hostname or endpoint is repeated in this file.

### 6.2 Network topology

- Public ingress accepts HTTPS and WSS on the approved secure application port (normally 443) at the designated reverse proxy/load balancer.
- Reverse proxy forwards only to the broker's loopback/private application listener. Do not expose the broker process port publicly.
- Broker reaches the Windows guests only through the private routed network and only on the fixed RDP port 3389 for explicitly allowlisted assignments.
- Public inbound TCP/3389 is denied. Egress to arbitrary destinations/ports is denied.
- Broker must not expose VM endpoint data in status/grant/error responses or logs.
- Trust forwarded headers only from the configured trusted proxy; reject client-forged forwarding headers.
- WSS must preserve binary stream order and backpressure; do not configure the route as a generic HTTP endpoint without WebSocket upgrade support.

### 6.3 Deployment sequence

1. Review broker release version and change set; verify signed artifact/hash.
2. Validate required environment configuration without printing values. Abort startup on missing keys, malformed allowlist, duplicate VM identity, unapproved port, absent certificate policy or capacity overrun.
3. Store server credentials and grant hashing keys in approved secret storage, not repository or process logs.
4. Deploy with loopback/private listener behind TLS proxy.
5. Verify /health and /ready reveal only safe readiness status.
6. Verify Supabase JWT/session_id validation and exactly-one assignment resolution with synthetic test identities.
7. Verify status probe 1500ms budget and UNKNOWN on timeout.
8. Verify grant TTL, entropy, hash-at-rest, atomic consume, 5/min/user, one-session/user and configured total cap.
9. Verify WSS Authorization header, binary-only frames, byte fidelity, queue/idle/ping limits and close handling.
10. Verify active session/assignment recheck at <=15s and termination <=30s.
11. Verify outbound connection only to approved private TCP/3389 destinations.
12. Scan from outside the environment to confirm public TCP/3389 is unreachable.
13. Run one approved controlled RDP connection and cleanup cycle, then controlled invalid-certificate/revoked-session tests.
14. Re-open new grants only after all security checks pass.

### 6.4 Routine health checks

- Liveness and readiness; no detailed inventory in unauthenticated endpoints.
- TLS certificate validity/expiry and proxy route.
- Broker capacity, queue depth, CPU/memory, open sockets and relay duration.
- Grant issued/denied/expired/replayed counters and rate-limit events.
- Active relay count and orphaned session cleanup.
- Supabase session/assignment adapter error rate.
- Private endpoint reachability and certificate pin validity.
- Public RDP exposure check after network changes.
- Safe audit event redaction.

## 7. Windows guest account and lockout operations

### 7.1 Account model

- R1 uses a distinct Windows lab account per student; it is not the Supabase/college password.
- Students are not local administrators unless separately approved and justified.
- Broker does not issue, see or store Windows NLA credentials.
- Credentials are entered each session and are not saved by the app.
- Disable/revoke guest accounts promptly when student access is removed or assignment is ended.

### 7.2 Lockout / failed-logon policy

**[UNKNOWN]** The approved institution Windows lockout threshold, observation window, lockout duration, reset/helpdesk process and failed-logon monitoring configuration are not evidenced.

Before pilot, Lab IT must approve:
- password length/complexity and password-reset channel;
- failed-logon threshold and observation window;
- lockout duration and unlock process;
- Windows Security event monitoring/alerting;
- per-student account mapping and disablement;
- emergency access for Lab IT that is not shared with students;
- incident procedure for suspected credential compromise.

Do not invent a threshold in this document. The client also rate-limits retry attempts and uses non-enumerating guest-auth messages; client retry limits do not replace Windows account lockout.

## 8. Backups, restore, retention and semester end

<badge>Current state: UNKNOWN</badge>

**[UNKNOWN]** Current VM persistence model, backup product/schedule, restore point, RPO/RTO, database PITR status, data retention window and semester-end deletion policy are not verified in the audited repository snapshot. Do not assume the VMs are ephemeral or persistent, and do not silently reset/delete student data.

### 8.1 Required owner/Lab IT decisions

| Area | Required decision/evidence |
|---|---|
| Student VM persistence | Persistent per student vs ephemeral pool; owner and Lab IT approval |
| Windows user data | What is stored, where, backed up by whom and for how long |
| VM backup | Guest/host backup method, cadence, encryption, retention and restore owner |
| Supabase data | Backup/PITR availability, retention and restore procedure verified for current plan |
| RPO / RTO | Owner-approved values based on lab impact; not invented in this document |
| Restore drill | Successful test restoring one synthetic pilot VM and its assignment metadata |
| Semester end | Freeze cohort, reconcile users/assignments, archive/export per policy, revoke sessions, disable accounts, reset/reimage/delete only with approval |
| Data deletion | Approved scope, approval, retention hold and proof of deletion |
| Incident backup | Isolated backup and evidence preservation procedure after security incident |

### 8.2 Proposed semester-end sequence

1. Announce cutoff and freeze assignment/VM changes.
2. Reconcile current roster and single active assignment per student.
3. Revoke active grants and close all RDP relays.
4. Disable/offboard guest and Supabase access according to approved schedule.
5. Take the required pre-reset backup and verify integrity.
6. Apply the owner-approved archive/reset/reimage/deletion policy.
7. Verify no previous student can reconnect or access another student's data.
8. Record retention/deletion evidence without copying student identifiers to Git.

No automatic reset, VM deletion or student reassignment is authorized by this runbook.

## 9. Incident response

### 9.1 Trigger conditions
- Cross-student assignment/routing success or client-selected destination.
- Grant replay, unauthorized relay or session-revocation delay >30 seconds.
- RDP certificate mismatch accepted or unexpected trust change.
- Public TCP/3389 exposure.
- Local bridge hijack or arbitrary TCP forwarding.
- Credential/grant/JWT/student data/private endpoint leakage.
- FreeRDP native crash or applicable high/critical unpatched vulnerability.
- Broker relay exhaustion, repeated abnormal resource use or orphaned session slots.
- Guest malware/compromise or abnormal failed-logon surge.

### 9.2 Containment sequence

1. Stop issuing new grants; set broker not-ready if authorization integrity is uncertain.
2. Revoke grants and terminate active relays for affected users/VMs; enforce the 30-second target.
3. Disable affected assignment/allowlist entry; VM4 remains excluded and is never substituted.
4. Isolate affected guest/network path if compromise or malicious traffic is suspected.
5. Preserve redacted audit metadata and exact build/config hashes. Do not export tokens, passwords, private IPs, hostnames or screen contents.
6. Notify Project Owner and Lab IT using approved private channels.
7. Rotate exposed Supabase/broker/signing/certificate credentials through their respective owner-approved processes.
8. Restore last reviewed client/broker version only after authorization, certificate and private-route checks pass.
9. Record incident cause, scope, containment, corrective action, affected gates and owner approval.

### 9.3 Rollback conditions

Rollback or halt if:
- authentication/assignment fails closed incorrectly or allows a cross-student session;
- certificate pin validation fails;
- active revoked relay cannot close within 30 seconds;
- LocalBridge cannot prove peer identity;
- broker exposes RDP publicly or routes to a non-allowlisted endpoint;
- new release fails G4 performance/cleanup gates;
- release signer/hash cannot be verified.

Do not roll back to an insecure configuration, disable TLS/certificate checks, expose public RDP or silently move student assignments.

## 10. Pilot go/no-go checklist

- [ ] Owner selects public Play, permanently private organization-only Play, or owner-managed MDM/APK.
- [ ] Owner-approved signing/upload key custody and recovery plan.
- [ ] Artifact built from recorded source and FreeRDP gitlink; SHA-256 and signer verified.
- [ ] Target SDK policy rechecked at submission time; current audited targetSdk37 is retained unless approved.
- [ ] Four approved R1 VM mappings verified; VM4 excluded.
- [ ] Guest account/lockout/failed-logon policy approved by Lab IT.
- [ ] Certificate fingerprints/CA policy installed, verified and rotation tested.
- [ ] Broker deployed with safe /health and /ready; WSS and session revocation tests pass.
- [ ] Public TCP/3389 scan passes; outbound allowlist verified.
- [ ] Supabase session revocation <=30s demonstrated.
- [ ] RD-1 physical device confirmed and all G4 performance/cleanup/security tests pass.
- [ ] Backup/restore, retention and semester-end policies approved and restore evidence available.
- [ ] Incident, support and rollback owner/contact paths tested.
- [ ] Project Owner signs the PRD G5 go/no-go decision.

## 11. Sources and current policy reference

- [Google Play target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en) — starting 31 August 2026, new apps and app updates submitted to Play must target API 36 or higher; the policy lists a narrowly defined exception for permanently private apps restricted to one organization.
- [Android Developers — target SDK requirement](https://developer.android.com/google/play/requirements/target-sdk) — current target API guidance.
- [Google Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?rd=1) — distinguishes publisher-held upload key from Google-managed app-signing key.

These sources must be rechecked at actual release submission time. This document does not provide legal or institution-specific compliance approval.
