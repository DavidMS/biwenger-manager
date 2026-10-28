export type Position =
  'GOALKEEPER' | 'DEFENDER' | 'MIDFIELDER' | 'FORWARD' | 'COACH'

export type PlayerStatus =
  'AVAILABLE' | 'DOUBTFUL' | 'INJURED' | 'SUSPENDED' | 'UNKNOWN'

/** A player's value on one day (backend `SquadResponse.PricePoint`). `day` is `YYYY-MM-DD`. */
export type PricePoint = {
  day: string
  price: number
}

/**
 * One recent match (backend `SquadResponse.Match`): played with `points`, or missed with the
 * `reason` (`AVAILABLE` means fit but not picked).
 */
export type Match =
  { played: true; points: number } | { played: false; reason: PlayerStatus }

/** One of your players (backend `SquadResponse.Player`). Money is whole euros. */
export type SquadPlayer = {
  id: number
  name: string
  slug: string
  teamId: number
  position: Position
  status: PlayerStatus
  price: number
  /** Value change over `trend`: last day minus first. */
  trendChange: number
  /** Value per day over the last 14 days, oldest first. */
  trend: PricePoint[]
  points: number
  /** The last matches, latest first. */
  form: Match[]
}

/**
 * `GET /api/squad` (backend `SquadResponse`). `missingPlayerIds` are players you own that the
 * daily sync has never stored (players who left La Liga).
 */
export type Squad = {
  players: SquadPlayer[]
  missingPlayerIds: number[]
}
