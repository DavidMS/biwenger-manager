---
name: write-adr
description: Write an Architecture Decision Record in docs/adr/ with the project's template and numbering rules. Use when a significant technical or product decision is made or changed (stack, module boundaries, data model, algorithm design, security), or when the user asks for an ADR.
argument-hint: <decision title>
---

# Write an ADR

An ADR records **one** significant decision: the context, what was decided and what it costs. It lets a later reader (human or Claude) understand why the code looks the way it does without re-arguing it.

Write one when a decision is hard to reverse or affects more than one module: frameworks and libraries, module boundaries, persistence and schema strategy, algorithm design for the advisor, security and how Biwenger writes are guarded. Don't write one for routine choices that follow existing conventions.

**The human owns decisions.** Claude drafts the ADR and lays out the options; the user decides. Leave the status as `Proposed` until the user confirms, then set `Accepted`.

## Numbering and file name

- Path: `docs/adr/NNNN-kebab-case-title.md`.
- `NNNN` is four digits, one more than the highest existing number: `ls docs/adr/ | sort | tail -1`. The first ADR is `0001`.
- Numbers are never reused, even if an ADR is rejected.
- The title is a short noun phrase for the decision: `0004-store-price-snapshots-daily.md`, not `0004-database.md`.

## Changing a decision

ADRs are not rewritten after they are accepted. To change a decision, write a new ADR and:

- set the old one's status to `Superseded by [NNNN](NNNN-title.md)`
- start the new one's context with `Supersedes [NNNN](NNNN-title.md)`

Fixing typos and broken links is fine.

## Template

```markdown
# NNNN. Title

- **Status:** Proposed | Accepted | Rejected | Superseded by [NNNN](NNNN-title.md)
- **Date:** YYYY-MM-DD
- **Issue:** #N (if any)

## Context

What problem or force makes a decision necessary. Facts and constraints, not the answer.

## Options considered

1. **Option A.** One or two lines, with its main pro and con.
2. **Option B.** ...

## Decision

What we chose, stated directly: "We will ...".

## Consequences

What becomes easier, what becomes harder, and what we must now do or watch.
```

## After writing

- Link the ADR from the PR body, and from `docs/IMPLEMENTATION_PLAN.md` or a module `CLAUDE.md` if it changes something they state.
- If the decision adds a rule Claude must follow every session, add the rule (one line) to the right `CLAUDE.md` and link the ADR.
