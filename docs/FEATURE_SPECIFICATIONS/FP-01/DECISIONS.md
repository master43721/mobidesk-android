# FP-01 Decision Register

**Version:** 0.1 — REVIEW DRAFT  
**Date:** 3 November 2026  
**Owner:** MobiDesk Project Owner  
**Rule:** A source fact is not the same as a product decision. An existing code path is not automatically an approved requirement.

## 1. Decision records

| ID | Topic | Current evidence / direction | Status |
|---|---|---|---|
| FP01-D-01 | Auth provider | Current app uses Supabase Auth through supabase-kt BOM 3.6.0. | VERIFIED BASELINE |
| FP01-D-02 | Sign-in method | Current app uses email/password; no other sign-in provider is wired. | VERIFIED BASELINE |
| FP01-D-03 | Auth flow | Current Supabase Auth plugin uses PKCE. | VERIFIED BASELINE |
| FP01-D-04 | App signup | Current Android UI has no Create account/sign-up route. | VERIFIED BASELINE |
| FP01-D-05 | Project-level signup | Historical Supabase discovery dated 27 Oct 2026 reported signup enabled. Current live value is not rechecked. | UNKNOWN — OWNER DECISION |
| FP01-D-06 | Account provisioning | Historical discovery described a roster-gated provisioning trigger. Current live trigger/settings require re-verification. | REPORTED — VERIFY |
| FP01-D-07 | Recovery redirect | App currently configures com.mobidesk.android://auth-callback/recovery. Historical discovery reported that exact redirect added to the allow-list. | VERIFIED IN APP CODE; LIVE ALLOW-LIST REPORTED |
| FP01-D-08 | Recovery channel | Current implementation sends a Supabase email recovery request and handles an app deep link. | VERIFIED BASELINE |
| FP01-D-09 | Recovery post-success session | Current code retains the session established by the recovery callback and then updates the password. Desired product behavior is not explicitly confirmed. | OWNER CONFIRMATION REQUIRED |
| FP01-D-10 | Email error enumeration | Current mapper distinguishes unconfirmed email; generic non-enumerating wording is proposed. | PROPOSED — OWNER DECISION |
| FP01-D-11 | Recovery acknowledgement | Current UI uses generic "If this account can receive recovery email..." acknowledgement. | VERIFIED BASELINE |
| FP01-D-12 | Password minimum | Current UI checks minimum 8 characters; live Supabase password policy is unknown. | VERIFIED IN CODE; PROVIDER POLICY UNKNOWN |
| FP01-D-13 | Session restoration | Current ViewModel observes Supabase sessionStatus. Exact persistence backend/security for SDK 3.6.0 is not yet inspected. | VERIFIED FLOW; STORAGE UNKNOWN |
| FP01-D-14 | Sign-out semantics | Current repository calls client.auth.signOut() without an explicit scope argument. Exact pinned SDK default and remote effect are unverified. | UNKNOWN / BLOCKING |
| FP01-D-15 | Auth/Workspace boundary | Current ViewModel owns Auth and also invokes StudentWorkspaceRepository after Authenticated. Target design separates FP-01 from FP-02. | PROPOSED REFACTOR |
| FP01-D-16 | MFA/passkeys/social auth | Not included in the current FP-01 draft. Institution requirement is unknown. | PROPOSED OUT OF SCOPE — OWNER CONFIRM |
| FP01-D-17 | Redirect architecture | Current app uses a custom URI scheme. Verified HTTPS Android App Links have not been evaluated in this repo. | ENGINEERING DECISION OPEN |
| FP01-D-18 | Password autofill | Current Compose fields specify keyboard types; password-manager autofill behavior has not been verified. | ENGINEERING DECISION OPEN |
| FP01-D-19 | Email delivery | Historical discovery reported custom SMTP disabled and delivery unverified. Current sender/config must be rechecked. | REPORTED HISTORICAL / BLOCKING FOR RECOVERY ACCEPTANCE |
| FP01-D-20 | Parent PRD mapping | Parent index lists FR-AUTH-01 and FR-AUTH-02. Exact normative wording must be checked against parent PDF before this draft is approved. | OPEN |

## 2. Recommended decisions for Owner review

| ID | Recommendation | Why |
|---|---|---|
| REC-01 | Disable provider-level public signup if all student identities must be institution-provisioned. | Hiding the app signup button does not disable the Supabase signup endpoint. |
| REC-02 | Use generic outward-facing sign-in/recovery errors for unknown, incorrect and unconfirmed accounts. | Reduces account enumeration. |
| REC-03 | Keep Supabase Auth SDK as the only credential/session authority. | Avoids duplicating password and token logic in the app. |
| REC-04 | Keep student profile/VM eligibility outside FP-01. | Authenticated identity is not the same as institution enrolment or VM authorization. |
| REC-05 | Verify exact SDK storage/sign-out/PKCE semantics before implementation changes. | Avoids undocumented assumptions about security-critical SDK behavior. |
| REC-06 | Retain email/password as the pilot login; add MFA only if institution policy requires it. | Avoids expanding scope without an explicit requirement. |

## 3. Change control

When a decision changes, record:
- decision ID and date;
- Owner approval;
- source requirement affected;
- Android/project configuration affected;
- test cases and evidence required;
- migration/rollback implications.

Do not delete old decisions. Mark them SUPERSEDED and record the replacement decision.
