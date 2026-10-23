---
name: biwenger-api
description: Domain knowledge about the unofficial Biwenger API and game rules - endpoints, headers, auth flow, sample payloads, scoring, market, clauses and lineups, and how to capture a new endpoint with browser DevTools. Use when working on the Biwenger client, sync jobs, advisor rules or docs/biwenger-api/.
---

# Biwenger API and game rules

The API is **unofficial and reverse-engineered** (reference: [pybiwenger](https://github.com/pablominue/pybiwenger)). It can change without notice. When reality disagrees with this file, trust a fresh capture, then update `docs/biwenger-api/` and this skill.

The reference is [`docs/biwenger-api/`](../../../docs/biwenger-api/README.md): one page per endpoint, written from real captures (#14), with anonymised samples in `docs/biwenger-api/samples/`. The samples are the source for WireMock stubs. **Tests never call the real API.**

## The client

`BiwengerClient` (in `backend/biwenger-client`) has one method per read endpoint: `account()`, `league()`, `squad()`, `market()`, `catalog()`, `roundStandings()` and `board()`. Each returns records from `client.model` (the conventions are in `backend/CLAUDE.md`). `leagueContext()` gives the league, user and scoring ids. `playerPrices(slug)` and `playerReports(slug)` call the public CDN without the token.

## Auth flow

1. `POST https://biwenger.as.com/api/v2/auth/login` with JSON `{"email": ..., "password": ...}`. Credentials come from environment variables (`.env`, never committed, never read by Claude).
2. The response is `{"token": "..."}`, with no `{status, data}` envelope (every other response has one). Keep it in memory only and send `Authorization: Bearer <token>`.
3. `GET /api/v2/account` lists the user's leagues and the user id in each. This app uses the account's single La Liga league (`competition == "la-liga"`) and fails clearly if there is none or more than one.
4. Every later call sends these headers:

| Header | Value |
|---|---|
| `Authorization` | `Bearer <token>` |
| `X-League` | league id |
| `X-User` | the user's id **in that league** (`data.leagues[].user.id`), not the account id |
| `X-Lang` | `es` |
| `Accept` | `application/json` |

Which of these each endpoint strictly needs is untested, so send them all. The CDN (`cf.biwenger.com`) needs none of the auth headers.

5. On `401`, log in again once and retry. A second `401` is an error.

## Endpoints (read-only)

Base `https://biwenger.as.com` unless the URL says otherwise.

| Purpose | Method & URL | Notes |
|---|---|---|
| Login | `POST /api/v2/auth/login` | Returns `{token}` |
| Account | `GET /api/v2/account` | Leagues, the user in each (balance, points, position), league settings |
| League | `GET /api/v2/league/{leagueId}` | League info and `users[]` as `{id, name, icon}` only: **no standings** |
| My squad | `GET /api/v2/user?fields=players(id,owner)` | Player ids with `owner.date` and `owner.clause`. Join with the catalog; a squad player can be missing from it (seen 3 of 13) |
| Market | `GET /api/v2/market` | `data.sales[]`; `user == null` means a free player. `data.status` has your `balance` and `maximumBid` |
| Player catalog | `GET /api/v2/competitions/la-liga/data?lang=es&score={scoreId}[&season=YYYY]` | All players (551, about 230 KB) and teams, both objects **keyed by id as a string**. Also seasons, rounds and scoring systems. Changes about once a day |
| Rounds and standings | `GET /api/v2/rounds/league` | The current round, and `data.league.standings[]` sorted by position: points, `teamValue`, `teamValueInc` and each manager's last lineup |
| Board (transfers) | `GET /api/v2/league/{leagueId}/board?type=transfer,market` | `data` is an **array**, newest first: auction wins with the losing bids, sales to the game, clause buys. Pagination unknown |
| Player detail | `GET https://cf.biwenger.com/api/v2/players/la-liga/{slug}?lang=es&fields=*,prices` | Price history: about a year of daily `[YYMMDD, price]` pairs, oldest first. No auth |
| Player reports | same URL with `&fields=*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away),rawStats)` | One report per club match, played or not. Played: points for **every** scoring system (keyed by score id) and `rawStats` (minutes), which must be asked for in `fields`. Not played: only a `status` (`injured`, `doubt`, `sanctioned`, `ok`). pybiwenger's version of this string lacks the last `)` |

Omitting `season` returns the current one. The catalog calls 2026/2027 season `"2027"`.

Conventions: money is whole euros (`int`), timestamps are Unix seconds, optional fields are usually **left out** rather than `null`.

**Write endpoints** (bid, cancel bid, list for sale, accept/reject offer, save lineup) are not known yet. Spike #19 documents them in `docs/biwenger-api/write-endpoints.md`. Never call one without explicit user confirmation; in code they only run through the propose → confirm flow (Phase 6).

## Game rules that drive features

- **Balance.** Each manager has cash. **A negative balance when a round starts scores 0 points.** Always warn about it. In the sample, `maximumBid = balance + 25% of teamValue` exactly (inferred), so a bid can push the balance below zero. Rivals' balances are hidden in this league (`settings.balance: "hidden"`); the board's bids and transfers are the only way to estimate them.
- **Market.** Each day free players and players listed by managers appear. Managers place sealed bids; the highest wins when the market closes. Managers can also send offers for other managers' players.
- **Player value** changes daily with performance and demand. The cf `prices` field goes back about a year, and only for one player per request, so we still snapshot the catalog daily.
- **Clauses.** With `settings.clause` / `clauseIncrement` enabled, a player can be bought by paying their release clause ("clausulazo").
- **Scoring** is per league (`scoreID`). The catalog lists 1 Diario AS, 2 SofaScore, 3 Estadísticas, 5 Media AS y SofaScore, 6 Biwenger Social, 7 Feeberse Score, 8 Media AS y Feeberse. Pass it as `score=<id>` to the catalog.
- **Lineups.** Formation, optional captain, coach, reserves and max players per club (`lineupCaptain`, `lineupCoach`, `lineupReserves`, `lineupMaxClubPlayers`, `lineupMultiPos`). Players who don't play score 0.
- **Status.** `status` is `ok`, `injured`, `doubt` (and probably `sanctioned`); `statusInfo` is present only when it isn't `ok`. `fitness` is the last 5 rounds, most recent first: a score, `null`, or a status string.
- **Positions.** 1 goalkeeper, 2 defender, 3 midfielder, 4 forward, 5 coach. Formations and lineup sizes are in the catalog's `meta.sports`.
- **Rounds.** A postponed round appears as a separate round with the same `short` name and a `part` number ("Jornada 6 (aplazada)").

## Be gentle with the API

- Cache the catalog and snapshot it once a day.
- The client already rate-limits (500 ms between requests, API and CDN together), retries `GET`s on 5xx and network errors with backoff, and has connect and read timeouts. Tune them with `biwenger.*` (see `backend/CLAUDE.md`), not with loops of your own.
- Writes are never retried automatically: a bid sent twice is a real loss.
- Failures are `BiwengerApiException`s with the call and status, never the token or the response body. A User-Agent names the app.

## Capture with the script

`scripts/biwenger_capture.py` captures every read endpoint above. **The human runs it** (`! python3 scripts/biwenger_capture.py`), because it logs in with the credentials in `.env`. It anonymises in memory, checks each file for leaks before writing, and prints only status codes and sizes. To capture a new read endpoint, add it to the script with a test in `scripts/test_biwenger_capture.py`, then ask the human to run it.

## Capture a new endpoint with DevTools

The user does the capture, because it needs their logged-in browser. Guide them:

1. Open https://biwenger.as.com, log in and open DevTools → **Network**, filter **Fetch/XHR**, tick **Preserve log**.
2. Do the action once in the UI (open the page, or for a write, the smallest real action the user is happy to perform).
3. Click the request. Note the method, URL, query string, request headers that matter (`X-League`, `X-User`, `X-Lang`) and the request body (**Payload** tab).
4. Copy the response from the **Response** tab.
5. **Anonymise before saving:** remove the `Authorization` header and token, emails, real names of other managers, and user/league ids if the user prefers (replace with consistent fake values).
6. Save the JSON to `docs/biwenger-api/samples/<endpoint>.json` and document it in `docs/biwenger-api/<endpoint>.md`: purpose, method + URL, headers, request body, response fields we use, and quirks.
7. Add WireMock stubs and deserialization tests that use the sample.
