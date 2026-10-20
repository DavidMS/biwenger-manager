---
name: tutorial-chapter
description: Structure and tone for the "building software with AI" tutorial in docs/tutorial/ - journal entries (one per issue) and chapters (one per phase), always quoting the real prompts. Use when writing or updating a journal entry or tutorial chapter.
---

# Write a journal entry or tutorial chapter

This repo is also a tutorial on building software with AI. The reader is a developer who wants to see **how** the work was done with Claude Code, including the mistakes, not a polished success story.

## Tone

- Plain English, short sentences, active voice. Explain jargon the first time.
- Honest. Show what Claude got wrong and what the human corrected. Corrections are the most useful part.
- Concrete. Name files, commands and issue numbers. Link them.
- Quote prompts **exactly** as the user typed them, typos included, in a blockquote. Never invent or clean up a prompt.
- No hype and no filler ("seamlessly", "powerful", "simply").

## Journal entry (one per issue)

Path: `docs/tutorial/journal/NNNN-slug.md`, where `NNNN` is the issue number padded to four digits and `slug` matches the branch. Aim for 30–60 lines.

```markdown
# #N — Title

- **Issue:** [#N title](https://github.com/DavidMS/biwenger-manager/issues/N)
- **Phase:** P — phase name
- **Date:** YYYY-MM-DD

## Prompt

> the user's exact prompt(s)

One or two lines on why the prompt worked (or didn't), if there is something to learn.

## What Claude did

Numbered steps: what was read, which skills or agents were used, what was built, how it was verified.

## Corrections

What went wrong and how it was fixed: bugs Claude's own checks caught, and changes the human asked for in review. Write "None yet" if there were none. Human review may add more before merging.

## Decisions made along the way

Bold one-line decision, then the reason. Link an ADR when there is one.

## Lesson

One short paragraph: the reusable takeaway for someone building with AI.
```

## Tutorial chapter (one per phase)

Path: `docs/tutorial/NN-slug.md` (two digits, `00` is planning). Add it to the index in `docs/tutorial/README.md`.

A chapter tells the story of a phase and teaches what generalises from it. Build it from that phase's journal entries, PRs and ADRs (`git log`, `gh pr list --state merged`); don't repeat them in full, link them.

1. **Goal.** What the phase set out to do and why it matters for the project.
2. **Concepts.** The Claude Code features or techniques the phase relied on, explained for a newcomer (for example: CLAUDE.md, skills, agents, hooks, plan mode, WireMock samples).
3. **How it went.** The main steps, with the real prompts that drove them and links to the journal entries.
4. **What went wrong.** The most instructive corrections, and what changed afterwards (a new rule in CLAUDE.md, a new hook, a better skill).
5. **Results.** What exists now, with numbers where useful: issues closed, PRs, tests, lines changed.
6. **Takeaways.** Three to five bullets the reader can apply to their own project.

## Before finishing

- Prompts are verbatim, links work and file paths exist.
- Nothing secret is quoted: no `.env` contents, tokens, emails or Biwenger ids.
