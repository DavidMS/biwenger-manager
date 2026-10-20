---
name: biwenger-api
description: Domain knowledge about the unofficial Biwenger API and game rules - endpoints, headers, auth flow, sample payloads, scoring, market, clauses and lineups, and how to capture a new endpoint with browser DevTools. Use when working on the Biwenger client, sync jobs, advisor rules or docs/biwenger-api/.
---

# Biwenger API and game rules

The API is **unofficial and reverse-engineered** (reference: [pybiwenger](https://github.com/pablominue/pybiwenger)). It can change without notice. When reality disagrees with this file, trust a fresh capture, then update `docs/biwenger-api/` and this skill.

Captured responses live in `docs/biwenger-api/samples/` (from issue 2.1) and are the source for WireMock stubs. **Tests never call the real API.**

## Auth flow

1. `POST https://biwenger.as.com/api/v2/auth/login` with JSON `{"email": ..., "password": ...}`. Credentials come from environment variables (`.env`, never committed, never read by Claude).
2. The response is `{"token": "..."}`. Keep it in memory only and send `Authorization: Bearer <token>`.
3. `GET /api/v2/account` lists the user's leagues and the user id in each. This app uses the account's single La Liga league and fails clearly if there is none.
4. Every later call sends these headers:

| Header | Value |
|---|---|
| `Authorization` | `Bearer <token>` |
| `X-League` | league id |
| `X-User` | the user's id in that league |
| `X-Lang` | `es` |
| `Accept` | `application/json` |

5. On `401`, log in again once and retry. A second `401` is an error.

## Endpoints (read-only)

Base `https://biwenger.as.com` unless the URL says otherwise.

| Purpose | Method & URL | Notes |
|---|---|---|
| Login | `POST /api/v2/auth/login` | Returns `{token}` |
| Account | `GET /api/v2/account` | Leagues, the user in each (balance, points, position), league settings |
| League | `GET /api/v2/league/{leagueId}` | Users and standings. Needs `X-League`, `X-User` |
| My squad | `GET /api/v2/user?fields=players(id,owner)` | |
| Market | `GET /api/v2/market` | `data.sales[]`. `user == null` means a free player |
| Player catalog | `GET /api/v2/competitions/la-liga/data?lang=es&score={scoreId}[&season=YYYY]` | All players: price, priceIncrement, fitness, points, status, position, team. Large, changes about once a day |
| Rounds | `GET /api/v2/rounds/league` | |
| Board (transfers) | `GET /api/v2/league/{leagueId}/board?type=transfer,market` | Transfer history in the league |
| Player detail | `GET https://cf.biwenger.com/api/v2/players/la-liga/{slug}?lang=es&season=YYYY&fields=*,prices` | Price history |
| Player reports | same URL with `&fields=*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away))` | Per-match points and stats |

**Write endpoints** (bid, cancel bid, list for sale, accept/reject offer, save lineup) are not known yet. Spike #19 documents them in `docs/biwenger-api/write-endpoints.md`. Never call one without explicit user confirmation; in code they only run through the propose → confirm flow (Phase 6).

## Game rules that drive features

- **Balance.** Each manager has cash. **A negative balance when a round starts scores 0 points.** Always warn about it.
- **Market.** Each day free players and players listed by managers appear. Managers place sealed bids; the highest wins when the market closes. Managers can also send offers for other managers' players.
- **Player value** changes daily with performance and demand. Biwenger keeps limited history (the cf `prices` field), so we snapshot the catalog daily.
- **Clauses.** With `settings.clause` / `clauseIncrement` enabled, a player can be bought by paying their release clause ("clausulazo").
- **Scoring** is per league (`scoreID`): SofaScore, AS picas, mixed stats, social, Jornada Perfecta or weighted mixes. Pass it as `score=<id>` to the catalog.
- **Lineups.** Formation, optional captain, coach, reserves and max players per club (`lineupCaptain`, `lineupCoach`, `lineupReserves`, `lineupMaxClubPlayers`, `lineupMultiPos`). Players who don't play score 0.
- **Status.** `status` / `statusInfo` (injured, doubtful, suspended...) and `fitness` (last 5 scores) are in the catalog.

## Be gentle with the API

- Cache the catalog and snapshot it once a day.
- Rate-limit, set timeouts and retry with backoff. Send a sensible `User-Agent`.
- Map failures to `BiwengerApiException` with the endpoint and status, never the token.

## Capture a new endpoint with DevTools

The user does the capture, because it needs their logged-in browser. Guide them:

1. Open https://biwenger.as.com, log in and open DevTools → **Network**, filter **Fetch/XHR**, tick **Preserve log**.
2. Do the action once in the UI (open the page, or for a write, the smallest real action the user is happy to perform).
3. Click the request. Note the method, URL, query string, request headers that matter (`X-League`, `X-User`, `X-Lang`) and the request body (**Payload** tab).
4. Copy the response from the **Response** tab.
5. **Anonymise before saving:** remove the `Authorization` header and token, emails, real names of other managers, and user/league ids if the user prefers (replace with consistent fake values).
6. Save the JSON to `docs/biwenger-api/samples/<endpoint>.json` and document it in `docs/biwenger-api/<endpoint>.md`: purpose, method + URL, headers, request body, response fields we use, and quirks.
7. Add WireMock stubs and deserialization tests that use the sample.
