import { useState, type FormEvent } from 'react'
import { ApiError } from '../api'
import { useAuth } from '../auth'

export default function LoginPage() {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [form, setForm] = useState({ companyName: '', name: '', email: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const [fields, setFields] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFields({})
    try {
      if (mode === 'login') await login(form.email, form.password)
      else await register(form)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFields(err.fields)
      } else setError('Não foi possível conectar ao servidor.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={submit}>
        <div className="brand brand-lg">
          <span className="brand-mark" aria-hidden>
            <i /> <i /> <i />
          </span>
          InsightBoard
        </div>
        <p className="muted">
          {mode === 'login'
            ? 'Entre para ver os dados da sua empresa.'
            : 'Crie o espaço da sua empresa e comece a enviar CSVs.'}
        </p>

        {mode === 'register' && (
          <>
            <label>
              Empresa
              <input value={form.companyName} onChange={set('companyName')} required maxLength={120} />
              {fields.companyName && <small className="field-error">{fields.companyName}</small>}
            </label>
            <label>
              Seu nome
              <input value={form.name} onChange={set('name')} required maxLength={120} />
            </label>
          </>
        )}
        <label>
          E-mail
          <input type="email" value={form.email} onChange={set('email')} required autoComplete="email" />
          {fields.email && <small className="field-error">{fields.email}</small>}
        </label>
        <label>
          Senha
          <input
            type="password"
            value={form.password}
            onChange={set('password')}
            required
            minLength={mode === 'register' ? 8 : undefined}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
          {fields.password && <small className="field-error">{fields.password}</small>}
        </label>

        {error && <div className="alert">{error}</div>}

        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar conta'}
        </button>
        <button
          type="button"
          className="link"
          onClick={() => {
            setMode(mode === 'login' ? 'register' : 'login')
            setError(null)
            setFields({})
          }}
        >
          {mode === 'login' ? 'Ainda não tem conta? Cadastre sua empresa' : 'Já tem conta? Entrar'}
        </button>
      </form>
    </div>
  )
}
