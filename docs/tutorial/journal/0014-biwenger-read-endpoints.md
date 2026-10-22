# #14 — Spike: capture and document the Biwenger read endpoints

- **Issue:** [#14 [2.1] Spike: capture and document Biwenger read endpoints](https://github.com/DavidMS/biwenger-manager/issues/14)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-22

## Prompt

> merged, continue with #14

The first issue that touches the real Biwenger API. At the end of #13 Claude had said it would propose a safe way to capture responses and wait for approval before anything reached Biwenger, so it started with a question, not a request.

## The problem

Almost every endpoint needs a logged-in token. Our own rules say Claude never reads `.env`, and the `biwenger-api-explorer` agent never logs in. So the capture had to be done by the human, without Claude ever seeing the credentials, the token or the raw responses.

Claude asked two questions (with `AskUserQuestion`). The human chose:

- **A capture script the human runs,** over copying responses from DevTools by hand, or capturing only the public endpoints.
- **Fake names and fake ids,** over fake names with the real ids, so nothing in the public repo points to the real league.

## What Claude did

1. Moved #13 to *Done* and #14 to *In progress*, branched, and read plan §4, the `biwenger-api` skill and pybiwenger's source on GitHub. The source showed that the account response holds more personal data than the plan listed: phone, birthday, location, push-notification device tokens, and the league's invite `secret`.
2. Wrote `scripts/biwenger_capture.py`, with the Python standard library only:
   - It reads the credentials from `.env` itself, logs in once, then makes 9 GET requests 2 seconds apart.
   - It anonymises in memory:
     - you become *Manager 1*, the other managers *Manager 2…*, and the league *Example League*
     - every manager, league and account id is replaced with a fake one
     - personal fields are replaced with a placeholder of the same type
   - **No raw response or token ever touches the disk.**
   - Before writing a file, it searches it for the email, password, token, every real name and id, and the values of removed fields. A file that fails isn't written.
   - It prints only endpoint names, status codes and sizes. No URLs, because they hold the league id.
3. Wrote 11 tests on made-up data, including a full run with faked HTTP that checks both the files and the printed output. One test was broken on purpose to see it fail.
4. The human ran it three times (see Corrections). The third run wrote all 9 samples.
5. Reviewed the samples by hand, using scripts that list every string field and every number field, without reading the raw data. Also dropped 40 KB of ad configuration from the catalog sample and the script.
6. Delegated the docs to the `biwenger-api-explorer` agent, as the issue asked, with an explicit "make no requests to Biwenger". It wrote `docs/biwenger-api/README.md` and one page per endpoint. Claude checked its main claims against the samples (standings, the scoring list, the price format, the maximum-bid formula, the squad players missing from the catalog). All held.
7. Updated the `biwenger-api` skill, plan §4 and the repo map in `CLAUDE.md` with what the capture disproved.

## What the real responses taught us

The plan, the skill and pybiwenger got several things wrong:

- **The league endpoint has no standings.** Points, team values and every manager's lineup come from `GET /rounds/league`.
- **`X-User` is your id in the league,** not your account id.
- **Price history goes back about a year,** as daily `[YYMMDD, price]` pairs, not the "limited history" the plan assumed.
- **Scoring systems:** there is no "Jornada Perfecta", and there are two new Feeberse scores.
- **3 of the 13 squad players were missing from the catalog,** so the client must cope with that.
- **`maximumBid` equals balance plus 25% of team value** (inferred), so a bid can make the balance negative. That's exactly the case the app must warn about.

## Corrections

- **Claude's own leak check had a false positive in its design, caught by a test.** Location values like "Madrid" were on the leak list, so "Real Madrid" in the player data would have blocked the catalog every time. Location and birthday are still removed, but no longer searched for.
- **The first run held back 3 of 9 files without saying why.** The check said only "personal or secret field value". Claude didn't open the raw data to find out. It changed the check to name the *field* (never the value), and the human ran it again: `instagram, twitter`. Footballers' social handles are public, but they were on the remove list meant for managers. They are now removed only from managers, leagues and the account.
- **Your team showed as "Account Owner".** Your account name and team name are the same, and the account was registered first. It's now *Manager 1*.
- **Claude committed with a failing test** once: it chained the commit after the test run without reading the result. The code was right and the test's expectation wrong. Fixed in the next commit.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **The human runs anything that logs in.** Claude writes and tests the script but never runs it, never reads `.env`, and never sees a raw response.
- **Fail closed:** a file that might leak isn't written, and the report says which kind of data and which field, never the value.
- **Commit samples, not raw captures.** They are anonymised and trimmed (39 of 551 catalog players), and they are the source for WireMock stubs from #15 on.
- **The script's tests are not in CI yet.** CI runs `backend` and `frontend` only; a `scripts` check would be a separate issue.

## Lesson

When the AI must not see the data, give it a way to ask questions about the data without seeing it. Here that was a leak check that names the field but not the value, and summaries of field names and types. Each run told Claude enough to fix the script, and the human's credentials and league never left their machine unprotected.
