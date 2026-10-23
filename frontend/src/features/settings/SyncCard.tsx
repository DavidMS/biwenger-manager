import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { ApiError } from '@/lib/api'
import { formatDateTime, formatDay } from '@/lib/format'
import { useStartSync, useSyncStatus } from './hooks'
import type { SyncRun } from './types'

export function SyncCard() {
  const status = useSyncStatus()
  const start = useStartSync()
  const running = status.data?.running ?? false
  const alreadyRunning =
    start.error instanceof ApiError && start.error.status === 409

  return (
    <Card>
      <CardHeader>
        <CardTitle>Data sync</CardTitle>
        <CardDescription>
          Every morning at 07:30 the app reads the catalog, the market and the
          standings from Biwenger. It only reads.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {status.isPending && <p className="text-muted-foreground">Loading…</p>}
        {status.isError && (
          <Alert variant="destructive">
            <AlertTitle>Sync status unavailable</AlertTitle>
            <AlertDescription>{status.error.message}</AlertDescription>
          </Alert>
        )}
        {status.isSuccess &&
          (status.data.lastRun ? (
            <LastRun run={status.data.lastRun} />
          ) : (
            <p className="text-muted-foreground">No sync has run yet.</p>
          ))}
        {start.isError && !alreadyRunning && (
          <Alert variant="destructive">
            <AlertTitle>Sync not started</AlertTitle>
            <AlertDescription>{start.error.message}</AlertDescription>
          </Alert>
        )}
        {alreadyRunning && (
          <p className="text-muted-foreground">A sync is already running.</p>
        )}
        <Button
          onClick={() => start.mutate()}
          disabled={running || start.isPending}
        >
          {running ? 'Syncing…' : 'Sync now'}
        </Button>
      </CardContent>
    </Card>
  )
}

type LastRunProps = {
  run: SyncRun
}

function LastRun({ run }: LastRunProps) {
  return (
    <div className="space-y-2">
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
    </div>
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
