# #86 — Shared "not connected to Biwenger" error state

- **Issue:** [#86 Shared 'not connected to Biwenger' error state](https://github.com/DavidMS/biwenger-manager/issues/86)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> yes, open the follow-up issue

and then

> continue with #86

## Where it came from

`react-reviewer` found during #29 that the squad page was the third feature to repeat the same lines:

- check `error instanceof ApiError && error.status === 503`;
- give `ErrorState` the title "Not connected to Biwenger" and a hint.

Claude suggested a follow-up issue in the PR instead of widening #29 (golden rule 7). The human asked for it, and then asked for it to be done next, before the market, standings and player pages (#30–#32) copy the pattern a fourth time.

## What Claude did

1. **Read the three call sites before designing the component.** One of them was different: Settings says to fix `.env`, because it *is* the Settings page, while the dashboard and the squad point to Settings. So `BiwengerErrorState` takes an optional `notConnectedHint`, and its default is the Settings hint.
2. **`components/state/BiwengerErrorState.tsx`**, with its own tests:
   - a 503 shows the title and the default hint;
   - a 503 with a custom hint shows that hint;
   - any other API error shows the backend's problem and no hint;
   - a non-API error shows the generic title.
3. **Replaced the three copies.** The features' own 503 tests were left as they were, as proof that nothing visible changed. Claude broke the shared status check on purpose: all five not-connected tests failed, the component's two and one in each feature. Then it put the check back.
4. **`frontend/CLAUDE.md`** now says a view whose data comes from Biwenger uses `BiwengerErrorState`, so the next pages start from it.

## Review

`react-reviewer`: **approve with nits.** It compared each call site with `main` and found no change in behaviour. For example, `title={undefined}` renders the same as no title, because `ErrorState` uses `??`. Claude applied two nits:

- The folder tree in `frontend/CLAUDE.md` read as if *every* view used `BiwengerErrorState`. It now says it is for Biwenger data.
- The component's tests now query inside `getByRole('alert')` instead of the whole page, and also check that the default hint is absent for a 502.

## Corrections

- **Claude said PR #85 (#29) was "waiting for your review" when it had already been merged.** The human answered: "I don't see any pull request open". Claude had opened the PR and reported it without checking again. The human merged it a few minutes later, and Claude answered from memory. It then checked `gh pr view 85`: merged, and #29 closed and on *Done*. The same slip happened in #28. The lesson there was to check the live state before *asking* about a PR; it applies just as much before *reporting* one.

## Lesson

A review finding outside the issue's scope is worth an issue of its own, not a quiet extra commit. Doing it right after, before the next three pages, kept the refactor to three call sites and about 100 lines.
