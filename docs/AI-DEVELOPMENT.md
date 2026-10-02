# CIADI+ AI Development Workflow

## Shared AI workflow

Use the same repository as the source of truth for ChatGPT, Claude Code, Cursor and GitHub Copilot.

**Requirement → inspect → plan → implement → test → security audit → build → real-device verification → review**

### ChatGPT
Use for architecture, debugging strategy, Supabase data-flow review, documentation, test planning and repository changes.

### Claude Code
Use for deep codebase inspection, refactoring, debugging and multi-file implementation. Claude should read `AGENTS.md` and `CLAUDE.md`.

### Cursor
Open the repository/workspace and keep the project rules visible to the AI agent. Use the repository's `AGENTS.md` as the shared engineering contract.

### GitHub Copilot
Repository-wide guidance is in `.github/copilot-instructions.md`. GitHub supports repository instructions and `AGENTS.md` for agent workflows.

## Collaboration rule
Never let two agents independently rewrite the same feature at the same time. One agent implements; another reviews/tests the resulting commit.

## CI
GitHub Actions is the final shared build gate. A local build is useful but does not replace CI verification.

## Security baseline
- Supabase publishable/anon client key may be used where appropriate; privileged keys must remain server-side.
- Backend authorization must derive identity from the authenticated session.
- LiveKit server token generation stays server-side.
- Sensitive credentials must never be committed.
