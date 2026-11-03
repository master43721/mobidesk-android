# MobiDesk baseline audit

**Audit date:** 2026-11-03. This is a read-only snapshot; it contains no VM endpoint addresses, student identifiers, passwords, tokens, tunnel URLs or signing material.

## Source baseline

- Repository: private `master43721/mobidesk-android`, branch `main`, commit `4986006` at inspection; working tree was clean before the PRD intake changes.
- FreeRDP submodule exists locally and points to v3.32.0 commit `d27a4f7c1c63b62a5e60e5d939ad116cfb58ffc1`.
- Existing source provides Kotlin/Compose UI and Supabase Auth/workspace reads. No MobiDesk RDP Activity/session coordinator or live broker client exists.
- Existing historical build report (2026-10-27) records successful debug build and JVM tests; no emulator, physical-device, real sign-in or RDP session evidence.
- No GitHub Actions workflow existed before the current continuation.

## Server snapshot (Tailscale SSH, read-only)

- Target host identity: `mobilab-server`, Ubuntu 22.04.5 LTS, x86_64; 24 logical CPUs, 15 GiB memory, about 6 GiB available at inspection.
- Libvirt showed five Windows guests running; `linux-01` and `win_10-lab` were shut off. Their state was observed, not changed.
- No MobiDesk broker service was active and its prototype listener port was not listening.
- `xrdp.service` was active and host TCP/3389 bound to a wildcard address. Internet reachability was not tested. UFW status could not be read because passwordless sudo was unavailable. Preserve xrdp until the owner confirms the GUI access path and firewall controls.
- A Guacamole-related listener and a `cloudflared` process were observed, but a stable MobiDesk HTTPS/WSS route was not verified.
- No server, guest, tunnel, DNS, firewall, Supabase, or VM settings were changed.

## Identity and VM assignment caveat

The dated Supabase audit from 2026-10-30 reported five student assignments over five Windows VM catalog rows. The owner-provided PRD v1.2 reports four approved endpoints and explicitly excludes unverified VM4. The data/endpoint mismatch is unresolved. Re-query current assignments and revalidate the approved allowlist before any real grant or RDP test; do not reassign users or guess endpoints.

## Broker prototype boundary

An out-of-repository local Python prototype was audited separately. Its local suite had 23 passing unit tests, but it has no deployed service, no stable TLS/WSS ingress, no `/v1/vm-status`, no active-session validation, and no periodic active-relay revocation. It is research code only until it is brought into the repository, reviewed and aligned with the approved API/security contract.

See `docs/PRD_TRACEABILITY.md`, `OPEN_ITEMS.md`, and `TEST_PLAN.md` for interpretation and next gates.