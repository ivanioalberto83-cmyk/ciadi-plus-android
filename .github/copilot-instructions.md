# CIADI+ Repository Instructions

## Project
CIADI+ is an Android application connected to the CIADI clinical/family ecosystem. The repository currently stores the Android source package as `ciadi+ (1).zip`; CI uses GitHub Actions to extract it, test it and build the debug APK.

## Engineering principles
- Preserve the existing CIADI visual identity and working behavior.
- Prefer incremental fixes over rewrites.
- Use real Supabase persistence and authorization; do not create fake success states.
- Treat create → save → read → edit/update → verify as the minimum functional cycle for backend-backed features.
- Keep authentication and authorization secure. Client-provided role/identity values are not trusted.
- Never commit service_role keys, LiveKit secrets, passwords, access/refresh tokens or other credentials.
- Never place sensitive tokens in URLs or logs.
- Keep video-room token issuance server-side and authorized. The Android app opens the web room; it does not contain LiveKit server secrets.
- Follow existing architecture and dependency choices unless a migration is explicitly requested.

## Validation
Before declaring a change complete:
1. Run relevant unit tests.
2. Run security/static checks.
3. Build the debug APK.
4. Verify the APK artifact.
5. Report exactly what was verified and any limitation.

## Working style
First inspect the affected code and data flow. Then make the smallest safe change. Do not silently remove functionality to make a test pass.
