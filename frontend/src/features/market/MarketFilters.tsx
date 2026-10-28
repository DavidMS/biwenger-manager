import { useId } from 'react'
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
  const id = useId()
  const rangeInvalid =
    values.minPrice !== '' &&
    values.maxPrice !== '' &&
    Number(values.maxPrice) < Number(values.minPrice)
  return (
    <form
      aria-label="Market filters"
      className="flex flex-wrap items-end gap-4"
      onSubmit={(event) => event.preventDefault()}
    >
      <div className="grid gap-1.5">
        <Label htmlFor={`${id}-position`}>Position</Label>
        <NativeSelect
          id={`${id}-position`}
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
        <Label htmlFor={`${id}-team`}>Team</Label>
        <NativeSelect
          id={`${id}-team`}
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
        <Label htmlFor={`${id}-status`}>Status</Label>
        <NativeSelect
          id={`${id}-status`}
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
        <Label htmlFor={`${id}-min-price`}>Min price (M€)</Label>
        <Input
          id={`${id}-min-price`}
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
        <Label htmlFor={`${id}-max-price`}>Max price (M€)</Label>
        <Input
          id={`${id}-max-price`}
          type="number"
          inputMode="decimal"
          min={0}
          step={0.1}
          className="w-28"
          value={values.maxPrice}
          aria-invalid={rangeInvalid || undefined}
          aria-describedby={rangeInvalid ? `${id}-range-error` : undefined}
          onChange={(event) => onChange('maxPrice', event.target.value)}
        />
      </div>
      <Button type="button" variant="outline" onClick={onClear}>
        Clear filters
      </Button>
      {rangeInvalid && (
        <p id={`${id}-range-error`} className="w-full text-sm text-destructive">
          The max price is below the min price.
        </p>
      )}
    </form>
  )
}
