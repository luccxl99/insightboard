import { lazy, Suspense } from 'react'
import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import type { Role } from './api'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DatasetsPage from './pages/DatasetsPage'
import UsersPage from './pages/UsersPage'
import AuditPage from './pages/AuditPage'

// a pagina de dashboard carrega o Recharts (a maior parte do bundle), entao so e baixada quando visitada
const DatasetPage = lazy(() => import('./pages/DatasetPage'))

function Protected({ roles }: { roles?: Role[] }) {
  const { me, loading } = useAuth()
  if (loading) return <div className="center-screen">Carregando…</div>
  if (!me) return <Navigate to="/login" replace />
  if (roles && !roles.includes(me.role)) return <Navigate to="/" replace />
  return <Outlet />
}

export default function App() {
  const { me } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={me ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route element={<Protected />}>
        <Route element={<Layout />}>
          <Route index element={<DatasetsPage />} />
          <Route
            path="datasets/:id"
            element={
              <Suspense fallback={<p className="muted">Carregando…</p>}>
                <DatasetPage />
              </Suspense>
            }
          />
          <Route element={<Protected roles={['ADMIN']} />}>
            <Route path="users" element={<UsersPage />} />
            <Route path="audit" element={<AuditPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
