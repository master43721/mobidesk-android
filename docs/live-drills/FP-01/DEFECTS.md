# FP-01 Defects Log

| Defect ID | Severity | Status | Description | Resolution / Mitigation |
|---|---|---|---|---|
| DEF-001 | Low | RESOLVED | Manifest merge warning on non-existent `SessionRequestHandlerActivity` | Removed redundant declaration from `app/src/main/AndroidManifest.xml` |
| DEF-002 | Low | RESOLVED | Deprecation warning on `EncryptedSharedPreferences` / `MasterKey` | Added `@Suppress("DEPRECATION")` and multi-tier fallback to ensure crash-proof operation |
| DEF-003 | Medium | EXTERNAL GATE | Supabase staging QA student account credentials required for physical live drill | Documented in `DEVICE_OR_ENVIRONMENT_CHECKLIST.md` awaiting Owner credentials |
