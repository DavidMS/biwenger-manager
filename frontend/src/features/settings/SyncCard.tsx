import { SectionCard } from '@/components/layout/SectionCard'
import { EmptyState } from '@/components/state/EmptyState'
import { ErrorState } from '@/components/state/ErrorState'
import { LoadingState } from '@/components/state/LoadingState'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ApiError } from '@/lib/api'
import { formatDateTime, formatDay } from '@/lib/format'
import { useSync } from './hooks'
import type { SyncRun } from './types'

export function SyncCard() {
  const { status, start, running } = useSync()
  const busy = running || start.isPending
  const alreadyRunning =
    start.error instanceof ApiError && start.error.status === 409

  return (
    <SectionCard
      title="Data sync"
      description="Every morning at 07:30 the app reads the catalog, the market and the standings from Biwenger. It only reads."
    >
      {/* Announced when it changes: "Running", then the result. */}
      <div role="status" className="space-y-2">
        {status.isPending && <LoadingState />}
        {status.isError && (
          <ErrorState error={status.error} title="Sync status unavailable" />
        )}
        {status.isSuccess &&
          (status.data.lastRun ? (
            <LastRun run={status.data.lastRun} />
          ) : (
            <EmptyState>No sync has run yet.</EmptyState>
          ))}
        {alreadyRunning && running && (
          <p className="text-muted-foreground">A sync is already running.</p>
        )}
      </div>
      {start.isError && !alreadyRunning && (
        <ErrorState error={start.error} title="Sync not started" />
      )}
      {/* aria-disabled, not disabled: the button keeps focus while the sync runs. */}
      <Button
        aria-disabled={busy}
        onClick={() => {
          if (!busy) start.mutate()
        }}
      >
        {running ? 'Syncing…' : 'Sync now'}
      </Button>
    </SectionCard>
  )
}

type LastRunProps = {
  run: SyncRun
}

function LastRun({ run }: LastRunProps) {
  return (
    <>
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-muted-foreground">Last sync</span>
        <ResultBadge run={run} />
        <span>{formatDateTime(run.finishedAt ?? run.startedAt)}</span>
        <span className="text-muted-foreground">
          ({run.trigger === 'MANUAL' ? 'started by you' : 'scheduled'})
        </span>
      </div>
      {run.result === 'SUCCEEDED' && (
        <p>
          Stored {run.day ? formatDay(run.day) : 'the day'}: {run.players}{' '}
          players, {run.marketListings} market listings, {run.standings}{' '}
          standings.
        </p>
      )}
      {run.result === 'FAILED' && (
        <p className="text-destructive">{run.error}</p>
      )}
    </>
  )
}

function ResultBadge({ run }: LastRunProps) {
  switch (run.result) {
    case 'RUNNING':
      return <Badge variant="secondary">Running</Badge>
    case 'SUCCEEDED':
      return <Badge>Succeeded</Badge>
    case 'FAILED':
      return <Badge variant="destructive">Failed</Badge>
  }
}
