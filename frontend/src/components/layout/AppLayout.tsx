import { NavLink, Outlet } from 'react-router'
import { navItems } from '@/app/navigation'
import { buttonVariants } from '@/components/ui/button'

export function AppLayout() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="border-b">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-6 px-4 py-3">
          <span className="font-heading text-lg font-semibold">
            Biwenger Manager
          </span>
          <nav aria-label="Main">
            <ul className="flex flex-wrap gap-1">
              {navItems.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.to === '/'}
                    className={({ isActive }) =>
                      buttonVariants({
                        variant: isActive ? 'secondary' : 'ghost',
                        size: 'sm',
                      })
                    }
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
