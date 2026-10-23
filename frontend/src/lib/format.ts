/**
 * Dates and times as the user sees them: in Madrid time, where La Liga and Biwenger run, whatever
 * the browser's zone.
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
