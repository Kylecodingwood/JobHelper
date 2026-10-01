const API_ORIGIN = import.meta.env.VITE_API_BASE_URL ?? ''
const BASE = `${API_ORIGIN}/api/v1`
const _u = import.meta.env.VITE_API_USERNAME ?? ''
const _p = import.meta.env.VITE_API_PASSWORD ?? ''
const AUTH_HEADER = _u ? `Basic ${btoa(`${_u}:${_p}`)}` : undefined

export class ApiError extends Error {
  code: string
  status: number
  details: unknown
  constructor(status: number, code: string, message: string, details?: unknown) {
    super(message)
    this.status = status
    this.code = code
    this.details = details
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(AUTH_HEADER ? { Authorization: AUTH_HEADER } : {}), ...(init?.headers || {}) },
    ...init,
  })
  if (!res.ok) {
    let body: { code?: string; message?: string; details?: unknown } = {}
    try {
      body = await res.json()
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, body.code || 'ERROR', body.message || res.statusText, body.details)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

async function uploadFormData<T>(path: string, form: FormData): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { method: 'POST', body: form, headers: AUTH_HEADER ? { Authorization: AUTH_HEADER } : undefined })
  if (!res.ok) {
    let body: { code?: string; message?: string; details?: unknown } = {}
    try {
      body = await res.json()
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, body.code || 'ERROR', body.message || res.statusText, body.details)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  upload: <T>(path: string, form: FormData) => uploadFormData<T>(path, form),
}
