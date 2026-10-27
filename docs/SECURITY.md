# MobiDesk security boundaries

## Current build

The first app shell has no live Supabase or VM control API configuration. Login callbacks must not simulate successful authentication. Preview mode is explicitly labeled and cannot start a VM or open an RDP session. Do not add historical VM names, IPs, student emails, or assignments as if they were live records.

## Required before live access

1. Verify the Supabase project, public client key, profiles/roles/assignments schema, and restrictive row-level security policies.
2. Verify a server-side control API that validates the Supabase access token and derives identity/role server-side.
3. Require server-side assignment checks on every status, power, and connection-ticket request. Ignore client-provided privilege claims.
4. Issue narrowly scoped, short-lived desktop connection details only after authorization. Do not embed service-role keys, permanent Windows passwords, or long-lived RDP credentials in the APK.
5. Verify the external Android network route, TLS certificate validation, endpoint identity, and expiry/revocation behavior. Do not expose public TCP/3389 or assume an HTTPS tunnel supports arbitrary native TCP.
6. Restrict implicit/exported entry points into the FreeRDP session activity. The MobiDesk app should invoke the native session only from a verified in-app authorization result.
7. Avoid logging tokens, passwords, clipboard payloads, or user/VM secrets. Clear temporary connection material after use and on disconnect/logout.

Until each item is evidenced, live access stays unavailable and the app must not be described as production-ready.
