import { useQuery } from '@tanstack/react-query'
import { fetchPlayer } from './api'

export const playerKeys = {
  all: ['player'] as const,
  detail: (playerId: number) =>
    [...playerKeys.all, 'detail', playerId] as const,
}

export function usePlayer(playerId: number) {
  return useQuery({
    queryKey: playerKeys.detail(playerId),
    queryFn: () => fetchPlayer(playerId),
    // The data is in the database: a 404 won't change by asking again.
    retry: false,
  })
}
