# MobiDesk Owner Decision Packet — Round 2

**Document:** `docs/dev-loop/OWNER_DECISIONS_ROUND2.md`  
**Date:** 2026-11-08  
**Author:** Antigravity Engineering (Lead Orchestrator)  
**Governance:** `AGENTS.md` & `docs/MANDATORY_LIVE_DRILL_POLICY.md`  
**Rule:** For each architectural question: question, options, recommendation, consequences, and which step is blocked. The agent presents the options and trade-offs; the Project Owner makes the final binding decision.

---

## 1. DEC-REVOKE: Live Relay Assignment Recheck Database Mechanism

- **Question:** How should the session broker re-verify student-to-VM assignment every 15 seconds during an active relay loop without utilizing a forbidden Supabase `service_role` key?
- **Options:**
  - **Option (a): Dedicated Low-Privilege Postgres Role.**  
    Create a specific Postgres database role (e.g. `broker_watchdog`) with `SELECT` permissions restricted strictly to `(student_id, vm_id, status)` on `assignments` and `students` tables. No write access, no access to auth schemas or password hashes.
  - **Option (b): `SECURITY DEFINER` Postgres Stored Function (RPC).**  
    Create a Postgres function `check_assignment_active(p_student_id UUID, p_assignment_tag TEXT) RETURNS BOOLEAN` defined with `SECURITY DEFINER`. Grant `EXECUTE` on this function to the broker's authenticated role. The function executes with elevated internal rights but only returns a boolean flag, preventing direct table queries.
- **Recommendation:** **Option (b) `SECURITY DEFINER` RPC.**
- **Consequences:**
  - *Option (a):* Requires managing separate Postgres credentials and connection pooling. Exposes table schemas directly to SQL queries.
  - *Option (b):* Minimizes attack surface to a single boolean query. If the broker token is compromised, an attacker can only test if a student is assigned, not enumerate database rows.
- **Step Blocked:** Live deployment of real broker database watchdog adapter in `session-broker` (currently implemented against mock/fake adapter on branch `broker/catch-up-v1.1`).

---

## 2. DEC-INGRESS: Session Broker Ingress & TLS Termination

- **Question:** How should incoming HTTPS/WSS traffic from the mobile application be routed and terminated at the session broker in the staging and pilot environment?
- **Options:**
  - **Option (a): Cloudflare Tunnel (Named Tunnel `cloudflared`).**  
    Run `cloudflared` daemon alongside the broker. Inbound traffic reaches Cloudflare's edge, routes through an encrypted outbound tunnel to the local broker port. No open inbound router ports.
  - **Option (b): Tailscale Funnel / VPN.**  
    Expose the broker node via Tailscale Funnel to a public URL or require devices to be on an authenticated Tailnet.
  - **Option (c): Direct Reverse Proxy (Nginx / Caddy with Let's Encrypt).**  
    Standard port-forwarding (TCP/443) on staging gateway to Nginx/Caddy with automated ACME TLS certificates.
- **Recommendation:** **Option (a) Cloudflare Tunnel.**
- **Consequences:**
  - *Option (a):* Zero open inbound ports on the lab router, automated DDoS protection, valid publicly-trusted TLS certificate without dynamic DNS hassles.
  - *Option (b):* Requires client-side Tailscale configuration or VPN profiles, complicating the student experience.
  - *Option (c):* Exposes lab host IP directly to the Internet, requiring firewall rules and dynamic DNS maintenance.
- **Step Blocked:** External broker deployment configuration and staging DNS record setup.

---

## 3. DEC-ABI: Release APK ABI Filtering (`arm64-v8a` only vs Universal)

- **Question:** Should the production/release APK artifact contain only `arm64-v8a` native FreeRDP binaries, or retain `x86_64`?
- **Options:**
  - **Option (a): `arm64-v8a` only for Release builds (Keep `x86_64` for Debug).**  
    Configure `build.gradle` so that `release` builds filter `ndk.abiFilters = ['arm64-v8a']`, while `debug` builds retain `['arm64-v8a', 'x86_64']`.
  - **Option (b): Multi-ABI Universal Release (`arm64-v8a` + `x86_64`).**  
    Package both ABIs in the release APK.
- **Recommendation:** **Option (a) `arm64-v8a` only for Release.**
- **Consequences:**
  - *Option (a):* Cuts the uncompressed native library footprint in half (~14 MB vs ~28 MB), reducing download size and RAM installation footprint on student handsets. Conforms to PRD §6 requirement that physical handsets run `arm64-v8a`. Emulators continue to run via Debug builds.
  - *Option (b):* Increases APK file size by ~14 MB with zero utility on physical phones (virtually 100% of modern Android student devices are 64-bit ARM).
- **Step Blocked:** Release build variant configuration in `app/build.gradle`.

---

## 4. AMD-02: PRD Amendment Approval — Pilot Admin Deferred to Dashboard / Scripts

- **Question:** Should the native Android College Administrator workspace (FPs 23 through 28) be deferred for the R1 pilot in favor of conducting provisioning and audit via the Supabase Web Dashboard and server-side CLI scripts?
- **Options:**
  - **Option (a): Formally Approve AMD-02.**  
    Promote `docs/PRD_CHANGE_PROPOSALS/PRD-1.2-AMD-02-ADMIN-DEFERRAL.md` to accepted project baseline for R1. R1 focus remains exclusively on student workspace, auth, status, and FreeRDP transport.
  - **Option (b): Retain In-App Admin in R1.**  
    Require building native Android administrator screens, batch CSV invitations, and host mapping before pilot launch.
- **Recommendation:** **Option (a) Approve AMD-02.**
- **Consequences:**
  - *Option (a):* Saves an estimated 60–100 engineering hours. Prevents security hazards associated with provisioning keys on Android. Meets the pilot timeline.
  - *Option (b):* Delays the R1 pilot by 4–6 weeks for administrative features not needed by the 4 participating students.
- **Step Blocked:** Scope finalization for R1 milestone release.

---

## 5. AMD-03: PRD Amendment Approval — Foreground-Only Sessions for Pilot

- **Question:** Should session continuity across background, lock screen, and process death (FP-14) be deferred for the R1 pilot, keeping remote sessions strictly foreground-only per PRD v1.2 baseline?
- **Options:**
  - **Option (a): Formally Approve AMD-03.**  
    Promote `docs/PRD_CHANGE_PROPOSALS/PRD-1.2-AMD-03-FOREGROUND-ONLY-SESSIONS.md` to accepted project baseline for R1. Navigating away or backgrounding the app disconnects the active RDP session.
  - **Option (b): Implement Background Continuity in R1.**  
    Implement Android persistent Foreground Service with notification, wake locks, and reconnection state machine.
- **Recommendation:** **Option (a) Approve AMD-03.**
- **Consequences:**
  - *Option (a):* Greatly simplifies FreeRDP native lifecycle, avoids OEM-specific background task killer failures, eliminates battery drain complaints during the pilot.
  - *Option (b):* Introduces complex background socket re-attachment and potential battery drain regressions.
- **Step Blocked:** Native session manager state machine implementation (FP-08/11/14).

---

## 6. APK Distribution: GitHub Actions CI Artifacts vs Private GitHub Release

- **Question:** How should the verified debug and release APKs be distributed to the Project Owner and student testers given that `master43721/mobidesk-android` is a public repository?
- **Options:**
  - **Option (a): GitHub Actions CI Run Artifacts (Current Baseline).**  
    APKs are generated by automated CI runs and attached as workflow run artifacts (stored for 90 days). Anyone with repo access downloads via GitHub Web or `gh run download`.
  - **Option (b): Draft / Private GitHub Releases or Out-of-Band S3/Drive Bucket.**  
    Tag releases or upload signed APKs to a private, authenticated staging bucket.
- **Recommendation:** **Option (a) for development/drills; Option (b) for student pilot distribution.**
- **Consequences:**
  - *Option (a):* Fully automated, zero additional infrastructure, complete traceability from Git commit SHA to artifact SHA-256. However, artifacts expire after 90 days.
  - *Option (b):* Permanent artifact storage, cleaner download URLs for students, but requires release tagging governance.
- **Step Blocked:** Student onboarding and live drill distribution protocol.

---

## 7. Pilot Size: Verified VM Inventory

- **Question:** How many Cloud PC VM endpoints are authorized for the R1 pilot?
- **Confirmed Baseline:** **Exactly 4 verified VMs.**
  - `win-01`
  - `win-02`
  - `win-03`
  - `win-05`
  - *(Note: `win-04` is explicitly excluded from the allowlist due to pending hypervisor verification per PRD v1.2 errata).*
- **All documentation and configuration updated:** Confirmed zero references to "5-student pilot" remain; configuration in `session-broker/mobidesk_broker/config.py` enforces `len(endpoints) == 4`.
