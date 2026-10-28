# #28 — Dashboard: balance, team value, position and next round

- **Issue:** [#28 [4.2] Dashboard](https://github.com/DavidMS/biwenger-manager/issues/28)
- **Phase:** 4 — Read-only UI
- **Date:** 2026-10-28

## Prompt

> Continue with task #28

## What Claude did

1. **Asked before writing code where the data comes from** (golden rule 1). The four numbers live in three places:
   - The balance is only in Biwenger's live `/account` call.
   - Team value is in the standings (stored daily and also live).
   - The next round's start is only in the catalog's `activeEvents`, which the client didn't model yet.

   Claude offered three options: live with a short cache, a hybrid, or everything from the database. The database option has a real flaw: a balance up to a day old makes the negative-balance warning wrong. The human chose **live, cached**, and a countdown to **the next round's start**, when lineups lock and a negative balance scores 0.
2. **Client:**
   - An `ActiveEvent` record on `Catalog.activeEvents`.
   - `start` is optional, because the sample's active round, already under way, has none. Documented in `docs/biwenger-api/catalog.md`.
3. **Backend:**
   - `GET /api/dashboard` returns `TeamStatus`: the balance from the account, and points, position, league size and team value from the standings.
   - It also returns the rounds with a known start (`ScheduledRound`).
   - `Dashboard.nextRound(now)` picks the first round that starts after now, ties broken by id.
   - The controller picks the next round with the `Clock` when the request arrives. A cached dashboard therefore never shows a round that has just started.
4. **Frontend:**
   - `features/dashboard`: a `DashboardSummary` container, `TeamCard`, `NextRoundCard` with a countdown that ticks every 10 seconds, and `NegativeBalanceAlert`, a red destructive alert naming the deadline.
   - `formatMoney`, `formatMoneyChange` and `formatTimeLeft` in `lib/format.ts`: the first shared money formatter.
   - Tests cover loading, both error kinds, no next round, the populated view and a negative balance.
   - Claude made the countdown and the red-class assertions fail once on purpose, to see that they can.
5. **Size:** about 1,140 changed lines in 32 files, well over the 400-line guideline. Most of it is tests, and the issue spans backend and frontend. Splitting it would have left an endpoint with no page.

## Review

Both reviewers ran, and Claude applied every finding.

`java-reviewer`: **approve with nits**, but its main finding was the most useful of the issue.

- **Major: the first version downloaded the whole catalog (~230 KB, 551 players) once a minute** only to read two round start times, which Biwenger changes about once a day. No test could see this: every test was green. **Fixed** by splitting the cache:
  - The team status (account and standings, two calls) stays fresh for 1 minute.
  - The schedule (the catalog) is kept until the next round starts, when the catalog names the one after, or 6 hours at most.
- **Position and points came from the account, the league size from the standings.** Those are two HTTP reads that can disagree, and the `position <= managers` invariant would then fail the whole dashboard with a 500. All three now come from one standings response. The account is read only for the balance, which only it has.
- The Javadoc call counts ("four calls" in one place, "three" in another) now agree.

`react-reviewer`: **approve with nits.**

- **When the countdown reached kickoff, the red alert still said "sell players before Jornada 10 starts"**: a deadline already passed, in exactly the case the alert exists for. **Fixed:** the dashboard refetches once when the round starts, and the backend then answers with the following round. A fake-timer test covers it.
- **The default retry doubled the wait on errors.** A 503 (not connected) won't fix itself, and a 502 comes after the backend has already retried Biwenger for up to ~80 seconds. Now `retry: false`, with the reason in a comment.

## Corrections

- **Claude asked about a PR that was already merged.** The git status at session start showed #82 (issue #27) still open. Claude asked whether to stack #28 on it. The human answered: "I've already merged the previous PR, please check again". It had been merged that morning. Claude re-checked with `gh pr view` and branched from `main`.
- **A new gateway test would have broken an old one depending on order.** `readsTheConnectedAccount` counts the account reads of a cold start. But the client keeps the league for the whole shared Spring context, so running the dashboard test first would leave nothing cold. Claude pinned the order with `@TestMethodOrder` and explained why in the class Javadoc. `@DirtiesContext` is not allowed, as #27 learned.
- **Edit scripts missed files the formatter had re-wrapped,** again (see #27). Spotless had put a test helper on one line, so a replace by exact text found nothing.
- **All three subagents were blocked by the `agent-guard.py` hook.** The session was started in `frontend/`, so `$CLAUDE_PROJECT_DIR` pointed there, and the hook's script path didn't exist. As a result:
  - The reviewers couldn't run Bash and reviewed by reading files.
  - The tutorial scribe couldn't write anything, so Claude wrote this entry itself.

  Needs a follow-up issue.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Live with a short cache, not stored:** the warning is only useful if the balance is current.
- **Two caches with different lifetimes:** what changes during the day (balance, standings) is kept for a minute. What changes once a day (the schedule) is kept until it can have changed.
- **The next round is picked at request time,** in the backend, and the frontend asks again at kickoff. The frontend never decides which round is next.

## Lesson

Two lessons, both about what happens before and around the code.

- **The data-source question decided half the design.** Asking it first, with the trade-off spelled out (a stale balance makes the warning wrong), took one round trip and avoided a rewrite.
- **Reviewers see costs that tests don't.** Every test passed while the dashboard downloaded 230 KB a minute from an unofficial API we promised to treat gently. Call volume isn't a correctness property, so only a reader who knows the rules of the API catches it.

A smaller one: the git status taken at session start is a snapshot. Check the live state (`gh pr view`) before asking the human about it.
