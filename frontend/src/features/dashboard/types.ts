/** The next round and when its first match kicks off (backend `DashboardResponse.NextRound`). */
export type NextRound = {
  name: string
  startsAt: string
}

/**
 * `GET /api/dashboard` (backend `DashboardResponse`). Money is whole euros. `nextRound` is left out
 * when no upcoming round has a known start.
 */
export type Dashboard = {
  balance: number
  balanceNegative: boolean
  points: number
  position: number
  managers: number
  teamValue: number
  teamValueChange: number
  nextRound?: NextRound
}
