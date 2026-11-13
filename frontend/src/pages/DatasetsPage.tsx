import { useCallback, useEffect, useRef, useState, type DragEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError, type Dataset } from '../api'
import { useAuth } from '../auth'
import { fmtDateTime, fmtNumber } from '../format'

export default function DatasetsPage() {
  const { can } = useAuth()
  const [datasets, setDatasets] = useState<Dataset[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [dragging, setDragging] = useState(false)
  const input = useRef<HTMLInputElement>(null)

  const load = useCallback(() => {
    api.datasets().then(setDatasets).catch((e: ApiError) => setError(e.message))
  }, [])
  useEffect(load, [load])

  async function upload(file: File | undefined) {
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      await api.upload(file)
      load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Falha no upload')
    } finally {
      setUploading(false)
      if (input.current) input.current.value = ''
    }
  }

  function onDrop(e: DragEvent) {
    e.preventDefault()
    setDragging(false)
    void upload(e.dataTransfer.files[0])
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Datasets</h1>
          <p className="muted">Envie um CSV e veja métricas e gráficos automaticamente.</p>
        </div>
      </header>

      {can('ADMIN', 'ANALYST') && (
        <div
          className={`dropzone ${dragging ? 'dragging' : ''}`}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          onClick={() => input.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && input.current?.click()}
        >
          <input
            ref={input}
            type="file"
            accept=".csv,text/csv"
            hidden
            onChange={(e) => void upload(e.target.files?.[0])}
          />
          {uploading ? (
            <strong>Enviando e analisando…</strong>
          ) : (
            <>
              <strong>Arraste um arquivo .csv aqui</strong>
              <span className="muted">ou clique para escolher (até 10 MB · vírgula, ponto e vírgula ou tab)</span>
            </>
          )}
        </div>
      )}

      {error && <div className="alert">{error}</div>}

      {datasets === null ? (
        <p className="muted">Carregando…</p>
      ) : datasets.length === 0 ? (
        <div className="empty">
          <strong>Nenhum dataset ainda</strong>
          <span className="muted">
            {can('ADMIN', 'ANALYST')
              ? 'Envie seu primeiro CSV acima para gerar o dashboard.'
              : 'Peça a um analista para enviar os dados.'}
          </span>
        </div>
      ) : (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th className="num">Linhas</th>
                <th className="num">Colunas</th>
                <th>Enviado por</th>
                <th>Data</th>
              </tr>
            </thead>
            <tbody>
              {datasets.map((d) => (
                <tr key={d.id}>
                  <td>
                    <Link to={`/datasets/${d.id}`}>{d.name}</Link>
                  </td>
                  <td className="num">{fmtNumber(d.rowCount)}</td>
                  <td className="num">{d.columns.length}</td>
                  <td>{d.createdBy}</td>
                  <td>{fmtDateTime(d.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
