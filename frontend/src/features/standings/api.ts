import { getJson } from '@/lib/api'
import type { RivalSquad, Standings } from './types'

export function fetchStandings(): Promise<Standings> {
  return getJson<Standings>('/api/standings')
}

export function fetchRivalSquad(managerId: number): Promise<RivalSquad> {
  return getJson<RivalSquad>(`/api/standings/${managerId}/squad`)
}
