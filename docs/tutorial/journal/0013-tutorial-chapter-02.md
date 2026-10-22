# #13 — Tutorial chapter 02

- **Issue:** [#13 [1.6] Tutorial chapter 02: scaffolding a project with AI](https://github.com/DavidMS/biwenger-manager/issues/13)
- **Phase:** 1 — Project skeleton
- **Date:** 2026-10-22

## Prompt

> I accept all the conventions. PR is merged continue with #13

The first sentence was the human review that #12's acceptance criteria asked for, so it was recorded in that issue's journal entry before starting.

## What Claude did

1. Moved #12 to *Done* and #13 to *In progress*, branched, and ran the `implement-issue` skill, which pointed to the `tutorial-chapter` skill for the chapter's structure.
2. Read the inputs: the five Phase 1 journal entries (#8–#12), ADR 0004, chapter 01 for tone, and `gh pr list --state merged` for the PR numbers and sizes. Measured the code size with `git diff --shortstat`, leaving out generated files (the npm lock file and the Maven Wrapper), because 10,000 lines of lock file would make the phase look ten times bigger than it is.
3. Wrote [`02-scaffolding-with-ai.md`](../02-scaffolding-with-ai.md) in the skill's six sections. It doesn't retell each issue; it pulls out what generalises:
   - the one-line prompts that `implement-issue` made possible, all quoted verbatim
   - asking live tools for versions
   - making the wrong environment fail loudly
   - rules enforced by the build
   - nested `CLAUDE.md` files
   - reviewing the reviewers
4. Linked it from the tutorial index, and checked that every relative link in the chapter points to an existing file.
5. Recorded the human's acceptance of the conventions in the #12 entry, and the confirmed package name in the #8 entry.
6. Marked Phase 1 as done in `CLAUDE.md` and the README.

## Corrections

None yet.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Group the corrections by what they teach, not by issue.** Two corrections had the same shape: the zsh no-op in #10 and the format hook in #12, both scripts that hid their errors and so seemed to succeed. The chapter puts them side by side, under the takeaway "silence is not evidence".
- **No reviewer agent for this PR.** The change is docs only, outside `backend/` and `frontend/`. Claude checked the facts against the journal entries, the PR list and `git`, and checked the links with a script.

## Lesson

A chapter is a second pass over the journal, and the second pass finds patterns a single entry can't show. Five separate issues each had a "verified by running" step. Put together, they show that cheap one-command checks, not clever ones, caught most of the phase's bugs.
