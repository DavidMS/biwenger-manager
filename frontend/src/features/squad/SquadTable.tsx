import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatMoney } from '@/lib/format'
import { RecentForm } from './RecentForm'
import { sortSquad, type Sort, type SortKey } from './sortSquad'
import { Sparkline } from './Sparkline'
import { StatusBadge } from './StatusBadge'
import type { Position, SquadPlayer } from './types'

type SquadTableProps = {
  players: SquadPlayer[]
  sort: Sort
  onSort: (key: SortKey) => void
}

const positionLabels: Record<Position, string> = {
  GOALKEEPER: 'Goalkeeper',
  DEFENDER: 'Defender',
  MIDFIELDER: 'Midfielder',
  FORWARD: 'Forward',
  COACH: 'Coach',
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

type SortableHeadProps = {
  column: SortKey
  label: string
  sort: Sort
  onSort: (key: SortKey) => void
}

function SortableHead({ column, label, sort, onSort }: SortableHeadProps) {
  const active = sort.key === column
  const Icon = !active
    ? ArrowUpDown
    : sort.order === 'asc'
      ? ArrowUp
      : ArrowDown
  return (
    <TableHead
      aria-sort={
        active ? (sort.order === 'asc' ? 'ascending' : 'descending') : 'none'
      }
    >
      <Button
        variant="ghost"
        size="sm"
        className="-ml-2"
        onClick={() => onSort(column)}
      >
        {label}
        <Icon aria-hidden="true" />
      </Button>
    </TableHead>
  )
}
