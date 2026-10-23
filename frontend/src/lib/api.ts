/**
 * The one place the app calls the backend. Every error comes back as an {@link ApiError}: the
 * backend answers with RFC 9457 problem details (`title`, `detail`, `status`).
 */

export class ApiError extends Error {
  readonly status: number
  readonly title: string

  constructor(status: number, title: string, detail: string) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.title = title
  }
}

type ProblemDetail = { title?: string; detail?: string; status?: number }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { Accept: 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail = {}
  try {
    problem = (await response.json()) as ProblemDetail
  } catch {
    // Not JSON (a proxy error page, say): fall back to the status line.
  }
  return new ApiError(
    response.status,
    problem.title ?? response.statusText ?? 'Request failed',
    problem.detail ?? `The server answered ${response.status}.`,
  )
}

export function getJson<T>(path: string): Promise<T> {
  return request<T>(path)
}

export function postJson<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'POST' })
}
