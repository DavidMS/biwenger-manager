import { describe, expect, it } from 'vitest'
import { applyView, parseView, type MarketView } from './marketView'
import type { MarketPlayer } from './types'

function player(
  overrides: Partial<MarketPlayer> & { id: number },
): MarketPlayer {
  return {
    name: `Player ${overrides.id}`,
    teamId: 1,
    position: 'MIDFIELDER',
    status: 'AVAILABLE',
    price: 1_000_000,
    value: 1_000_000,
    trendChange: 0,
    trend: [],
    points: 0,
    pointsPerMillion: 0,
    closesAt: '2026-10-29T05:00:00Z',
    seller: { free: true },
    ...overrides,
  }
}

const ids = (players: MarketPlayer[]) => players.map((p) => p.id)
const byPrice: MarketView = { sort: 'price', order: 'desc' }

const market = [
  player({
    id: 4,
    position: 'FORWARD',
    teamId: 3,
    price: 5_000_000,
    points: 40,
    trendChange: -10,
    pointsPerMillion: 8,
  }),
  player({
    id: 2,
    position: 'GOALKEEPER',
    teamId: 1,
    status: 'INJURED',
    price: 9_000_000,
    points: 12,
    trendChange: 30,
    pointsPerMillion: 1.33,
  }),
  player({
    id: 7,
    position: 'FORWARD',
    teamId: 1,
    price: 2_500_000,
    points: 40,
    trendChange: 0,
    pointsPerMillion: 16,
  }),
]

describe('applyView', () => {
  it('filters by position, team and status', () => {
    expect(ids(applyView(market, { ...byPrice, position: 'FORWARD' }))).toEqual(
      [4, 7],
    )
    expect(ids(applyView(market, { ...byPrice, teamId: 1 }))).toEqual([2, 7])
    expect(ids(applyView(market, { ...byPrice, status: 'INJURED' }))).toEqual([
      2,
    ])
    expect(
      ids(applyView(market, { ...byPrice, position: 'FORWARD', teamId: 1 })),
    ).toEqual([7])
  })

  it('filters by a price range in millions, both ends included', () => {
    expect(
      ids(applyView(market, { ...byPrice, minPrice: 2.5, maxPrice: 5 })),
    ).toEqual([4, 7])
    expect(ids(applyView(market, { ...byPrice, minPrice: 5.01 }))).toEqual([2])
    expect(ids(applyView(market, { ...byPrice, maxPrice: 1 }))).toEqual([])
  })

  it('sorts by price, points, trend and points per million', () => {
    expect(ids(applyView(market, byPrice))).toEqual([2, 4, 7])
    expect(ids(applyView(market, { sort: 'ppm', order: 'desc' }))).toEqual([
      7, 4, 2,
    ])
    expect(ids(applyView(market, { sort: 'trend', order: 'asc' }))).toEqual([
      4, 7, 2,
    ])
  })

  it('breaks ties by player id, lowest first, in either order', () => {
    expect(ids(applyView(market, { sort: 'points', order: 'desc' }))).toEqual([
      4, 7, 2,
    ])
    expect(ids(applyView(market, { sort: 'points', order: 'asc' }))).toEqual([
      2, 4, 7,
    ])
  })

  it('leaves its input alone', () => {
    applyView(market, { sort: 'ppm', order: 'desc' })
    expect(ids(market)).toEqual([4, 2, 7])
  })
})

describe('parseView', () => {
  it('reads every filter and the sort', () => {
    expect(
      parseView(
        new URLSearchParams(
          'position=FORWARD&team=3&status=DOUBTFUL&minPrice=1.5&maxPrice=10&sort=ppm&order=asc',
        ),
      ),
    ).toEqual({
      position: 'FORWARD',
      teamId: 3,
      status: 'DOUBTFUL',
      minPrice: 1.5,
      maxPrice: 10,
      sort: 'ppm',
      order: 'asc',
    })
  })

  it('ignores unknown or malformed values, and sorts by highest price by default', () => {
    expect(
      parseView(
        new URLSearchParams(
          'position=STRIKER&team=x&status=&minPrice=cheap&sort=name',
        ),
      ),
    ).toEqual({
      position: undefined,
      teamId: undefined,
      status: undefined,
      minPrice: undefined,
      maxPrice: undefined,
      sort: 'price',
      order: 'desc',
    })
  })
})
