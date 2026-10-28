import { getJson } from '@/lib/api'
import type { Squad } from './types'

export function fetchSquad(): Promise<Squad> {
  return getJson<Squad>('/api/squad')
}
