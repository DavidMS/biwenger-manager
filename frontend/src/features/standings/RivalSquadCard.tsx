import { SectionCard } from '@/components/layout/SectionCard'
import { StatusBadge } from '@/components/player/StatusBadge'
import { BiwengerErrorState } from '@/components/state/BiwengerErrorState'
import { EmptyState } from '@/components/state/EmptyState'
import { LoadingState } from '@/components/state/LoadingState'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDateTime, formatMoney } from '@/lib/format'
import { positionLabels, positionRank } from '@/lib/player'
import { useRivalSquad } from './hooks'
import type { RivalPlayer } from './types'

type RivalSquadCardProps = {
  managerId: number
}

/** In lineup order: by position, then by player id. */
function byPosition(players: RivalPlayer[]): RivalPlayer[] {
  return players.toSorted(
    (a, b) =>
      positionRank(a.position) - positionRank(b.position) || a.id - b.id,
  )
}

/** One manager's squad and team value, as his saved lineup shows it. */
export function RivalSquadCard({ managerId }: RivalSquadCardProps) {
  const squad = useRivalSquad(managerId)

  if (squad.isPending) {
    return <LoadingState label="Loading the squad…" />
  }
  if (squad.isError) {
    return <BiwengerErrorState error={squad.error} />
  }
  const { name, teamValue, lineupSavedAt, players, missingPlayerIds } =
    squad.data
  return (
    <SectionCard
      title={`${name}'s squad`}
      description={`Team value ${formatMoney(teamValue)}. From the lineup saved ${formatDateTime(lineupSavedAt)}: players bought or sold since then are not reflected.`}
    >
      {players.length === 0 ? (
        <EmptyState>No players in this lineup.</EmptyState>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Player</TableHead>
              <TableHead>Position</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Value</TableHead>
              <TableHead>Points</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {byPosition(players).map((player) => (
              <TableRow key={player.id}>
                <TableCell className="font-medium">{player.name}</TableCell>
                <TableCell>{positionLabels[player.position]}</TableCell>
                <TableCell>
                  <StatusBadge status={player.status} />
                </TableCell>
                <TableCell className="tabular-nums">
                  {formatMoney(player.value)}
                </TableCell>
                <TableCell className="tabular-nums">{player.points}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {missingPlayerIds.length > 0 && (
        <p className="text-sm text-muted-foreground">
          Not shown, because the sync has never stored them (probably left La
          Liga): player ids {missingPlayerIds.join(', ')}.
        </p>
      )}
    </SectionCard>
  )
}
