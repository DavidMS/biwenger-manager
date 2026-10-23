import { getJson, postJson } from '@/lib/api'
import type { ConnectedAccount, SyncStatus } from './types'

export function fetchAccount(): Promise<ConnectedAccount> {
  return getJson<ConnectedAccount>('/api/account')
}

export function fetchSyncStatus(): Promise<SyncStatus> {
  return getJson<SyncStatus>('/api/sync/status')
}

/** Starts a sync in the background. Rejects with a 409 `ApiError` if one is already running. */
export function startSync(): Promise<SyncStatus> {
  return postJson<SyncStatus>('/api/sync')
}
