import { QueryClient } from '@tanstack/react-query'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Data comes from our own backend, which syncs with Biwenger on a schedule.
        staleTime: 60_000,
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  })
}
