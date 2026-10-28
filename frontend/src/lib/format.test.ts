import { describe, expect, it } from 'vitest'
import {
  formatDateTime,
  formatDay,
  formatMoney,
  formatMoneyChange,
  formatTimeLeft,
} from './format'

describe('format', () => {
  it('shows times in Madrid time, summer and winter', () => {
    expect(formatDateTime('2026-10-22T05:30:00Z')).toBe('22 Oct 2026, 07:30')
    expect(formatDateTime('2026-11-02T06:30:00Z')).toBe('2 Nov 2026, 07:30')
  })

  it('shows a calendar day as that day', () => {
    expect(formatDay('2026-10-22')).toBe('22 Oct 2026')
  })

  it('shows whole euros', () => {
    expect(formatMoney(51_420_000)).toBe('€51,420,000')
    expect(formatMoney(-1_250_000)).toBe('-€1,250,000')
    expect(formatMoney(0)).toBe('€0')
  })

  it('signs a change of money, but not a zero one', () => {
    expect(formatMoneyChange(120_000)).toBe('+€120,000')
    expect(formatMoneyChange(-70_000)).toBe('-€70,000')
    expect(formatMoneyChange(0)).toBe('€0')
  })

  it('shows the time left to the minute, rounding down', () => {
    const minute = 60_000
    expect(formatTimeLeft((2 * 24 * 60 + 4 * 60 + 13) * minute + 59_000)).toBe(
      '2d 4h 13m',
    )
    expect(formatTimeLeft(4 * 60 * minute)).toBe('4h 0m')
    expect(formatTimeLeft(13 * minute)).toBe('13m')
    expect(formatTimeLeft(59_999)).toBe('under a minute')
    expect(formatTimeLeft(-5_000)).toBe('under a minute')
  })
})
