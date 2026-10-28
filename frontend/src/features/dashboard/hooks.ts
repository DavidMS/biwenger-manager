import { useQuery } from '@tanstack/react-query'
import { fetchDashboard } from './api'

export const dashboardKeys = {
  all: ['dashboard'] as const,
  summary: () => [...dashboardKeys.all, 'summary'] as const,
}

export function useDashboard() {
  return useQuery({
    queryKey: dashboardKeys.summary(),
    queryFn: fetchDashboard,
    // A 503 (not connected) won't fix itself, and a 502 comes after the backend has already
    // retried Biwenger for up to ~80 s: show either at once instead of trying again.
    retry: false,
  })
}
