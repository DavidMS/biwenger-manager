import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'

beforeEach(() => {
  // jsdom has no ResizeObserver, which Recharts' ResponsiveContainer needs to size a chart.
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  )
  // No real HTTP in tests: a test that forgets to mock its feature's api.ts fails here instead
  // of calling a server (the backend has the same safety net since #78).
  vi.stubGlobal(
    'fetch',
    vi.fn(() =>
      Promise.reject(
        new Error('Real fetch in a test: mock the feature api module instead'),
      ),
    ),
  )
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})
