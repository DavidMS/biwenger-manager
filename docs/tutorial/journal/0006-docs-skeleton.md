# #6 — Documentation skeleton

- **Issue:** [#6 [0.5] Documentation skeleton: tutorial, journal and ADRs](https://github.com/DavidMS/biwenger-manager/issues/6)
- **Phase:** 0 — AI-ready foundation
- **Date:** 2026-10-20

## Prompt

> merged, go with #6

## What Claude did

1. Created `docs/6-docs-skeleton` (a `docs/` branch, since the issue is documentation only), moved the card to *In progress* and read the issue. Its note said chapter 01 should describe what #2–#5 really built, and they were all merged.
2. **Pulled the real prompts from the session transcript** for chapter 00, not from memory. Two of them only make sense next to the question they answered ("1", "drop it"), so Claude also extracted its own replies from that stretch to give each prompt its context.
3. Wrote the tutorial:
   - `docs/tutorial/README.md`: how to read the tutorial, the chapter table (00–08, with planned ones marked) and the journal table.
   - `00-planning-with-ai.md`: the brief, the research on pybiwenger, the review that dropped the AI chat, and how the plan became 51 issues.
   - `01-setting-up-claude-code.md`: what `CLAUDE.md`, settings, hooks, skills and agents are, when each loads, which ones are enforced, and a table of checks a reader can repeat.
4. Added `docs/tutorial/journal/TEMPLATE.md` and pointed the `tutorial-chapter` skill at it, so the structure lives in one place.
5. Wrote ADRs 0001 (record decisions), 0002 (tech stack) and 0003 (monorepo layout) with the `write-adr` template, and updated the repo map in `CLAUDE.md`.
6. Ran the `guard-bash.sh` example quoted in chapter 01 to check it prints the block message and exits with `2`.

## Corrections

None yet.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **ADRs 0002 and 0003 are `Accepted`, not `Proposed`.** The `write-adr` skill says Claude drafts as `Proposed`, but these record decisions the human already made in the plan review (plan §7). If the reviewer disagrees, they go back to `Proposed`.
- **One source for the journal structure.** The template was embedded in the `tutorial-chapter` skill. It now lives in `TEMPLATE.md`, and the skill lists the sections and links to it, so the two can't drift apart.
- **Planned chapters are listed in the index** with their phase, so readers see where the tutorial is going.
- **Chapter 01 leads with "where is each rule enforced?"** That was the main lesson of #3–#5, and it is what a reader can reuse in their own project.

## Lesson

When documenting work done with AI, take the prompts from the transcript, not from memory. Claude Code keeps every session as a JSONL file under `~/.claude/projects/`. A remembered prompt is quietly cleaned up; the real one, with its typos and its one-word answers, is what shows how the collaboration actually worked.
