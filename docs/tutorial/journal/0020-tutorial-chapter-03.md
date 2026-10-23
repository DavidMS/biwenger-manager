# #20 — Tutorial chapter 03

- **Issue:** [#20 [2.7] Tutorial chapter 03: reverse-engineering an API with AI](https://github.com/DavidMS/biwenger-manager/issues/20)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-23

## Prompt

> merged, continue with #20

## What Claude did

1. Moved #19 to *Done* and #20 to *In progress*, branched, and used the `tutorial-chapter` skill for the structure.
2. Built the chapter from the six Phase 2 journal entries (#14–#19), and measured the phase with `git diff --shortstat` by kind of file. The recorded samples (about 6,900 lines of JSON) are counted apart from code (1,500), tests (1,500), scripts (1,000) and docs (1,100), so the phase isn't mistaken for 12,000 lines of code.
3. Wrote [`03-reverse-engineering-an-api.md`](../03-reverse-engineering-an-api.md) around the four topics the issue names:
   - pybiwenger as a reference, with a table of where it or the plan was wrong
   - the explorer agent
   - recorded samples
   - WireMock

   It adds two that the phase taught: "the human captures, the AI reads the anonymised output", and "strict about data, gentle with the server".
4. Quoted every prompt and the human's answers to Claude's questions verbatim.
5. Checked its own text before committing:
   - "months ahead of the first write" was the same unsupported claim already removed from the #18 journal
   - a results bullet was about Phase 1
   - "11 endpoint pages" was really 10 plus the write page

   Every relative link was checked with a script.
6. Linked the chapter from the index and marked Phase 2 as done in `CLAUDE.md` and the README.

## Corrections

None yet.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Count what Claude sent to Biwenger.** The results give the number of requests Claude made to Biwenger (2 public `GET`s, after asking) and the number of writes sent by code (0). For a phase about an unofficial API on a real account, those are the numbers that show the rules held.
- **No reviewer agent:** docs only. Facts were checked against the journals, the samples and `git`.

## Lesson

Re-reading your own journals finds the claims that crept in. Two unsupported or misplaced statements were fixed before the chapter was committed, one of them a repeat of a claim already corrected once. A chapter is the second pass that a journal never gets.
