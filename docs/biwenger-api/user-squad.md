# My squad

Returns the players you own in the league, with the date you got each one and its release clause. Used by **my squad** (F2), **sell recommendations** (F9), the **lineup optimizer** (F10) and **clause watch** (F13). Player details come from the [catalog](catalog.md), joined by id.

## Request

`GET https://biwenger.as.com/api/v2/user?fields=players(id,owner)`

Headers sent: `Authorization: Bearer <token>`, `X-League: {leagueId}`, `X-User: {userId}`, `X-Lang: es`, `Accept: application/json`, `User-Agent`.

| Query parameter | Value | Meaning |
|---|---|---|
| `fields` | `players(id,owner)` | Field selector: only the `players` list, and in it only `id` and `owner` |

The user comes from the `X-User` / `X-League` headers, not from the URL.

## Response

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.players[]` | array | Your squad (13 players in the sample) | |
| `data.players[].id` | int | Player id. Key into `catalog.data.players` (as a string) | `2001` |
| `data.players[].owner.date` | int (Unix s) | When you got the player | `1785356515` |
| `data.players[].owner.clause` | int (€) | Release clause another manager must pay to take the player | `9560000` |

## Notes / quirks

- The `fields` selector works: the response holds only what was asked for. Other `fields` values may expose more (`owner.price`, for example, which the market shows for other managers' players). Not tried.
- All 13 players share the same `owner.date` (2026-07-29). It looks like the date the squad was assigned at the start of the season rather than a real purchase (inferred).
- No purchase price here, unlike `owner` in the market (`owner.price`).
- **Three squad players (33691, 34181, 41090) are not in the catalog.** The script kept every squad id that the catalog had, so they were missing from the full catalog too. They are probably players who left La Liga (inferred). The client must handle a squad player with no catalog entry.
- The squad can include coaches (catalog `position: 5`).

## Sample

[samples/user-squad.json](samples/user-squad.json)

Anonymised: nothing needed. The response holds only public player ids, dates and clauses. It was still checked for leaks.
