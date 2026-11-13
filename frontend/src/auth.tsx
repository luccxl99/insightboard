import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore, type AuthResponse, type Me, type Role } from './api'

interface AuthContextValue {
  me: Me | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (data: { companyName: string; name: string; email: string; password: string }) => Promise<void>
  logout: () => void
  can: (...roles: Role[]) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(() => {
    tokenStore.clear()
    setMe(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) return
    api
      .me()
      .then(setMe)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [logout])

  const accept = useCallback((res: AuthResponse) => {
    tokenStore.set(res.token)
    setMe(res.me)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      me,
      loading,
      login: async (email, password) => accept(await api.login(email, password)),
      register: async (data) => accept(await api.register(data)),
      logout,
      can: (...roles) => me !== null && roles.includes(me.role),
    }),
    [me, loading, accept, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
