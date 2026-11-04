# FP-02 Security and Privacy Requirements — Student Dashboard and Workspace

**Version:** 0.1  
**Date:** 4 November 2026  
**Status:** Feature-level security draft, subordinate to parent PRD v1.2 and SECURITY_THREAT_MODEL.md.

## 1. Security objective

FP-02 must show a student only their own approved profile and assigned Cloud PC display information. It must not reveal infrastructure routing or permit the student UI to act as an authorization, assignment or VM-management authority.

The Android client is untrusted. The UI's Connect enablement is a usability control only; the broker and FP-03/FP-04 services remain authoritative.

## 2. Data classification

| Data | Allowed in student UI? | Rule |
|---|---|---|
| Student display name | Yes, if present in approved profile | Current user's RLS-scoped record only |
| College/institution label | Yes, if present and approved | Optional; omit when absent |
| Student email | Only where explicitly approved | Do not display as a fallback name by default |
| Roll number / roster row | No by default | Not included in existing screen definition |
| Student Auth user ID | No | Internal repository key only |
| Cloud PC display label | Yes | Must be a student-safe label from FP-03 |
| Assignment state | Yes | Typed state such as assigned/not assigned/review needed |
| VM ID / UUID / hypervisor name | No | Never render or pass from a UI selector |
| Hostname / private IP / port / network route | No | Never fetch into FP-02 DTO |
| Guacamole connection ID/base URL | No | Never request from Supabase into student UI |
| Lifecycle state / RDP transport state | Yes | Use approved enums and safe wording |
| sampledAt / sample age | Yes | Only as status freshness metadata |
| requestId | Not as user content | Opaque diagnostic only, safely redacted if logged |
| Supabase access JWT / refresh token | No | SDK only; never Compose state, navigation or logs |
| Session grant | No | Not part of FP-02 |
| Windows credentials | No | Owned by FP-05; FP-02 never collects them |
| Raw backend exception / response body | No | Map to exact PRD error code/copy |

## 3. Identity and authorization controls

- FP-01 must provide a fully authenticated student session before workspace data is requested.
- Use the current session/user token; don't accept user ID from text fields, navigation arguments, saved state or client-supplied API payload.
- Student profile and assignment reads remain RLS-scoped and are implemented/owned by FP-03.
- The status request uses the current Supabase access JWT in its Authorization header and no target selector.
- A missing/ambiguous/inactive/unmapped assignment fails closed.
- Do not show a VM card for a profile merely because a row exists in a previous cache.
- Clear cached workspace/status data on sign-out, session expiry and subject change.
- Broker revalidates assignment/readiness during grant flow independently.

## 4. Infrastructure and action boundary

Student workspace must not include:
- VM start/stop/restart/power controls;
- assignment selection or change;
- host terminal, SSH, libvirt console or server settings;
- arbitrary network endpoint fields;
- RDP hostname/IP/port;
- admin links or student directory.

The user may refresh readiness and request a connection only when eligible. FP-02 must not make the client directly open RDP to a guest.

## 5. Freshness and UI tampering

- Status is fresh only within the contract's stated sample lifetime (15 seconds in current API contract).
- Stale, missing, malformed, unknown or failed status disables Connect.
- A failed refresh cannot preserve an actionable prior Ready state.
- UI state cannot override server assignment or authorization; FP-06/broker rechecks.
- The status endpoint itself must not return private target information. The current API contract explicitly excludes VM identifiers, names, endpoints, hostnames and ports from the status response.

## 6. Logging and telemetry

Permitted diagnostics may include:
- feature/screen stage;
- safe PRD error code;
- request outcome class;
- elapsed duration;
- non-sensitive, opaque request correlation ID if approved by parent telemetry rules.

Forbidden:
- student name/email/roll number/subject ID in general app logs;
- JWT/access/refresh token/Authorization header;
- raw status response body;
- VM ID/UUID, hostname, private IP, port, Guacamole ID;
- guest credentials or grant;
- raw backend exception, stack trace containing user data, desktop pixels.

Do not introduce a new telemetry event schema inside FP-02; follow FR-OBS-01 and the parent telemetry allowlist.

## 7. Threat/abuse cases specific to FP-02

| Threat | Required control |
|---|---|
| Stale cache shows ready after the VM stopped | Age-gated status; disable Connect at expiry; refresh-before-Connect |
| Multiple VM rows appear and the screen chooses one | FP-03 exactly-one typed contract; FP-02 displays only an Assigned singleton; ambiguity fails closed |
| User modifies an ID in navigation to display another user's VM | No caller-selected subject/VM selector; current authenticated identity and RLS |
| UI equates TCP open port with RDP readiness | Separate lifecycle/transport display; only RDP_NEGOTIATION_OK can qualify |
| API returns internal host fields | DTO allowlist; reject/ignore unknown fields; static/contract test for no endpoint data |
| Previous student data remains after account switch | Clear state before loading the new identity |
| User tries to start or reassign their Cloud PC | No student power/assignment controls; backend independently denies such operations |
| Server error leaks host/database detail | Exact PRD error mapping; raw exception never shown/logged |

## 8. Dependencies and residual risk

- FP-03's current data/schema/policies need live verification; SUPABASE_DISCOVERY.md is a historical snapshot dated 27 October 2026 and must not be treated as current assignment truth.
- FP-04 status endpoint/host probe needs deployment and contract evidence; SSH availability is not proof.
- Existing current-main dashboard code iterates a list of VM results. FP-03 must replace that with exactly-one semantics before FP-02 can claim compliance.
- The exact safety of `vms.name` as a student-facing label is UNKNOWN.
- The device/server sample-time relationship and clock skew need a documented freshness decision.

## 9. Security acceptance

FP-02 is security-acceptable only when:
- cross-student negative tests pass;
- UI/DTO/logs expose no forbidden infrastructure or identity fields;
- status freshness blocks Connect when stale/unknown;
- Connect has no caller-selected assignment/VM target;
- account switch/sign-out clears prior user's data;
- the broker remains the final authorization authority;
- no student administration or power controls exist.
