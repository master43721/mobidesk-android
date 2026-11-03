# MobiDesk R1 progress

**Last updated:** 2026-11-03. **Target:** PRD v1.2, still an owner-review draft. No release gate is passed solely because a build succeeds.

## Verified baseline

- Canonical private GitHub repo: `master43721/mobidesk-android`. PRD intake docs were pushed in `cbcdea6`; the first Android CI workflow was pushed in `a6c7282`. The local worktree currently has unpushed CI/audit/manifest updates.
- FreeRDP Android source is pinned at v3.32.0, submodule commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`.
- Native Kotlin/Jetpack Compose shell, Supabase Auth/recovery, student/profile/assigned-VM reads, and truthful empty/error states exist. No RDP session launch, broker status UI, or working desktop connection is implemented.
- A separate out-of-repo broker prototype has 23 local unit tests reported passing. It is not deployed and has material gaps against PRD v1.2; see `docs/PRD_TRACEABILITY.md`.
- Tailscale SSH identity to the server was verified. A read-only inventory confirmed five Windows VMs running and two other configured guests shut off. The broker is not deployed. Host xrdp listens on wildcard TCP/3389; outside reachability and firewall state remain unverified. No server configuration was changed.

## Work completed in this continuation

- Local command `./gradlew --no-daemon --stacktrace --max-workers=2 :app:testDebugUnitTest :app:assembleDebug` completed successfully after manifest hardening: 2 JVM tests passed, 0 failures; 70 Gradle tasks were actionable (9 executed, 61 up-to-date); native FreeRDP compilation and APK assembly succeeded.
- The merged debug manifest was parsed after the build and contained **zero exported FreeRDP components**. The APK passed `unzip -tq` and `apksigner verify`; size is 217,307,804 bytes and SHA-256 is `acd5a7a9e26d174652238439ddc342c5232064ca9d3be87e933fd84556062513`. This is an intermediate debug APK, not an RDP-capable or owner-signed release.
- The first GitHub Actions run (`41585309927`) failed at `Set up Android SDK tools` before tests/build. Its log shows `android-actions/setup-android@v3` invoked `sdkmanager tools`, and the current SDK repository returned `Failed to find package 'tools'`. The workflow was locally changed to use the hosted runner's installed SDK Manager and avoid that retired package. The fix is not yet pushed or rerun.
- Source audit [`docs/FREERDP_ANDROID_API_AUDIT.md`](docs/FREERDP_ANDROID_API_AUDIT.md) confirms FreeRDP exposes native C transport-I/O callbacks, but the checked-in Android Java wrapper has no raw socket/stream injection API. RD Gateway WebSocket uses distinct upgrade and RDG framing; it is not the PRD's transparent raw-byte relay. A native/JNI transport spike is required before implementing the client bridge.
- The merged manifest previously showed unused exported FreeRDP shortcut/settings/about/help entry points. App-manifest overrides now close them; the completed rebuild verified the final merged manifest.

## Gates

| Gate | Status | Evidence / blocker |
|---|---|---|
| G0 Product/baseline freeze | **BLOCKED** | PRD Owner approval and final endpoint/repo freeze pending; five historical assignments conflict with PRD's four approved endpoints and VM4 exclusion. |
| G1 Contract/threat-model freeze | **NOT STARTED** | API contract, active-session lookup, revocation, RDP pin and local-bridge design/tests incomplete. |
| G2 Broker ready | **NOT STARTED** | No broker deployed; status API, session revocation, stable TLS/WSS route and VM allowlist reconciliation incomplete. |
| G3 Android session ready | **NOT STARTED** | Source audit complete; native/JNI transport feasibility, MobiDesk session Activity, credential prompt and certificate policy are not implemented. |
| G4 Physical R1 acceptance | **NOT STARTED** | No emulator RDP or physical-device RDP attempt, performance, negative authorization or cleanup evidence. |
| G5 Release/operations ready | **NOT STARTED** | No owner-signed artifact or approved operations/backup/retention/rollback evidence. |

The detailed checklist is `TODO.md`; acceptance methods are in `TEST_PLAN.md`; blockers are in `OPEN_ITEMS.md`. The GitHub Actions CI rerun, opt-in emulator smoke test, live Supabase/RLS tests, broker deployment and RDP end-to-end flow remain outstanding.
