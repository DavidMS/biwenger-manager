import type { ReactNode } from 'react'
import { PageHeader } from '@/components/layout/PageHeader'
import { SectionCard } from '@/components/layout/SectionCard'
import { StatusBadge } from '@/components/player/StatusBadge'
import { EmptyState } from '@/components/state/EmptyState'
import { ErrorState } from '@/components/state/ErrorState'
import { LoadingState } from '@/components/state/LoadingState'
import { ApiError } from '@/lib/api'
import { formatMoney } from '@/lib/format'
import { positionLabels } from '@/lib/player'
import { usePlayer } from './hooks'
import { MatchesTable } from './MatchesTable'
import { PriceChart } from './PriceChart'

type PlayerDetailViewProps = {
  playerId: number
}

/** One player: status, value history and points per match, all from the last sync. */
export function PlayerDetailView({ playerId }: PlayerDetailViewProps) {
  const player = usePlayer(playerId)

  if (player.isPending) {
    return (
      <PlaceholderPage>
        <LoadingState />
      </PlaceholderPage>
    )
  }
  if (player.isError) {
    const notFound =
      player.error instanceof ApiError && player.error.status === 404
    return (
      <PlaceholderPage>
        <ErrorState
          error={player.error}
          hint={
            notFound
              ? 'The sync stores every La Liga player: if this one is new, run a sync on the Settings page.'
              : undefined
          }
        />
      </PlaceholderPage>
    )
  }
  const { name, teamName, position, status, value, points, prices, matches } =
    player.data
  return (
    <div className="space-y-6">
      <PageHeader
        title={name}
        description={[teamName, positionLabels[position]]
          .filter(Boolean)
          .join(' · ')}
      />
      <SectionCard title="Status">
        <dl className="grid grid-cols-[max-content_1fr] items-center gap-x-6 gap-y-2">
          <dt className="text-muted-foreground">Status</dt>
          <dd>
            <StatusBadge status={status} />
          </dd>
          <dt className="text-muted-foreground">Value</dt>
          <dd className="tabular-nums">{formatMoney(value)}</dd>
          <dt className="text-muted-foreground">Season points</dt>
          <dd className="tabular-nums">{points}</dd>
        </dl>
      </SectionCard>
      <SectionCard title="Value history">
        {prices.length < 2 ? (
          <EmptyState>Not enough value history yet.</EmptyState>
        ) : (
          <PriceChart prices={prices} />
        )}
      </SectionCard>
      <SectionCard title="Points per match">
        {matches.length === 0 ? (
          <EmptyState>No matches yet.</EmptyState>
        ) : (
          <MatchesTable matches={matches} />
        )}
      </SectionCard>
    </div>
  )
}

/** The page's heading while the player loads or fails, so it always has one. */
function PlaceholderPage({ children }: { children: ReactNode }) {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Player"
        description="Value history, points per match and status."
      />
      {children}
    </div>
  )
}
