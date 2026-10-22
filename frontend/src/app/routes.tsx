import type { RouteObject } from 'react-router'
import { AppLayout } from '@/components/layout/AppLayout'
import { AdvisorPage } from '@/pages/AdvisorPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { MarketPage } from '@/pages/MarketPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { SquadPage } from '@/pages/SquadPage'
import { StandingsPage } from '@/pages/StandingsPage'

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
