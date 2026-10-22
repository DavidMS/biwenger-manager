/** Sections in the main navigation. Each `to` must match a route in `routes.tsx`. */
export const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/squad', label: 'Squad' },
  { to: '/market', label: 'Market' },
  { to: '/standings', label: 'Standings' },
  { to: '/advisor', label: 'Advisor' },
] as const
