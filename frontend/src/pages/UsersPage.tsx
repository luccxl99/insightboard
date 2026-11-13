import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, ApiError, type Role, type UserRow } from '../api'
import { fmtDateTime } from '../format'

const ROLE_LABEL: Record<Role, string> = { ADMIN: 'Administrador', ANALYST: 'Analista', VIEWER: 'Visualizador' }
const ROLE_HELP: Record<Role, string> = {
  ADMIN: 'Tudo, inclusive usuários, auditoria e exclusão',
  ANALYST: 'Envia CSVs e exporta PDF',
  VIEWER: 'Somente visualiza dashboards',
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserRow[] | null>(null)
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'VIEWER' as Role })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    api.users().then(setUsers).catch((e: ApiError) => setError(e.message))
  }, [])
  useEffect(load, [load])

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await api.createUser(form)
      setForm({ name: '', email: '', password: '', role: 'VIEWER' })
      load()
    } catch (err) {
      if (err instanceof ApiError) setError(Object.values(err.fields)[0] ?? err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Usuários</h1>
          <p className="muted">Pessoas que têm acesso aos dados da sua empresa.</p>
        </div>
      </header>

      <form className="card inline-form" onSubmit={submit}>
        <label>
          Nome
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        </label>
        <label>
          E-mail
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </label>
        <label>
          Senha inicial
          <input
            type="password"
            minLength={8}
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        </label>
        <label>
          Papel
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
            {(Object.keys(ROLE_LABEL) as Role[]).map((r) => (
              <option key={r} value={r}>
                {ROLE_LABEL[r]}
              </option>
            ))}
          </select>
        </label>
        <button className="btn btn-primary" disabled={busy}>
          Adicionar
        </button>
        <small className="muted full">{ROLE_HELP[form.role]}</small>
      </form>

      {error && <div className="alert">{error}</div>}

      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>E-mail</th>
              <th>Papel</th>
              <th>Criado em</th>
            </tr>
          </thead>
          <tbody>
            {users?.map((u) => (
              <tr key={u.id}>
                <td>{u.name}</td>
                <td>{u.email}</td>
                <td>
                  <span className={`badge badge-${u.role.toLowerCase()}`}>{ROLE_LABEL[u.role]}</span>
                </td>
                <td>{fmtDateTime(u.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
