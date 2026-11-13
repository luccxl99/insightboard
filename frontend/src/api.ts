export type Role = 'ADMIN' | 'ANALYST' | 'VIEWER'

export interface Me {
  id: string
  name: string
  email: string
  role: Role
  tenantName: string
}

export interface AuthResponse {
  token: string
  me: Me
}

export interface ColumnInfo {
  name: string
  type: 'NUMBER' | 'TEXT'
}

export interface Dataset {
  id: string
  name: string
  originalFilename: string
  rowCount: number
  columns: ColumnInfo[]
  createdBy: string
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Bin {
  label: string
  count: number
}

export interface NumericSummary {
  column: string
  count: number
  sum: number
  min: number
  max: number
  avg: number
  histogram: Bin[]
}

export interface CategorySummary {
  column: string
  distinct: number
  filled: number
  top: { value: string; count: number }[]
}

export interface Dashboard {
  datasetId: string
  name: string
  rowCount: number
  columnCount: number
  numeric: NumericSummary[]
  categorical: CategorySummary[]
}

export interface UserRow {
  id: string
  name: string
  email: string
  role: Role
  createdAt: string
}

export interface AuditEntry {
  id: string
  actor: string
  action: string
  entity: string | null
  entityId: string | null
  detail: string | null
  createdAt: string
}

export class ApiError extends Error {
  status: number
  fields: Record<string, string>

  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

const TOKEN_KEY = 'insightboard.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function raw(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const res = await fetch(`/api${path}`, { ...init, headers })
  if (res.ok) return res

  let message = 'Algo deu errado. Tente novamente.'
  let fields: Record<string, string> = {}
  try {
    const body = await res.json()
    message = body.message ?? message
    fields = body.fields ?? {}
  } catch {
    if (res.status === 403) message = 'Você não tem permissão para esta ação.'
  }
  // 401 com token salvo = sessao expirada. Sem token (ex.: login errado) so exibe o erro.
  if (res.status === 401 && token) onUnauthorized()
  throw new ApiError(res.status, message, fields)
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await raw(path, init)
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) })

export const api = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', json('POST', { email, password })),
  register: (data: { companyName: string; name: string; email: string; password: string }) =>
    request<AuthResponse>('/auth/register', json('POST', data)),
  me: () => request<Me>('/auth/me'),

  datasets: () => request<Dataset[]>('/datasets'),
  dataset: (id: string) => request<Dataset>(`/datasets/${id}`),
  upload: (file: File, name?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (name) form.append('name', name)
    return request<Dataset>('/datasets', { method: 'POST', body: form })
  },
  deleteDataset: (id: string) => request<void>(`/datasets/${id}`, { method: 'DELETE' }),
  rows: (id: string, page: number, size = 10) =>
    request<Page<Record<string, string>>>(`/datasets/${id}/rows?page=${page}&size=${size}`),
  dashboard: (id: string) => request<Dashboard>(`/datasets/${id}/dashboard`),
  downloadPdf: async (id: string, fallbackName: string) => {
    const res = await raw(`/datasets/${id}/export/pdf`)
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${fallbackName.replace(/[^\w.-]+/g, '_')}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  },

  users: () => request<UserRow[]>('/users'),
  createUser: (data: { name: string; email: string; password: string; role: Role }) =>
    request<UserRow>('/users', json('POST', data)),

  audit: (page: number, size = 20) => request<Page<AuditEntry>>(`/audit?page=${page}&size=${size}`),
}
