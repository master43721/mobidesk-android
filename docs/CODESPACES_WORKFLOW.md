# MobiDesk Android — GitHub Codespaces Workflow

**Canonical repository:** master43721/mobidesk-android  
**Target:** Kotlin + Jetpack Compose + pinned FreeRDP Android build  
**Recommended Codespace:** existing `didactic-giggle`, upgraded to 4 cores / 16 GB / 32 GB in Central India.

## 1. One workspace, one repository

Use only this repository for the MobiDesk Android application. Do not mix changes from the older Flutter starter repository. Keep one long-lived Codespace and short-lived Git branches per feature/bug. The Codespace is a development environment, not the source of truth; Git commits and CI are.

Open the existing workspace:
https://didactic-giggle-xr5r9j9g76q629974.github.dev

## 2. Important safety warning for the existing workspace

GitHub reported uncommitted AND unpushed changes on `main` when the 4-core/16-GB upgrade was applied. Do not run `git reset --hard`, `git clean -fd`, force checkout, or overwrite files until these changes are reviewed.

Before any branch switch, run:

```bash
git status --short --branch
git diff --stat
git diff
git diff --cached
git ls-files --others --exclude-standard
```

Inspect each changed/untracked file. Do not include `local.properties`, signing keys, service-role keys, keystores, credentials or generated secrets in a commit.

Preservation choice:
- **Preferred:** commit only the intended files to a named backup branch after review.
- **Alternative:** after review, use `git stash push -u -m "codespace-main-safety-snapshot-2026-11-05"` and verify with `git stash list` and `git status`.
- Do not create a blind stash/commit if you have not inspected what it contains.

## 3. Move the preserved worktree to FP-02 v0.2

After the current work is safely committed or stashed:

```bash
git fetch origin --prune
git switch -c docs/fp-02-v0.2-corrected-codespaces --track origin/docs/fp-02-v0.2-corrected-codespaces
git submodule update --init --recursive
```

If the local branch already exists, use `git switch docs/fp-02-v0.2-corrected-codespaces` after verifying its upstream and status. Do not use `-f`.

## 4. Environment verification

The committed `.devcontainer` installs the same core Android toolchain pinned in the checked-in Android CI workflow:
- Temurin/OpenJDK 21
- Android SDK platform 37 and build-tools 37.0.0
- NDK 29.0.13113456
- CMake 4.1.2
- NASM and C/C++ build prerequisites
- recursive FreeRDP submodule

Verify:

```bash
java -version
sdkmanager --list_installed
adb version
git submodule status --recursive
./gradlew --version
```

If the Codespace asks to rebuild the container, rebuild only after the dirty worktree is preserved. The 4-core/16-GB machine update applies on the next start.

## 5. Build/test loop

Fast first-pass:

```bash
./gradlew --no-daemon --max-workers=3 :app:testDebugUnitTest
```

Debug APK:

```bash
./gradlew --no-daemon --max-workers=3 :app:assembleDebug
```

Before opening a PR:

```bash
./gradlew --no-daemon --max-workers=3 :app:testDebugUnitTest :app:assembleDebug
git status --short
git diff --check
```

CI remains the authoritative clean-room build:
`./gradlew --no-daemon --stacktrace --max-workers=2 :app:testDebugUnitTest :app:assembleDebug`.
The GitHub Actions workflow also verifies APK integrity/signature and uploads a debug-only artifact. Debug APK is not the signed R1 release artifact.

For native FreeRDP work, build/test only the affected target and preserve the pinned gitlink. Do not update the FreeRDP submodule unless a separate compatibility/security review approves it. Keep host-side and Android NDK caches in the Codespace's persistent cache directory.

## 6. Faster, safer iteration

- Use the 4-core/16-GB Codespace for Gradle, Kotlin, CMake and native compilation; avoid building in a slow local laptop environment.
- Build only the changed module/test task during iteration; run the full CI task before PR.
- Do not run multiple Gradle builds concurrently in the same worktree.
- Keep the Gradle dependency cache and Android user cache outside the Git repository under `/workspaces/.cache`. A ccache directory is provisioned there, but native compiler caching is effective only after the relevant CMake/Gradle targets explicitly configure ccache as their compiler launcher; do not assume it is active merely because the binary is installed.
- Keep the pinned FreeRDP submodule and generated build outputs out of commits.
- Use GitHub Actions for clean-room JVM tests and debug APK. Use an API 36 emulator workflow only when explicitly requested; physical R1 acceptance still requires the nominated physical device.
- Codespaces prebuilds may reduce startup time after the devcontainer works, but they consume GitHub Actions/storage/billing resources. Enable them only after the first successful build and owner approval.
- Use GitHub Secrets or Codespaces Secrets for runtime secrets. Never put secrets in Dockerfile, devcontainer.json, repo environment files or committed shell scripts.

## 7. MobiDesk context to keep

- Native Kotlin + Jetpack Compose; FreeRDP v3.32.0 pinned in `third_party/FreeRDP`.
- Supabase Auth and RLS; no service-role key in Android.
- FP-01 auth/session is an upstream gate; FP-02 dashboard/status; FP-03 assignment; FP-04 broker status; FP-05 Windows credentials/NLA; FP-06 grant/relay/FreeRDP session.
- Server status, broker/WSS, VMs and Cloudflare tunnel are separate deployment dependencies. Codespaces does not replace the Ubuntu Server, libvirt host or physical Android device.
- A green build does not prove live broker, VM assignment, NLA, RDP rendering or physical performance.

## 8. Definition of a clean feature handoff

Every feature PR records: base SHA, branch, changed-file list, exact Gradle tasks, test report paths, CI run URL, app artifact hash where relevant, physical device evidence where required, unresolved dependencies and the reviewer. Mark unavailable tests NOT RUN/BLOCKED, never PASS by implication.
