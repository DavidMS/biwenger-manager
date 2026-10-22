# #17 — CDN endpoints: price history and match reports

- **Issue:** [#17 [2.4] CDN endpoints: price history and per-match reports](https://github.com/DavidMS/biwenger-manager/issues/17)
- **Phase:** 2 — Biwenger API client (read-only)
- **Date:** 2026-10-22

## Prompt

> merged, start #17

## What Claude did

1. Moved #16 to *Done* and #17 to *In progress*, and branched.
2. **Compared the issue with the data before coding.** The acceptance criteria ask for each match's minutes and status, but the #14 sample had neither: minutes live in `rawStats`, which the capture hadn't requested, and the one player captured had played every match.
3. **Asked before contacting Biwenger.** The missing data was on the public CDN, which needs no login. Claude offered three options: it captures two public calls itself, the human runs them, or no capture. The human chose:

   > I capture 2 public CDN calls (Recommended)

4. Added a `--cdn` mode to the capture script that never reads `.env` or logs in, with a test proving it. Then it ran it: 2 GET requests, 2 seconds apart.
5. Read the new samples. They changed what we knew:
   - **`rawStats` must be asked for in `fields`.** Then it has `minutesPlayed`, goals, assists and more.
   - **A match the player missed still has a report,** with only a `status`: `injured`, `doubt`, `sanctioned`, or `ok` (fit but not picked).
   - **Reports come in date order,** so a postponed J1 comes after J2.
6. Wrote the records and two client methods:
   - **`playerPrices(slug)`** returns `(LocalDate, price)` points. Decoding the `YYMMDD` date is the client's one translation: the encoding has only one meaning.
   - **`playerReports(slug)`** returns, per match: the round, home or away, points per scoring system, minutes, and the reason a match was missed.
   - **The CDN gets its own `RestClient`, without the auth interceptor,** so the token never leaves for the CDN. A test checks the headers.
   - The client constructor now takes both `RestClient`s, and the app has a `biwenger.cdn-base-url` setting.
7. Tests, against the samples: a year of daily prices, Ayoze's minutes per match, and his points adding up to the catalog's 35. Pablo Marín's nine reports check played vs. injured vs. sanctioned. Breaking `played()` on purpose made the missed-match test fail.
8. Updated `player-reports.md` with the new findings, the API README, and the `biwenger-api` skill.

## Review

`java-reviewer`: **approve with nits.** All seven applied. The most important was ranked as a nit:

- **A mistyped `--cdn` would have read `.env`.** The script ignored unknown arguments, so `--cnd` fell through to the full capture, which loads the credentials. Since Claude was the one running `--cdn`, a typo would have broken the rule that Claude never reads `.env`. **Fixed:** the script now uses `argparse` and stops on an unknown option, with a test.
- **The `YYMMDD` decoding accepted too much.** If Biwenger switched to `YYYYMMDD`, `20251022` would have decoded to the year 4025 without a word. It now rejects anything outside six digits, with an end-to-end test that the error reaches the "doesn't match docs" message.
- **`points(scoreId)` returned a nullable `Integer`,** against `backend/CLAUDE.md`'s rule to use `Optional` for absent return values. It now returns an `OptionalInt`.
- **Nits:**
  - a report must have points and `rawStats` together, or neither
  - a slug is required
  - the docs no longer say `*(),` must be URL-encoded

## Corrections

- **The first capture (#14) had asked for the wrong fields.** Without `,rawStats`, the CDN sends no minutes. Claude had copied pybiwenger's `fields` and fixed its missing parenthesis, but pybiwenger seems to get `rawStats` through exactly that malformed string (not tested). Capturing again with `rawStats` named explicitly settled it.
- **An edit script had a syntax slip** and stopped after the first two files. Claude reran the rest.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Claude ran the public capture itself,** because the human allowed it for this case, it needs no login, and it is two requests. Anything that logs in is still run by the human.
- **Decode `YYMMDD` in the client.** Records otherwise mirror Biwenger, but a date encoding has nothing to interpret.
- **`minutesPlayed()` is 0 when he didn't play,** which is true, and `played()` tells the cases apart. Points are absent, not 0: a missed match is not a 0-point match.
- **Only minutes from `rawStats` for now.** Goals, assists and picas are documented and can be added when a feature needs them.

## Lesson

Read the acceptance criteria against the data, not only against the plan. "Minutes and status" looked like a modelling task, but the data had neither. One question and two public requests turned an unmeetable criterion into a tested one. And when a script can touch secrets, its argument parsing is part of the security: an option that is silently ignored is a trapdoor.
