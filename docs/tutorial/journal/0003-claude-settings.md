# #3 — Shared Claude Code settings: permissions and hooks

- **Issue:** [#3 [0.2] Shared Claude Code settings: permissions and hooks](https://github.com/DavidMS/biwenger-manager/issues/3)
- **Phase:** 0 — AI-ready foundation
- **Date:** 2026-10-20

## Prompt

> done, PR is merged. start #3

## What Claude did

1. Pulled `main`, created `feat/3-claude-settings`, moved the card to *In progress* and read the issue.
2. Loaded the `update-config` skill (the issue's notes asked for it). It gives the exact settings format and a method for building hooks: pipe-test → write the JSON → validate → prove it fires.
3. Wrote `.claude/settings.json`:
   - **allow:** Maven wrapper, npm scripts and tests, Vitest, Docker Compose, the `gh` commands the workflow uses, and the everyday `git` commands.
   - **deny:** reading `.env`, `.env.local`, `.env.*.local` and anything under `secrets/`.
   - **hooks:** a PreToolUse guard on Bash and a PostToolUse formatter on Edit/Write.
4. Put the hook logic in two scripts in `.claude/hooks/` instead of inline one-liners, so they can be read, reviewed and tested:
   - `guard-bash.sh` blocks force-pushes to `main` and commands that read `.env`.
   - `format-file.sh` runs Spotless on `.java` files and Prettier on frontend files. It does nothing until Phase 1 installs those tools.
5. Tested the guard with 18 sample commands piped in as hook input (9 that must be blocked, 9 that must be allowed).
6. Proved both hooks fire in the live session (details in the PR).

## Corrections

- **Claude's own test caught a bug.** The first version of the guard blocked `cp .env.example .env`, which creates `.env` from the template and is harmless. It also had the opposite problem: mentioning `.env.example` anywhere in a command let a real `.env` read through (`cat .env .env.example`). Both were fixed, and both cases are now in the test list.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **`git push` is not in the allowlist.** Pushing sends work outside the machine, so Claude still asks each time. Easy to add later if the prompts get annoying.
- **The guard only blocks *force* pushes to `main`**, as the issue asks. A plain push to `main` is still possible (CLAUDE.md forbids it, but nothing enforces it). GitHub branch protection on `main` would enforce it properly.
- **Python instead of `jq` in the hooks.** `jq` isn't installed on every machine, while `python3` ships with macOS developer tools and most Linux systems.
- **Two layers for `.env`.** Deny rules stop the Read tool. The Bash guard stops `cat .env` and similar commands, which deny rules on Read don't cover.

## Lesson

Test hooks the way you test code: pipe sample hook input into them, list the expected result for each case, then prove they fire in a real session. A hook that silently does nothing is worse than no hook, because you believe you're protected.
