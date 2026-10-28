import { useSearchParams } from 'react-router'
import { SectionCard } from '@/components/layout/SectionCard'
import { EmptyState } from '@/components/state/EmptyState'
import { BiwengerErrorState } from '@/components/state/BiwengerErrorState'
import { LoadingState } from '@/components/state/LoadingState'
import { useSquad } from './hooks'
import { firstOrder, parseSort, type SortKey } from './sortSquad'
import { SquadTable } from './SquadTable'

/** Your players in a sortable table. The sort is kept in the URL, so it survives a reload. */
export function SquadList() {
  const squad = useSquad()
  const [params, setParams] = useSearchParams()
  const sort = parseSort(params)

  const onSort = (key: SortKey) => {
    const order =
      key === sort.key
        ? sort.order === 'asc'
          ? 'desc'
          : 'asc'
        : firstOrder(key)
    // Replace, not push: Back leaves the page instead of stepping through every sort.
    setParams({ sort: key, order }, { replace: true })
  }

  if (squad.isPending) {
    return <LoadingState />
  }
  if (squad.isError) {
    return <BiwengerErrorState error={squad.error} />
  }
  const { players, missingPlayerIds } = squad.data
  return (
    <SectionCard
      title="Your players"
      description="Value and status from the last sync; the squad itself is live."
    >
      {players.length === 0 ? (
        <EmptyState>
          No players to show. If you own players, run a sync on the Settings
          page.
        </EmptyState>
      ) : (
        <SquadTable players={players} sort={sort} onSort={onSort} />
      )}
      {missingPlayerIds.length > 0 && (
        <p className="text-sm text-muted-foreground">
          Not shown, because they aren&apos;t in La Liga&apos;s player list
          (probably left the league): player ids {missingPlayerIds.join(', ')}.
        </p>
      )}
    </SectionCard>
  )
}
