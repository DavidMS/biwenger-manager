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
  const headers = new Headers(init?.headers)
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    throw await toApiError(response)
  }
  // 204 No Content, or an empty body: nothing to parse.
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
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
    // statusText is '' over HTTP/2, so `||`, not `??`.
    problem.title || response.statusText || 'Request failed',
    problem.detail || `The server answered ${response.status}.`,
  )
}

export function getJson<T>(path: string): Promise<T> {
  return request<T>(path)
}

/**
 * A POST, with an optional JSON body. It always declares a JSON content type: a write endpoint
 * must require it, so another web page can't send one without the browser asking first
 * (backend/CLAUDE.md).
 */
export function postJson<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}
