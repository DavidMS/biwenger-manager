import { describe, expect, it } from 'vitest'
import { parseSort, sortSquad } from './sortSquad'
import type { SquadPlayer } from './types'

function player(overrides: Partial<SquadPlayer> & { id: number }): SquadPlayer {
  return {
    name: `Player ${overrides.id}`,
    slug: `player-${overrides.id}`,
    teamId: 1,
    position: 'MIDFIELDER',
    status: 'AVAILABLE',
    price: 1_000_000,
    trendChange: 0,
    trend: [],
    points: 0,
    form: [],
    ...overrides,
  }
}

const ids = (players: SquadPlayer[]) => players.map((p) => p.id)

describe('sortSquad', () => {
  const squad = [
    player({
      id: 3,
      position: 'FORWARD',
      price: 5_000_000,
      points: 40,
      trendChange: -10,
    }),
    player({
      id: 1,
      position: 'COACH',
      price: 2_000_000,
      points: 40,
      trendChange: 30,
    }),
    player({
      id: 2,
      position: 'GOALKEEPER',
      price: 9_000_000,
      points: 12,
      trendChange: 0,
    }),
  ]

  it('sorts by position in lineup order', () => {
    expect(ids(sortSquad(squad, { key: 'position', order: 'asc' }))).toEqual([
      2, 3, 1,
    ])
    expect(ids(sortSquad(squad, { key: 'position', order: 'desc' }))).toEqual([
      1, 3, 2,
    ])
  })

  it('sorts by value, points and trend', () => {
    expect(ids(sortSquad(squad, { key: 'value', order: 'desc' }))).toEqual([
      2, 3, 1,
    ])
    expect(ids(sortSquad(squad, { key: 'trend', order: 'desc' }))).toEqual([
      1, 2, 3,
    ])
    expect(ids(sortSquad(squad, { key: 'trend', order: 'asc' }))).toEqual([
      3, 2, 1,
    ])
  })

  it('breaks ties by player id, lowest first, in either order', () => {
    expect(ids(sortSquad(squad, { key: 'points', order: 'desc' }))).toEqual([
      1, 3, 2,
    ])
    expect(ids(sortSquad(squad, { key: 'points', order: 'asc' }))).toEqual([
      2, 1, 3,
    ])
  })

  it('leaves its input alone', () => {
    sortSquad(squad, { key: 'value', order: 'desc' })
    expect(ids(squad)).toEqual([3, 1, 2])
  })
})

describe('parseSort', () => {
  it('reads the column and the order', () => {
    expect(parseSort(new URLSearchParams('sort=points&order=asc'))).toEqual({
      key: 'points',
      order: 'asc',
    })
  })

  it("uses the column's first order when none is given", () => {
    expect(parseSort(new URLSearchParams('sort=value'))).toEqual({
      key: 'value',
      order: 'desc',
    })
  })

  it('falls back to position for an unknown column', () => {
    expect(parseSort(new URLSearchParams('sort=name&order=desc'))).toEqual({
      key: 'position',
      order: 'asc',
    })
  })
})
