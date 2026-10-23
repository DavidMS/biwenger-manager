# 03 — Reverse-engineering an API with AI

Phase 2 builds the client for Biwenger's unofficial API, the one part of the app that talks to the outside world. Nobody publishes this API, so the work started with questions and ended with a documented, recorded and tested client. This chapter covers how Claude Code got there without ever seeing the human's password, and without sending a single request that changed the account.

Issues: [#14](https://github.com/DavidMS/biwenger-manager/issues/14) capture and document the read endpoints, [#15](https://github.com/DavidMS/biwenger-manager/issues/15) authentication, [#16](https://github.com/DavidMS/biwenger-manager/issues/16) typed models, [#17](https://github.com/DavidMS/biwenger-manager/issues/17) CDN endpoints, [#18](https://github.com/DavidMS/biwenger-manager/issues/18) resilience, [#19](https://github.com/DavidMS/biwenger-manager/issues/19) write endpoints and [#20](https://github.com/DavidMS/biwenger-manager/issues/20) this chapter.

## Goal

Everything the app will show (squad, market, standings) and everything it will advise comes through this client. An unofficial API raises three problems that an official one doesn't:

- **Nobody tells you the truth about it.** There is no documentation, only other people's guesses.
- **It can change without notice.** The client must notice the change and fail loudly, not quietly show wrong numbers.
- **Its data is personal and its writes are real.** Responses hold other managers' names and the account's email, and a request that bids or sells really happens.

So the phase had to produce three things: a reference to work from, recorded samples to test against, and a client that is strict where it matters and gentle with Biwenger.

## Concepts

### A reference project is a map, not the territory

[pybiwenger](https://github.com/pablominue/pybiwenger), a Python client, gave the starting list of endpoints in plan §4.2. Claude read its source with `gh api` rather than relying on memory. That paid off at once: its account type listed fields the plan didn't mention (phone, birthday, location, device tokens, the league's invite `secret`), and the anonymiser had to remove all of them ([#14](journal/0014-biwenger-read-endpoints.md)).

Then real responses corrected the map in several places:

| pybiwenger or the plan said | The real API |
|---|---|
| The league endpoint has the standings | It has users only; standings come from `/rounds/league` |
| Price history is "limited" | About a year of daily values |
| Scoring includes "Jornada Perfecta" | No such option; two new Feeberse scores instead |
| `lineupMaxClubPlayers` is a boolean | It's a number |
| The reports `fields` string (missing its last `)`) | A balanced string, plus `,rawStats` to get minutes |

Treat a reference project like a colleague's notes: a good place to start, then check every claim you rely on against a real response.

### The human captures, the AI reads the anonymised output

Most endpoints need a login, and two golden rules collide here. Claude never reads `.env`, and never sends a Biwenger write. The pattern that resolved it was used three times, each time a little more cautious:

| Capture | Who runs it | What Claude sees |
|---|---|---|
| Read endpoints, `scripts/biwenger_capture.py` ([#14](journal/0014-biwenger-read-endpoints.md)) | The human, `! python3 …` | Anonymised samples, plus status codes and sizes |
| Public CDN, `--cdn` mode ([#17](journal/0017-cdn-endpoints.md)) | Claude, after asking, because it needs no login | The same |
| Write endpoints, a HAR file from the browser and `scripts/biwenger_har_extract.py` ([#19](journal/0019-write-endpoints.md)) | The human, doing each action themselves | Anonymised samples, and a `--list` mode with every number masked |

In each one, the script does the risky work on the human's machine:

- **Anonymisation.** You become *Manager 1*, rivals *Manager 2…*, the league *Example League*, ids are fake, and personal fields are removed.
- **A leak check** before anything is written.
- **Output that holds nothing real:** no URLs, no bodies.

Claude decides *what* to capture by asking the human with a question tool, and the human's choices are quoted in the journal. These are the real answers:

> Capture script you run (Recommended)

> Fake names and ids (Recommended)

> I capture 2 public CDN calls (Recommended)

> HAR export + script (Recommended) · Bid, then cancel it · List a player, then withdraw · Re-save your lineup

The last one is the one to notice. The human chose which real actions to perform, and declined "accept an offer" because it can't be undone. The doc lists it as *not captured* rather than guessed.

### The explorer agent works from the samples

The `biwenger-api-explorer` agent from Phase 0 wrote the endpoint pages in `docs/biwenger-api/`. Its prompt said, in so many words, to make no requests to Biwenger: everything it needed was on disk. Its hook only allows `GET` requests anyway. It returned more than documentation: findings such as *`maximumBid` = balance + 25% of team value*, *three squad players are missing from the catalog*, and *`X-User` is your id in the league, not the account's*.

Claude checked each claim against the samples before putting it in the skill. All of them held, and the inferred ones stayed marked *(inferred)*. An agent that writes documentation is still a source to verify, like a reference project.

### Recorded samples are the test fixtures

The samples live in `docs/biwenger-api/samples/` and Maven copies them onto the test classpath (`<testResources>` in both modules). Every client test stubs Biwenger with [WireMock](https://wiremock.org/) using a real, anonymised answer, never the live API (golden rule 3). That made the tests specific in a way hand-written JSON never is:

- **The maximum bid** is 57,005,000.
- **12 of the 19 sales** are free players.
- **Ayoze's points over 9 matches** add up to the catalog's 35.
- **Pablo Marín's form** is `["sanctioned", -4, null, "doubt", "doubt"]`.

Two practical lessons came with it:

- **Hand-written fixtures hide strictness.** In [#16](journal/0016-typed-models.md), tests built on minimal JSON broke when the records became strict. The tests were rewritten to derive their variants from the real samples.
- **Test infrastructure has its own bugs.** On plain `http://`, the JDK HTTP client tries an HTTP/2 upgrade that WireMock's Jetty breaks, which showed up as `RST_STREAM` errors ([#15](journal/0015-biwenger-auth.md)). Biwenger is HTTPS, so the fix is test-only and commented.

### Strict about data, gentle with the server

Two design rules for an API that can change ([#16](journal/0016-typed-models.md), [#18](journal/0018-client-resilience.md)):

- **Ignore what's added, fail on what's dropped.** Unknown fields are ignored by the client's *own* JSON mapper, whatever the app configures. A field is required unless marked `@Nullable`. If Biwenger drops one, the call fails with "doesn't match docs/biwenger-api/" instead of a silent `0`, which could otherwise trigger a false negative-balance warning.
- **Be gentle, and never repeat a write.**
  - 500 ms between any two requests.
  - Exponential backoff on 5xx and network errors.
  - **Only `GET`s are retried.** The only `POST` today is the login, but a retried bid in Phase 6 could be placed twice. A test proves the rule before the first write exists.

Timing was tested without waiting: the client takes a clock and a pause, and the test versions record each pause, so tests assert exact delays ("100 ms, then 150 ms") in a second.

## How it went

| Issue | Prompt | Result |
|---|---|---|
| [#14](journal/0014-biwenger-read-endpoints.md) | > merged, continue with #14 | Capture script, 9 samples, one doc per endpoint |
| [#15](journal/0015-biwenger-auth.md) | > merged, continue with #15 | Login, token in memory, re-login once on 401, league headers |
| [#16](journal/0016-typed-models.md) | > merged, continue with #16 | 28 records, one method per endpoint, strict-but-tolerant mapping |
| [#17](journal/0017-cdn-endpoints.md) | > merged, start #17 | Price history as dates, match reports with minutes and absence reasons |
| [#18](journal/0018-client-resilience.md) | > merged, continue with #18 | Rate limit, retries with backoff, timeouts, clear errors |
| [#19](journal/0019-write-endpoints.md) | > Merged, go ahead with #19 | Five write actions documented from the browser, Phase 6 issues updated |

The prompts stayed one line long. The judgement went into the questions Claude asked before acting: how to capture, how much to anonymise, which real actions the human would perform.

## What went wrong

- **A personal photo URL nearly reached the repo** ([#19](journal/0019-write-endpoints.md)).
  - What happened: the human's Google profile picture was in two write samples. The anonymiser recognised managers by Python object identity, which only worked because the first script happened to anonymise the same objects it scanned.
  - Why the check missed it: the leak check only looked for values it already knew.
  - How it was caught: the field-by-field review Claude does before committing.
  - **What changed:** managers are now recognised by id, and the leak check refuses any personal avatar host, with no list needed.
- **A recording was saved inside the repo,** with no extension that `.gitignore` covered ([#19](journal/0019-write-endpoints.md)).
  - Claude checked with `git status` that it had never been committed, and asked for it to be moved.
  - Until then, Claude staged files by name only, because its usual `git add -A` would have picked the file up.
  - **What changed:** the script refuses any HAR inside the repo.
- **The leak check held back files without saying why** ([#14](journal/0014-biwenger-read-endpoints.md)).
  - Claude didn't open the raw data to find out. It made the check name the *field* (never the value), and the next run said `instagram, twitter`: footballers' public handles.
  - **What changed:** social handles are removed only from managers.
- **A missing field went unnoticed** ([#17](journal/0017-cdn-endpoints.md)). The #14 capture never asked for `rawStats`, so the acceptance criterion "minutes per match" had no data behind it. Claude compared the criteria with the samples before writing code, asked, and captured again.
- **Reviewers found real bugs, and once a wrong blocker.** Highlights from `java-reviewer`:
  - A path-matching shortcut that would recurse until `StackOverflowError` behind a proxy ([#15](journal/0015-biwenger-auth.md)).
  - "Primitive = required" missing every required object and string ([#16](journal/0016-typed-models.md)).
  - A mistyped `--cdn` falling through to the logged-in capture that reads `.env` ([#17](journal/0017-cdn-endpoints.md)).
  - An interrupted thread that kept retrying ([#18](journal/0018-client-resilience.md)).
  - One claim, "`altPositions` isn't in the sample", was wrong: it was on 12 of 39 players.
- **Claude committed with a failing test, twice** ([#14](journal/0014-biwenger-read-endpoints.md), [#19](journal/0019-write-endpoints.md)), because `… | tail -1` hid the exit code. **What changed:** commits run only inside `if <tests pass>; then … fi`.

## Results

- 6 issues, 6 PRs ([#67](https://github.com/DavidMS/biwenger-manager/pull/67)–[#72](https://github.com/DavidMS/biwenger-manager/pull/72)).
- **Client:** about 1,500 lines of code and 1,500 of tests: 64 Java tests (mostly WireMock against real samples) and 24 Python tests for the capture scripts.
- **API reference:** 10 read-endpoint pages plus the write-endpoints page, in about 1,100 lines of docs. There are 16 anonymised samples, about 6,900 lines of JSON.
- **Requests Claude sent to Biwenger:** 2, both public `GET`s to the CDN, after asking. **Write requests sent by code: 0.**
- 2 new sections in `backend/CLAUDE.md`: how client records map JSON, and how gently the client treats Biwenger.

## Takeaways

- **Start from a reference project, then check every claim you rely on against a real response.** Mark what you inferred as inferred.
- **When the AI must not see the data, give it a way to ask about the data without seeing it:** a script the human runs, a leak check that names the field but not the value, a listing with every number masked.
- **Let the human choose the real-world actions.** Ask which irreversible steps they accept, and write "not captured" rather than guess.
- **Test against recorded, anonymised responses,** and derive edge cases from them instead of hand-writing JSON.
- **Read every field of anything you're about to publish.** The automatic check caught names, ids and tokens. Only the review caught the photo URL no list contained.
