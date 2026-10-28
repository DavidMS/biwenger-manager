import type { PlayerStatus, Position } from '@/lib/player'

/** A manager's row (backend `StandingsResponse.Row`). Money is whole euros. */
export type StandingRow = {
  managerId: number
  name: string
  position: number
  points: number
  teamValue: number
  teamValueChange: number
  /** Your own row. */
  you: boolean
}

/** `GET /api/standings` (backend `StandingsResponse`): by position. */
export type Standings = {
  standings: StandingRow[]
}

/** A player in a manager's squad (backend `RivalSquadResponse.Player`). */
export type RivalPlayer = {
  id: number
  name: string
  teamId: number
  position: Position
  status: PlayerStatus
  value: number
  points: number
}

/**
 * `GET /api/standings/{managerId}/squad` (backend `RivalSquadResponse`): the squad as the lineup
 * saved at `lineupSavedAt` shows it, so players bought or sold since are not reflected.
 */
export type RivalSquad = {
  managerId: number
  name: string
  position: number
  teamValue: number
  lineupSavedAt: string
  players: RivalPlayer[]
  missingPlayerIds: number[]
}
