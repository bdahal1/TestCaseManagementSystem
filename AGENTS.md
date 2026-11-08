# Project instructions for coding agents

TCMS is a Java 26 / Spring Boot API with a React / TypeScript frontend for manual, BDD, and keyword test management. Read the files needed for the task, not the entire repository.

## Start here

- For read-only questions with pasted code or attached files, answer from that content. Do not run onboarding steps, read status guides, or search for more files unless the answer actually requires them. The setup and handoff steps below apply to implementation work.
- Read `docs/ai/WORKING_STATE.md` before editing; it records paused work, unverified changes, and the latest handoff.
- Use `docs/ai/PROJECT_GUIDE.md` for the product and source map, `docs/ai/TROUBLESHOOTING.md` for setup and failures, and `docs/ai/COLLABORATION.md` for coordinating agents.
- Check `git status --short` and relevant diffs. Preserve changes made by the user, Antigravity, Continue, and other agents. Never reset, discard, or claim their changes as your own.
- Current source/configuration overrides stale descriptive documentation. Distinguish code present, tests passed, UI verified, and proposed features.

## Implementation rules

- Preserve project authorization (`ProjectAccess`), result permissions (`ExecutionPolicy`), immutable run snapshots, worker leases, and secret masking.
- Cucumber Manual uses Given/When/Then as its expected outcome; do not add a separate expected-result requirement to its runner. Conventional Manual retains expected results.
- Automation changes must consider both Cucumber and Keyword execution, authoring, validation, imports, serialization, and downloaded projects.
- Use Java 26 and the Node version in `.nvmrc`; maintain the npm lockfile. Do not fix dependency conflicts with `--force` or `--legacy-peer-deps`.
- Do not read or copy `.env`, tokens, passwords, private session data, SQL dumps, or execution evidence into documentation or prompts. Refer to variable names only.
- Keep patches scoped. Avoid unrelated refactors, dependency upgrades, database changes, or stopping running services. No automatic commits, pushes, deployments, or messages to others.
- Run checks appropriate to changed behavior. Report actual commands and results; do not label an untested dirty workspace as passing. Documentation-only changes need link/content checks, not application test suites.

## Handoff

Record completed scope, files changed, checks, limitations, and next action in `docs/ai/WORKING_STATE.md` when handing work to another agent. These files provide shared context, not automatic communication or authorization to start a new task.
