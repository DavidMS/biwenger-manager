# #23 — Daily sync job

- **Issue:** [#23 [3.3] Daily sync job](https://github.com/DavidMS/biwenger-manager/issues/23)
- **Phase:** 3 — Domain, persistence & history
- **Date:** 2026-10-23

## Prompt

> merged, continue with #23

## What Claude did

1. Moved #22 to *Done* and #23 to *In progress*, and branched. The manual trigger and the status endpoint are #25, so this issue is only the scheduled job.
2. Split the work so each piece has one job:
   - **`BiwengerGateway`** in `app.biwenger` reads the catalog, market and standings and returns domain types. It's still the only code that touches the client.
   - **`DailySyncService`** takes the Madrid day from the `Clock`, fetches everything first, then writes, then logs a summary.
   - **`SnapshotWriter`** stores the whole day in **one transaction**, after every Biwenger call has succeeded, so no transaction stays open during HTTP calls and a failure stores nothing.
   - **`DailySyncScheduler`** runs at `biwenger.sync.cron` (default 07:30 Madrid time, after the market close), skips with a log line when no login is set, and logs failures instead of throwing them.
3. A second run on the same day updates the players and prices, and **replaces** the day's market and standings, so a listing that sold in between isn't kept.
4. Tested the whole job, with WireMock serving the real samples and PostgreSQL storing them. A mutable test clock syncs on the 22nd, again the same morning, then on the 23rd. The tests cover:
   - the stored counts and the logged summary
   - no duplicates on a re-run
   - new rows the next day
   - a sale that left the market
   - a Biwenger failure that stores nothing
   - 23:30 UTC being the next day in Madrid
   - the configured cron, the on/off switch, and skipping without a login

   Breaking the market's same-day replacement on purpose made its test fail.
5. Exposed the settings as `BIWENGER_SYNC_CRON` and `BIWENGER_SYNC_ENABLED` in `.env.example` and Docker Compose. The README now says the app syncs every morning, read-only.

## Review

`java-reviewer`: **changes requested,** for the tests. It confirmed the transaction, the same-day replacement, the time zones and that no secret reaches the error log.

- **major: the sync test left rows in the shared database,** and two repository tests count rows. Claude tried to reproduce the failure, with surefire's reverse order and JUnit's random class order with three seeds, and couldn't. The reason: the test class happened to end with the one test that writes nothing, so it was clean by luck. **Fixed anyway:** the class now empties the tables after every test.
- **minor: a manager who left the league stayed in the day's table.** **Fixed** like the market, with a test.
- **minor: `@EnableScheduling` sat on the conditional scheduler,** so switching the sync off would have silently stopped every future scheduled job. **Moved** to an unconditional configuration.
- **For #25:** the guard against two syncs at once belongs in the service, so the scheduled run respects it too. **Noted on #25.**
- **nits, added:** the next day's market count, and a write that fails halfway (a listing that breaks a database check) rolling everything back.

## Corrections

- **Two lazy spots in the first test draft:** a placeholder method left behind, and an `assertThat(true).isTrue()` that tested nothing. Both were replaced before review, the second by a real check that the sync ran and nothing was thrown.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The sync is on by default.** It's the app's purpose, and it only reads. Without a login it skips, so the app still runs without one.
- **07:30 Madrid** is after the market close whether the close is 06:00 or 07:00 Madrid time (05:00 UTC in winter or in summer).
- **Fetch first, write after.** No database transaction waits on the network, and a partial day is never stored.

## Lesson

A test that passes by luck is still broken. Here, test order and method order hid a dependency between test classes, and three attempts to expose it failed. The fix (every class cleans up after itself) costs one annotation, and it doesn't need a reproduction to be right.
