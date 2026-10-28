import { useSearchParams } from 'react-router'
import { SectionCard } from '@/components/layout/SectionCard'
import { BiwengerErrorState } from '@/components/state/BiwengerErrorState'
import { EmptyState } from '@/components/state/EmptyState'
import { LoadingState } from '@/components/state/LoadingState'
import { useStandings } from './hooks'
import { RivalSquadCard } from './RivalSquadCard'
import { StandingsTable } from './StandingsTable'

/** The league table and, for the manager picked in the URL (`?manager=`), his squad. */
export function StandingsView() {
  const standings = useStandings()
  const [params] = useSearchParams()

  if (standings.isPending) {
    return <LoadingState />
  }
  if (standings.isError) {
    return <BiwengerErrorState error={standings.error} />
  }
  const rows = standings.data.standings
  // Only a manager in the table: a stale or mistyped link shows no squad.
  const selected = rows.find(
    (row) => String(row.managerId) === params.get('manager'),
  )
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <SectionCard
        title="League table"
        description="Pick a manager to see their squad."
      >
        {rows.length === 0 ? (
          <EmptyState>The league has no standings yet.</EmptyState>
        ) : (
          <StandingsTable
            standings={rows}
            selectedManagerId={selected?.managerId}
          />
        )}
      </SectionCard>
      {selected && (
        <RivalSquadCard
          key={selected.managerId}
          managerId={selected.managerId}
        />
      )}
    </div>
  )
}
