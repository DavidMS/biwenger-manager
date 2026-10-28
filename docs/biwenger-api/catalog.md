# Player catalog

Returns every La Liga player with current value, daily value change, status, recent form and points, plus all teams with their next game, the season's rounds and the active round's games. It is the main data source: **my squad** (F2) and **market** (F3) join on it, the **daily price snapshot** (F6) stores it, and the whole **advisor** (F7 to F10) reads it.

## Request

`GET https://biwenger.as.com/api/v2/competitions/la-liga/data?lang=es&score={scoreId}`

| Query parameter | Value | Meaning |
|---|---|---|
| `lang` | `es` | Language of names and `statusInfo` texts |
| `score` | the league's `scoreID` (`1` here) | Scoring system for `points`, `fitness` and the home/away splits |
| `season` | not sent | Omitted gives the current season. The catalog names seasons by end year (`"2027"` for 2026/2027), so that is probably the value to send (inferred) |

Headers sent: `Authorization: Bearer <token>`, `X-League`, `X-User`, `X-Lang: es`, `Accept: application/json`, `User-Agent`. The data is not league-specific, so it may work without auth (pybiwenger calls it the same way); untested.

## Response: players

`data.players` is an **object keyed by player id as a string**, not an array: `data.players["2001"].id == 2001`.

| JSON path (under `data.players["{id}"]`) | Type | Meaning | Example |
|---|---|---|---|
| `id` | int | Player id | `2001` |
| `name` | string | Short display name | `"Ayoze"` |
| `slug` | string | URL slug. Used for the CDN endpoints | `"ayoze-perez"` |
| `teamID` | int | Club id. Key into `data.teams` | `19` |
| `position` | int | `1` goalkeeper (PT), `2` defender (DF), `3` midfielder (MC), `4` forward (DL), `5` coach (E) | `4` |
| `altPositions` | int[], optional | Other positions allowed when the league has `lineupMultiPos` | `[4]` |
| `price` | int (€) | Current market value | `4780000` |
| `priceIncrement` | int (€) | Value change since the last daily update. Can be negative or `0` | `30000` |
| `status` | string | `"ok"`, `"injured"`, `"doubt"` seen. `"sanctioned"` appears in `fitness` so it is likely a status too | `"ok"` |
| `statusInfo` | string, optional | Spanish text about the injury or doubt. Only when status is not `ok` | `"Molestias musculares."` |
| `statusChange` | int (Unix s), optional | When the status last changed (inferred). Only with `statusInfo` | `1792609209` |
| `fitness` | array of 5 | Last five rounds, **most recent first**. Each item is the points (int), `null` (did not play) or a status string (`"injured"`, `"doubt"`, `"sanctioned"`) | `[6, 2, 9, 6, 0]` |
| `points` | int | Season points in the chosen score. Can be negative | `35` |
| `playedHome` / `playedAway` | int | Games played at home / away | `4` / `5` |
| `pointsHome` / `pointsAway` | int | Points at home / away. `pointsHome + pointsAway == points` | `21` / `14` |
| `pointsLastSeason` | int or `null` | Last season's points. `null` for players new to the competition | `73` |
| `number` | int | Shirt number. `0` for coaches | `22` |
| `fantasyPrice` | int | Unknown. Much larger than `price` (42 M vs 4.78 M); maybe a value from another game mode | `42000000` |
| `iconHero` | string, optional | Relative image path | `"i/p/hero/2001.png"` |

The `fitness` order was checked against player 2001's [reports](player-reports.md): J9 = 6, J8 = 2, J7 = 9, J6 (part 2) = 6, J5 = 0. The `points` total and home/away split also match the reports.

## Response: teams, rounds, games

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.teams["{id}"]` | object keyed by string id | The 20 La Liga clubs | |
| `data.teams["{id}"].name` / `.slug` | string | Club name and slug | `"Athletic"`, `"athletic"` |
| `data.teams["{id}"].nextGames[]` | array | Next game(s): `id`, `date` (Unix s), `home.id`, `away.id`, `round.id` | |
| `data.season.id` | **string** | Season, named by end year | `"2027"` |
| `data.season.rounds[]` | array | Every round: `id`, `name`, `short`, `status` (`finished`, `active`, `pending`), optional `part` | |
| `data.activeEvents[]` | array | The active round and the next one: `id`, `name`, `short`, `status`, `start`, `end` (Unix s) and `games[]`. `start` is left out for a round already under way. The dashboard's countdown reads the next one's `start` | `{4908, "Jornada 10", "pending", 1792782000}` |
| `data.activeEvents[].games[]` | array | `id`, `date`, `status` (`finished`, `pending`, `preview`), `home` / `away` with `id`, `name`, `score` (null before the game) and, for upcoming games, `difficulty.rating` (0 to 100) | |
| `data.scores[]` | array | Available scoring systems: `id`, `name`, `kind` | `{1, "Diario AS", "as"}` |
| `data.update` | int (Unix s) | Next catalog update, it seems: it was a day after the capture, 10 s after the market close (inferred) | `1792731610` |
| `data.etag` | int | Version of the data. Could drive caching (inferred) | `1360896726` |
| `data.scoreID` | int | Echo of the `score` parameter | `1` |

## Notes / quirks

- **Size.** The full response is about 230 KB with 551 players. The sample keeps 39 players: the 29 squad and market players that exist in the catalog, plus the 10 lowest other ids. So players in the sample are not in id order. The script also drops `meta.config` (ads, feature flags and app-store links, about 40 KB), and keeps `meta.sports` and `meta.competitions`.
- **Squad players can be missing.** Three of our 13 squad players are not in the catalog at all (probably players who left La Liga, inferred). Handle a missing entry.
- **Postponed rounds** are separate rounds with the same `short` name and a `part` number: `"Jornada 6 (aplazada)"` exists as part 2 and part 3, with their own ids. The active round in the sample is one of them (`5176`). `season.rounds` is sorted neither by id nor by round number (J6 comes before J4); it is probably calendar order (inferred).
- **Scores offered:** 1 Diario AS, 7 Feeberse Score, 8 Media AS y Feeberse, 2 SofaScore, 5 Media AS y SofaScore, 3 Estadísticas, 6 Biwenger Social. No "Jornada Perfecta" option appears, unlike the plan's list.
- Formations, positions and lineup sizes are in `meta.sports[0]` (`football`): 11 players, standard and premium formations (for example `3-3-4`, used by a rival), and the position names above.
- `difficulty` has extra arrays (`standings`, `homeAway`, `form`, `goalDiff`) whose meaning is not known.
- Footballers' social handles would be kept by the anonymiser, but none appear in the catalog.

## Sample

[samples/catalog.json](samples/catalog.json)

Anonymised: nothing replaced, the catalog has only public data. Trimmed to 39 of 551 players as described above.
