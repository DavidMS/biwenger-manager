# 0001. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-10-20
- **Issue:** #6

## Context

This project is built mostly by Claude Code, one issue per session. Each session starts with no memory of earlier conversations, so a decision made in chat ("use Maven", "no LLM in the app") is lost unless it is written into the repo. The project is also a tutorial, and readers need to see why things were decided, not only what the code does.

`CLAUDE.md` holds the rules every session must follow, but it is loaded into every session and must stay short. It can't hold the reasoning behind each decision.

## Options considered

1. **Keep decisions in the implementation plan.** One place, but the plan describes future work and becomes stale. It has no history of changed decisions.
2. **Rely on PR descriptions and commit messages.** The history is there, but it's scattered and hard for a new session to find.
3. **Architecture Decision Records** (Michael Nygard's format): one short Markdown file per decision in `docs/adr/`, numbered, never rewritten after acceptance.

## Decision

We will record significant decisions as ADRs in `docs/adr/NNNN-title.md`, following the `write-adr` skill (`.claude/skills/write-adr/SKILL.md`): four-digit numbers, statuses Proposed / Accepted / Rejected / Superseded, and a new ADR to change an accepted one.

Claude drafts ADRs with status `Proposed`. The human accepts them.

## Consequences

- A new session can find the reason for a decision by reading `docs/adr/`, without the original conversation.
- Rules that every session must follow still go into `CLAUDE.md`, as one line linking the ADR.
- Writing an ADR costs a few minutes per significant decision. Routine choices that follow existing conventions don't need one.
- ADRs 0002 and 0003 record the decisions taken while reviewing the implementation plan (plan §2 and §7).
