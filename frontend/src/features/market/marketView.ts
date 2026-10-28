import type { SortOrder } from '@/components/table/SortableHead'
import {
  positions,
  statuses,
  type PlayerStatus,
  type Position,
} from '@/lib/player'
import type { MarketPlayer } from './types'

/** What the market shows, as kept in the URL. Prices are in millions of euros, as typed. */
export type MarketView = {
  position?: Position
  teamId?: number
  status?: PlayerStatus
  minPrice?: number
  maxPrice?: number
  sort: SortKey
  order: SortOrder
}

export const sortKeys = ['price', 'points', 'trend', 'ppm'] as const
export type SortKey = (typeof sortKeys)[number]

/** The URL parameters the filters use. */
export const filterNames = [
  'position',
  'team',
  'status',
  'minPrice',
  'maxPrice',
] as const
export type FilterName = (typeof filterNames)[number]

const MILLION = 1_000_000

const sortValue: Record<SortKey, (player: MarketPlayer) => number> = {
  price: (player) => player.price,
  points: (player) => player.points,
  trend: (player) => player.trendChange,
  ppm: (player) => player.pointsPerMillion,
}

function oneOf<T extends string>(list: readonly T[], value: string | null) {
  return list.includes(value as T) ? (value as T) : undefined
}

function number(value: string | null): number | undefined {
  if (value === null || value.trim() === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

/** Reads the view from URL parameters; anything unknown is left out. By default, highest price first. */
export function parseView(params: URLSearchParams): MarketView {
  const teamId = number(params.get('team'))
  return {
    position: oneOf(positions, params.get('position')),
    teamId:
      teamId !== undefined && Number.isInteger(teamId) ? teamId : undefined,
    status: oneOf(statuses, params.get('status')),
    minPrice: number(params.get('minPrice')),
    maxPrice: number(params.get('maxPrice')),
    sort: oneOf(sortKeys, params.get('sort')) ?? 'price',
    order: params.get('order') === 'asc' ? 'asc' : 'desc',
  }
}

/** The players that pass every filter, sorted. Ties are broken by player id, lowest first. */
export function applyView(
  players: MarketPlayer[],
  view: MarketView,
): MarketPlayer[] {
  const direction = view.order === 'asc' ? 1 : -1
  const value = sortValue[view.sort]
  return players
    .filter(
      (player) =>
        (view.position === undefined || player.position === view.position) &&
        (view.teamId === undefined || player.teamId === view.teamId) &&
        (view.status === undefined || player.status === view.status) &&
        (view.minPrice === undefined ||
          player.price >= view.minPrice * MILLION) &&
        (view.maxPrice === undefined ||
          player.price <= view.maxPrice * MILLION),
    )
    .sort((a, b) => direction * (value(a) - value(b)) || a.id - b.id)
}
