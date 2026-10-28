import type { PlayerStatus, Position, PricePoint } from '@/lib/player'

/**
 * One club match (backend `PlayerDetailResponse.Match`): played, with points and minutes, or
 * missed, with the reason (`AVAILABLE` means fit but not picked).
 */
export type PlayerMatch = {
  matchId: number
  roundId: number
  date: string
  home: boolean
} & (
  | { played: true; points: number; minutes: number }
  | { played: false; reason: PlayerStatus }
)

/**
 * `GET /api/players/{playerId}` (backend `PlayerDetailResponse`). Money is whole euros. `prices`
 * oldest first, `matches` in the order they were played. `teamName` is left out when the club
 * isn't stored yet.
 */
export type PlayerDetail = {
  id: number
  name: string
  teamId: number
  teamName?: string
  position: Position
  status: PlayerStatus
  value: number
  points: number
  prices: PricePoint[]
  matches: PlayerMatch[]
}
