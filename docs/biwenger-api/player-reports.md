# Player reports (CDN)

Returns one player's **per-match reports** this season: the match, home or away, events and the points under every scoring system. Used by **player detail** (F5: points per match) and by the **advisor** (F7, F10: form, home/away, minutes).

## Request

`GET https://cf.biwenger.com/api/v2/players/la-liga/{slug}?lang=es&fields=*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away),rawStats)`

| Query parameter | Value | Meaning |
|---|---|---|
| `lang` | `es` | Language |
| `fields` | `*,reports(points,home,events,status(status,statusInfo),match(*,round,home,away),rawStats)` | Default fields plus `reports`, and inside each report the listed sub-fields. `rawStats` must be asked for explicitly (#17) |
| `season` | not sent | Omitted gives the current season |

Headers sent: `Accept: application/json`, `X-Lang: es`, `User-Agent`. No auth headers (public CDN).

The `fields` value must be URL-encoded by the client. **pybiwenger's string is missing the last `)`** (`...match(*,round,home,away)`); ours closes it. We have not checked how the server treats the unbalanced version.

## Response

Same player fields as [player-prices](player-prices.md) (without `prices`), plus:

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.reports[]` | array | One report per **club match**, played or not, in date order (a postponed round comes when it was played) | |
| `data.reports[].home` | bool | The player's team played at home | `false` |
| `data.reports[].match.id` / `.date` | int / int (Unix s) | Match | `50715`, `1786892400` |
| `data.reports[].match.status` | string | `"finished"` in all reports | |
| `data.reports[].status.status` | string, optional | Only when he **didn't** play: `injured`, `doubt`, `sanctioned`, or `ok` (fit but not picked) | `"injured"` |
| `data.reports[].rawStats` | object, optional | Only when he played | |
| `data.reports[].rawStats.minutesPlayed` | int | Minutes on the pitch | `56` |
| `data.reports[].rawStats.*` | int | Also seen: `goals`, `assists`, `picas`, `homeScore`, `awayScore`, `cleanSheet`, `lost`, `price`, `roundPhase`, `score1`…`score8`, `pos4`, `home`/`away`. Only what happened is sent (no `goals: 0`) | |
| `data.reports[].match.home` / `.away` | object | `id`, `name`, `slug`, `score` | `{812, "Racing", ..., 2}` |
| `data.reports[].match.round` | object | `id`, `name`, `short`, optional `part` | `{4899, "Jornada 1", "J1"}` |
| `data.reports[].points` | object keyed by score id (string) | Points under each scoring system: `"1"`, `"2"`, `"3"`, `"5"`, `"6"`, `"7"`, `"8"` | `{"1": 2, "2": 3, ...}` |
| `data.reports[].events[]` | array | `type` (int), `period` (`"firstTime"` / `"secondTime"`), `metadata` (int, the minute, inferred) | `{4, "secondTime", 56}` |

## Notes / quirks

- **`points` holds every scoring system**, keyed by the catalog's `scores[].id` as strings. Pick the league's `scoreID`. Summing `points["1"]` over the reports gives the catalog's `points` (35) for player 2001.
- Event `type` codes are unknown. Type 4 at minutes 56 to 84 for a starting forward looks like "substituted off" and type 1 at minute 41 in his 9-point game like a goal (both inferred). Types 3 and 5 also appear.
- **A match he didn't play still has a report**, with only `match`, `home` and `status`: no `points`, `rawStats` or `events`. The second sample (Pablo Marín) shows `injured`, `doubt`, `sanctioned` and `ok`; `ok` matches a `null` in the catalog's `fitness`.
- `status` never had a `statusInfo`, although we asked for it.
- **`rawStats` must be in `fields`.** The first capture (#14) left it out and got none. pybiwenger reads `rawStats.minutesPlayed` with a `fields` string that lacks its last `)`; the server may fall back to all fields then (not tested).
- `match.summary` is sometimes `null` and sometimes missing. `match.preview` is `null`.
- Ayoze has 9 reports, one per club match so far (J1 to J9, J6 as its postponed part 2), and played all of them. Pablo Marín also has 9 and played one (J8, 66 minutes, −4 points). His postponed J1 (part 2) comes after J2: the order is by date, not by round.
- `birthday` is zeroed by the anonymiser, as in player-prices.

## Samples

- [samples/player-reports.json](samples/player-reports.json): Ayoze, played every match.
- [samples/player-reports-missed-matches.json](samples/player-reports-missed-matches.json): Pablo Marín, missed eight matches (injured, doubt, sanctioned, not picked).

Both captured in #17 with `python3 scripts/biwenger_capture.py --cdn` (public CDN, no login). Anonymised: `birthday` set to `0`. Everything else is public player and match data.
