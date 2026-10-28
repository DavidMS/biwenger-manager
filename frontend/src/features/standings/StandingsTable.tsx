import { Link } from 'react-router'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatMoney, formatMoneyChange } from '@/lib/format'
import { cn } from '@/lib/utils'
import type { StandingRow } from './types'

type StandingsTableProps = {
  standings: StandingRow[]
  selectedManagerId?: number
}

/** The league table. A manager's name links to their squad. */
export function StandingsTable({
  standings,
  selectedManagerId,
}: StandingsTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Position</TableHead>
          <TableHead>Manager</TableHead>
          <TableHead>Points</TableHead>
          <TableHead>Team value</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {standings.map((row) => {
          const selected = row.managerId === selectedManagerId
          return (
            <TableRow
              key={row.managerId}
              data-state={selected ? 'selected' : undefined}
            >
              <TableCell className="tabular-nums">{row.position}</TableCell>
              <TableCell>
                <span className="inline-flex items-center gap-2">
                  <Link
                    to={{ search: `?manager=${row.managerId}` }}
                    aria-current={selected ? 'true' : undefined}
                    className={cn(
                      'underline-offset-4 hover:underline',
                      selected && 'font-medium',
                    )}
                  >
                    {row.name}
                  </Link>
                  {row.you && <Badge variant="secondary">You</Badge>}
                </span>
              </TableCell>
              <TableCell className="tabular-nums">{row.points}</TableCell>
              <TableCell className="whitespace-nowrap tabular-nums">
                {formatMoney(row.teamValue)}{' '}
                <span className="text-muted-foreground">
                  ({formatMoneyChange(row.teamValueChange)})
                </span>
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
