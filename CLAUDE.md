# CIADI+ — Claude Code Instructions

Read `@AGENTS.md` first. These rules are shared with other AI agents.

## Additional Claude rules
- Work from the existing repository state; do not assume an earlier audit is still valid.
- Before editing, identify the files and data flow involved.
- Prefer minimal, reversible changes.
- After edits, run tests and build through the repository's documented workflow where possible.
- If verification cannot be performed, state exactly what could not be verified.
- Do not invent credentials, database records, API responses, test results or deployment status.
