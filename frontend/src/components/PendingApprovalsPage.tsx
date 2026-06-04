import type { PendingApprovalApiResponse } from '../api'

interface PendingApprovalsPageProps {
  approvals: PendingApprovalApiResponse[]
  isLoading: boolean
  error: string | null
  onBack: () => void
  onOpenRound: (roundId: number) => void
  onApprove: (sessionPlayerId: number) => void
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

export default function PendingApprovalsPage({
  approvals,
  isLoading,
  error,
  onBack,
  onOpenRound,
  onApprove,
}: PendingApprovalsPageProps) {
  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>

        <h2>Rundor att godkänna</h2>
      </div>

      {isLoading && <p>Laddar rundor att godkänna…</p>}

      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && approvals.length === 0 && (
        <section className="list-card">
          <p className="muted-text">Det finns inga rundor att godkänna.</p>
        </section>
      )}

      {!isLoading && !error && approvals.length > 0 && (
        <section className="round-list">
          {approvals.map((approval) => (
            <article
              className="round-card"
              key={approval.session_player_id}
            >
              <div className="round-card-header">
                <div>
                  <h3>
                    {approval.player_name} · {approval.course_name}
                  </h3>

                  <p>
                    {approval.layout_name ?? 'Layout saknas'} ·{' '}
                    {formatDateTime(approval.started_at)}
                  </p>

                  <p>
                    Inlagd av {approval.added_by_username}
                  </p>
                </div>

                <span className="inactive-badge">Väntar</span>
              </div>

              <div className="approval-actions">
                <button
                  className="secondary-button"
                  onClick={() => onOpenRound(approval.round_id)}
                >
                  Visa runda
                </button>

                <button
                  className="primary-button"
                  onClick={() => onApprove(approval.session_player_id)}
                >
                  Godkänn
                </button>
              </div>
            </article>
          ))}
        </section>
      )}
    </main>
  )
}