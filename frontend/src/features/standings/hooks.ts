import { useQuery } from '@tanstack/react-query'
import { fetchRivalSquad, fetchStandings } from './api'

export const standingsKeys = {
  all: ['standings'] as const,
  table: () => [...standingsKeys.all, 'table'] as const,
  squad: (managerId: number) =>
    [...standingsKeys.all, 'squad', managerId] as const,
}

// As on the dashboard: a 503, a 502 or a 404 won't be fixed by asking again at once.

export function useStandings() {
  return useQuery({
    queryKey: standingsKeys.table(),
    queryFn: fetchStandings,
    retry: false,
  })
}

export function useRivalSquad(managerId: number) {
  return useQuery({
    queryKey: standingsKeys.squad(managerId),
    queryFn: () => fetchRivalSquad(managerId),
    retry: false,
  })
}
