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
import { formatDateTime, formatMoney } from '@/lib/format'
import { positionLabels } from '@/lib/player'
import type { SortOrder } from '@/components/table/SortableHead'
import type { SortKey } from './marketView'
import type { MarketPlayer, Seller } from './types'

type MarketTableProps = {
  players: MarketPlayer[]
  teamNames: ReadonlyMap<number, string>
  sort: { key: SortKey; order: SortOrder }
  onSort: (key: SortKey) => void
}

const pointsPerMillion = new Intl.NumberFormat('en-GB', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Players on sale, in the order given: filtering and sorting happen before. */
export function MarketTable({
  players,
  teamNames,
  sort,
  onSort,
}: MarketTableProps) {
  const header = (key: SortKey, label: string) => (
    <SortableHead column={key} label={label} sort={sort} onSort={onSort} />
  )
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Player</TableHead>
          <TableHead>Team</TableHead>
          <TableHead>Position</TableHead>
          <TableHead>Status</TableHead>
          {header('price', 'Price')}
          {/* The backend's window: Market.TREND_DAYS. */}
          {header('trend', 'Trend (14 days)')}
          {header('points', 'Points')}
          {header('ppm', 'Points per M€')}
          <TableHead>Seller</TableHead>
          <TableHead>Closes</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {players.map((player) => (
          <TableRow key={player.id}>
            <TableCell className="font-medium">{player.name}</TableCell>
            <TableCell>{teamNames.get(player.teamId) ?? '–'}</TableCell>
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
            <TableCell className="tabular-nums">
              {pointsPerMillion.format(player.pointsPerMillion)}
            </TableCell>
            <TableCell>
              <SellerName seller={player.seller} />
            </TableCell>
            <TableCell className="whitespace-nowrap">
              {formatDateTime(player.closesAt)}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function SellerName({ seller }: { seller: Seller }) {
  if (seller.free) {
    return <span className="text-muted-foreground">Free agent</span>
  }
  return <>{seller.managerName}</>
}
