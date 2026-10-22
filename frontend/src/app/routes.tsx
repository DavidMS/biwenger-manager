import type { RouteObject } from 'react-router'
import { AppLayout } from '@/components/layout/AppLayout'
import { AdvisorPage } from '@/pages/AdvisorPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { MarketPage } from '@/pages/MarketPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { SquadPage } from '@/pages/SquadPage'
import { StandingsPage } from '@/pages/StandingsPage'

export const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/squad', label: 'Squad' },
  { to: '/market', label: 'Market' },
  { to: '/standings', label: 'Standings' },
  { to: '/advisor', label: 'Advisor' },
] as const

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'squad', element: <SquadPage /> },
      { path: 'market', element: <MarketPage /> },
      { path: 'standings', element: <StandingsPage /> },
      { path: 'advisor', element: <AdvisorPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
