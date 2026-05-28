import type { PlayerRoundApiResponse } from '../api'
import type { SelectablePlayer } from './PlayersPage'

interface PlayerRoundsPageProps {
  player: SelectablePlayer
  rounds: PlayerRoundApiResponse[]
  isLoading: boolean
  error: string | null
  onBack: () => void
  onStatsClick: () => void
  onRoundClick: (roundId: number) => void
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

function formatRelativeScore(value: number): string {
  return value > 0 ? `+${value}` : `${value}`
}

function playerCountText(playerCount: number): string {
  if (playerCount <= 1) {
    return 'Själv'
  }

  if (playerCount === 2) {
    return 'Tillsammans med 1 annan spelare'
  }

  return `Tillsammans med ${playerCount - 1} andra spelare`
}

function statusText(round: PlayerRoundApiResponse): string {
  if (round.status === 'in_progress') {
    return `Pågående · ${round.played_holes} av ${round.layout_hole_count} hål spelade`
  }

  if (round.status === 'completed') {
    return 'Avslutad'
  }

  return round.status
}

export default function PlayerRoundsPage({
  player,
  rounds,
  isLoading,
  error,
  onBack,
  onStatsClick,
  onRoundClick,
}: PlayerRoundsPageProps) {
  return (
    <main className="content-page">
      <div className="page-toolbar player-page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>

        <div className="player-page-heading">
          <h2>{player.name}</h2>
          <p className="muted-text">{player.roundCount} rundor</p>
        </div>

        <div className="player-view-buttons">
          <button className="primary-button">Rundor</button>
          <button className="secondary-button" onClick={onStatsClick}>
            Statistik
          </button>
        </div>
      </div>

      {isLoading && <p>Laddar rundor…</p>}

      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && rounds.length === 0 && (
        <section className="list-card">
          <p className="muted-text">Inga rundor hittades.</p>
        </section>
      )}

      {!isLoading && !error && rounds.length > 0 && (
        <section className="round-list">
          {rounds.map((round) => {
            const relativeScore = round.total_throws - round.total_par
            const title = round.layout_name
              ? `${round.course_name} - ${round.layout_name}`
              : round.course_name

            return (
              <button
                key={round.id}
                className="round-card"
                onClick={() => onRoundClick(round.id)}
              >
                <div className="round-card-header">
                  <div>
                    <h3>{title}</h3>
                    <p>{formatDateTime(round.started_at)}</p>
                  </div>

                  <div className="round-result">
                    {formatRelativeScore(relativeScore)} ({round.total_throws})
                  </div>
                </div>

                <p>{playerCountText(round.player_count)}</p>
                <p className="round-status">{statusText(round)}</p>
              </button>
            )
          })}
        </section>
      )}
    </main>
  )
}
