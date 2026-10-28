import { Sparkline } from '@/components/player/Sparkline'
import { StatusBadge } from '@/components/player/StatusBadge'
import { SortableHead } from '@/components/table/SortableHead'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatMoney } from '@/lib/format'
import { positionLabels } from '@/lib/player'
import { RecentForm } from './RecentForm'
import { sortSquad, type Sort, type SortKey } from './sortSquad'
import type { SquadPlayer } from './types'

type SquadTableProps = {
  players: SquadPlayer[]
  sort: Sort
  onSort: (key: SortKey) => void
}

/** Your players, one row each, sorted by the column the user picked. */
export function SquadTable({ players, sort, onSort }: SquadTableProps) {
  const header = (key: SortKey, label: string) => (
    <SortableHead column={key} label={label} sort={sort} onSort={onSort} />
  )
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Player</TableHead>
          {header('position', 'Position')}
          <TableHead>Status</TableHead>
          {header('value', 'Value')}
          {/* The backend's window: Squad.TREND_DAYS. */}
          {header('trend', 'Trend (14 days)')}
          {header('points', 'Points')}
          <TableHead>Form</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {sortSquad(players, sort).map((player) => (
          <TableRow key={player.id}>
            <TableCell className="font-medium">{player.name}</TableCell>
            <TableCell>{positionLabels[player.position]}</TableCell>
            <TableCell>
              <StatusBadge status={player.status} />
            </TableCell>
            <TableCell className="tabular-nums">
              {formatMoney(player.price)}
            </TableCell>
            <TableCell>
              <Sparkline trend={player.trend} change={player.trendChange} />
            </TableCell>
            <TableCell className="tabular-nums">{player.points}</TableCell>
            <TableCell>
              <RecentForm form={player.form} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
