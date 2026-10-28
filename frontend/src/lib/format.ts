/**
 * Values as the user sees them. Dates and times are in Madrid time, where La Liga and Biwenger run,
 * whatever the browser's zone; money is whole euros.
 */

const MADRID = 'Europe/Madrid'

const dateTime = new Intl.DateTimeFormat('en-GB', {
  timeZone: MADRID,
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const day = new Intl.DateTimeFormat('en-GB', {
  timeZone: MADRID,
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

/** `2026-10-22T05:30:00Z` → `22 Oct 2026, 07:30`. */
export function formatDateTime(isoInstant: string): string {
  return dateTime.format(new Date(isoInstant))
}

/** `2026-10-22` (a calendar day, no time) → `22 Oct 2026`. */
export function formatDay(isoDate: string): string {
  // Noon UTC is the same calendar day in Madrid all year round.
  return day.format(new Date(`${isoDate}T12:00:00Z`))
}

const euros = new Intl.NumberFormat('en-GB', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
})

const euroChange = new Intl.NumberFormat('en-GB', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
  signDisplay: 'exceptZero',
})

/** Whole euros, as the backend sends money: `51420000` → `€51,420,000`. */
export function formatMoney(euroAmount: number): string {
  return euros.format(euroAmount)
}

/** A change of money, always signed: `-70000` → `-€70,000`, `120000` → `+€120,000`. */
export function formatMoneyChange(euroAmount: number): string {
  return euroChange.format(euroAmount)
}

const MINUTE_MS = 60_000

/**
 * Time left, to the minute: `2d 4h 13m`, `4h 0m`, `13m`, or `under a minute`. Rounds down, so it
 * never promises more time than there is.
 */
export function formatTimeLeft(ms: number): string {
  const totalMinutes = Math.floor(ms / MINUTE_MS)
  if (totalMinutes < 1) {
    return 'under a minute'
  }
  const days = Math.floor(totalMinutes / (24 * 60))
  const hours = Math.floor((totalMinutes % (24 * 60)) / 60)
  const minutes = totalMinutes % 60
  if (days > 0) {
    return `${days}d ${hours}h ${minutes}m`
  }
  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }
  return `${minutes}m`
}
