# #25 — Manual sync endpoint and sync status

- **Issue:** [#25 [3.5] Manual sync endpoint and sync status](https://github.com/DavidMS/biwenger-manager/issues/25)
- **Phase:** 3 — Domain, persistence & history
- **Date:** 2026-10-23

## Prompt

> merged, start with #25

## What Claude did

1. Moved #24 to *Done* and #25 to *In progress*, and branched. This is the app's first REST code.
2. Made four decisions and stated them, for the human to overrule in review:
   - **`POST /api/sync` answers `202 Accepted` and syncs in the background.** A sync can take a minute when Biwenger is slow. It answers `409 Conflict` if one is already running.
   - **The "only one at a time" guard is in `DailySyncService`,** as noted on the issue in #23, so the schedule respects it too.
   - **Runs are recorded in a `sync_run` table** (`V3`), so the status survives a restart.
   - **Errors are `ProblemDetail`** through the first `@RestControllerAdvice`, as `backend/CLAUDE.md` set out in #12.
3. Wrote:
   - the sealed `SyncRun` (`Running` / `Succeeded` / `Failed`)
   - its entity and recorder, which also marks runs left running by a crash as failed at the next startup
   - the guard in the service
   - the controller and its response DTO
   - `app.web.ApiExceptionHandler`
4. Tested at three levels:
   - **`@WebMvcTest`, 6 tests:** 202 with `Location`, 409 as `ProblemDetail`, the status in its different states, and 405 as `ProblemDetail`.
   - **Integration:** runs recorded, two syncs at once refused (a slow WireMock keeps the first one running), and an interrupted run.
   - **Unit:** the scheduler skips while a manual sync runs.

   Removing the guard on purpose made four tests fail.
5. **Ran it for real** through nginx, in a separate Docker Compose project with no Biwenger login, so nothing reached Biwenger. That showed a small race: the `POST` reply said "running" but had no run yet. The run is now recorded before the background thread starts.

## Review

`java-reviewer`: **changes requested.** Both major findings were real bugs:

- **major: a failure could vanish.** The run was recorded as failed *before* the error was logged. With the database down, the recording failed too, its exception replaced the original, and nothing was logged, while the scheduler swallowed it. **Fixed:** log first, then record, with a failed recording attached as "suppressed" to the original error.
- **major: the startup cleanup could fail a fresh run.** Spring Boot starts the web server and the scheduler *before* the "ready" event. A sync started in that window got its new "running" row marked interrupted, and then couldn't record its success, because the error text was never cleared and broke a database check. **Fixed:** only runs from before this boot are cleaned up, and a success clears the error. The new test fails without the fix (see Corrections).
- **minor:** tests for the `sync_run` database checks. **Added.**
- **nits:**
  - an exception without a message now stores its type
  - truncation can't split a character
  - `location(URI)` in the controller
- **CSRF:** the API has no login, so any web page in the browser could send a plain `POST`. That's acceptable for a read-only sync, but not for Phase 6. **Recorded** in `backend/CLAUDE.md` and on #43: write endpoints must require a JSON content type or a custom header.

## Corrections

- **The first proof for the boot cutoff was invalid.** To show the test fails without the fix, Claude replaced the cutoff with `Instant.MAX`, which PostgreSQL can't store. The app didn't even start, so every test failed for the wrong reason. Repeated with a far-future date the database accepts: exactly the right test failed.
- **Edit scripts stopped twice on lines the formatter had re-wrapped.** The second time it was partway through a set of files. Claude checked which files were already changed before applying the rest.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The status combines two sources:** `running` comes from memory (the guard), and the last run from the database. The guard is one app instance's, which is all this single-user app runs.
- **A sync without a login** is accepted and recorded as failed with "credentials not set". The status then tells the user what's missing.

## Lesson

A check that fails for the wrong reason proves nothing. Breaking the code on purpose is only evidence when the failure is the one you expect: here, an impossible value broke startup instead of the rule. Read *why* the test failed, not just *that* it did.
