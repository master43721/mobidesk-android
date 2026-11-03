# MobiDesk — Open Items and Owner Decisions

**Snapshot:** 3 November 2026 · **Accountable owner:** Project Owner.

| ID | Open item | Why it matters | Required closure |
|---|---|---|---|
| OI-01 | Physical VM host/location | Defines broker network path, availability and operations | Owner confirms host/site and Lab IT access model |
| OI-02 | Persistent vs ephemeral student VMs | Defines reset, semester rollover, data retention and recovery | Approve lifecycle and deletion/reset policy |
| OI-03 | Windows guest account lifecycle | NLA requires a distinct Windows identity | Lab IT approves creation, reset, lockout, expiry and offboarding |
| OI-04 | Android reference device | Performance gates need reproducible physical hardware | Record model, OS/API, ABI and app build |
| OI-05 | Distribution method | Drives package signing and target API/compliance work | Choose Play Store, institutional MDM or direct distribution |
| OI-06 | Release key custody | Protects package integrity and recovery | Document owner, storage, backup and rotation |
| OI-07 | Broker hostname and route | App needs stable secure control and relay endpoints | Approve DNS/TLS and private egress path |
| OI-08 | Telemetry retention | Stage timings are sensitive metadata | Approve fields, access roles and retention |
| OI-09 | FreeRDP WSS transport | WSS does not imply FreeRDP byte-stream compatibility | Demonstrate exact pinned API, framing and reconnect behaviour |
| OI-10 | Dock R2 architecture | Double-transcoding may waste power/latency | Compare phone transcoding with Pi-native FreeRDP |

No item is closed by inference. Record approvals in DECISIONS.md with date, consequence and owner.
