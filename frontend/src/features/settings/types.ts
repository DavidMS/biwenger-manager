/** `GET /api/account` (backend `AccountResponse`). */
export type ConnectedAccount = {
  accountName: string
  teamName: string
  leagueName: string
}

export type SyncTrigger = 'SCHEDULED' | 'MANUAL'
export type SyncResult = 'RUNNING' | 'SUCCEEDED' | 'FAILED'

/** A sync run. Fields that don't apply to its result are left out (backend `SyncStatusResponse`). */
export type SyncRun = {
  trigger: SyncTrigger
  result: SyncResult
  startedAt: string
  finishedAt?: string
  day?: string
  players?: number
  marketListings?: number
  standings?: number
  error?: string
}

/** `GET /api/sync/status`, and the body of `POST /api/sync`. */
export type SyncStatus = {
  running: boolean
  lastRun?: SyncRun
}
