# FP-01 Revision Notes — v0.1 to v0.2

**Date:** 4 November 2026  
**Source:** Project Owner's detailed critique of the v0.1 package.

## 1. Parent traceability
- Corrected FR-AUTH-01 to cover sign-in, session restoration, password recovery and sign-out in FP-01.
- Removed the incorrect mapping of password recovery to FR-AUTH-02.
- FR-AUTH-02 profile/assignment query authorization using user token and RLS is assigned to FP-02/FP-03.

## 2. Recovery lifecycle defects
- Added RECOVERY_ONLY AuthLevel.
- Added durable CALLBACK_PENDING and PASSWORD_UPDATE_REQUIRED recovery stages.
- Require marker write before asynchronous PKCE exchange.
- Prevent any Authenticated SDK event during recovery from triggering workspace loading.
- Recovery cancel/back signs out the recovery session and clears local state.
- Process death during callback pending does not replay the code; it shows interrupted recovery and requests a fresh link.
- Process death after recovery is ready restores the password-update form if the SDK session remains valid.
- Successful password update clears the marker and promotes the state to FULLY_AUTHENTICATED.

## 3. Callback idempotency
- MainActivity remains the callback entry point; no new AuthDeepLinkRouter is proposed.
- Require exact scheme, host and recovery path.
- Add durable one-time fingerprint/stage handling; never store the raw callback code or URL.
- Runtime recreation/process-death behavior remains a required test, not a claimed observed runtime defect.

## 4. Sign-out
- Verified supabase-kt 3.6.0 default scope is LOCAL (current session, not all devices).
- The SDK makes a remote current-session logout request and clears local storage on success.
- If the request fails, current app behavior remains authenticated; v0.2 specifies an explicit local clearSession fallback and honest UI copy.

## 5. Architecture reduction
Removed the v0.1 recommendation for:
- a 14-state Auth state machine;
- AuthSessionCoordinator;
- AuthDeepLinkRouter;
- a broad new package hierarchy or UI redesign.

Added only:
- app-owned AuthLevel;
- a small durable recovery marker;
- AuthRepository interface and WorkspaceRepository test seam;
- a lightweight factory for real/fake adapters;
- workspace-load gate on FULLY_AUTHENTICATED.

## 6. Supabase email delivery
- Reclassified custom SMTP as a release blocker, not an unspecified future check.
- Historical discovery reported custom SMTP disabled.
- Supabase docs state that the built-in hosted email service only sends to organization team members and has a low project-wide rate limit; custom SMTP or Send Email Hook is required for ordinary student recipients.
- Added explicit email_address_not_authorized and over_email_send_rate_limit UI handling.
- Added owner action to choose provider/domain and configure project secrets. No credentials are stored in GitHub/docs/chat.
- Added local Supabase + Mailpit as an automated Auth-flow test option; it does not replace a real external-mailbox test.

## 7. Error and account-enumeration policy
- Removed the contradictory "MUST — proposal" requirement.
- Keep email_not_confirmed actionable; test deployed GoTrue order before changing copy. Current upstream GoTrue source checks password validity before email-confirmation status.
- Reset request acknowledgement remains generic for known/unknown recipients.
- Map over_email_send_rate_limit separately from over_request_rate_limit.

## 8. Test and planning scope
- Defined exact four-test baseline.
- Reduced test scope to a must-run 24-case core suite plus focused manual email, device, URI collision, TalkBack and storage checks.
- Removed assumption that a hosted staging project exists.
- Added per-task estimates and dated planning windows from 4 to 13 November 2026.

## 9. No implementation
All changes in v0.2 are documentation-only. No Android source, build config, CI, Supabase project settings, database, server or VM was modified.
