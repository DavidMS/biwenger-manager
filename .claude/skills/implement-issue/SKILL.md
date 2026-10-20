---
name: implement-issue
description: Implement one GitHub issue of this repo end to end, from reading the issue to an open PR with a journal entry. Use when the user says "start #N", "implement issue N", "work on #N" or runs /implement-issue N.
argument-hint: <issue-number>
---

# Implement issue #$0

One issue = one branch = one PR. The issue's acceptance criteria define done. Follow the steps in order and don't skip any. If no issue number was given above, ask for it.

## 1. Read and plan

1. `gh issue view $0`. Read the context, acceptance criteria, out of scope and *Notes for Claude*.
2. If the issue says "Blocked by #M", check that #M is closed. If it isn't, stop and tell the user.
3. Read whatever the issue touches: the relevant section of `docs/IMPLEMENTATION_PLAN.md`, `backend/CLAUDE.md` or `frontend/CLAUDE.md`, existing code and ADRs.
4. Make a short plan: files to create or change, tests to write, and how each acceptance criterion will be shown to be met.
5. If a product or architecture choice is ambiguous, ask the user before writing code (golden rule 1). If the plan is large or risky, show it first.

## 2. Branch and board

```bash
git switch main && git pull --ff-only
git switch -c feat/$0-<slug>     # fix/, docs/ or chore/ when more fitting
```

Move the card to *In progress*:

```bash
item=$(gh project item-list 8 --owner DavidMS --limit 200 --format json \
  --jq '.items[] | select(.content.number==$0) | .id')
gh project item-edit --project-id PVT_kwHOAIoWBc4Bn4xr --id "$item" \
  --field-id PVTSSF_lAHOAIoWBc4Bn4xrzhmbVVA --single-select-option-id <option>
```

| Column | Option id |
|---|---|
| Backlog | `93251bfe` |
| Ready | `ba3824f0` |
| In progress | `2ea16c08` |
| In review | `101df90a` |
| Done | `f7969c99` |

## 3. Implement and test

- Use the matching skill: `backend-feature`, `frontend-feature`, `biwenger-api`, `write-adr`.
- Write tests with the code, first where practical. A feature without tests is not done.
- Run the checks before committing: `cd backend && ./mvnw verify`, `cd frontend && npm test && npm run build`. Report failures honestly, never hide them.
- Stay inside the issue's scope. If you find other work, suggest a new issue instead of doing it.
- Update docs in the same PR when the change makes them stale (`CLAUDE.md`, module `CLAUDE.md`, API reference).
- Commit with Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `test:`, `refactor:`). Several small commits are fine.

## 4. Review

Run the reviewer agents on the diff (`git diff main...HEAD`) when they exist in `.claude/agents/`:

- `java-reviewer` for changes under `backend/`
- `react-reviewer` for changes under `frontend/`

Fix what they find or explain why not. Mention the review in the PR.

## 5. Journal entry

Write `docs/tutorial/journal/NNNN-<slug>.md`, where `NNNN` is the issue number padded to four digits. Use the `tutorial-chapter` skill for the structure (or the `tutorial-scribe` agent if it exists). Quote the user's real prompt.

## 6. Pull request

Pushing sends work off the machine, so the user approves it. Then:

```bash
git push -u origin HEAD
gh pr create --title "<conventional commit style title>" --body-file <file>
```

The PR body has:

- `Closes #$0`
- a short summary of the change
- the acceptance criteria as a checklist, each with how it was met or verified
- anything the user must decide or do (settings, secrets, follow-up issues)

Move the card to *In review* and give the user the PR link.

## Never

- Merge a PR, push to `main` or force-push.
- Call the real Biwenger API from tests, or run a Biwenger write action without explicit confirmation.
- Read or print `.env`.
