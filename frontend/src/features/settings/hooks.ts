import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { fetchAccount, fetchSyncStatus, startSync } from './api'
import type { SyncStatus } from './types'

export const settingsKeys = {
  all: ['settings'] as const,
  account: () => [...settingsKeys.all, 'account'] as const,
  syncStatus: () => [...settingsKeys.all, 'sync-status'] as const,
}

/** How often to ask for the status while a sync runs. */
export const POLL_MS = 2_000

/** Poll while a sync runs, and stop once it has finished. */
export function pollWhileRunning(
  status: SyncStatus | undefined,
): number | false {
  return status?.running ? POLL_MS : false
}

export function useConnectedAccount() {
  return useQuery({
    queryKey: settingsKeys.account(),
    queryFn: fetchAccount,
    // "Not connected" won't fix itself in a second: show it at once instead of retrying.
    retry: false,
  })
}

export function useSyncStatus() {
  return useQuery({
    queryKey: settingsKeys.syncStatus(),
    queryFn: fetchSyncStatus,
    refetchInterval: (query) => pollWhileRunning(query.state.data),
  })
}

export function useStartSync() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => startSync(),
    onSuccess: (status) => {
      // The answer is the new status: show "running" at once, and the polling takes over.
      queryClient.setQueryData(settingsKeys.syncStatus(), status)
    },
    onSettled: () =>
      queryClient.invalidateQueries({ queryKey: settingsKeys.syncStatus() }),
  })
}

/**
 * The sync's status, the "Sync now" action, and what happens when a run finishes: an old start
 * error no longer applies, and every other view refreshes, because the sync changed the data
 * they show.
 */
export function useSync() {
  const queryClient = useQueryClient()
  const status = useSyncStatus()
  const start = useStartSync()
  const running = status.data?.running ?? false
  const wasRunning = useRef(running)
  const { reset } = start

  useEffect(() => {
    if (wasRunning.current && !running) {
      reset()
      void queryClient.invalidateQueries({
        predicate: (query) =>
          query.queryKey[1] !== settingsKeys.syncStatus()[1],
      })
    }
    wasRunning.current = running
  }, [running, reset, queryClient])

  return { status, start, running }
}
