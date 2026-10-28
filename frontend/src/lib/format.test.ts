import { describe, expect, it } from 'vitest'
import { formatDateTime, formatDay } from './format'

describe('format', () => {
  it('shows times in Madrid time, summer and winter', () => {
    expect(formatDateTime('2026-10-22T05:30:00Z')).toBe('22 Oct 2026, 07:30')
    expect(formatDateTime('2026-11-02T06:30:00Z')).toBe('2 Nov 2026, 07:30')
  })

  it('shows a calendar day as that day', () => {
    expect(formatDay('2026-10-22')).toBe('22 Oct 2026')
  })
})
