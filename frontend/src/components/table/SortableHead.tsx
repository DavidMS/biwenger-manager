import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { TableHead } from '@/components/ui/table'

export type SortOrder = 'asc' | 'desc'

type SortableHeadProps<K extends string> = {
  column: K
  label: string
  sort: { key: K; order: SortOrder }
  onSort: (column: K) => void
}

/** A column header that sorts the table: a real button, and `aria-sort` on the sorted column. */
export function SortableHead<K extends string>({
  column,
  label,
  sort,
  onSort,
}: SortableHeadProps<K>) {
  const active = sort.key === column
  const Icon = !active
    ? ArrowUpDown
    : sort.order === 'asc'
      ? ArrowUp
      : ArrowDown
  return (
    <TableHead
      aria-sort={
        active ? (sort.order === 'asc' ? 'ascending' : 'descending') : undefined
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
