import type { SortOrder } from '@/components/table/SortableHead'
import { positionRank } from '@/lib/player'
import type { SquadPlayer } from './types'

export const sortKeys = ['position', 'value', 'points', 'trend'] as const
export type SortKey = (typeof sortKeys)[number]
export type Sort = { key: SortKey; order: SortOrder }

export type { SortOrder }

export const defaultSort: Sort = { key: 'position', order: 'asc' }

/** The order a column starts in when first clicked: the lineup's order, or the biggest first. */
export function firstOrder(key: SortKey): SortOrder {
  return key === 'position' ? 'asc' : 'desc'
}

const sortValue: Record<SortKey, (player: SquadPlayer) => number> = {
  position: (player) => positionRank(player.position),
  value: (player) => player.price,
  points: (player) => player.points,
  trend: (player) => player.trendChange,
}

/** A sorted copy. Ties are broken by player id, lowest first, whatever the order. */
export function sortSquad(players: SquadPlayer[], sort: Sort): SquadPlayer[] {
  const direction = sort.order === 'asc' ? 1 : -1
  const value = sortValue[sort.key]
  return players.toSorted(
    (a, b) => direction * (value(a) - value(b)) || a.id - b.id,
  )
}

function isSortKey(value: string | null): value is SortKey {
  return sortKeys.includes(value as SortKey)
}

/** Reads a sort from URL parameters, falling back to the default for anything unknown. */
export function parseSort(params: URLSearchParams): Sort {
  const key = params.get('sort')
  if (!isSortKey(key)) {
    return defaultSort
  }
  const order = params.get('order')
  return {
    key,
    order: order === 'asc' || order === 'desc' ? order : firstOrder(key),
  }
}
