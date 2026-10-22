# League

Returns basic data about the league and its managers (id, name, icon). Used to name managers in **standings and rivals** (F4) and to resolve manager ids seen in the market and board.

**It does not return standings.** Points, positions and team values are in [rounds-league](rounds-league.md). This contradicts plan §4.2 ("Users/standings").

## Request

`GET https://biwenger.as.com/api/v2/league/{leagueId}`

Headers sent: `Authorization: Bearer <token>`, `X-League: {leagueId}`, `X-User: {userId}`, `X-Lang: es`, `Accept: application/json`, `User-Agent`. No query parameters.

## Response

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.id` | int | League id | `1000001` |
| `data.name` | string | League name | `"Example League"` |
| `data.scoreID` | int | Scoring system (same as in the account) | `1` |
| `data.type` / `mode` / `marketMode` | string | League kind | `"premium"`, `"league"`, `"normal"` |
| `data.created` | int (Unix s) | League creation time | `1659204919` |
| `data.users[]` | array | Every manager in the league, including you | |
| `data.users[].id` | int | Manager's user id in this league | `2000002` |
| `data.users[].name` | string | Manager's team name | `"Manager 2"` |
| `data.users[].icon` | string (URL) | Avatar | |

## Notes / quirks

- `users[]` is **not** ordered by standing (you are second in the sample, but sixth in the table).
- No `settings`, balances, points or squads here. Settings are in the account; standings in rounds-league.
- pybiwenger calls this URL without a `fields` parameter too. A `fields=` query might return more, but we have not tried it.
- The sample league has 6 managers.

## Sample

[samples/league.json](samples/league.json)

Anonymised: league id and name, manager ids and names (you are `Manager 1`), `icon` / `cover` URLs.
