# MobiDesk — College Administrator and Student Account Model

**Document version:** 1.0 · **Date:** 4 November 2026  
**Canonical repository:** master43721/mobidesk-android  
**Status:** Owner-directed product model; implementation and successor-PRD work required.  
**Scope:** The single-college prototype's Android client, Supabase identity model, trusted control plane, host/VM operations and student RDP access.

## 1. Owner-confirmed account model

MobiDesk has two application roles:

1. **College Administrator** — the institution-provisioned operator account. It manages student accounts, student-to-VM assignments, approved server/VM operations and active student connections.
2. **Student** — an institution-provisioned account. It can sign in and use only the Cloud PC and application data assigned to that student.

There is no student self-registration. The Android app MUST NOT display a Create account / Sign up action or invoke the public Supabase signup flow. A student may complete an invitation/activation link and set their first password, but the account already exists because the College Administrator provisioned it. Activation is not self-registration.

For the prototype, begin with one College Administrator account, bootstrapped outside the Android client by the Project Owner using a trusted provisioning path. The College Administrator creates student accounts. The College Administrator cannot promote a student to College Administrator or create another administrator through the normal student-provisioning workflow. Additional administrator accounts require an explicit owner-controlled process.

The product has one college/institution tenant in this prototype. Multi-college tenancy and delegated administrator hierarchies are not part of this initial model.

## 2. Role and permission matrix

| Capability | College Administrator | Student |
|---|---:|---:|
| Sign in to MobiDesk | Yes | Yes |
| View own profile | Yes | Yes |
| View all student records in this college | Yes | No |
| Create/provision student accounts | Yes | No |
| Resend student invitation | Yes | No |
| Suspend/reactivate/disable student access | Yes | No |
| View all configured hosts and VMs | Yes | No |
| View server and VM health | Yes | Own assigned VM status only |
| Start/shut down/restart an approved VM | Yes, through guarded server actions | No |
| Assign/unassign a student to a VM | Yes | No |
| Choose a VM destination for an RDP session | No client-side choice; broker resolves the student assignment | No |
| View active student connections | Yes, limited operational metadata | Own current session only |
| Terminate an active student session | Yes, audited policy action | Disconnect own session |
| View operational audit events | Yes, authorized fields only | No |
| Access host shell/SSH or raw libvirt credentials from Android | No | No |
| Read another student's password, refresh token or RDP content | No | No |

The administrator role is an application role, not a Supabase service-role credential. Administrative authority never gives the Android app a database-owner key or arbitrary host command access.

## 3. College Administrator bootstrap

1. The Project Owner creates the initial College Administrator Auth user through the Supabase Dashboard or a trusted server-side bootstrap operation.
2. The trusted bootstrap path inserts the College Administrator into the proposed private.app_user_roles authority table with role college_admin and active status. The initial admin is not inserted into the student roster or student profile table.
3. The admin account uses a unique institutional email and a strong individual password. Do not use a shared default password or commit credentials to the repository.
4. Require MFA for the College Administrator before enabling privileged production operations. For the prototype, the MFA setup and recovery path is a release prerequisite for remote/real server actions.
5. The admin signs in through the same MobiDesk Android application. After the authenticated identity is resolved and the backend validates the role, the app opens the College Administrator workspace instead of the Student workspace.
6. A role cannot be selected from the login screen, inferred from email domain, or changed by editing UI state or user metadata. The backend resolves private.app_user_roles on each privileged request.

If the initial admin account is lost or disabled, recovery is an owner-controlled bootstrap operation. A student account cannot recover or elevate itself into the College Administrator role.

## 4. Student account provisioning and first activation

### 4.1 Administrator-driven workflow

1. The College Administrator opens Student Management and enters the student's institution-approved name, institutional email, roll/registration number and any required roster fields.
2. The backend validates required fields and checks duplicate roster, email and account records, then assigns the server-controlled student role in private.app_user_roles.
3. The backend creates or confirms the pending row in the private student roster before inviting the Auth user.
4. A trusted server-side provisioning service invokes the Supabase Auth Admin invitation operation. The secret/service-role credential exists only in the server environment, never in the Android app.
5. The existing roster-gated Auth trigger may create/link the public student profile when the invited Auth user is created. The exact trigger behavior must be tested with the invitation path before production use; do not bypass or weaken its roster check.
6. The student receives an institution-branded invitation/activation email at the provisioned address. It tells the student that the college has created the account and asks them to activate it and set their own password.
7. The student opens the invitation link, completes the supported invitation verification flow, sets a first password, and is then able to sign in using their provisioned institutional email and password.
8. The College Administrator sees the invitation and activation state in Student Management. The administrator may resend an expired invitation, suspend access, reactivate an eligible account or disable the account.

### 4.2 Activation is not student signup

- The Auth user and roster eligibility are created by the college-controlled workflow before the student opens the email.
- The student cannot enter an arbitrary email and create an account.
- There is no public registration route or client-side Supabase signUp call.

**Supabase setting note:** After the trusted invitation path has passed end-to-end tests, disable the project-level **Allow new users to sign up** setting to block self-service account creation. Keep the Email provider enabled so existing students can continue email/password sign-in. Supabase documents that disabling new-user signup leaves invitations as the supported way to create users; invitation itself is an Auth Admin action performed from a trusted server or Dashboard. Verify current project settings and existing-user sign-in plus invitation delivery before changing production configuration. This documentation PR does not change Supabase settings.

References:
- https://supabase.com/docs/guides/auth/general-configuration
- https://supabase.com/docs/guides/auth/users
- The invitation link is scoped to the pre-created user and has the expiry/one-time semantics configured by Supabase.
- A student without a valid invitation cannot create an account by opening a crafted callback or deep link.

### 4.3 Invitation callback requirement

The current Android callback path uses Supabase Kotlin 3.6.0 PKCE handling. Do not assume that the existing password-recovery callback automatically handles an invitation link. The invite flow must be separately proven against the exact 3.6.0 SDK and configured email template/redirect behavior. Supabase Kotlin 3.6.0 exposes an Email INVITE OTP type; use the SDK-supported invitation verification path only after the actual redirect payload and callback semantics are verified.

Until that spike passes:
- do not mark first-time activation as implemented;
- do not change production signup settings based on assumption;
- do not clear or replace an unrelated authenticated session after a forged/invalid invite callback;
- keep invite tokens and PKCE verifiers out of logs, URLs after processing, persistence outside the SDK, and telemetry.

### 4.4 Account states

Keep invitation/provisioning state distinct from authorization state.

| State | Meaning | Student access |
|---|---|---|
| ROSTER_PENDING | Admin has recorded an eligible student; Auth invitation not yet confirmed as sent | Denied |
| INVITED | Auth user created and invitation sent; first activation not complete | Denied until activation |
| ACTIVE | Invitation accepted, account active and assignment policy satisfied | Allowed only to own assigned resources |
| SUSPENDED | Admin temporarily blocks access | Denied; active sessions revoked |
| DISABLED | Account permanently disabled/offboarded | Denied; sessions revoked |

A failed email delivery does not silently mark a student ACTIVE. Resend operations are idempotent and auditable. Never send the student's initial or reset password in plain text to the administrator.

## 5. Student access and assignment boundary

- Every student request is authorized from the authenticated subject and a server-verified role.
- Students can read only their own profile and assignment under RLS.
- The broker derives exactly one active VM assignment from trusted server-side records.
- Student requests cannot include a destination VM ID, hostname, IP address or port.
- The student cannot see the server inventory, other students, unassigned VMs or administrator controls. The student role is assigned by trusted provisioning, not student-supplied metadata.
- Zero, multiple, inactive or unapproved assignments fail closed.
- VM4 remains excluded until independently verified and explicitly approved.
- When an administrator changes or removes an assignment, the backend revokes affected grants and terminates any now-unauthorized active relay within the documented revocation window.

## 6. College Administrator workspace

The same native Kotlin/Jetpack Compose application selects the workspace only after the backend has resolved the authenticated account's role.

### 6.1 Administrator home

Show operational summaries without exposing secrets:
- host/service health;
- VM counts by lifecycle state;
- pending student invitations;
- active student sessions;
- recent safe administrative events;
- clear warnings for unavailable or stale status.

### 6.2 Student management

The administrator can:
- create a roster-backed student account and send the invitation;
- search/filter provisioned students;
- view invitation and activation status;
- resend an expired/failed invitation;
- suspend, reactivate or disable a student;
- view the student's current assignment and safe connection state.

### 6.3 Assignment management

The administrator can:
- view approved VM inventory and assignment occupancy;
- assign one eligible VM to a student;
- change or remove an assignment;
- see conflicts, unverified VMs and unavailable capacity;
- confirm a change before it affects a running session.

The server checks the administrator's authority, student status, VM allowlist, eligibility and uniqueness. The UI is not the security boundary.

### 6.4 Server and VM management

The administrator can:
- inspect configured host availability and coarse CPU, memory and storage health;
- inspect approved VM inventory and lifecycle/readiness;
- request guarded start, graceful shutdown and restart actions on eligible VMs;
- mark an approved VM unavailable/maintenance;
- see action progress, final state and safe errors.

Server actions are executed by a trusted server-side host agent/control plane with a narrow allowlist. The Android app MUST NOT connect directly to libvirt, expose SSH credentials, accept arbitrary shell commands, or let an admin enter a raw host/VM endpoint. Destructive actions require confirmation and an audit event. A powered-off VM is not represented as RDP-ready.

### 6.5 Student connection management

The administrator can:
- view active sessions with student, assigned VM display label, start time, duration and safe session state;
- inspect connection-stage/error summaries;
- terminate a session for support, maintenance or access revocation;
- suspend a student and trigger session/grant revocation;
- verify that the broker closed the relay and released the session slot.

The admin view MUST NOT reveal Windows passwords, Supabase tokens, one-use grants, raw RDP bytes, screen contents or private VM addresses.

## 7. Trusted architecture and authorization

- The Android app contains only the public Supabase client configuration and the current user's session token.
- A trusted control-plane API validates the Supabase JWT, active session, server-controlled role and action-specific policy on every administrative operation.
- The authoritative role source is the proposed private.app_user_roles table with role and active status. It is written only by trusted bootstrap/provisioning services, protected by RLS and not directly readable or writable by client roles. The backend resolves it on every privileged request. Never authorize using user_metadata, email suffix, a local role preference, navigation route or client-supplied role.
- Supabase RLS remains enabled. Student policies are ownership-scoped; administrator access uses a separately reviewed policy or trusted backend path.
- Supabase Auth Admin operations (invite/create/update/disable) execute only on a trusted server using a secret key. The key MUST NOT be present in the Android APK, BuildConfig, source, CI artifact or mobile request body.
- Hypervisor/host actions execute only on the trusted server/host agent. The Android client never receives a host credential or shell endpoint.
- Revalidate active administrator session and role for privileged operations. Sensitive operations should require recent authentication/MFA assurance.
- Every create/invite, resend, suspension, reactivation, disable, assignment change, VM lifecycle action and session termination is audit logged with actor, action, target reference, timestamp, result and opaque correlation ID.
- Audit records exclude passwords, tokens, invitation links, raw grants, private endpoints and desktop contents.

## 8. Initial prototype boundaries

Included:
- one College Administrator account created through a trusted bootstrap;
- student account provisioning initiated by that administrator;
- invitation/activation and password setup;
- role-aware administrator and student workspaces in the same Android app;
- student roster, account lifecycle and assignment management;
- approved host/VM health and guarded VM lifecycle actions;
- active connection visibility and administrative termination;
- RLS, server-side RBAC and audit events.

Not included:
- student self-registration;
- a student ability to grant themselves a VM or change their role;
- creating additional administrators through the ordinary student-provisioning screen;
- arbitrary SSH, host shell, terminal or raw libvirt access from Android;
- unrestricted VM image/template editing, network reconfiguration or host firewall management from the app;
- multi-college tenancy, delegated admin hierarchies or public administrator signup.

## 9. Requirements and acceptance summary

| ID | Requirement | Acceptance evidence |
|---|---|---|
| IAM-01 | Exactly two prototype application roles: college_admin and student | Auth/role contract and negative role tests |
| IAM-02 | Initial college_admin is provisioned out-of-band by trusted owner bootstrap | Bootstrap runbook and account/claim verification |
| IAM-03 | Student self-signup is unavailable in UI and client | UI test, source scan and auth configuration verification |
| IAM-04 | Only college_admin can provision student Auth users | Backend authorization tests; student requests denied |
| IAM-05 | Student account is roster-created before invitation | Trigger integration test and duplicate/idempotency tests |
| IAM-06 | Student invitation activation is verified using the exact SDK flow | Physical-device deep-link and expired/replayed invite tests |
| IAM-07 | Role is server-derived; user_metadata and client role input are ignored | JWT/RLS/API tampering tests |
| IAM-08 | Students can access only their own profile and assigned VM | Cross-student RLS and broker tests |
| ADM-01 | College administrator can view/manage eligible student records | Admin integration tests |
| ADM-02 | College administrator can assign/revoke student VM assignments | Assignment policy and audit tests |
| ADM-03 | College administrator can inspect host/VM health | Broker/host agent contract and freshness tests |
| ADM-04 | Guarded server actions are admin-only and allowlisted | Deny student, deny unapproved VM/action, test confirmations |
| ADM-05 | Administrator can list and terminate active student sessions | Relay termination and slot-release tests |
| ADM-06 | All privileged changes are auditable without secrets | Audit completeness and redaction scan |

## 10. Source references

- Current repository's historical provisioning/RLS inventory: docs/SUPABASE_DISCOVERY.md (snapshot 27 October 2026; revalidate live project before implementation).
- Current Android implementation baseline: app/src/main/AndroidManifest.xml, MainActivity.kt, MobiDeskApplication.kt and MobiDeskViewModel.kt.
- Supabase Kotlin 3.6.0 pinned SDK source: https://github.com/supabase/supabase-kt/tree/3.6.0
- Supabase invitation action is an Auth Admin operation and must run in a trusted server environment: https://supabase.com/docs/guides/auth/users
- Kotlin Admin invite API: https://supabase.com/docs/reference/kotlin/auth-admin-inviteuserbyemail
- Kotlin invite verification types: https://supabase.com/docs/reference/kotlin/auth-verifyotp
