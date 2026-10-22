# Rounds and standings

Returns the current round of the league and the **standings**: every manager's points, position, team value and lineup for that round, plus the league's round bonuses. Used by **standings and rivals** (F4), the **dashboard** (F1: team value, position) and **season analytics** (F15). pybiwenger calls this URL `url_ranking`.

## Request

`GET https://biwenger.as.com/api/v2/rounds/league`

Headers sent: `Authorization: Bearer <token>`, `X-League: {leagueId}`, `X-User: {userId}`, `X-Lang: es`, `Accept: application/json`, `User-Agent`. No query parameters. The league comes from `X-League`.

## Response

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.round.id` | int | Current round id (matches the `active` round in the catalog) | `5176` |
| `data.league.id` / `.name` / `.scoreID` | int / string / int | The league | `1000001` |
| `data.league.standings[]` | array | Managers, **sorted by position** | |
| `data.league.standings[].id` / `.name` | int / string | Manager | `2000004`, `"Manager 4"` |
| `data.league.standings[].points` | int | Season points | `515` |
| `data.league.standings[].position` | int | Position, 1 = first | `1` |
| `data.league.standings[].teamValue` | int (€) | Sum of the squad's values | `64530000` |
| `data.league.standings[].teamValueInc` | int (€) | Team value change since the last update. Can be negative | `-70000` |
| `...standings[].lineup.type` | string | Formation, defenders-midfielders-forwards | `"3-4-3"` |
| `...standings[].lineup.points` | int | Points of this lineup in the round | `71` |
| `...standings[].lineup.players` | (int or null)[] | The 11 starters by player id. `null` is an empty slot | `[30487, 35156, ...]` |
| `...standings[].lineup.reserves` | (int or null)[] | Always 4 slots; `null` is empty. Slot meaning unknown (maybe one per position) | `[41268, null, 41411, null]` |
| `...standings[].lineup.discarded` | int[] | Squad players not in the lineup. Together with `players` and `reserves`, it gives each rival's squad (inferred) | |
| `...standings[].lineup.captain.id` | int, optional | Captain. Left out when none | `26930` |
| `...standings[].lineup.coach.id` | int, optional | Coach. Left out when none | `35316` |
| `...standings[].lineup.date` | int (Unix s) | When the lineup was saved | `1788461545` |
| `...standings[].lineup.count` | bool | Unknown, `true` for everyone | `true` |
| `data.league.settings.bonusPoint` | int (€) | Money per point scored in a round (inferred) | `30000` |
| `data.league.settings.bonusIdealLineup` / `bonusGameMVP` | int (€) | Bonus for being in the ideal lineup / match MVP (inferred) | `500000` / `250000` |

## Notes / quirks

- **This is where standings live**, not in the league endpoint.
- The lineup is the one that counts for the current round, saved earlier. It can be **stale**: our lineup includes player 17937, whom the [board](board.md) shows was taken from us by clause days later, and an empty slot (`null`).
- Rivals' lineups are visible here (`settings.lineupShow: "round"`), which gives their squads for F4 and F13.
- Team value is not in the account, so the dashboard reads it here.
- `settings` also holds lineup change rules (`lineupRoundChanges`, `lineupRoundChangesIn`) whose meaning is not confirmed.

## Sample

[samples/rounds-league.json](samples/rounds-league.json)

Anonymised: league id and name, manager ids, names and icons. Player and round ids are public and unchanged.
