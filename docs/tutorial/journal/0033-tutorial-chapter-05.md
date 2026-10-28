# #33 — Tutorial chapter 05

- **Issue:** [#33 [4.7] Tutorial chapter 05: building UI with AI](https://github.com/DavidMS/biwenger-manager/issues/33)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> continue with #33

## What Claude did

1. **Checked that #90 (#32) was merged** before branching, as #28 and #86 taught.
2. **Asked where the screenshots should come from** (golden rule 1). The issue asks for screenshots, and the real app shows real people: rivals' names, the balance, the league. Claude offered three options: demo data captured by Claude, screenshots the human takes and blurs, or Claude capturing the real app for the human to blur. The human chose **demo data**.
3. **Built a throwaway demo server** in its scratch folder, kept out of the repo. It serves the anonymised samples from `docs/biwenger-api/samples/` in the shape of the app's `/api` endpoints. The real frontend ran against it with `npm run dev`, and headless Chrome took the screenshots, as in #27.
4. **Looked at every screenshot before using it**, and found three problems:
   - **Every match showed "02:00".** Claude checked the backend: `PlayerDetailResponse` sends the kickoff `Instant`. The demo server was cutting it to a date, so the bug was Claude's, not the app's. Fixed in the demo server.
   - **The market table is wider than its card at 1280px,** and the "Closes" column starts at the card's edge. The chapter shows it, and the human asked for a follow-up issue: [#92](https://github.com/DavidMS/biwenger-manager/issues/92).
   - **Player names are links but look like bold text.** Follow-up: [#93](https://github.com/DavidMS/biwenger-manager/issues/93).
5. **The 390px screenshot was cut off on the right.** Headless Chrome won't make a window narrower than about 500px, so the page was laid out at 500px and then cropped. A 500px screenshot showed everything fits. The chapter warns about this.
6. **Wrote [`05-building-ui-with-ai.md`](../05-building-ui-with-ai.md)** around the UI iteration loop (ask, build and test, check the build, review, look, run) and what each step caught. Claude quoted the human's answers from the session transcripts. The answer for #28 wasn't in them, so the chapter gives the choice from the journal and says it isn't the exact wording.
7. **Measured the phase** with `gh pr view` and `git diff --shortstat` by kind of file. Checked every relative link, linked the chapter from the index, and updated the status line in `CLAUDE.md`.

## Corrections

- **A crop went wrong.** `sips --cropOffset` cut from the wrong edge, and the dashboard image lost its header. Claude retook each screenshot at the exact window height instead.
- **A cleanup was blocked.** Claude's `rm -f $D/*.png` was refused by Claude Code's safety check, because an empty `$D` would point at `/`. The retake didn't need it, because Chrome overwrites the files.
- **Claude first said the market's "Closes" column was cut off.** While writing the follow-up issue, it read the table's code: shadcn's wrapper scrolls the table sideways, and Claude's own `--hide-scrollbars` flag had hidden the scrollbar. The issue is real, because macOS and phones also hide scrollbars until you scroll. But it is "no hint that the table scrolls", not "the column is lost". The chapter, this entry and the PR now say so.
- **First drafts of the numbers were off:** 8 endpoints instead of 7, and 9 questions instead of 8. Both were fixed by counting the mappings in the code and the answers in the transcripts.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Screenshots from demo data,** so the published tutorial shows no real person.
- **The demo server stays out of the repo** for now. It could go into `scripts/` if screenshots are needed again.
- **The bugs the screenshots found are shown, not hidden.** The market table's hidden column is in the chapter as an example of what only looking catches.
- **No reviewer agent:** this is docs and images only. Facts were checked against the journals, `git`, the PRs and the code.

## Lesson

**Looking at the UI is a step of its own.** Every test and review in the phase passed, and two layout problems were still visible in the first screenshots. But a screenshot can also mislead: one bug was in the demo data, and one was a limit of headless Chrome. Before blaming the app, check the layer below the page.
