import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, ApiError, type CategorySummary, type Dashboard, type Dataset, type NumericSummary } from '../api'
import { useAuth } from '../auth'
import { fmtCompact, fmtDateTime, fmtNumber } from '../format'

const ACCENT = '#4f46e5'

export default function DatasetPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { can } = useAuth()
  const [dataset, setDataset] = useState<Dataset | null>(null)
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)

  useEffect(() => {
    setDataset(null)
    setDashboard(null)
    Promise.all([api.dataset(id), api.dashboard(id)])
      .then(([d, dash]) => {
        setDataset(d)
        setDashboard(dash)
      })
      .catch((e: ApiError) => setError(e.status === 404 ? 'Dataset não encontrado.' : e.message))
  }, [id])

  async function exportPdf() {
    if (!dataset) return
    setExporting(true)
    try {
      await api.downloadPdf(dataset.id, dataset.name)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Falha ao exportar')
    } finally {
      setExporting(false)
    }
  }

  async function remove() {
    if (!dataset || !window.confirm(`Excluir "${dataset.name}"? Essa ação não pode ser desfeita.`)) return
    try {
      await api.deleteDataset(dataset.id)
      navigate('/')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Falha ao excluir')
    }
  }

  if (error) return <div className="alert">{error}</div>
  if (!dataset || !dashboard) return <p className="muted">Carregando…</p>

  // colunas de texto em que todo valor e unico (ids, nomes) nao geram um grafico util
  const charts = dashboard.categorical.filter((c) => c.distinct > 0 && c.distinct < c.filled)

  return (
    <>
      <header className="page-header">
        <div>
          <h1>{dataset.name}</h1>
          <p className="muted">
            {dataset.originalFilename} · enviado por {dataset.createdBy} em {fmtDateTime(dataset.createdAt)}
          </p>
        </div>
        <div className="actions">
          {can('ADMIN', 'ANALYST') && (
            <button className="btn" onClick={exportPdf} disabled={exporting}>
              {exporting ? 'Gerando…' : 'Exportar PDF'}
            </button>
          )}
          {can('ADMIN') && (
            <button className="btn btn-danger" onClick={remove}>
              Excluir
            </button>
          )}
        </div>
      </header>

      <section className="kpis">
        <Kpi label="Linhas" value={fmtNumber(dashboard.rowCount)} />
        <Kpi label="Colunas" value={String(dashboard.columnCount)} />
        <Kpi label="Colunas numéricas" value={String(dashboard.numeric.length)} />
        <Kpi label="Colunas de texto" value={String(dashboard.categorical.length)} />
      </section>

      {dashboard.numeric.length > 0 && (
        <>
          <h2>Métricas</h2>
          <section className="grid">
            {dashboard.numeric.map((n) => (
              <NumericCard key={n.column} data={n} />
            ))}
          </section>
        </>
      )}

      {charts.length > 0 && (
        <>
          <h2>Distribuições</h2>
          <section className="grid">
            {charts.map((c) => (
              <CategoryCard key={c.column} data={c} />
            ))}
          </section>
        </>
      )}

      <h2>Dados</h2>
      <RowsTable dataset={dataset} />
    </>
  )
}

function Kpi({ label, value }: { label: string; value: string }) {
  return (
    <div className="card kpi">
      <span className="muted">{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function NumericCard({ data }: { data: NumericSummary }) {
  const bins = data.histogram.map((b) => ({ ...b, short: b.label.split(' – ')[0] }))
  return (
    <div className="card">
      <div className="card-title">{data.column}</div>
      <dl className="stats">
        <div>
          <dt>Soma</dt>
          <dd>{fmtNumber(data.sum)}</dd>
        </div>
        <div>
          <dt>Média</dt>
          <dd>{fmtNumber(data.avg)}</dd>
        </div>
        <div>
          <dt>Mín</dt>
          <dd>{fmtNumber(data.min)}</dd>
        </div>
        <div>
          <dt>Máx</dt>
          <dd>{fmtNumber(data.max)}</dd>
        </div>
      </dl>
      <div className="chart">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={bins} margin={{ top: 4, right: 4, bottom: 0, left: -12 }}>
            <CartesianGrid vertical={false} stroke="#e5e7eb" />
            <XAxis dataKey="short" tick={{ fontSize: 11 }} tickLine={false} interval="preserveStartEnd" />
            <YAxis tick={{ fontSize: 11 }} tickLine={false} axisLine={false} tickFormatter={fmtCompact} />
            <Tooltip
              formatter={(v: number) => [fmtNumber(v), 'Linhas']}
              labelFormatter={(_, p) => p[0]?.payload.label ?? ''}
            />
            <Bar dataKey="count" fill={ACCENT} radius={[3, 3, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function CategoryCard({ data }: { data: CategorySummary }) {
  const height = Math.max(140, data.top.length * 30 + 20)
  return (
    <div className="card">
      <div className="card-title">
        {data.column} <span className="muted">· {data.distinct} valores distintos</span>
      </div>
      <div style={{ height }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data.top} layout="vertical" margin={{ top: 0, right: 12, bottom: 0, left: 0 }}>
            <CartesianGrid horizontal={false} stroke="#e5e7eb" />
            <XAxis type="number" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} />
            <YAxis type="category" dataKey="value" width={110} tick={{ fontSize: 11 }} tickLine={false} />
            <Tooltip formatter={(v: number) => [fmtNumber(v), 'Linhas']} />
            <Bar dataKey="count" fill={ACCENT} radius={[0, 3, 3, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function RowsTable({ dataset }: { dataset: Dataset }) {
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<Record<string, string>[]>([])
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    api.rows(dataset.id, page).then((p) => {
      setRows(p.content)
      setTotalPages(p.totalPages)
    })
  }, [dataset.id, page])

  return (
    <div className="card table-wrap">
      <table>
        <thead>
          <tr>
            {dataset.columns.map((c) => (
              <th key={c.name} className={c.type === 'NUMBER' ? 'num' : ''}>
                {c.name}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              {dataset.columns.map((c) => (
                <td key={c.name} className={c.type === 'NUMBER' ? 'num' : ''}>
                  {r[c.name]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="pager">
        <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>
          Anterior
        </button>
        <span className="muted">
          Página {page + 1} de {Math.max(totalPages, 1)}
        </span>
        <button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>
          Próxima
        </button>
      </div>
    </div>
  )
}
