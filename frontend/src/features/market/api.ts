import { getJson } from '@/lib/api'
import type { Market } from './types'

export function fetchMarket(): Promise<Market> {
  return getJson<Market>('/api/market')
}
