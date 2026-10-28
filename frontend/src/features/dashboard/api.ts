import { getJson } from '@/lib/api'
import type { Dashboard } from './types'

export function fetchDashboard(): Promise<Dashboard> {
  return getJson<Dashboard>('/api/dashboard')
}
