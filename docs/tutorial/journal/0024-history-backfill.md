# #24 — Backfill the season's price history and match points

- **Issue:** [#24 [3.4] Backfill price and points history for the current season](https://github.com/DavidMS/biwenger-manager/issues/24)
- **Phase:** 3 — Domain, persistence & history
- **Date:** 2026-10-23

## Prompt

> merged, continue with #24

## What Claude did

1. Moved #23 to *Done* and #24 to *In progress*, and branched.
2. **Worked out the cost first:** two public CDN calls per player for ~551 players is about 1,100 requests, roughly 10 minutes at the client's rate limit. "On demand" could mean three things, so Claude asked. The human chose:

   > Setting in .env (Recommended)

   With `BIWENGER_BACKFILL_ENABLED=true`, a restart runs the backfill in the background.
3. Wrote it in layers:
   - **Domain:** `DailyPrice`, and `MatchResult` with a sealed `Played(points, minutes)` / `Missed(reason)`.
   - **Gateway:** a plan (season, scoring system, every catalog player) and each player's history.
   - **A `V2` migration** for `backfill_progress`.
   - **`HistoryWriter`** writes one player's history and his progress row in **one transaction**, so an interrupted run resumes after the last finished player. It uses batched `INSERT … ON CONFLICT`, as ADR 0005 asked, and never overwrites a daily-sync day.
   - **`BackfillService`** skips players already done, stops after 3 failed players in a row, and logs progress.
4. Tested it end to end, with WireMock answering every player's CDN calls with the samples and a real PostgreSQL:
   - exact row counts, and a second run that makes no CDN calls
   - resuming with only the player that failed
   - a daily-sync day left untouched
   - stopping after 3 failures
   - points summing to the catalog's 35
   - **the rate limit measured on the wire**

   Each guard was broken on purpose once, and its test failed.

## Review

`java-reviewer`: **approve with nits,** with one finding worth fixing before merge:

- **major: the backfill also wrote today's price.** If the daily sync was writing at the same moment, its insert could hit the backfill's row, and the sync's whole day would roll back, losing a market and a table that can't be fetched later. **Fixed:** today belongs to the daily sync, and the backfill stops at yesterday. Two tests fail if it writes today again. Added to ADR 0005.
- **minor: no missed match ever reached the database in the tests,** so the null points and the reason checks were only tested by reading the code. **Fixed:** one player now gets the missed-matches sample, and his rows are checked.
- **minor: a failure that isn't Biwenger's** (a bug, a database error) was logged without its stack trace, and would repeat on every restart. **Fixed.**
- **minor: shutting the app down** didn't stop the loop. The next players would fail with "data source closed", giving three misleading warnings. **Fixed** with a stop flag set before Spring closes the database.
- **nit, for the human:** match points stop at the backfill date, because nothing refreshes them later. Raised in the PR.

## Corrections

- **The first rate-limit test checked each gap between requests, and failed at 22 ms instead of 25.** The limiter was right. The limiter spaces the *starts* of requests in the client, but WireMock records when each one *arrives*, and a request a few milliseconds slower on the wire shortens the next gap. The test now checks the total span: 80 gaps can't fit in less than 80 × 25 ms. With the limiter off, that test fails.
- **An edit script missed a line that the formatter had re-wrapped,** and stopped before writing anything. Claude reran it with the formatted line.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **A setting, not an endpoint.** The human's choice. Because a finished backfill does nothing, the setting can simply stay on.
- **Skip a failed player, stop after three in a row.** One bad player shouldn't stop the season's import, but a Biwenger that's down shouldn't get 1,100 requests either.
- **Plain SQL for bulk writes, JPA for the rest:** about 200,000 rows, where `save()` would `SELECT` each one first.

## Lesson

Two writers that each look correct can still collide. The backfill and the daily sync were each right on their own; together, on one row (today's price), they could undo a whole day. Ask "who else writes this row?" for every table, and give each row one owner.
