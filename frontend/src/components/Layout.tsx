import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'

const ROLE_LABEL = { ADMIN: 'Administrador', ANALYST: 'Analista', VIEWER: 'Visualizador' } as const

export default function Layout() {
  const { me, logout, can } = useAuth()
  if (!me) return null

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden>
            <i /> <i /> <i />
          </span>
          InsightBoard
        </div>
        <nav>
          <NavLink to="/" end>
            Datasets
          </NavLink>
          {can('ADMIN') && <NavLink to="/users">Usuários</NavLink>}
          {can('ADMIN') && <NavLink to="/audit">Auditoria</NavLink>}
        </nav>
        <div className="sidebar-footer">
          <div className="tenant">{me.tenantName}</div>
          <div className="who">
            {me.name}
            <span className="muted"> · {ROLE_LABEL[me.role]}</span>
          </div>
          <button className="btn btn-ghost" onClick={logout}>
            Sair
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
