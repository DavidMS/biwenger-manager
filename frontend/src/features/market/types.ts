import type { PlayerStatus, Position, PricePoint } from '@/lib/player'

/** Who sells (backend `MarketResponse.Seller`): the game, or a manager. */
export type Seller =
  { free: true } | { free: false; managerId: number; managerName: string }

/** A player on sale (backend `MarketResponse.Player`). Money is whole euros. */
export type MarketPlayer = {
  id: number
  name: string
  teamId: number
  position: Position
  status: PlayerStatus
  /** Asking price. */
  price: number
  /** The player's value, which can differ from the asking price. */
  value: number
  /** Value change over `trend`: last day minus first. */
  trendChange: number
  /** Value per day over the last 14 days, oldest first. */
  trend: PricePoint[]
  points: number
  /** Season points per million euros of asking price, to two decimals. */
  pointsPerMillion: number
  closesAt: string
  seller: Seller
}

/** A club (backend `MarketResponse.TeamItem`). */
export type Team = {
  id: number
  name: string
}

/**
 * `GET /api/market` (backend `MarketResponse`). `teams` are every club, by name.
 * `missingPlayerIds` are players on sale that the daily sync has never stored.
 */
export type Market = {
  players: MarketPlayer[]
  teams: Team[]
  missingPlayerIds: number[]
}
