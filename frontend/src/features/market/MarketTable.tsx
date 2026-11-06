import { Sparkline } from '@/components/player/Sparkline'
import { PlayerLink } from '@/components/player/PlayerLink'
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
import { formatDateTime, formatMoney, formatShortDateTime } from '@/lib/format'
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
            <TableCell>
              <div className="font-medium">
                <PlayerLink id={player.id} name={player.name} />
              </div>
              <div className="flex gap-1 text-xs text-muted-foreground">
                <span>{teamNames.get(player.teamId) ?? '–'}</span>
                <span aria-hidden="true">·</span>
                <span>{positionLabels[player.position]}</span>
              </div>
            </TableCell>
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
            <TableCell className="whitespace-normal">
              <SellerName seller={player.seller} />
            </TableCell>
            <TableCell>
              <time
                dateTime={player.closesAt}
                title={formatDateTime(player.closesAt)}
              >
                {formatShortDateTime(player.closesAt)}
              </time>
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
