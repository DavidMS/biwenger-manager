import { useSearchParams } from 'react-router'
import { SectionCard } from '@/components/layout/SectionCard'
import { BiwengerErrorState } from '@/components/state/BiwengerErrorState'
import { EmptyState } from '@/components/state/EmptyState'
import { LoadingState } from '@/components/state/LoadingState'
import { useMarket } from './hooks'
import { MarketFilters } from './MarketFilters'
import { MarketTable } from './MarketTable'
import {
  applyView,
  filterNames,
  parseView,
  type FilterName,
  type SortKey,
} from './marketView'

/** Today's market, filtered and sorted. Filters and sort are kept in the URL. */
export function MarketList() {
  const market = useMarket()
  const [params, setParams] = useSearchParams()
  const parsed = parseView(params)

  // Replace, not push: Back leaves the page instead of stepping through every filter.
  const update = (changes: Record<string, string>) =>
    setParams(
      (current) => {
        const next = new URLSearchParams(current)
        for (const [name, value] of Object.entries(changes)) {
          if (value === '') {
            next.delete(name)
          } else {
            next.set(name, value)
          }
        }
        return next
      },
      { replace: true },
    )
  const onSort = (key: SortKey) =>
    update({
      sort: key,
      // A new column starts with the biggest first; the same column flips.
      order: key === parsed.sort && parsed.order === 'desc' ? 'asc' : 'desc',
    })
  const onClear = () =>
    update(Object.fromEntries(filterNames.map((name) => [name, ''])))

  if (market.isPending) {
    return <LoadingState />
  }
  if (market.isError) {
    return <BiwengerErrorState error={market.error} />
  }
  const { players, teams, missingPlayerIds } = market.data
  // A team that isn't a club (a stale or mistyped link) filters nothing, so the select and the
  // list agree.
  const view = teams.some((team) => team.id === parsed.teamId)
    ? parsed
    : { ...parsed, teamId: undefined }
  const shown = applyView(players, view)
  // The selects show the view; the price inputs show what was typed, so "1." stays while typing.
  const values: Record<FilterName, string> = {
    position: view.position ?? '',
    team: view.teamId === undefined ? '' : String(view.teamId),
    status: view.status ?? '',
    minPrice: params.get('minPrice') ?? '',
    maxPrice: params.get('maxPrice') ?? '',
  }
  return (
    <SectionCard
      title="Players on sale"
      description={`${shown.length} of ${players.length} players. The listings are live; status, trend and points are from the last sync.`}
    >
      {players.length > 0 && (
        <MarketFilters
          values={values}
          teams={teams}
          onChange={(name, value) => update({ [name]: value })}
          onClear={onClear}
        />
      )}
      {players.length === 0 ? (
        <EmptyState>Nobody is on sale right now.</EmptyState>
      ) : shown.length === 0 ? (
        <EmptyState>No player matches these filters.</EmptyState>
      ) : (
        <MarketTable
          players={shown}
          teamNames={new Map(teams.map((team) => [team.id, team.name]))}
          sort={{ key: view.sort, order: view.order }}
          onSort={onSort}
        />
      )}
      {missingPlayerIds.length > 0 && (
        <p className="text-sm text-muted-foreground">
          Not shown, because the last sync hasn&apos;t stored them yet: player
          ids {missingPlayerIds.join(', ')}.
        </p>
      )}
    </SectionCard>
  )
}
