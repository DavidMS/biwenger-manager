# #78 — Keep match points up to date after each round

- **Issue:** [#78 [3.7] Keep match points up to date after each round](https://github.com/DavidMS/biwenger-manager/issues/78)
- **Phase:** 3 — Domain, persistence & history (a follow-up to #24, raised in its review)
- **Date:** 2026-10-23

## Prompt

> go on with #78

Claude first checked that #79 (#25), whose one-sync-at-a-time guard this issue builds on, was merged.

## What Claude did

1. Moved #25 to *Done* and #78 to *In progress*, and branched. The issue says to settle the scope with the human first, so Claude asked two questions. The human chose:

   > All catalog players (Recommended)

   > Inside the daily sync (Recommended)

2. Built it:
   - **A `V4` table, `points_refresh`,** remembers which finished rounds are refreshed. When the 07:30 sync's catalog shows a finished round not in it yet, `PointsRefresher` fetches every player's reports (one CDN call each, which returns the whole season) and records the rounds.
   - **It runs inside the sync,** so it can't overlap one. It skips the day while the backfill runs.
   - **It never throws:** the day's snapshot is already stored.
   - **The match-report writer moved** out of the backfill into a shared `player.service.MatchReports`.
3. Tested it against WireMock and PostgreSQL:
   - every player refreshed when rounds have finished
   - the same rounds not refreshed twice
   - a newly finished round triggering a refresh
   - waiting while the backfill runs
   - a failed player keeping the rounds pending
   - an outage that stops the refresh but not the sync
   - no price rows written

   Breaking the "pending" rule on purpose made two of these tests fail.

## Corrections

- **A test called the real Biwenger API.** `DailySyncTest`, written in #23, pointed only `biwenger.base-url` at WireMock. The CDN address kept its default, the real `cf.biwenger.com`. Once the sync started refreshing points, one run of that test sent **39 real GET requests to the public CDN**, one per player in the sample, at full speed because the test turns the rate limit off. That broke golden rule 3 ("never call the real Biwenger API from tests").
  - **What it touched:** the requests were read-only fetches of public player reports. No login, no token, no account data. The answers went into the throwaway test database.
  - **How it showed up:** an unrelated repository test failed on the unexpected rows. The sync test's log ("Points refresh done: … 39 players done") confirmed it.
  - **What Claude did:** stopped running the build, told the human, and fixed it in two layers. `DailySyncTest` now points the CDN at WireMock too. And a **safety net** in `app/src/test/resources/config/application.properties` points both Biwenger URLs at `127.0.0.1:9`, where nothing listens, so any test that forgets fails instead of reaching the internet. `NoRealBiwengerInTestsTest` checks the net is there, and `backend/CLAUDE.md` now tells the story so the rule sticks.
  - **The root cause** is a test written for #23's behaviour, which then gained a new outgoing call in #78. A test that is safe today can become unsafe when the code under it starts calling something new. Only a default that fails closed protects against that.
- **The test suite ran out of database connections.** About 13 cached Spring contexts, each with a pool of 10, against one PostgreSQL that accepts 100. The logs said `Connection is not available … (total=0)`. Test pools are now capped at 3.

## Review

`java-reviewer`: **changes requested.** It confirmed that no test can reach Biwenger any more.

- **major: one player broken for good would block every round forever.** Rounds were marked only when *every* player succeeded. A single slug that answers 404, or a report that can't be read, would make every daily sync re-fetch about 550 players for nothing. **Fixed:** `BiwengerApiException` now says whether a failure is transient (5xx, 429, timeout, network) or not (4xx, a format mismatch). Only transient ones keep rounds pending; the others are logged. A test with a 404 player shows the rounds marked and nothing fetched the next day.
- **minor: two `backend/CLAUDE.md` rules broken.** Another feature used the player tables directly, and `@Transactional` sat on a repository. **Fixed:** the writer is now a service, `player.service.MatchReports`.
- **minor: "never at the same time as the backfill" is only best effort.** The check runs before the refresh, and the backfill could still start during it. It's harmless, because both write the same reports idempotently. **Recorded** in ADR 0005, with the sync taking about 5 minutes longer when a refresh happens.
- **minor: Claude's new paragraph had broken the tests table** in `backend/CLAUDE.md`. **Moved** below the table.
- **nit:** a test that no finished round means no CDN calls. **Added.**

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **One CDN call per player covers every pending round,** because each call returns the whole season. So the first refresh after deploy, when the table is empty, costs the same as any other.
- **Transient vs permanent failures** decide whether a round stays pending, not whether every player succeeded.

## Lesson

Make the safe outcome the default. The test had been correct when it was written; it became dangerous when the code under it started calling a second host. A rule in a document ("point every URL at WireMock") didn't stop that. A default that fails closed (every URL goes nowhere unless a test points it somewhere) does, for every future test too.
