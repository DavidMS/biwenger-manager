# #26 — Tutorial chapter 04

- **Issue:** [#26 [3.6] Tutorial chapter 04: letting AI design a schema](https://github.com/DavidMS/biwenger-manager/issues/26)
- **Phase:** 3 — Domain, persistence & history
- **Date:** 2026-10-23

## Prompt

> merged, go on with the next issue

## What Claude did

1. Moved #78 to *Done* and #26 to *In progress*, and branched.
2. Built [`04-letting-ai-design-a-schema.md`](../04-letting-ai-design-a-schema.md) from the Phase 3 journal entries (#21–#25, #78), ADR 0005 and the merged PRs. It measured the phase with `git diff --shortstat` by kind of file.
3. Organised the chapter around five ideas the phase taught, rather than issue by issue:
   - the human answers the schema's product questions, quoted with their costs
   - natural keys make the sync repeatable
   - the database enforces the rules
   - one owner per row
   - prove it on the real stack
4. Put the incident first in *What went wrong*: a test sent 39 real requests to the public CDN. Then the corrections grouped by kind: half rows, two writers, a round blocked forever, startup races, lost failures, tests passing by luck, and proofs that proved nothing.
5. Quoted every prompt verbatim, including the two that changed how Claude works: approving the follow-up issue and the push, and the standing "push it and open the PR when the review is done".
6. Checked every relative link, linked the chapter from the index, and updated the status lines. `CLAUDE.md` had said "the app doesn't call Biwenger yet" since #23, when it started to.

## Corrections

- **A stale status line** in `CLAUDE.md`, wrong for three issues, was found while writing the chapter and fixed here. Status lines are easy to forget when a feature lands. The chapter pass is a good moment to check them.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The incident is named, with its numbers,** in a public tutorial. Its point is that a safe default prevents what a rule in a document doesn't, and that only works if the reader sees what actually happened.
- **No reviewer agent:** docs only. Facts were checked against the journals, `git` and the PR list.

## Lesson

A phase's corrections tell a story that the individual entries don't. Read together, most of Phase 3's bugs were about *time*: two jobs at once, a startup order, a test order, a failure order. That's the pattern to look for in the next phase's reviews.
