# Player prices (CDN)

Returns one player's details and their **daily value history** for about the last year. Used by **player detail** (F5: price chart), the value trend in **my squad** and **market** (F2, F3), and to seed the **price history** (F6) before our own daily snapshots exist.

## Request

`GET https://cf.biwenger.com/api/v2/players/la-liga/{slug}?lang=es&fields=*,prices`

| Query parameter | Value | Meaning |
|---|---|---|
| `lang` | `es` | Language |
| `fields` | `*,prices` | All default fields plus the `prices` history |
| `season` | not sent | Omitted gives the current season. pybiwenger sends `season={yyyy}` |

`{slug}` is the catalog's `slug` (for example `ayoze-perez`), not the player id.

Headers sent: `Accept: application/json`, `X-Lang: es`, `User-Agent`. **No `Authorization`, `X-League` or `X-User`**: this is a public CDN and it answered without them.

## Response

| JSON path | Type | Meaning | Example |
|---|---|---|---|
| `data.id` | int | Player id | `2001` |
| `data.slug` / `data.name` | string | Slug and display name | `"ayoze-perez"` |
| `data.position` | int | Position code, as in the catalog | `4` |
| `data.price` | int (€) | Current value. Equals the last `prices` entry | `4780000` |
| `data.priceIncrement` | int (€) | Last daily change | `30000` |
| `data.status` | string | Status, as in the catalog | `"ok"` |
| `data.prices` | array of `[date, price]` | Daily value history, oldest first | `[[251022, 6730000], ...]` |
| `data.prices[][0]` | int, `YYMMDD` | Day, as a 6-digit number: `251022` is 2025-10-22 | `261022` |
| `data.prices[][1]` | int (€) | Value on that day | `4780000` |
| `data.country` | string | Nationality, ISO code | `"ES"` |
| `data.canonicalURL` | string | Public player page on the blog | |

## Notes / quirks

- `prices` had **366 entries**, one per day from 2025-10-22 to 2026-10-22 (the capture day). It spans two seasons and the summer break. It looks like a rolling one-year window (inferred from one player).
- Dates are `YYMMDD` integers, not Unix timestamps. Parse as `20YY-MM-DD`. Two-digit years: fine until 2099.
- There is no `teamID` and no `fitness` or `points` here. Use the catalog for those.
- `birthday` is a real field (footballers' birth dates are public), but the anonymiser removes every `birthday` key, so it shows as `0` in the sample. Don't take `0` as the real type of an empty value: the real type is unknown (probably a number or a date string).
- The CDN response is cacheable and the same for everyone. Still keep calls rare: one player per request.

## Sample

[samples/player-prices.json](samples/player-prices.json)

Anonymised: `birthday` set to `0`. Everything else is public player data.
