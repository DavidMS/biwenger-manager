# #5 — Project subagents

- **Issue:** [#5 [0.4] Subagents: java-reviewer, react-reviewer, test-writer, biwenger-api-explorer, tutorial-scribe](https://github.com/DavidMS/biwenger-manager/issues/5)
- **Phase:** 0 — AI-ready foundation
- **Date:** 2026-10-20

## Prompt

> done, go with #5

Short because the issue and the `implement-issue` skill (from #4) already hold the context: acceptance criteria, branch naming and board steps.

## What Claude did

1. Read the issue and the existing skills, then created `feat/5-project-agents`.
2. Wrote five subagents in `.claude/agents/`. A subagent is a Markdown file with frontmatter (name, description, tools, skills, hooks) and a system prompt. Each one gets only the tools it needs and preloads the skill it relies on:
   - `java-reviewer` (Read, Grep, Glob, Bash; skill `backend-feature`): reviews layering, Spring idioms, determinism and tests, and runs Maven. It never edits.
   - `react-reviewer` (same tools; skill `frontend-feature`): hooks rules, accessibility, state, component size and types. Runs npm test, lint and build.
   - `test-writer` (adds Edit, Write; skills `backend-feature` and `frontend-feature`): writes tests only. If production code is wrong, it marks the test as disabled with a reason and reports the bug instead of fixing it.
   - `biwenger-api-explorer` (adds WebFetch, Write, Edit; skill `biwenger-api`): GET-only research that documents endpoints under `docs/biwenger-api/`.
   - `tutorial-scribe` (Write, Edit, Bash; skill `tutorial-chapter`; `model: sonnet`): writes journal entries under `docs/tutorial/`.
3. Wrote `.claude/hooks/agent-guard.py` (see Corrections) and wired it into each agent's `PreToolUse` hook with a profile name.
4. Pipe-tested the guard by feeding it JSON on stdin, then ran each agent once as a smoke test against fixtures in `smoke/` (not part of the deliverable).

## Smoke tests

Each agent ran once in a fresh headless session (`claude -p "Use the <agent> subagent to ..."`). The fixtures in `smoke/` were small files with planted problems, deleted before committing. Each run also included one deliberate rule-breaking action to prove the guard fires live.

| Agent | Task | Result | Guard check |
|---|---|---|---|
| `java-reviewer` | Review a Spring service with planted problems | Found all of them: token printed in a log, bid placed without propose → confirm, `HashSet` order breaking determinism, integer division, `null` return, field injection | `git branch smoke-guard-check` blocked |
| `react-reviewer` | Review a component with planted problems | Found the hook after an early return, the bid on a `<div>` click with no confirmation, the effect with no dependency array, `any`, the missing label and missing keys | `npm run format` blocked |
| `test-writer` | Write Vitest tests for a money formatter | Wrote the tests and found the planted bug (`1000` → `1000 €`) plus a real one nobody planted (`999999` → `1000K €`). Both are `it.skip` with a reason, and the production file was untouched | Edit of `money.ts` blocked |
| `biwenger-api-explorer` | Compare the `biwenger-api` skill with pybiwenger | Every endpoint pybiwenger calls is already in the skill. Leads for spike #19: pybiwenger defines `POST /api/v2/market` and `/api/v2/market?player=` (list and withdraw a player) without using them | `curl -s -X POST https://example.com` blocked |
| `tutorial-scribe` | Write this journal entry | Wrote it from the issue, the files and the context it was given, kept the prompt verbatim and left this section for the main session. Didn't create the tutorial index, which doesn't exist until #6 | Edit of `CLAUDE.md` and `printf >> CLAUDE.md` blocked |

## Corrections

1. **The `tools` field is too coarse.** It can only allow or deny whole tools. "Bash, but only Maven" or "Edit, but only test files" cannot be expressed. Claude wrote `agent-guard.py`, a `PreToolUse` hook that reads the tool call as JSON on stdin and exits with code 2 to block it. Each agent's frontmatter passes its own profile: a list of allowed command patterns and a list of editable path patterns. Redirections and command substitution are rejected, and pipelines are checked segment by segment.
2. **Pipe-testing found a hole.** After 31 cases, `curl -X POST` was allowed for `biwenger-api-explorer`: the regex consumed the space before `-X`, so the check never matched. Claude fixed the pattern and added 11 curl cases (methods, `-d`, `--data`, `-F`, uploads, `--json`).
3. **`git branch` was removed** from the read-only list. It looks harmless but can create and delete branches.
4. **`eslint --fix` is blocked** for the reviewers. A reviewer must not change files, and `--fix` rewrites them.
5. **One smoke run never started.** In `claude -p --allowedTools "Bash(curl *)" "<prompt>"`, `--allowedTools` accepts several values, so it swallowed the prompt as a second tool name. Passing the prompt on stdin fixed it.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Enforce limits in a hook, not in the prompt.** "You never edit files" in a system prompt is a request. A hook that returns exit code 2 is a rule.
- **Allow-list, not deny-list.** The guard permits known commands and blocks everything else, so a command nobody thought of is blocked by default.
- **One script with profiles** instead of five scripts: one place to fix bugs and one test suite.
- **Reviewers cannot edit.** They report findings and the main session decides what to fix, which keeps review and implementation separate.
- **`test-writer` never fixes production code.** A failing test that exposes a real bug is reported, not hidden.
- **The scribe runs on a cheaper model** (`model: sonnet`), since writing from facts needs little reasoning.

## Lesson

Tool lists give you coarse permissions; real least privilege needs a hook. And a hook is code, so test it like code: pipe adversarial inputs into it. The `curl -X POST` bypass would have gone unnoticed if only the happy path had been tried, and it sat in the one agent that talks to an external API.
