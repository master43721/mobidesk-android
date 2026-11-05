# FP-01 Defect Log

**Feature Package:** FP-01 — Authentication, Session State & Recovery
**Tracking Repository:** `master43721/mobidesk-android`
**Current Commit:** `cf01c89deb7dd0b0cb4a272811d9b322f33a93e8`
**Status:** Open

---

### Defect DEF-001: FreeRDP ExternalFFmpeg Unbounded Parallelism Linker OOM Failure

- **Severity:** High / Build Blocker
- **Discovered In:** Native build `:app:assembleDebug` / `:freeRDPCore:buildCMakeDebug[arm64-v8a]`
- **Root Cause:** In `third_party/FreeRDP/client/Android/cmake/ExternalFFmpeg.cmake:79`, the build command specifies:
  ```cmake
  BUILD_COMMAND ${CMAKE_COMMAND} -E env "PATH=${NDK_TOOLCHAIN_BIN}:$ENV{PATH}" make -j
  ```
  Unbounded `make -j` causes multiple large compiler/linker instances to run concurrently. Linking `libavcodec.so.62` exhausts memory on container environments with ~8 GB RAM and 0 swap, resulting in the clang linker being terminated by the operating system (`clang: error: unable to execute command: Terminated`).
- **Proposed Correction:** Constrain compilation parallelism (e.g. `make -j2` or passing `-j` bounded to available core/memory budget, or configuring swap/bounded workers).
- **Rule Compliance:** In accordance with Section I ("Never silently fix a defect during the live drill and then report the original drill as PASS. Record defects separately, preserve evidence and request permission for source changes"), this issue is recorded here and permission is requested from the Owner prior to modifying submodule cmake files.
- **Resolution Status:** RESOLVED — Approved by Project Owner (2026-11-05). Updated `third_party/FreeRDP/client/Android/cmake/ExternalFFmpeg.cmake:79-80` to `make -j2`. Verified with successful native build.

---

### Defect DEF-002: Missing Supabase Publishable Key in Local Environment

- **Severity:** Blocker for Physical Device Live Drill
- **Discovered In:** Preflight check of `local.properties`
- **Root Cause:** `local.properties` defines `SUPABASE_URL=https://kthfhjwltcpjhjemugwm.supabase.co` but leaves `SUPABASE_PUBLISHABLE_KEY=` empty.
- **Runtime Impact:** `MobiDeskApplication.supabaseClient` initializes to `null`, setting `serviceConfigured = false`. The UI disables the Sign In button and renders "This build is missing its Supabase client configuration. Sign-in is disabled."
- **Proposed Correction:** Supply the non-privileged Supabase publishable/anon key in `local.properties`.
- **Resolution Status:** OPEN — Awaiting staging publishable key.
