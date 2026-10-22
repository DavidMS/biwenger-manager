# Biwenger API reference

The Biwenger API is unofficial and reverse-engineered. It can change without notice. This folder documents what we have seen in real responses, so the client (`backend/biwenger-client`) and its tests can rely on it. The starting point was [pybiwenger](https://github.com/pablominue/pybiwenger); every page here was checked against a real, anonymised capture.

When a fresh capture disagrees with these pages, trust the capture and update the page.

The Java side is `BiwengerClient` in `backend/biwenger-client`, with one method and one set of records (`client.model`) per endpoint below. Its tests run against these samples.

## Endpoints (read-only)

`API` is `https://biwenger.as.com/api/v2`. `CDN` is `https://cf.biwenger.com/api/v2`.

| Endpoint | Method + URL | Auth needed? | Doc | Sample |
|---|---|---|---|---|
| Login | `POST API/auth/login` | No (it gives the token) | [login.md](login.md) | none (it would only hold a token) |
| Account | `GET API/account` | Bearer token | [account.md](account.md) | [account.json](samples/account.json) |
| League | `GET API/league/{leagueId}` | Token + league headers | [league.md](league.md) | [league.json](samples/league.json) |
| My squad | `GET API/user?fields=players(id,owner)` | Token + league headers | [user-squad.md](user-squad.md) | [user-squad.json](samples/user-squad.json) |
| Market | `GET API/market` | Token + league headers | [market.md](market.md) | [market.json](samples/market.json) |
| Player catalog | `GET API/competitions/la-liga/data?lang=es&score={scoreId}` | Sent with token + league headers (see the doc) | [catalog.md](catalog.md) | [catalog.json](samples/catalog.json) |
| Rounds and standings | `GET API/rounds/league` | Token + league headers | [rounds-league.md](rounds-league.md) | [rounds-league.json](samples/rounds-league.json) |
| Board (transfers) | `GET API/league/{leagueId}/board?type=transfer,market` | Token + league headers | [board.md](board.md) | [board.json](samples/board.json) |
| Player prices | `GET CDN/players/la-liga/{slug}?lang=es&fields=*,prices` | No | [player-prices.md](player-prices.md) | [player-prices.json](samples/player-prices.json) |
| Player reports | `GET CDN/players/la-liga/{slug}?lang=es&fields=*,reports(...)` | No | [player-reports.md](player-reports.md) | [player-reports.json](samples/player-reports.json), [player-reports-missed-matches.json](samples/player-reports-missed-matches.json) |

## Common headers

These are the headers the capture script sent. We have not tested which ones the server really requires: the client should send all of them.

| Header | Value | Sent to |
|---|---|---|
| `Accept` | `application/json` | every request |
| `X-Lang` | `es` | every request |
| `User-Agent` | an identifying string, for example `biwenger-manager/0.1 (...)` | every request |
| `Content-Type` | `application/json` | login only (the one request with a body) |
| `Authorization` | `Bearer <token>` | every `biwenger.as.com` request after login. Not sent to the CDN |
| `X-League` | the league id (`data.leagues[].id` in the account) | every `biwenger.as.com` request after the account |
| `X-User` | your user id **in that league** (`data.leagues[].user.id`), not the account id | every `biwenger.as.com` request after the account |

The account request itself was sent without `X-League` and `X-User`, because they come from its response.

## Response envelope

Every captured `biwenger.as.com` and CDN response has the same envelope:

```json
{ "status": 200, "data": { ... } }
```

- `status` repeats the HTTP status as a number.
- `data` is usually an object. In the board it is an **array**.
- The catalog also has a top-level `meta` object. `meta.sports` holds formations and lineup sizes (see [catalog.md](catalog.md)). Its ads and app config, `meta.config`, is dropped from the sample.
- Login is the exception: its body is `{ "token": "..." }` with no envelope.

General conventions seen in all samples:

- **Money** is whole euros as an integer: `4780000` is 4.78 M€.
- **Timestamps** are Unix seconds (UTC), for example `1792731600`.
- **Ids** are integers in the JSON, but objects keyed by id (`catalog.data.players`, `catalog.data.teams`, report `points`) use string keys.
- Optional fields are often **left out** rather than set to `null` (`statusInfo`, `altPositions`, `extended`, `iconHero`, `lineup.coach`). Map them as optional.

## How the samples were captured

`python3 scripts/biwenger_capture.py --cdn` fetches only the two public CDN report samples: no login and no `.env`, so Claude may run it after the human agrees (#17). The full capture below needs the login and is run by the human.

The human ran `python3 scripts/biwenger_capture.py` from the repo root. The script:

- reads `BIWENGER_EMAIL` and `BIWENGER_PASSWORD` from `.env` or the environment;
- logs in once, then makes **GET requests only**, 2 seconds apart (10 requests in total: login, 7 API GETs, 2 CDN GETs). Nothing in Biwenger changes;
- calls the account first and stops unless there is exactly one La Liga league;
- uses the league's `scoreID` for the catalog and the slug of the first squad player found in the catalog for the two CDN calls;
- anonymises every response in memory. Raw responses and the token are never written to disk;
- checks each file for leaks before writing it (email, password, token, real manager and league names and ids, values of removed fields). A file that fails is not written.

Anonymisation rules:

| What | Replaced with |
|---|---|
| Your team in the league | `Manager 1`, id `2000001` |
| Other managers (any `{id, name}` object with a manager hint such as `icon`, `balance`, `role`) | `Manager 2`, `Manager 3`... with ids `2000002`, `2000003`... in the order they were found |
| The La Liga league | `Example League`, id `1000001` (other leagues: `Other League n`, `100000n`) |
| The account | `Account Owner`, id `3000001`. In the sample the account name equals the team name, so it shows as `Manager 1` |
| Manager, league and account `icon` / `cover` URLs | `https://example.invalid/icon.png` / `cover.png` |
| `email`, `phone`, `birthday`, `devices`, `token`, `password`, `secret`, `location`, `ip`, `google_id`, `apple_id` (anywhere) | a placeholder of the same JSON type: `"removed"`, `0`, `[]`, `{}` or `false` |
| `facebook`, `twitter`, `instagram` of managers, leagues and the account | the same placeholders. Footballers' and the competition's social handles are public and kept |

Trimming: the catalog keeps your squad and market players plus 10 more (at most 40 players); the board keeps at most 20 entries. Player, team, round and match ids are public Biwenger data and are not changed.

## Not yet known

- **Write endpoints** (bid, cancel bid, list a player for sale, accept or reject an offer, save a lineup). Spike issue #19 documents them in `write-endpoints.md` from browser captures made by the human. pybiwenger names `POST API/market` (list a player) and `API/market?player=` (remove one), but we have not verified either. Never call them outside the propose and confirm flow.
- Which headers are strictly required per endpoint.
- Board pagination: the board returned 10 entries and we don't know the parameters for older ones.
- The meaning of some fields, marked *unknown* in each page (for example `fantasyPrice`, report event `type` codes, `lineup.count`).
- The `season` parameter: every capture omitted it and got the current season. The catalog names the season `"2027"` for 2026/2027, so `season=2027` is probably the value to send (inferred, not tested).
