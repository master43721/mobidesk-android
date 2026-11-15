# FP-05..11 FreeRDP Pipeline Physical Device & Environment Checklist

**Feature Point:** FP-05 through FP-11 (FreeRDP Native Pipeline & Rendering)  
**Branch:** `fp-05-freerdp-pipeline`  
**Governance:** `docs/MANDATORY_LIVE_DRILL_POLICY.md`  

---

## 1. Hardware Prerequisites

- [ ] Physical Android device running Android 10+ (API 29–37), architecture `arm64-v8a`.
- [ ] USB cable connected to development/test machine with `adb` authorized.
- [ ] Physical screen resolution recorded (e.g. 1080x2400).

## 2. Server & Network Prerequisites

- [ ] Assigned Windows Server VM reachable on private network with NLA enabled on port 3389.
- [ ] Running MobiDesk Session Broker instance providing `/v1/session-grants` and `/v1/rdp`.
- [ ] Valid student account provisioned in staging Supabase Auth with exactly one assigned VM.
- [ ] Known SHA-256 fingerprint of the Windows VM RDP leaf certificate.

## 3. Pre-Drill Execution Check

- [ ] Install candidate APK: `adb install -r app-debug.apk`
- [ ] Clear logcat before test run: `adb logcat -c`
