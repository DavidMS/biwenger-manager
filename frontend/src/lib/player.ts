/** What every feature that shows players shares: positions, statuses and the value trend. */

export type Position =
  'GOALKEEPER' | 'DEFENDER' | 'MIDFIELDER' | 'FORWARD' | 'COACH'

export type PlayerStatus =
  'AVAILABLE' | 'DOUBTFUL' | 'INJURED' | 'SUSPENDED' | 'UNKNOWN'

/** A player's value on one day. `day` is `YYYY-MM-DD`. */
export type PricePoint = {
  day: string
  price: number
}

/** Positions in lineup order. */
export const positions = [
  'GOALKEEPER',
  'DEFENDER',
  'MIDFIELDER',
  'FORWARD',
  'COACH',
] as const satisfies readonly Position[]

export const positionLabels: Record<Position, string> = {
  GOALKEEPER: 'Goalkeeper',
  DEFENDER: 'Defender',
  MIDFIELDER: 'Midfielder',
  FORWARD: 'Forward',
  COACH: 'Coach',
}

export const statuses = [
  'AVAILABLE',
  'DOUBTFUL',
  'INJURED',
  'SUSPENDED',
  'UNKNOWN',
] as const satisfies readonly PlayerStatus[]

export const statusLabels: Record<PlayerStatus, string> = {
  AVAILABLE: 'Available',
  DOUBTFUL: 'Doubtful',
  INJURED: 'Injured',
  SUSPENDED: 'Suspended',
  UNKNOWN: 'Unknown status',
}

/** A position's place in the lineup order, for sorting. */
export function positionRank(position: Position): number {
  return positions.indexOf(position)
}
