# #4 — Project skills

- **Issue:** [#4 [0.3] Skills: implement-issue, backend-feature, frontend-feature, biwenger-api, write-adr, tutorial-chapter](https://github.com/DavidMS/biwenger-manager/issues/4)
- **Phase:** 0 — AI-ready foundation
- **Date:** 2026-10-20

## Prompt

> done, PR is merged. start #4

## What Claude did

1. Pulled `main`, created `feat/4-project-skills`, moved the card to *In progress* and read the issue and plan §3, §4 and §6.
2. Wrote six skills in `.claude/skills/<name>/SKILL.md`. Each has a `description` that says what it covers and when to use it, because that sentence is all Claude sees when it decides whether to load the skill:
   - `implement-issue`: the whole loop from `gh issue view` to an open PR, including the board ids, so a session doesn't have to rediscover them. Takes the issue number as an argument: `/implement-issue 5`.
   - `backend-feature` and `frontend-feature`: the order of work and the test types. Marked *provisional* until Phase 1 (#12) writes the real conventions.
   - `biwenger-api`: auth flow, headers, endpoint table, game rules and a step-by-step DevTools capture guide that includes anonymising samples.
   - `write-adr`: numbering rules, how to supersede an ADR, and the template. Claude drafts ADRs as `Proposed`; the human accepts them.
   - `tutorial-chapter`: tone, the journal entry structure used in #2 and #3, and a chapter outline.
3. Updated the repo map in `CLAUDE.md`.
4. Tested the skills in **fresh sessions**: ran `claude -p` in plan mode (read-only) once to list the skills, then once per skill with a question only that skill could answer. All six were discovered and answered as described: the right layer order, headers, ADR path `0001-use-maven-as-build-tool.md`, journal path `0005-claude-agents.md`, and so on.

## Corrections

- **The fresh-session test caught a bug.** `implement-issue` used `$ARGUMENTS`, which expands to *everything* after the command. `/implement-issue 5 Do not execute anything` turned into `gh issue view 5 Do not execute anything`. Switching to `$0` (the first argument only) fixed it, and a re-test confirmed it.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Skills, not CLAUDE.md, for long know-how.** CLAUDE.md is loaded every session; skills load only when their description matches the task. The endpoint table and the ADR template would be noise while writing React components.
- **Provisional backend and frontend skills.** Writing detailed conventions for code that doesn't exist would invent decisions. Both skills say that `backend/CLAUDE.md` and `frontend/CLAUDE.md` win once they exist.
- **The human captures new endpoints.** DevTools capture needs the user's logged-in browser, and a write endpoint can only be captured by doing a real action. So the `biwenger-api` skill guides the user rather than acting.
- **Reviewer agents are optional for now.** `implement-issue` runs `java-reviewer` and `react-reviewer` "when they exist", because they arrive in #5.

## Lesson

Test a skill the way a new session will meet it: start a fresh `claude -p`, invoke it with a realistic argument and ask a question only the skill can answer. Reading the file yourself doesn't catch problems like argument expansion, which only show up when the skill is actually run.
