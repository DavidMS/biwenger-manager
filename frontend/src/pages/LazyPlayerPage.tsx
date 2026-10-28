import { lazy, Suspense } from 'react'
import { LoadingState } from '@/components/state/LoadingState'

// The player page is the only one with a chart: loading it on demand keeps Recharts (about
// 100 KB gzipped) out of the first download.
const PlayerPage = lazy(() =>
  import('@/pages/PlayerPage').then((module) => ({
    default: module.PlayerPage,
  })),
)

export function LazyPlayerPage() {
  return (
    <Suspense fallback={<LoadingState />}>
      <PlayerPage />
    </Suspense>
  )
}
