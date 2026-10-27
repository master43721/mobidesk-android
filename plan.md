# MobiDesk Android — Implementation Plan

## Goal and scope

Build a polished Android client for students to authenticate, see only their assigned Cloud PC, request authorized VM actions, and enter a native Windows RDP session. A role-protected administrator surface may be added only when the backend supports trusted roles and server authorization. The first deliverable is a native Android foundation and verified FreeRDP integration path; no current backend, assignments, VM endpoint, or external RDP transport have been supplied or verified.

## Architecture

- **Android shell:** Native Android app (Kotlin-first) with responsive student/admin screens and explicit loading, empty, offline, error, and disconnected states.
- **RDP engine:** Pin upstream FreeRDP Android source at v3.32.0. Reuse its Android `freeRDPCore`/JNI implementation and its established session rendering/input lifecycle. Keep upstream source/licensing notices intact.
- **Identity/control:** Supabase Auth and trusted profile/assignment data, then a separate authenticated server-side control API for VM status/power and short-lived connection tickets. No service-role credentials or permanent desktop passwords in the APK.
- **Desktop data path:** Native FreeRDP session connects only using server-issued, expiring connection details over a separately verified secure transport. Never expose TCP/3389 publicly. Do not route screen graphics through the control API.
- **Testing/release:** Android Gradle build; debug APK for install/testing; Android Emulator checks if this sandbox can run the SDK emulator. No claim of device or end-to-end verification until actual successful results exist.

## Project structure

- `app/` — MobiDesk Android shell, navigation, screens, theme, app configuration, and tests.
- `third_party/FreeRDP/` — pinned FreeRDP upstream git submodule, containing the Android JNI/core code used by the app.
- `docs/` — screen flows, integration contracts, security decisions, and evidence log.
- `.github/workflows/` — reproducible Android build and test automation without secrets.
- `plan.md`, `TODO.md`, `README.md` — architecture, current status, and developer handoff.

## Screen inventory and navigation

1. Splash/session restoration → sign-in or dashboard.
2. Sign-in, recovery/error, and signed-out states.
3. Student dashboard with exactly the authenticated user's assignment and truthful VM status.
4. VM start/connect preparation with progress, authorization denial, offline, and retry states.
5. Native FreeRDP desktop session with connection lifecycle, keyboard/touch, and disconnect behavior.
6. Reconnecting/disconnected/failure states and safe session exit.
7. Profile/settings and session management.
8. Role-protected administrator inventory/actions, only after server roles and authorization are verified.

Student flow: authenticate → fetch trusted profile and assigned VM → fetch real status → request a one-time connection ticket → launch native session → disconnect/expire → return to dashboard. Admin flow: authenticate → server-verified admin role → fetch authorized inventory → request an audited allowed action. UI visibility alone is never treated as an authorization boundary.

## Initial interaction contract

- The dashboard displays `Unknown`/`Not configured` when there is no verified status source; it must not present historical VM names, IPs, or sample data as live data.
- Students cannot submit an arbitrary VM identifier as authority; the server derives/checks the assignment and returns only an authorized, short-lived connection ticket.
- The native RDP screen is opened only for a valid ticket. Credentials must not be persisted in plaintext or logged. Certificate validation must not be silently bypassed.
- Pointer/touch modes, keyboard, special keys, clipboard, scaling, audio, and reconnection are enabled only when the selected FreeRDP build and device behavior are verified. Unimplemented controls stay hidden/disabled with an explanation.
- Session termination clears temporary connection details and modifier state.

## Design system

**Direction:** Calm industrial modernism for a personal cloud workstation: precise and trustworthy rather than gamer-like or template-heavy.

**Principles:** one clear primary action; status before decoration; high-contrast accessible controls; data is either verified or explicitly identified as unavailable.

**Color tokens:** `ink` #101923, `panel` #17232F, `paper` #F5F7F8, `muted` #6B7785, signature `signalTeal` #20BFA9, `warning` #D99028, `danger` #C74D4D, `success` #1E8E67. Dark surfaces are the default desktop/session mode; light mode is supported for account/settings surfaces.

**Typography:** Android system sans for headings/body; system monospace only for technical endpoint/session diagnostics. Title 28sp/32sp semibold, section 18sp/24sp semibold, body 16sp/24sp regular, supporting 14sp/20sp, technical labels 12sp/16sp.

**Spacing/radii:** 4dp base; common gaps 8/12/16/24/32dp; cards 18dp; buttons/fields 14dp; controls at least 48dp high. Elevation is restrained and reserved for modal/session overlays.

**Components/states:** Primary/secondary/quiet buttons; email/password inputs; VM status indicator with text label; assigned-PC panel; progress, empty, offline, error, and confirmation surfaces. Focus, pressed, disabled, and loading states must remain visually distinct.

**Motion:** short 160–220ms transitions for state changes; avoid motion during RDP rendering; respect system reduced-motion settings.

**Brand:** “MobiDesk — your assigned Windows workspace, ready on your phone.” Personality: capable, calm, dependable. Headlines are concise; actions state their effect (e.g. “Connect to my Cloud PC”, “Request startup”). Wordmark concept: a compact open desktop-window glyph with a single teal cursor/connection notch, paired with the MobiDesk name.

## Dependencies and material unknowns

- FreeRDP Android upstream documents NDK/CMake/JNI and a Gradle Android client. Use the pinned v3.32.0 release source rather than writing an RDP implementation.
- Supabase project URL, public anon key, schema/RLS, recovery settings, and test users are not supplied.
- Control API origin/contracts, VM inventory, server authorization, audit behavior, and operational credentials are not supplied.
- External native RDP transport without Tailscale is unverified. HTTPS Cloudflare Tunnel is not assumed to carry arbitrary native TCP RDP.
- This sandbox currently has Java but no Flutter/Android SDK/Gradle/ADB, no Android Studio binary, and no `/dev/kvm`; Android SDK installation and software-emulator viability must be established before claiming emulator tests.
- Historical infrastructure and student mappings are excluded from app defaults until independently verified.

## Decision log

- **2026-10-27:** User authorized a from-scratch implementation and allowed stack choice; retained the MobiDesk purpose and native FreeRDP desktop path. Chosen initial direction: native Android shell + upstream FreeRDP Android core, because this avoids Flutter/native bridge complexity and can reuse a maintained Android RDP client. This is a project decision, not a claim that integration or transport is already working.
- **2026-10-27:** Created a new private repo `master43721/mobidesk-android`. A separate public repo `master43721/mobidesk-updated` exists, but the user asked for a clean start; it remains untouched.
