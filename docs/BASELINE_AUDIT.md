# MobiDesk — Baseline Audit

**Audit date:** 3 November 2026  
**Audit mode:** Read-only repository and PRD inspection  
**Canonical implementation repository:** `master43721/mobidesk-android`  
**Audited ref:** `main` at `5979320b05e1e6c1d872d82dcc17e65b0901e7e5`  
**Normative product source:** `docs/MobiDesk_PRD_v1.2.pdf` (23 pages, PRD v1.2)

> This file is an evidence snapshot. It does not change the PRD or implementation. Every repo/infrastructure statement is tagged with its evidence class. No credentials, student identifiers, private IP addresses or private hostnames are recorded.

## 1. G0 decision — repository and stack

<badge>G0 STACK/REPOSITORY CHECK: PASS</badge>

- **[VERIFIED: GitHub repository metadata, 3 Nov 2026]** Repository full name is `master43721/mobidesk-android`; remote is `https://github.com/master43721/mobidesk-android.git`; default branch is `main`.
- **[VERIFIED: GitHub repository metadata, 3 Nov 2026]** GitHub currently reports repository visibility as **public**.
- **[VERIFIED: `README.md` at `main@5979320`]** The repository describes the Android client as native Kotlin/Jetpack Compose.
- **[VERIFIED: `settings.gradle` at `main@5979320`]** The Gradle root is `MobiDesk`; it includes `:app` and `:freeRDPCore`.
- **[VERIFIED: `app/src/main/java/com/mobidesk/android/MainActivity.kt` at `main@5979320`]** The launcher Activity extends AndroidX `ComponentActivity`, calls Compose `setContent`, and renders `MobiDeskApp`.
- **[VERIFIED: `app/build.gradle` and `build.gradle` at `main@5979320`]** The app applies Kotlin Android, Kotlin Compose and serialization plugins; the root build pins the Android Gradle Plugin and Kotlin versions recorded below.
- **[VERIFIED: root tree at `main@5979320`]** The repository contains no `pubspec.yaml`; its application build is Gradle-based. The main source tree contains Kotlin files under `app/src/main/java/com/mobidesk/android/`.
- **[VERIFIED: PRD v1.2 PDF, §6.1, pp. 8–9]** The PRD explicitly specifies a native Kotlin/Jetpack Compose application, a Compose/ViewModel dashboard, a native `RdpSessionActivity`, and no Flutter PlatformView or Flutter texture path.
- **[VERIFIED: PRD v1.2 PDF, cover, p. 2]** The PRD names `master43721/mobidesk-android` as its target repository.
- **[UNKNOWN]** The exact repository/ref examined by the earlier Flutter + Kotlin v1.0 audit is not established by the current target repository evidence. That audit is not used as evidence about this repository.
- **[VERIFIED: `docs/MobiDesk_PRD_v1.2.pdf` on `main@5979320`]** The 23-page PDF is the complete, normative PRD v1.2 source.
- **[VERIFIED: prior branch `docs/mobidesk-prd-v1.2`]** Its `docs/PRD.md` is an incomplete text projection and is not treated as authoritative. This suite adds a new `docs/PRD.md` navigational requirement index; the PDF remains normative and unmodified.
- **[OWNER DIRECTION: 3 Nov 2026]** The Project Owner states that PRD v1.2 is the final decided product baseline and instructs that the PRD must not be edited in this work. The PDF's printed status still says “Owner review draft — owner approval still required” (p. 2); that wording conflict is recorded in `OPEN_ITEMS.md`, not silently changed.

**G0 conclusion:** the actual repository and stack match PRD §6.1. No stack/repository blocker was found. The earlier Flutter finding remains an unresolved provenance question about a different audit, not evidence that this repository is Flutter.

## 2. Repository refs, branches and drift

- **[VERIFIED: GitHub commit API, 3 Nov 2026]** Current `main` HEAD is `5979320b05e1e6c1d872d82dcc17e65b0901e7e5`, message `fix: stabilize Android CI and contain FreeRDP UI`.
- **[VERIFIED: GitHub commit diff for `main@5979320`]** The latest mainline commit updates Android CI SDK bootstrap, repository status docs, FreeRDP API audit, and manifest containment. It does not change the app's SDK version or FreeRDP gitlink.
- **[VERIFIED: GitHub branch listing, 3 Nov 2026]** Relevant branch refs at audit time are `main`, `docs/mobidesk-prd-v1.2`, and `docs/prd-v1.2-suite`.
- **[VERIFIED: GitHub PR metadata, 3 Nov 2026]** PR #1 is open from `docs/mobidesk-prd-v1.2` to `main`; its recorded head was `0cfa086fb783dced791da42c374f28436af3074f`, and its recorded base was the older `4986006a5e6823206f2c179bfe8f24d342ede5eb`.
- **[VERIFIED: GitHub compare API, 3 Nov 2026]** The older `docs/mobidesk-prd-v1.2` branch is three commits ahead and three behind current `main`; it is not the branch used for this task.
- **[VERIFIED: PR #1 changed-file list, 3 Nov 2026]** The prior PR changes documentation paths only; no application source file was listed in its 19-file diff.
- **[VERIFIED: GitHub compare API, 3 Nov 2026]** The requested `docs/prd-v1.2-suite` branch already existed at `ca34e425a9f8c02dfc3f831ca620e590ecb6c789`. It is 18 commits ahead and one commit behind current `main`; its merge base with `main` is `a6c7282b4652b10b561ca0578c006c6d4b71cbc3`.
- **[UNKNOWN]** No local working-tree status is available from this GitHub-only audit; commit/tree state is used instead.

## 3. Build, SDK and ABI baseline

| Property | Observed value | Evidence |
|---|---|---|
| Root project / app module | `MobiDesk` / `:app` | [VERIFIED: `settings.gradle`, `main@5979320`] |
| Application ID | `com.mobidesk.android` | [VERIFIED: `app/build.gradle`, `main@5979320`] |
| Android Gradle Plugin | `9.2.1` | [VERIFIED: `build.gradle`, `main@5979320`] |
| Kotlin | `2.3.21` | [VERIFIED: `build.gradle`, `main@5979320`] |
| Gradle wrapper | `9.6.1` | [VERIFIED: `gradle/wrapper/gradle-wrapper.properties`, `main@5979320`] |
| minSdk | `29` | [VERIFIED: `build.gradle`, `app/build.gradle`, `release.properties`, `main@5979320`] |
| compileSdk | `37` | [VERIFIED: `build.gradle`, `app/build.gradle`, `release.properties`, `main@5979320`] |
| targetSdk | `37` | [VERIFIED: `build.gradle`, `app/build.gradle`, `release.properties`, `main@5979320`] |
| Android Build Tools | `37.0.0` | [VERIFIED: `build.gradle`, `release.properties`, `main@5979320`] |
| NDK | `29.0.13113456` | [VERIFIED: `build.gradle`, `release.properties`, `main@5979320`] |
| CMake | `4.1.2` | [VERIFIED: `build.gradle`, `release.properties`, `main@5979320`] |
| Java source/target | `11` | [VERIFIED: `app/build.gradle`, `main@5979320`] |
| Kotlin JVM target | `11` | [VERIFIED: `app/build.gradle`, `main@5979320`] |
| ABIs | `arm64-v8a`, `x86_64` | [VERIFIED: `build.gradle`, `release.properties`, `main@5979320`] |
| App version in Gradle | `0.2.0` / versionCode `1` | [VERIFIED: `app/build.gradle`, `main@5979320`] |

- **[VERIFIED: `release.properties` and `app/build.gradle`, `main@5979320`]** `release.properties` reports `VERSION_NAME=0.1.0`, while Gradle app configuration reports `versionName '0.2.0'`. This is configuration drift; the app Gradle configuration is the build input observed here.
- **[VERIFIED: PRD v1.2 PDF, §8.1, p. 16]** PRD minimum is API 29 or newer; the current verified repository minimum is API 29.
- **[VERIFIED: PRD v1.2 PDF, §8 and §8.1, pp. 15–16]** NFR-COMP-01 says preserve the verified repository min/compile/target SDK and ABI baseline unless an approved change is recorded.
- **G0 resolution:** set the documented minimum to **API 29**. Preserve compile/target **37/37** and the existing **arm64-v8a + x86_64** ABI set. Section 8.1 and NFR-COMP-01 are consistent when applied to the actual repository baseline; no PRD change is needed for minSdk.

## 4. FreeRDP submodule

- **[VERIFIED: `.gitmodules` and root tree at `main@5979320`]** Submodule path is `third_party/FreeRDP`, remote URL is the official `FreeRDP/FreeRDP` repository, and the gitlink is `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`.
- **[VERIFIED: upstream FreeRDP commit API, 3 Nov 2026]** That exact gitlink has commit message `release 3.32.0`, dated 23 Sep 2026, and its GitHub signature verification result is valid.
- **[VERIFIED: PRD v1.2 PDF, §§6.1, 6.6 and 6.8, pp. 8–11]** PRD requires inspection of the exact pinned Android client APIs for surface, credentials, certificate callback and raw-stream transport; it explicitly prohibits assuming RD Gateway WebSocket is the custom raw-WSS transport.
- **[UNKNOWN]** The app's required FreeRDP Android integration points (custom raw-stream injection, surface rebind, RDPEDISP, certificate and credential callback lifecycle) have not been demonstrated in this audit.

## 5. Manifest, mock/demo, WebView and credential observations

- **[VERIFIED: `app/src/main/AndroidManifest.xml`, `main@5979320`]** `android:usesCleartextTraffic="false"`; Internet and network-state permissions are declared.
- **[VERIFIED: same manifest]** `MainActivity` is exported as launcher; upstream FreeRDP `SessionActivity` and `SessionRequestHandlerActivity` are non-exported. No MobiDesk-owned `RdpSessionActivity` is declared in the manifest.
- **[VERIFIED: GitHub code search, 3 Nov 2026]** Search returned no matches for `WebView` or the combined mock/demo/fake assignment query in this repository. This is a search result, not proof of absence from every generated/dependency path.
- **[VERIFIED: `MobiDeskViewModel.kt` and `MobiDeskRepositories.kt`, `main@5979320`]** Current workspace code observes Supabase Auth session state and requests student/VM display rows. The repository selects `students(id,name,college)` filtered by user ID and `vms(id,name)` relying on existing VM RLS. It does not contain a broker status/grant implementation in the inspected path.
- **[VERIFIED: `MobiDeskRepositories.kt`, `main@5979320`]** Supabase sign-in accepts the college-account email/password; password reset and update methods belong to Supabase Auth. This is not a Windows NLA credential prompt.
- **[VERIFIED: `README.md`, `main@5979320`]** README says no final APK exists and no native RDP session is complete; it also describes the historical debug APK only.
- **[UNKNOWN]** The current UI's every branch has not undergone a full source-wide static scan for demo/sample behavior in this read-only pass. No sample VM is endorsed or allowed by PRD; a full release static scan is required under FR-REL-02.
- **[VERIFIED: `app/build.gradle`, `main@5979320`]** Release build type has `minifyEnabled false` and ProGuard file configuration, but no explicit `signingConfig` is present in the inspected Gradle file.
- **[REPORTED: `README.md` and `docs/ANDROID_BUILD_REPORT.md`, historical 27 Oct 2026 build]** A debug APK was built and signed with the debug key. This is not an owner-controlled release artifact.
- **[UNKNOWN]** Owner release/upload key custody and release signing configuration are not established by the current repository evidence.

## 6. Current CI evidence

- **[VERIFIED: `.github/workflows/android-ci.yml` at `main@5979320`]** An Android CI workflow now exists. It runs JVM unit tests and assembles a debug APK on pushes/PRs to main; it verifies the debug APK and uploads a debug-only artifact.
- **[VERIFIED: same workflow]** API 36 emulator instrumentation is opt-in through manual workflow dispatch; it is not run on every push/PR.
- **[VERIFIED: same workflow]** Workflow configures Java 21 for Gradle; app Java/Kotlin source/bytecode targets are 11.
- **[UNKNOWN]** No physical-device RDP, live Supabase/Windows NLA, secure relay, certificate pin, or performance acceptance result is evidenced by this workflow.

## 7. PRD requirement status at baseline

The detailed requirement-to-test mapping is in `TRACEABILITY_CHECK.md`. This table is a summary only.

| PRD area | Baseline status | Evidence / gap |
|---|---|---|
| FR-AUTH-01 | PARTIAL | Supabase sign-in/session/recovery methods exist; full frozen-baseline regression not evidenced. |
| FR-AUTH-02 | PARTIAL | RLS-scoped client reads are implemented; live cross-student negative tests are not evidenced. |
| FR-VM-01 | PARTIAL / GAP | Client can read RLS-filtered VM rows; explicit exactly-one fail-closed handling is not demonstrated by the repository query. |
| FR-VM-02 / 03 | NOT MET | No conforming broker grant route is implemented in this repo. |
| FR-VM-04 / 05 | NOT MET | No trusted live status API/client; no stale sample/connect-time recheck. |
| FR-SES-01–04 | NOT MET | No production grant, WSS relay, session revocation or server concurrency enforcement. |
| FR-GUEST-01 / 02 | NOT MET | Separate Windows credential prompt and volatile buffer lifecycle absent. |
| FR-RDP-01–05 | NOT MET | FreeRDP dependency is pinned/packaged historically; no native MobiDesk session, NLA, pin callback, render-target/RDPEDISP or protected raw-stream bridge. |
| FR-INPUT-01–03 | NOT MET | No MobiDesk RDP controls, tested input model or FLAG_SECURE evidence. |
| FR-LIFE-01–04 | NOT MET | No RDP lifecycle, teardown/reconnect/background timeout implementation evidence. |
| FR-OBS-01 | NOT MET | No session-stage telemetry implementation evidence. |
| FR-SEC-04 | NOT MET | Root/unlocked-device block and UNKNOWN integrity state not evidenced. |
| FR-REL-01 / 02 | NOT MET | No reproducible owner-signed release; no verified release static scan. |
| FR-DOCK-01 / 02 | R2 / NOT STARTED | Dock is correctly outside R1; no authenticated dock/display implementation claimed. |
| NFR-PERF-01–06 | NOT MET | No 20-attempt physical-device measurement dataset. |
| NFR-SEC-01 / NFR-PRIV-01 | NOT MET | No end-to-end negative security test or completed secrets/log/packet scan. |
| NFR-COMP-01 | BASELINE VERIFIED | SDK/ABI values are verified above; preservation must be rechecked on each build. |

## 8. Material conflicts and open evidence

1. **Repository visibility conflict:** GitHub metadata says public, while the current README describes the repository as private. The suite therefore contains no secrets, student identifiers, private network addresses or hostnames. Project Owner must confirm intended visibility and access policy before adding operationally sensitive material.
2. **PRD text companion:** current main contains the complete 23-page PDF. The older branch's `docs/PRD.md` is not faithful. This suite adds a navigational index and identifier inventory at `docs/PRD.md`; it does not replace or modify the PDF.
3. **PRD approval label:** the Project Owner's current instruction declares v1.2 the final decided baseline, while the PDF cover still says owner-review draft/approval pending. Record the formal approval decision or correct the status through an approved future PRD revision; this suite does not edit the PRD.
4. **RD-1:** the PRD nominates a Pixel 6 / Android 16 / API 36 physical device. Actual available test hardware is UNKNOWN; no substitute is guessed.
5. **Windows/VM and R2 facts:** server guest account policy, VM persistence, backup/restore and the exact R2 dock hardware remain owner/Lab IT dependencies; no private host details are copied into this audit.

## 9. Audit boundary

No source code, Gradle file, git submodule, workflow, Supabase record, server, VM, firewall, certificate or release key was changed during this audit. This documentation branch must remain under `docs/` only.

## 4. CI SDK provisioning observation

- **[VERIFIED: GitHub Actions run `41595687606`, job `125782381543`, 3 Nov 2026]** The `JVM tests and debug APK` job failed in the step `Accept Android SDK licenses and install pinned packages` because the runner package catalog returned `Failed to find package 'platforms;android-37'`. The Gradle test/build, APK verification and emulator steps were skipped.
- **[VERIFIED: `app/build.gradle` at `main@5979320`]** The app is configured for compileSdk 37 / targetSdk 37. This configuration was not changed by the documentation branch.
- **Conclusion:** repository source/build settings are observed, but the hosted CI runner's ability to provision API 37 is not verified. Resolve runner SDK availability in a separate CI/build task; this documentation-only PR does not modify CI or Gradle.

