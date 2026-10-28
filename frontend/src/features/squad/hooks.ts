import { useQuery } from '@tanstack/react-query'
import { fetchSquad } from './api'

export const squadKeys = {
  all: ['squad'] as const,
  list: () => [...squadKeys.all, 'list'] as const,
}

export function useSquad() {
  return useQuery({
    queryKey: squadKeys.list(),
    queryFn: fetchSquad,
    // As on the dashboard: a 503 or a 502 won't be fixed by asking again at once.
    retry: false,
  })
}
