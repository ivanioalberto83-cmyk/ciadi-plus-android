# CIADI+ — AI Agent Instructions

## Mission
CIADI+ is the Android application for the CIADI ecosystem. Preserve existing functionality and visual identity while making changes reliable, testable and secure.

## Source of truth
The repository currently packages the Android source inside `ciadi+ (1).zip`. The GitHub Actions workflow extracts that package before testing and building.

## Rules for every AI agent
1. Inspect the existing implementation before changing it.
2. Do not replace working modules with mock/demo implementations.
3. Preserve existing navigation, authentication, Supabase integration, clinical workflows and video-room architecture unless the task explicitly changes them.
4. A feature is functional only when it can create, save, read, edit/update and be verified against the real backend where applicable.
5. Never place Supabase service_role keys, LiveKit secrets, access tokens, refresh tokens or other secrets in Android source, URLs, logs or committed files.
6. Authorization must be enforced by the backend/RLS/Edge Functions; do not trust role or identity values supplied by the client.
7. The video room remains a web interface opened by the Android app through the controlled WebView flow. Do not move LiveKit secrets or token generation into the APK.
8. Do not put sensitive authentication or LiveKit tokens in query strings.
9. Keep changes small and reviewable. Explain affected files and behavior.
10. Add or update tests for behavior changes.
11. Run the project's tests and build before claiming completion.
12. Never report a task as fixed, complete or passing without fresh verification evidence.

## CI/CD
The authoritative build is GitHub Actions workflow `.github/workflows/ciadi-plus-apk.yml`. Keep it green.

## Definition of done
A change is done only after:
- implementation is present;
- tests pass;
- security checks pass;
- APK build succeeds;
- the resulting artifact is available from GitHub Actions when the workflow is triggered;
- any remaining limitation is explicitly documented.
