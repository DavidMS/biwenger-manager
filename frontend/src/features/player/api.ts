import { getJson } from '@/lib/api'
import type { PlayerDetail } from './types'

export function fetchPlayer(playerId: number): Promise<PlayerDetail> {
  return getJson<PlayerDetail>(`/api/players/${playerId}`)
}
