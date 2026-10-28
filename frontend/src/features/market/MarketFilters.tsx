import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { positionLabels, positions, statusLabels, statuses } from '@/lib/player'
import type { FilterName } from './marketView'
import type { Team } from './types'

type MarketFiltersProps = {
  /** The filters as typed, from the URL: an empty string means no filter. */
  values: Record<FilterName, string>
  teams: Team[]
  onChange: (name: FilterName, value: string) => void
  onClear: () => void
}

/** Position, team, status and asking-price range. Every control has a label. */
export function MarketFilters({
  values,
  teams,
  onChange,
  onClear,
}: MarketFiltersProps) {
  return (
    <form
      aria-label="Market filters"
      className="flex flex-wrap items-end gap-4"
      onSubmit={(event) => event.preventDefault()}
    >
      <div className="grid gap-1.5">
        <Label htmlFor="market-position">Position</Label>
        <NativeSelect
          id="market-position"
          value={values.position}
          onChange={(event) => onChange('position', event.target.value)}
        >
          <NativeSelectOption value="">Any position</NativeSelectOption>
          {positions.map((position) => (
            <NativeSelectOption key={position} value={position}>
              {positionLabels[position]}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="market-team">Team</Label>
        <NativeSelect
          id="market-team"
          value={values.team}
          onChange={(event) => onChange('team', event.target.value)}
        >
          <NativeSelectOption value="">Any team</NativeSelectOption>
          {teams.map((team) => (
            <NativeSelectOption key={team.id} value={String(team.id)}>
              {team.name}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="market-status">Status</Label>
        <NativeSelect
          id="market-status"
          value={values.status}
          onChange={(event) => onChange('status', event.target.value)}
        >
          <NativeSelectOption value="">Any status</NativeSelectOption>
          {statuses.map((status) => (
            <NativeSelectOption key={status} value={status}>
              {statusLabels[status]}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="market-min-price">Min price (M€)</Label>
        <Input
          id="market-min-price"
          type="number"
          inputMode="decimal"
          min={0}
          step={0.1}
          className="w-28"
          value={values.minPrice}
          onChange={(event) => onChange('minPrice', event.target.value)}
        />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="market-max-price">Max price (M€)</Label>
        <Input
          id="market-max-price"
          type="number"
          inputMode="decimal"
          min={0}
          step={0.1}
          className="w-28"
          value={values.maxPrice}
          onChange={(event) => onChange('maxPrice', event.target.value)}
        />
      </div>
      <Button type="button" variant="outline" onClick={onClear}>
        Clear filters
      </Button>
    </form>
  )
}
