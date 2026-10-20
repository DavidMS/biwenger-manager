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

Copy [`docs/tutorial/journal/TEMPLATE.md`](../../../docs/tutorial/journal/TEMPLATE.md) to `docs/tutorial/journal/NNNN-slug.md`, where `NNNN` is the issue number padded to four digits and `slug` matches the branch. Aim for 30–60 lines.

The sections, in order:

1. **Header**: issue link, phase, date.
2. **Prompt**: the user's exact prompt(s) in a blockquote, plus a line on why it worked if there's something to learn.
3. **What Claude did**: numbered steps covering what was read, the skills or agents used, what was built and how it was verified.
4. **Corrections**: bugs Claude's own checks caught and changes asked for in review. Write "None yet" if there were none.
5. **Decisions made along the way**: a bold one-line decision, then the reason. Link an ADR when there is one.
6. **Lesson**: one short paragraph with the reusable takeaway.

Add the entry to the journal table in `docs/tutorial/README.md`.

## Tutorial chapter (one per phase)

Path: `docs/tutorial/NN-slug.md` (two digits, `00` is planning). Update its row in the chapter table in `docs/tutorial/README.md`.

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
