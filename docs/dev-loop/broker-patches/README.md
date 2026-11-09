# MobiDesk Session Broker Catch-Up v1.1 Patch Record

**Document:** `docs/dev-loop/broker-patches/README.md`  
**Patch File:** `docs/dev-loop/broker-patches/0001-broker-catch-up-v1.1.patch`  
**Target Repository:** `master43721/session-broker`  
**Base Commit in `session-broker`:** `57a9e42f008bf528f28d738a121ac19b765844ee` (`origin/hardening/production-baseline`)  
**Patch Commit SHA:** `40eb181539d83f2ecd77b5711f0ee2a298fbbb41`  
**Patch SHA-256 Checksum:** `909f8025a9725f3f0d09f9dd4a9819670ed7c614f6e5524a5454f5847c484fe8`  

---

## 1. Application Instructions (`git am`)

To review and apply this patch to a clean clone or working tree of `master43721/session-broker`:

```bash
cd /path/to/session-broker
# Verify base commit
git checkout -b broker/catch-up-v1.1 57a9e42f008bf528f28d738a121ac19b765844ee

# Verify patch checksum before applying
sha256sum /path/to/mobidesk-android/docs/dev-loop/broker-patches/0001-broker-catch-up-v1.1.patch
# Expected: 909f8025a9725f3f0d09f9dd4a9819670ed7c614f6e5524a5454f5847c484fe8

# Apply patch
git am < /path/to/mobidesk-android/docs/dev-loop/broker-patches/0001-broker-catch-up-v1.1.patch

# Verify test suite
python3 -m unittest discover tests -v
```

---

## 2. Independent Contract v1.1 Review & Audit Matrix

Review of patch `40eb181` against Contract v1.1 and Owner Requirements:

| # | Item / Contract Requirement | Implementation File & Line Citations | Status / Verification |
|:---|:---|:---|:---:|
| 1 | **`vm-status` Schema & X.224 Probe Bytes** | `mobidesk_broker/rdp_probe.py:12-16` (X.224 CR TPDU packet: `0x03, 0x00, 0x00, 0x13, 0x0e, 0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x08, 0x00, 0x03, 0x00, 0x00, 0x00`)<br>`mobidesk_broker/app.py:224-233` (Returns `vmId`, `lifecycle`, `rdpTransport`, `assignedStudentId`, `freshAt`, `staleAfterSeconds: 15`. No VM IP/port/host/creds). | IMPLEMENTED & VERIFIED |
| 2 | **VM Removal & Client Body Rejection** | `mobidesk_broker/app.py:262-273` (Rejects any non-empty body with 400 `BODY_PROHIBITED`, reason `GRANT-01`). Destination is strictly resolved server-side from Supabase assignment table; client cannot supply target VM or host. | IMPLEMENTED & VERIFIED |
| 3 | **Revocation Loop with No Service-Role Key** | `mobidesk_broker/rechecker.py:37-47` (`BlockedSupabaseRechecker` explicitly enforces that real Supabase adapter is BLOCKED per DEC-REVOKE; service-role key is prohibited by `AGENTS.md` §1.3).<br>`mobidesk_broker/app.py:485-515` (Active relay loop runs 15s watchdog rechecking `rechecker.check_active_assignment`). | BLOCKED on DEC-REVOKE (Fake adapter tested; real adapter blocked pending Owner choice) |
| 4 | **Rate Limits Checked Before Upstream Calls** | `mobidesk_broker/app.py:157-162` (`unauth_ip:{client_ip}` 60/min before token parse)<br>`mobidesk_broker/app.py:178-183` (`user_status:{user_id}` 30/min before VM/prober)<br>`mobidesk_broker/app.py:257-261` (`unauth_ip` before grant token parse)<br>`mobidesk_broker/app.py:294-299` (`user_grants:{user_id}` 5/min before grant issue). | IMPLEMENTED & VERIFIED |
| 5 | **4-Endpoint Allowlist Configuration** | `mobidesk_broker/config.py:105-114` (Validates 1..5 endpoints, normalized UUID keys, RFC1918 IPv4 or ULA/scoped IPv6 only, hostname rejection). Supports 4-endpoint pilot VM config. | IMPLEMENTED & VERIFIED |
| 6 | **`/ready` Endpoint** | `mobidesk_broker/app.py:118, 133-148` (`/ready` checks config validity and allowlist count >= 1; returns 200 `status: ready` or 503 `status: not_ready`; leaks zero topology). | IMPLEMENTED & VERIFIED |
| 7 | **Structured JSON Audit Logging** | `mobidesk_broker/audit.py:11-49` (`AuditLogger.log_event` emits structured JSON; never logs JWTs, passwords, raw grants, student names, VM UUIDs, or IPs; strictly allowlists safe extra metadata). | IMPLEMENTED & VERIFIED |
| 8 | **PRD Error Catalogue Mapping** | `mobidesk_broker/errors.py:7-88, 90-126` (Maps all 16 PRD error codes `AUTH-01`..`SESSION-03` with exact user-facing messages, `retryable` booleans, HTTP status codes, and `no-store` headers). | IMPLEMENTED & VERIFIED |
| 9 | **RDP Certificate Fingerprint Field** | `mobidesk_broker/app.py:338-341` (`rdpCertificate: { type: "SHA256", fingerprintSha256: ... }` in `/v1/grants` response per Contract v1.1). | IMPLEMENTED & VERIFIED |
