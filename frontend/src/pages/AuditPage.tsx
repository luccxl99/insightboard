import { useEffect, useState } from 'react'
import { api, type AuditEntry } from '../api'
import { fmtDateTime } from '../format'

const ACTION_LABEL: Record<string, string> = {
  TENANT_REGISTERED: 'Empresa cadastrada',
  LOGIN: 'Login',
  USER_CREATED: 'Usuário criado',
  DATASET_UPLOADED: 'Dataset enviado',
  DATASET_DELETED: 'Dataset excluído',
  DATASET_EXPORTED_PDF: 'PDF exportado',
}

export default function AuditPage() {
  const [page, setPage] = useState(0)
  const [entries, setEntries] = useState<AuditEntry[]>([])
  const [totalPages, setTotalPages] = useState(1)

  useEffect(() => {
    api.audit(page).then((p) => {
      setEntries(p.content)
      setTotalPages(p.totalPages)
    })
  }, [page])

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Auditoria</h1>
          <p className="muted">Registro de ações feitas na sua empresa, da mais recente para a mais antiga.</p>
        </div>
      </header>

      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>Quando</th>
              <th>Quem</th>
              <th>Ação</th>
              <th>Detalhe</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id}>
                <td>{fmtDateTime(e.createdAt)}</td>
                <td>{e.actor}</td>
                <td>{ACTION_LABEL[e.action] ?? e.action}</td>
                <td className="muted">{e.detail}</td>
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
    </>
  )
}
