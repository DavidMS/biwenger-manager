# #7 — GitHub scaffolding

- **Issue:** [#7 [0.6] GitHub scaffolding: templates, labels, README, .gitignore](https://github.com/DavidMS/biwenger-manager/issues/7)
- **Phase:** 0 — AI-ready foundation
- **Date:** 2026-10-20

## Prompt

> merged, go with #7

## What Claude did

1. Created `chore/7-github-scaffolding`, moved the card to *In progress* and read the issue, the current `.gitignore` and `README.md`.
2. Wrote three issue forms in `.github/ISSUE_TEMPLATE/`:
   - `feature.yml`
   - `spike.yml`, which adds questions to answer and a time box
   - `bug.yml`, which adds steps to reproduce and expected vs actual

   Each has *Context*, *Acceptance criteria*, *Out of scope* and *Notes for Claude*, applies its `type:*` label and adds the issue to the project board. `config.yml` turns off blank issues and links the plan.
3. Wrote `.github/pull_request_template.md`. Its checklist covers tests, no real API calls in tests, no secrets, writes through propose → confirm, docs, journal entry, reviewer agents and screenshots.
4. Rewrote `README.md`:
   - what the project is
   - a "start here" table linking the plan, tutorial, ADRs and `CLAUDE.md`
   - how issues, labels, the board and branches are organised
   - a disclaimer about the unofficial API
5. Extended `.gitignore` and checked it with `git check-ignore` against 15 sample paths.
6. Verified the GitHub setup with `gh` and the GraphQL API: the 17 project labels from the plan (18 with `type:bug`, below), 8 milestones, the board with five columns and 51 items.

## Corrections

The verification step found four real problems. Three were in what already existed, and one was in Claude's own first fix:

- **`.env` was not gitignored.** `CLAUDE.md`, the hooks and the plan all said credentials live in a gitignored `.env`, but nothing ignored it. It would have been committed the first time someone ran `git add .` in Phase 1. Now `.env` and `.env.*` are ignored, except `.env.example`.
- **The original `.gitignore` ignored `*.jar`,** which would also have ignored the Maven wrapper jar coming in #8. The first exception (`!.mvn/wrapper/maven-wrapper.jar`) only matched at the repo root, because a pattern containing a slash is anchored to the `.gitignore`'s folder. `git check-ignore` caught it, and `!**/.mvn/...` fixed it.
- **Merged issues stayed in *In review*.** The board doesn't move closed issues to *Done* by itself. Claude moved #2–#6 by hand. The project's built-in "Item closed" workflow would do it automatically, but it can only be switched on in the GitHub UI.
- **There was no label for bugs.** Claude created `type:bug` so the bug form has one.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Issue forms (YAML), not Markdown templates.** Forms make *Context* and *Acceptance criteria* required, so an issue can't be opened without the parts Claude depends on.
- **Blank issues are disabled.** Every issue goes through a template. Issues created with `gh issue create --body` (as Claude does) aren't affected.
- **Phase labels and milestones are set after creating an issue.** A form can't pick a label from a dropdown, so the form says to add them.
- **The project board stays private for now.** The repo is public and the README links the board, so readers can't see it until the owner makes it public. That's the owner's call.

## Lesson

"Verify and document" tasks pay off. Checking what already exists, rather than only adding new files, found that the most important rule in the project (never commit credentials) had no enforcement behind it. Run `git check-ignore` on the paths that matter, rather than trusting that a pattern does what it looks like.
