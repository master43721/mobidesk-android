# MobiDesk Server Implementation Plan

> **Historical plan.** The 2026-11-03 owner-review draft [`MobiDesk_PRD_v1.2.pdf`](MobiDesk_PRD_v1.2.pdf) proposes the current R1 contract and excludes student VM power controls. Owner approval is pending; do not treat this older plan's power/admin/API suggestions as approved scope. Tailscale SSH has now been verified and a read-only host inventory recorded in [`../BASELINE_AUDIT.md`](../BASELINE_AUDIT.md). No server configuration has been changed.

## Recommended way to connect the server

Use Tailscale for private network access, then use ordinary SSH to the Ubuntu server's Tailscale IP or tailnet name. Do not publish SSH or Windows RDP ports to the internet just so this task can reach the machine. Tailscale authorization should happen in your own browser, and no password, private key, service-role key, or one-time login secret should be pasted into chat.

The current execution environment is a temporary task sandbox. If you later authorize it to join your tailnet, access will last only while that sandbox and its Tailscale state remain available; it is not a permanent management node. We can decide the exact attachment path and confirm the target hostname when you are ready. A desktop GUI is not required for most work. It can remain available for an occasional console or browser approval, but SSH is easier to audit and less fragile than controlling the GUI for server administration.

At the time this historical plan was written, no server had been accessed. Read-only Tailscale SSH discovery was later completed on 2026-11-03; see `../BASELINE_AUDIT.md`. No firewall, libvirt, VM power, DNS, or Cloudflare configuration was changed. Any future changes still require a reviewed plan and appropriate authorization.

## Verified product constraints

The Android client is being built as a native Kotlin/Jetpack Compose app with the FreeRDP Android engine. The product brief explicitly chooses native RDP and rejects Guacamole as the desktop interaction layer. The existing Supabase database does contain Guacamole-named legacy columns, but the Android client does not read them and they should not be repurposed or deleted without a separate decision.

Supabase already has roster-based student provisioning, student-scoped row-level security, and an assignment relationship. The current project has no administrator role, VM status/power API, Supabase Edge Functions, or connection-ticket service. No database DDL or data rows have been changed. The mobile recovery redirect is the only Supabase project configuration change recorded so far. See [`SUPABASE_DISCOVERY.md`](SUPABASE_DISCOVERY.md).

The server is described as Ubuntu 24.04 LTS with a GUI. The project brief also refers to KVM/libvirt and Cloudflare Tunnel, but their presence, versions, topology, and live configuration remain unverified until the server is connected.

## Target architecture

Keep the control plane separate from the desktop data path:

```text
Android app --HTTPS--> MobiDesk control API --restricted libvirt access--> assigned Windows VM
      |                         |
      +-- Supabase Auth/JWT ----+-- assignment and role checks
      |
      +-- FreeRDP --secure, separately validated RDP transport--> assigned Windows VM
```

The control API may use Cloudflare Tunnel for HTTPS access if the existing domain and Cloudflare account support it. That does not by itself make a native RDP stream reachable from a phone. The RDP transport is a separate architecture decision and must be tested from an external phone network before access is enabled. Public TCP/3389 is not an acceptable shortcut.

A provisional control service could be a small Python/FastAPI service managed by systemd, because Ubuntu's libvirt integration is accessible from Python. The final runtime choice should follow read-only server inventory. The service must run as a dedicated non-root account, bind to loopback behind the tunnel, and receive only narrowly scoped permission to operate approved libvirt domains.

## Work phases on the server

### 1. Read-only discovery

Record OS/kernel, CPU/RAM/disk, network interfaces and default route, public and private addresses, Tailscale state if already installed, UFW/nftables rules, listening ports, DNS/tunnel setup, systemd services, and backup locations. Check whether KVM, libvirt, and the expected Windows test VM exist; record only the resource identifiers needed for planning. Confirm how many VMs are production/student machines and identify one owner-approved staging VM. Do not collect Windows passwords or application secrets during inventory.

**Acceptance:** a redacted inventory document identifies existing services, exposed ports, resource constraints, VM ownership, current backups, and the safest maintenance window. No service or VM configuration changes occur.

### 2. Identity and authorization contract

Keep Supabase Auth as the user identity provider. The API must validate the Supabase access token signature and issuer, use the verified `sub` user ID, and check the student's assignment server-side for every status, power, or connection request. It must not trust a client-supplied email, role, or VM ID as authorization. The Supabase service-role key, if required for backend data access, belongs only in a root-owned server secret file or secret manager and never in the APK, GitHub, logs, or command output.

Before adding an admin dashboard or admin endpoints, choose and document an authoritative administrator provisioning method. The current schema has no admin role; UI hiding alone is not security. Consider a private role table or trusted Auth app-metadata claims, then add a reviewed migration, least-privilege grants, and server-side checks. Also decide whether public Supabase signup should remain enabled: the Android app does not expose signup and the current trigger only provisions roster-matching students, but project-level signup remains enabled.

**Acceptance:** tests prove an unauthenticated request gets `401`, an authenticated but unassigned student gets `403`, a student can act only on their own assigned VM, and only an explicitly provisioned admin can use admin actions.

### 3. Control API and safe VM operations

Define versioned HTTPS endpoints for the signed-in user's workspace, VM status, approved start/stop operations, a short-lived connection ticket, and audit outcomes. Keep status derived from libvirt or a trusted monitor rather than inventing an online/offline state in the mobile client. Map public app VM identifiers to trusted libvirt domain UUIDs on the server; do not expose hostnames, hypervisor socket paths, or credentials to the phone.

Power operations must use an allowlisted domain mapping and fixed actions. Never build shell commands from request text or let clients choose arbitrary `virsh` arguments. Add idempotency, rate limits, timeouts, and audit records containing actor, VM, action, result, and timestamp but no passwords or bearer tokens. Prefer direct libvirt APIs; if a command wrapper is unavoidable, pass only validated fixed identifiers and arguments.

**Acceptance:** API integration tests cover start/stop state transitions, duplicate requests, libvirt outage, timeouts, denied assignments, and audit completeness without changing a production VM.

### 4. RDP transport and session tickets

Keep FreeRDP as the Android desktop engine. Choose the phone-to-VM path only after the server topology and Windows VM configuration are known. Evaluate supported options such as a properly configured Windows RD Gateway over HTTPS, a tightly controlled private-network client path, or a purpose-built authenticated RDP relay. Compare licensing, Android client compatibility, credential handling, latency, reconnect support, and operational burden. Do not assume Cloudflare Tunnel's HTTPS route carries the app's native RDP stream.

The control API should issue a one-time or short-lived, audience-bound ticket only after checking the current Supabase identity and VM assignment. The ticket must expire quickly, be replay-resistant, and contain no permanent Windows password. Validate server identity/certificates, use Network Level Authentication where supported, and define how Windows credentials are provisioned without logging or permanently storing them on the phone.

**Acceptance:** from a staging phone on an external network, an assigned student can establish a FreeRDP session; an unassigned account cannot. Expired, replayed, malformed, and wrong-VM tickets fail closed. Public RDP ports remain closed.

### 5. Deployment and operations

Deploy the API as a systemd service under a dedicated user, with a restrictive filesystem layout and environment file readable only by that service. Keep the service on loopback if Cloudflare Tunnel is the HTTPS ingress. Store tunnel credentials and server-only Supabase credentials outside the repository with owner-only permissions. Keep administrator SSH reachable only through the approved private management path after checking existing dependencies; do not rewrite the host firewall blindly.

Add health checks, structured redacted logs, log rotation, resource limits, automatic restart policy, versioned migrations, and a rollback procedure. Back up the API configuration, libvirt domain definitions, Supabase schema/migrations, and VM data using the server owner's existing backup policy. Test restoration before relying on a backup. Avoid snapshots or backups that inadvertently retain sensitive login material.

**Acceptance:** a reboot test confirms services recover, health checks work, secrets are not world-readable, the tunnel reaches only the intended API routes, logs contain no credentials, and rollback instructions are exercised on staging.

### 6. End-to-end validation and release gate

Use one staging Windows VM and one authorized test student account. Test login/session refresh, profile and assignment reads, status, start/stop, ticket issuance, RDP graphics/input/keyboard/orientation, clipboard behavior if enabled, disconnect/reconnect, expired sessions, network loss, and cleanup. Test a second student account against the first student's VM and verify denial. Test admin paths only after the admin role exists. Repeat connection tests over Wi-Fi and a mobile carrier network.

Do not declare the product end-to-end until the Android APK installs on a real emulator or device, the actual assigned Windows VM opens through FreeRDP, authorization-denial tests pass, and a signed release can be produced with an owner-controlled key.

## What to have ready when you connect the server

You do not need to send secrets in chat. Be ready to authorize the server's Tailscale node from your own browser and identify the exact Ubuntu host. If you can, designate one non-production Windows VM for testing and confirm that you are authorized to operate it. We can inspect the existing KVM/libvirt and Cloudflare configuration directly after access is established. If a browser login, MFA prompt, or Cloudflare authorization is needed, you can complete that step in your own browser; do not paste passwords or private keys into chat.
