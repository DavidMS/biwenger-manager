# #93 — Player names look like links

- **Issue:** [#93 [4.x] Player names don't look like links until hovered](https://github.com/DavidMS/biwenger-manager/issues/93)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-11-06

## Prompt

> continue with #92 and #93

Two issues in one prompt. The repo's rule is one issue, one branch, one PR, so Claude did #93 first because it was the smaller one.

## What Claude did

1. **Read the issue and `PlayerLink`,** and checked that PR #91 was merged with `gh pr view`, not from the git status at the start of the session, as chapter 05 asks.
2. **Changed `PlayerLink` only.** The underline is always there, faint at `decoration-muted-foreground/40`, and gets stronger on hover. Keyboard focus shows the same ring as shadcn's `Button`. Squad, market and the rival's squad on the standings page all use the component, so all three changed at once.
3. **Wrote down the rule in `frontend/CLAUDE.md`,** so the advisor tables in Phase 5 look the same.
4. **Rebuilt the demo server.** Chapter 05's was a throwaway and was gone, so Claude wrote a new one in its scratch folder, with invented players and long names.
5. **Checked the focus ring in a real browser.** A plain screenshot can't press Tab. A short script drove headless Chrome through its debugging port, pressed Tab until a player name had focus, and took the [screenshot](../images/93/squad.png).
6. **Ran `react-reviewer`,** lint, the format check, tests and the build.

## Corrections

- **The format check failed on the first run.** Prettier sorts Tailwind classes and Claude had written them by hand. `frontend/CLAUDE.md` says not to.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **No new test.** The change is three CSS classes, and jsdom does no layout or painting, so a test could only repeat the class names. The convention is not to query by class. The existing tests still cover the links, and the screenshot covers the look.

## Lesson

A throwaway tool you will need again isn't throwaway. Chapter 05 kept the demo server out of the repo and described it instead, so this issue paid for it a second time. Before deleting a helper, ask whether the next issue's acceptance criteria will ask for it.
