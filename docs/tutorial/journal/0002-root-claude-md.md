# #2 — Root CLAUDE.md

- **Issue:** [#2 [0.1] Root CLAUDE.md — project memory](https://github.com/DavidMS/biwenger-manager/issues/2)
- **Phase:** 0 — AI-ready foundation
- **Date:** 2026-10-20

> This is the first journal entry. Issue #6 defines the journal template, so this entry may be reformatted to match it later.

## Prompt

> ok, I've merged the PR. Start implementing the first issue #2

The prompt is short on purpose. The issue already carries the context and the acceptance criteria, so it doesn't need repeating. Detailed issues make for short prompts.

## What Claude did

1. Pulled `main`, read the issue with `gh issue view 2` and created the branch `feat/2-root-claude-md`.
2. Moved the issue's card to *In progress* on the Project board.
3. Wrote `CLAUDE.md` from the plan (§1, §3, §4 and §7), covering each acceptance criterion:
   - project description with a link to the plan
   - repo map, with not-yet-existing folders marked *(planned)*
   - build, test and run commands, marked as placeholders until Phase 1
   - eight golden rules
   - the issue → branch → PR workflow
   - pointers to the future `backend/CLAUDE.md` and `frontend/CLAUDE.md`
4. Opened a PR that closes #2 and moved the card to *In review*.

## Decisions made along the way

- **Mark what doesn't exist yet.** A CLAUDE.md describing folders and commands that aren't there would make Claude try to run them. Marking them *(planned)* keeps the file honest now and gives Phase 1 a clear list of what to update.
- **Rules, not explanations.** CLAUDE.md is loaded into every session, so it holds the rules and points to the plan for the reasons. Long background belongs in docs or skills, which load only when needed.
- **Determinism as a rule.** "No randomness, no LLM calls in the app, ties broken by player id" is in CLAUDE.md so it applies to every future session, not only the Phase 5 issues.
- **Claude never merges.** The last workflow step keeps the human as the reviewer of every change.

## Corrections

None yet. The human review of the PR may add some. If it does, record them here before merging.

## Lesson

Write CLAUDE.md for the repo as it is today, not as it will be. Keep it short (this one is 62 lines) and update it in the same PR as any change that makes it stale.
