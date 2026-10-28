import { useQuery } from '@tanstack/react-query'
import { fetchMarket } from './api'

export const marketKeys = {
  all: ['market'] as const,
  today: () => [...marketKeys.all, 'today'] as const,
}

export function useMarket() {
  return useQuery({
    queryKey: marketKeys.today(),
    queryFn: fetchMarket,
    // As on the dashboard: a 503 or a 502 won't be fixed by asking again at once.
    retry: false,
  })
}
