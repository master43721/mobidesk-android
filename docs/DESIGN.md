# MobiDesk — Product and UX Design Document

**Version:** 1.0 · **Date:** 3 November 2026  
**Normative source:** MobiDesk PRD v1.2, §§4–5, 6, 8, 9 and 12.  
**Audience:** Product Owner, Android designer/engineer, QA and accessibility reviewer.  
**Scope:** Student-facing R1 experience plus explicitly labelled R2 dock placeholders. This document contains interaction design and screen behavior, not low-level API or native code.

## 1. Experience principles

1. **One assigned PC:** the student sees one assigned Cloud PC only. The app does not offer a VM picker or reveal infrastructure identifiers.
2. **Honest state:** display assignment, VM lifecycle and RDP transport as distinct information. A reachable TCP port is not a successful Windows sign-in.
3. **Clear identity boundary:** college sign-in and Windows lab sign-in are separate steps with visibly different wording.
4. **Explicit security:** do not offer “accept certificate”, “continue anyway”, or insecure fallback. Device policy and secure-session restrictions are explained plainly.
5. **Predictable recovery:** show a short progress state, map failure to the PRD error catalogue, and only retry when the catalogue allows it.
6. **Desktop-first controls:** keep the remote canvas central; make keyboard, pointer and modifier controls available without covering the desktop permanently.
7. **Accessible by default:** controls have labels, focus order, large touch targets and text labels in addition to icons.

## 2. Screen inventory

| Screen | Purpose | Primary content | Primary action | Important states |
|---|---|---|---|---|
| S-01 Sign in | Authenticate college account | Email, password, visibility toggle, recovery link | Sign in | Restoring, invalid credentials, network error, recovery |
| S-02 Workspace | Show student's authenticated identity and single assigned PC | Greeting, student display name, college label, one Cloud PC card | Refresh / Connect when ready | Loading, no profile, no assignment, error |
| S-03 Cloud PC status detail | Explain VM lifecycle and RDP transport separately | Lifecycle label, RDP transport label, sample age, refresh | Refresh status | All lifecycle/transport enums; stale/unknown |
| S-04 Windows credentials | Collect separate guest NLA credentials | Domain (optional), username, password, explanation | Continue | Validation, reveal/hide, cancel |
| S-05 Connecting | Show each session phase without displaying technical secrets | Progress title, current stage, cancel | Cancel | Every pre-connected state from PRD §5.3 |
| S-06 Remote desktop | Render actual Windows desktop and controls | Native RDP render target, compact toolbar, connection indicator | Keyboard, pointer, disconnect | Connected, keyboard visible, resize, reconnecting |
| S-07 Error / recovery | Explain safe next action | PRD message, optional retry, operator contact instruction | Retry or return | Each PRD §12 error code |
| S-08 Recovery flow | Complete Supabase password reset | Email confirmation and recovery password update | Send recovery / update password | Link invalid, expired, completed |
| S-09 Dock placeholder (R2) | Show roadmap concept only | “External display / dock — planned for a later release” | None in R1 | Disabled / informational only |

R1 must not show a VM power-on/off button or student assignment controls.

## 3. Student journey and screen flow

~~~mermaid
flowchart TD
  A["Launch MobiDesk"] --> B["Restore Supabase session"]
  B -->|No valid session| C["S-01 Sign in"]
  B -->|Authenticated| D["S-02 Workspace"]
  C -->|Successful sign in| D
  C -->|Recovery link| E["Password recovery"]
  E --> C
  D --> F["S-03 Check Cloud PC status"]
  F -->|No / ambiguous assignment| G["S-07 Assignment error"]
  F -->|Not ready / unknown| H["Show truthful status; refresh later"]
  F -->|RUNNING + RDP_NEGOTIATION_OK + fresh| I["S-04 Windows credentials"]
  I -->|Cancel| D
  I -->|Continue| J["S-05 Connecting"]
  J -->|NLA success + first frame| K["S-06 Remote desktop"]
  J -->|Failure| G
  K -->|Disconnect| D
  K -->|Network loss| L["Reconnecting with fresh grant"]
  L -->|Recovered| K
  L -->|Failed / revoked| G
~~~

## 4. Status vocabulary and visual treatment

### 4.1 Lifecycle states — PRD §5.2

| API state | Student-facing label | Visual treatment | Connect behavior |
|---|---|---|---|
| STOPPED | Powered off | Neutral/grey state with power-off icon | Disabled; contact lab administrator |
| STARTING | Starting | Neutral progress indicator | Disabled |
| RUNNING | Powered on | Positive status text and icon | Not sufficient by itself |
| STOPPING | Shutting down | Neutral progress indicator | Disabled |
| ERROR | Needs attention | Warning/error icon plus safe error copy | Disabled |
| UNKNOWN | Status unavailable | Neutral warning; show last sample age | Disabled |

### 4.2 RDP transport states — PRD §5.2

| API state | Student-facing label | Meaning shown to student | Connect behavior |
|---|---|---|---|
| NOT_CHECKED | Checking connection | No recent transport result | Disabled while checking |
| TCP_UNREACHABLE | Cannot reach remote desktop service | The service could not be reached | Disabled |
| TCP_REACHABLE | Remote desktop service responds | Port reachability only; desktop sign-in has not been tested | Disabled pending the stronger PRD readiness result |
| RDP_NEGOTIATION_OK | Remote desktop service is ready for sign-in | RDP negotiation succeeded; Windows credentials are still required | Eligible, subject to fresh status and assignment |
| UNKNOWN | Connection status unavailable | Probe timed out or state could not be established | Disabled |

Always show the status sample age. At 15 seconds old the sample is stale; refresh and disable Connect until the fresh result is received. Do not use “Online” for TCP_REACHABLE or infer status from the presence of an assignment row.

## 5. Wireframes

These are layout contracts, not pixel-perfect final artwork. They use existing MobiDesk design tokens where verified and leave detailed visual polish for a later design review.

### 5.1 S-01 — Sign in

~~~text
┌──────────────────────────────────────┐
│                                      │
│                MobiDesk              │
│       Your college Cloud PC          │
│                                      │
│  Sign in with your college account   │
│                                      │
│  Email                               │
│  [ institution email                ] │
│                                      │
│  Password                            │
│  [ •••••••••••••••••••          ◉  ] │
│                                      │
│  [            Sign in             ]  │
│                                      │
│          Forgot password?            │
│                                      │
│  Accounts are provided by your       │
│  institution. Self-registration      │
│  is not available.                   │
└──────────────────────────────────────┘
~~~

Behavior:
- Use Supabase email/password sign-in only.
- Restore existing session automatically; show a compact “Restoring session…” state without flashing the sign-in form first.
- Do not show “Create account”.
- Password visibility toggle changes display only; it does not persist the value.
- Forgot password follows the existing recovery/deep-link path. Do not claim email delivery until SMTP/recovery delivery is verified.
- Disable Sign in during submission and prevent duplicate requests.
- Authentication errors use safe, non-enumerating messages; PRD AUTH-01 applies to expired session.

### 5.2 S-02 — Workspace

~~~text
┌──────────────────────────────────────┐
│ MobiDesk                      [⋮]    │
│                                      │
│ Welcome back                         │
│ Student display name                 │
│ College label                        │
│                                      │
│ YOUR CLOUD PC                        │
│ ┌──────────────────────────────────┐ │
│ │ [Computer]  Assigned Cloud PC    │ │
│ │                                  │ │
│ │ Lifecycle       Powered on       │ │
│ │ RDP service     Ready for sign-in│ │
│ │ Last checked    Just now         │ │
│ │                                  │ │
│ │ [         Connect              ] │ │
│ └──────────────────────────────────┘ │
│                                      │
│ [ Refresh status ]                   │
│                                      │
│ External display / dock              │
│ Planned for a later release (R2)     │
└──────────────────────────────────────┘
~~~

Behavior:
- The card shows only the one valid assigned Cloud PC. Display name may come from the allowed user-scoped display row; do not show VM UUID, IP, hostname, port, hypervisor or another student's details.
- If zero assignments exist, show ASSIGN-01. If assignment lookup is ambiguous, show ASSIGN-03 and block Connect.
- If assignment cannot be verified transiently, show ASSIGN-02 and allow bounded refresh.
- Do not infer online/offline state from Supabase data.
- Refresh status must not trigger VM power changes.
- Dock placeholder is informational and non-interactive for R1.

### 5.3 S-03 — Cloud PC status detail

~~~text
┌──────────────────────────────────────┐
│ ‹ Cloud PC status                    │
│                                      │
│ Lifecycle                            │
│ [●] Powered on                       │
│                                      │
│ Remote Desktop                       │
│ [●] Ready for sign-in                │
│                                      │
│ Last checked: 6:00:00 AM              │
│ Probe: 384 ms                        │
│                                      │
│ [      Refresh status             ]  │
│                                      │
│ Status can change. MobiDesk checks   │
│ again before starting a session.     │
└──────────────────────────────────────┘
~~~

Display sample time, not an invented live indicator. Use a human-readable relative age only if calculated from the actual sample timestamp. When a sample expires at 15 seconds, label it “Status needs refresh”.

### 5.4 S-04 — Windows credentials

~~~text
┌──────────────────────────────────────┐
│ ‹ Windows sign-in                    │
│                                      │
│ Enter your Windows lab credentials. │
│ These are different from your       │
│ college sign-in.                    │
│                                      │
│ Domain (optional)                    │
│ [                                ]  │
│                                      │
│ Username                             │
│ [                                ]  │
│                                      │
│ Password                             │
│ [ •••••••••••••••••••          ◉  ] │
│                                      │
│ [          Continue               ] │
│ [           Cancel                ] │
│                                      │
│ Your Windows password is not sent   │
│ to MobiDesk's connection broker.    │
└──────────────────────────────────────┘
~~~

Behavior:
- Explicitly distinguish Windows lab credentials from college identity.
- Domain is optional; use only when required by the approved guest account setup.
- Do not prefill from Supabase password, Android account, previous guest session or clipboard.
- Continue validates non-empty required fields locally, then starts the grant flow.
- Cancel clears the field buffers and returns to workspace.
- No “remember password” option.
- Repeated NLA failures are rate-limited and must not reveal whether the Windows username exists.

### 5.5 S-05 — Connecting

~~~text
┌──────────────────────────────────────┐
│ Connecting to your Cloud PC          │
│                                      │
│  ✓ Checking assignment and status    │
│  ✓ Windows credentials received     │
│  ● Authorizing secure session        │
│  ○ Opening secure connection         │
│  ○ Starting Remote Desktop           │
│                                      │
│ [             Cancel              ]  │
│                                      │
│ Please keep MobiDesk open.           │
└──────────────────────────────────────┘
~~~

Behavior:
- The exact current stage is visible; completed stages use a check mark and the current stage uses progress.
- Never show raw grant, JWT, WSS URL with token, private endpoint or FreeRDP stack trace.
- Cancel requests cleanup immediately; returning to workspace is allowed only after local cleanup is confirmed or the app shows SESSION-03 and blocks a duplicate attempt.

### 5.6 S-06 — Remote desktop

~~~text
┌──────────────────────────────────────┐
│ [Pointer] [Keyboard] [Ctrl] [Alt]    │
│ [Shift] [Win] [Esc] [Tab] [Del] [⋯] │
├──────────────────────────────────────┤
│                                      │
│                                      │
│          NATIVE RDP RENDER           │
│          TARGET / DESKTOP            │
│                                      │
│                                      │
├──────────────────────────────────────┤
│ [←] [↑] [↓] [→] [Ctrl+Alt+Delete]    │
│ [Hide keyboard]       [Disconnect]   │
└──────────────────────────────────────┘
~~~

Behavior:
- Native RDP render target fills available space. Compose must not decode and redraw desktop frames.
- Toolbar is a native Android control overlay owned by RdpSessionActivity.
- The toolbar can collapse to maximize desktop area, but an accessible way to restore it remains available.
- Provide explicit Disconnect. Disconnect immediately disables input and enters cleanup.
- Show a small connection-state indicator. Do not show private diagnostics or endpoint text.
- Apply FLAG_SECURE before first frame.

## 6. Pointer, touch, shortcut and keyboard model

### 6.1 Default trackpad model — PRD FR-INPUT-01

| Gesture | Action | Feedback |
|---|---|---|
| One-finger move | Relative pointer movement | Pointer indicator/cursor follows movement |
| One-finger tap | Left click | Short visual click pulse |
| Long-press, then drag | Left-button drag | Drag state visible while pressed |
| Two-finger drag | Scroll | Vertical/horizontal scroll mapped to wheel/scroll events |
| Two-finger tap | Right click | Right-click indicator/pulse |

Design rules:
- Provide a clear visual boundary for the trackpad mode and an explicit toggle between trackpad and direct-touch mode if direct touch is supported by the pinned client.
- Never trigger local Android context menus when the gesture is intended for the remote desktop.
- Support mouse hover, external mouse buttons and scroll wheel.
- Show a brief first-use gesture hint that can be dismissed; do not re-show on every connection.

### 6.2 Shortcut toolbar — PRD FR-INPUT-02

Primary quick controls:
- Ctrl, Alt, Shift and Win are momentary modifiers by default; user may lock them for multi-key sequences.
- Esc, Tab and Delete send individual keys.
- Arrow cluster: left, up, down, right.
- Keyboard button opens/hides the Android soft keyboard.
- Dedicated Ctrl+Alt+Delete control sends the secure attention sequence through the supported FreeRDP API; it must not emulate the sequence as ordinary text.
- A visible modifier-lock indicator and “Clear modifiers” action prevent stuck Ctrl/Alt/Shift/Win states.
- Shortcut controls provide TalkBack labels and selected/locked state.

### 6.3 Keyboard behavior
- Physical keyboard input is forwarded when the RDP window has focus.
- The soft keyboard supports Unicode text entry plus special-key handling.
- Opening the IME must resize/reposition toolbar and canvas without creating a false background session state.
- Back first dismisses the IME/toolbar overlays; a second deliberate action invokes session disconnect confirmation/flow.
- No keyboard text or clipboard content is sent to telemetry.
- R1 clipboard, file, audio, microphone, camera and drive redirection remain out of scope unless separately approved by security review.

## 7. Rotation, resize and lifecycle UX

- Rotation/configuration change must not orphan the broker relay or native worker. Keep the session coordinator identity separate from transient Activity view instances.
- A resize event may update the RDP desktop size only if the pinned FreeRDP surface/DisplayControl spike proves it works with the guest. The UI must not promise seamless resizing before that test.
- If live render-target reattachment cannot be supported safely, the implementation must follow the documented reconnect-to-the-same-assigned-VM fallback approved for R2; never silently switch to a different VM.
- IME appearance, system bars and multi-window inset changes do not count as session backgrounding.
- If the RDP Activity is truly stopped/backgrounded or the device is screen-locked, start the PRD's five-second cancelable teardown. If it does not resume within five seconds, close FreeRDP/WSS/local bridge, clear transient credentials and broker session; return requires status refresh, a new Windows prompt and a new grant.
- Network loss shows SESSION-01 and a bounded reconnect state; reconnect always obtains a new grant and revalidates assignment/status.
- Administrator/policy revocation shows SESSION-02; do not offer retry until auth/assignment is revalidated.
- Cleanup uncertainty shows SESSION-03 and blocks a new session until cleanup is confirmed.

## 8. Exact error catalogue — PRD §12

English text below is reproduced exactly from PRD v1.2. Telugu strings are draft translations only and MUST be reviewed by a native Telugu speaker before release. Do not paraphrase or alter English PRD copy silently.

| Code | Exact English user copy | Telugu draft — HUMAN REVIEW REQUIRED |
|---|---|---|
| AUTH-01 | Your sign-in session expired. Sign in again. | మీ సైన్-ఇన్ సెషన్ గడువు ముగిసింది. మళ్లీ సైన్ ఇన్ చేయండి. |
| ASSIGN-01 | No Cloud PC is assigned to your account. Contact the lab administrator. | మీ ఖాతాకు Cloud PC కేటాయించబడలేదు. ల్యాబ్ నిర్వాహకుడిని సంప్రదించండి. |
| ASSIGN-02 | We could not verify your assigned Cloud PC. Try again. | మీకు కేటాయించిన Cloud PCని ధృవీకరించలేకపోయాము. మళ్లీ ప్రయత్నించండి. |
| ASSIGN-03 | Your Cloud PC assignment needs administrator review. | మీ Cloud PC కేటాయింపును నిర్వాహకుడు సమీక్షించాలి. |
| VM-01 | Your Cloud PC is powered off. Contact the lab administrator. | మీ Cloud PC ఆఫ్‌లో ఉంది. ల్యాబ్ నిర్వాహకుడిని సంప్రదించండి. |
| VM-02 | Your Cloud PC is starting or is not ready yet. | మీ Cloud PC ప్రారంభమవుతోంది లేదా ఇంకా సిద్ధంగా లేదు. |
| VM-03 | Your Cloud PC cannot be reached right now. | ప్రస్తుతం మీ Cloud PCని చేరుకోలేకపోతున్నాము. |
| GRANT-01 | We could not authorize a session. Try again. | సెషన్‌కు అనుమతి ఇవ్వలేకపోయాము. మళ్లీ ప్రయత్నించండి. |
| GRANT-02 | This session request expired. Try connecting again. | ఈ సెషన్ అభ్యర్థన గడువు ముగిసింది. మళ్లీ కనెక్ట్ చేయండి. |
| TUNNEL-01 | The secure connection service could not be reached. | సురక్షిత కనెక్షన్ సేవను ప్రస్తుతం చేరుకోలేకపోతున్నాము. |
| RDP-CERT-01 | The Cloud PC security certificate could not be verified. Connection blocked. | Cloud PC భద్రతా సర్టిఫికేట్‌ను ధృవీకరించలేకపోయాము. కనెక్షన్ నిలిపివేయబడింది. |
| RDP-AUTH-01 | Windows sign-in failed. Check your separate lab credentials. | Windows సైన్-ఇన్ విఫలమైంది. మీ ప్రత్యేక ల్యాబ్ ఆధారాలను తనిఖీ చేయండి. |
| RDP-01 | The Cloud PC could not complete the remote desktop connection. | Cloud PC రిమోట్ డెస్క్‌టాప్ కనెక్షన్‌ను పూర్తి చేయలేకపోయింది. |
| SESSION-01 | Connection lost. Reconnecting securely… | కనెక్షన్ కోల్పోయింది. సురక్షితంగా మళ్లీ కనెక్ట్ చేస్తున్నాము… |
| SESSION-02 | Your session was ended by the administrator or access policy. | నిర్వాహకుడు లేదా యాక్సెస్ విధానం మీ సెషన్‌ను ముగించింది. |
| SESSION-03 | The session did not close cleanly. Reopen only after cleanup is verified. | సెషన్ సరిగ్గా మూసివేయబడలేదు. క్లీనప్ పూర్తయిందని నిర్ధారించిన తర్వాత మాత్రమే మళ్లీ తెరవండి. |

### Error presentation rules

- Keep the exact English copy as the fallback language until Telugu review is approved.
- Add a short action button only where the PRD retry policy allows it.
- ASSIGN-01 and ASSIGN-03 do not retry automatically.
- RDP-CERT-01 has no user override.
- SESSION-03 blocks another attempt until cleanup is verified.
- Never show raw backend error text, hostname, IP, port, VM UUID, stack trace or another student's identity.

## 9. Accessibility

- All interactive targets are at least 48 × 48 dp unless a larger target is required for the toolbar.
- TalkBack labels include role, action and current state; icons alone do not carry meaning.
- Status is announced as text (“Powered on”, “Remote Desktop reachable”) rather than color alone.
- Maintain logical focus order: title → content → primary action → secondary action.
- Focus remains visible for hardware keyboard navigation; every modifier/shortcut has a semantic selected/locked state.
- Support system font scaling without clipping status, errors or credential labels.
- Do not use color as the only way to identify RUNNING, ERROR, SELECTED or LOCKED.
- Verify text and control contrast against WCAG 2.1 AA targets; test both light and dark themes.
- The RDP desktop content is remote/untrusted; do not merge its accessibility tree with MobiDesk controls in a way that makes Disconnect or security status unreachable.
- Error announcements must not interrupt password entry; announce changes once and move focus to the message only when needed.

## 10. Visual tokens

### Existing palette and typography

- **[VERIFIED: app/src/main/java/com/mobidesk/android/ui/MobiDeskTheme.kt at main@5979320...]** Existing dark/light Material 3 schemes define:
  - Ink: #101923
  - Panel: #17232F
  - Elevated panel: #1D2B38
  - Paper: #F5F7F8
  - Muted text: #9AA7B4
  - Teal: #20BFA9
  - Dark teal: #168C7D
  - Warning: #E4AA4F
  - Danger: #DF7474
- **[VERIFIED: same file]** Existing typography includes 28sp headline, 20sp titleLarge, 16sp titleMedium/bodyLarge, 14sp bodyMedium and 12sp labelMedium with SansSerif.
- **[VERIFIED: app/src/main/res/values/styles.xml at main@5979320...]** Android status/navigation bar base color is #0B1219 and accent is #20BFA9.

### Usage rules
- Use existing MobiDesk colors as the starting system; do not introduce arbitrary new brand colors in R1.
- Use teal for primary actions/focus, warning for not-ready or transient warnings, danger for blocking/security errors, neutral surface for unknown/stale.
- Check contrast pairings, especially muted text in dark mode and danger/warning colors on both surfaces; adjust only through a reviewed theme change.
- Use 8dp spacing rhythm, 16dp standard screen inset, 20–24dp card padding and restrained elevation as proposed layout tokens. These spacing values are proposed design tokens, not verified current values.

## 11. R2 dock UX placeholder

R1 displays a disabled informational card only:

~~~text
┌──────────────────────────────────────┐
│ External display / dock               │
│ Planned for a later release (R2).    │
│ Dock pairing and display support     │
│ are not available in this pilot.     │
└──────────────────────────────────────┘
~~~

No connect button, device discovery, USB permission request, stream preview or AOA pairing flow is active in R1. R2 must have explicit user confirmation and key-backed challenge-response. AOA manufacturer/model/serial strings are discovery hints only, not proof of identity.

## 12. UX acceptance

- Every PRD §5.2 lifecycle and transport state is distinguishable, accessible and correctly gates Connect.
- Every PRD §5.3 connection state has a visible progress representation or safe transition; no secret is shown.
- Every PRD §12 code has exact English copy and a Telugu draft marked for human review.
- Sign-in has no self-registration; workspace displays one assignment only; no student power/assignment controls.
- Windows credential prompt is explicitly separate from college sign-in and has no remember-password option.
- Native RDP Activity has FLAG_SECURE, accessible toolbar, touch/pointer/keyboard model and explicit disconnect.
- Rotation, true background, screen lock and network loss do not leave an orphaned session.
- R2 dock placeholder cannot initiate a connection in R1.
