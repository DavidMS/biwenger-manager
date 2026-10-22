# Player reports (CDN)

Returns one player's **per-match reports** this season: the match, home or away, events and the points under every scoring system. Used by **player detail** (F5: points per match) and by the **advisor** (F7, F10: form, home/away, minutes).

## Request

`GET https://cf.biwenger.com/api/v2/players/la-liga/{slug}?lang=es&fields=*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away))`

| Query parameter | Value | Meaning |
|---|---|---|
| `lang` | `es` | Language |
| `fields` | `*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away))` | Default fields plus `reports`, and inside each report the listed sub-fields |
| `season` | not sent | Omitted gives the current season |

Headers sent: `Accept: application/json`, `X-Lang: es`, `User-Agent`. No auth headers (public CDN).

The `fields` value must be URL-encoded by the client. **pybiwenger's string is missing the last `)`** (`...match(*,round,home,away)`); ours closes it. We have not checked how the server treats the unbalanced version.

## Response

Same player fields as [player-prices](player-prices.md) (without `prices`), plus:

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.reports[]` | array | One report per match played, oldest first | |
| `data.reports[].home` | bool | The player's team played at home | `false` |
| `data.reports[].match.id` / `.date` | int / int (Unix s) | Match | `50715`, `1786892400` |
| `data.reports[].match.status` | string | `"finished"` in all reports | |
| `data.reports[].match.home` / `.away` | object | `id`, `name`, `slug`, `score` | `{812, "Racing", ..., 2}` |
| `data.reports[].match.round` | object | `id`, `name`, `short`, optional `part` | `{4899, "Jornada 1", "J1"}` |
| `data.reports[].points` | object keyed by score id (string) | Points under each scoring system: `"1"`, `"2"`, `"3"`, `"5"`, `"6"`, `"7"`, `"8"` | `{"1": 2, "2": 3, ...}` |
| `data.reports[].events[]` | array | `type` (int), `period` (`"firstTime"` / `"secondTime"`), `metadata` (int, the minute, inferred) | `{4, "secondTime", 56}` |

## Notes / quirks

- **`points` holds every scoring system**, keyed by the catalog's `scores[].id` as strings. Pick the league's `scoreID`. Summing `points["1"]` over the reports gives the catalog's `points` (35) for player 2001.
- Event `type` codes are unknown. Type 4 at minutes 56 to 84 for a starting forward looks like "substituted off" and type 1 at minute 41 in his 9-point game like a goal (both inferred). Types 3 and 5 also appear.
- We asked for `status(status,statusInfo)` but no report has a `status`. It is probably only sent when the player was injured or suspended for that match (inferred).
- No `rawStats` here. pybiwenger reads `rawStats` (minutes played, picas, SofaScore rating, price) from reports, so a different `fields` value may return it. Worth a test capture: it would give minutes played.
- `match.summary` is sometimes `null` and sometimes missing. `match.preview` is `null`.
- There are 9 reports, one per club match so far (J1 to J9, with J6 played as its postponed part 2), and the player played all of them (`playedHome + playedAway == 9`). Whether a match he missed gets a report is unknown: capture a player with `null` in `fitness` to find out.
- `birthday` is zeroed by the anonymiser, as in player-prices.

## Sample

[samples/player-reports.json](samples/player-reports.json)

Anonymised: `birthday` set to `0`. Everything else is public player and match data.
