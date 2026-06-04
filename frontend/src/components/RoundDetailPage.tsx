import { type FormEvent, useEffect, useState } from 'react'
import type {
  RoundDetailApiResponse,
  RoundDetailHoleApiResponse,
  RoundDetailPlayerApiResponse,
  RoundUpdateRequest,
} from '../api'

interface RoundDetailPageProps {
  round: RoundDetailApiResponse | null
  currentUserId: number | null
  currentUserRole: string | null
  canEdit: boolean
  startInEditMode: boolean
  canDelete: boolean
  isLoading: boolean
  error: string | null
  onBack: () => void
  onSaveRound: (request: RoundUpdateRequest) => Promise<void>
  onDeleteRound: () => Promise<void>
  onEditModeConsumed: () => void
}

interface EditableScore {
  sessionPlayerHoleId: number
  value: string
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

function toDatetimeLocal(value: string | null): string {
  if (!value) {
    return ''
  }

  return value.slice(0, 16)
}

function fromDatetimeLocal(value: string): string | null {
  if (value.trim() === '') {
    return null
  }

  return value
}

function formatRelativeScore(value: number): string {
  return value > 0 ? `+${value}` : `${value}`
}

function roundStatusText(status: string): string {
  if (status === 'completed') {
    return 'Avslutad'
  }

  if (status === 'in_progress') {
    return 'Pågående'
  }

  if (status === 'cancelled') {
    return 'Avbruten'
  }

  return status
}

function chunkHoles(
  holes: RoundDetailHoleApiResponse[],
  size: number,
): RoundDetailHoleApiResponse[][] {
  const result: RoundDetailHoleApiResponse[][] = []

  for (let index = 0; index < holes.length; index += size) {
    result.push(holes.slice(index, index + size))
  }

  return result
}

function playerSummary(player: RoundDetailPlayerApiResponse) {
  const playedHoles = player.holes.filter((hole) => hole.throws_count !== null)

  const totalThrows = playedHoles.reduce(
    (sum, hole) => sum + (hole.throws_count ?? 0),
    0,
  )

  const totalPar = playedHoles.reduce(
    (sum, hole) => sum + hole.par_snapshot,
    0,
  )

  return {
    totalThrows,
    relativeToPar: totalThrows - totalPar,
  }
}

export default function RoundDetailPage({
  round,
  canEdit,
  canDelete,
  startInEditMode,
  isLoading,
  error,
  onBack,
  onSaveRound,
  onDeleteRound,
  onEditModeConsumed,
}: RoundDetailPageProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [startedAt, setStartedAt] = useState('')
  const [endedAt, setEndedAt] = useState('')
  const [status, setStatus] = useState('completed')
  const [scores, setScores] = useState<EditableScore[]>([])
  const [saveError, setSaveError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    if (round && startInEditMode && canEdit) {
      setIsEditing(true)
      onEditModeConsumed()
    }
  }, [round, startInEditMode, canEdit, onEditModeConsumed])

  useEffect(() => {
    if (!round) {
      return
    }

    setStartedAt(toDatetimeLocal(round.started_at))
    setEndedAt(toDatetimeLocal(round.ended_at))
    setStatus(round.status)

    const editableScores = round.players.flatMap((player) =>
      player.holes.map((hole) => ({
        sessionPlayerHoleId: hole.id,
        value: hole.throws_count === null ? '' : String(hole.throws_count),
      })),
    )

    setScores(editableScores)
  }, [round])

  function scoreValue(sessionPlayerHoleId: number): string {
    return (
      scores.find((score) => score.sessionPlayerHoleId === sessionPlayerHoleId)
        ?.value ?? ''
    )
  }

  function setScoreValue(sessionPlayerHoleId: number, value: string) {
    setScores((current) =>
      current.map((score) =>
        score.sessionPlayerHoleId === sessionPlayerHoleId
          ? { ...score, value }
          : score,
      ),
    )
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setSaveError(null)

    const preparedScores = scores.map((score) => {
      const trimmed = score.value.trim()

      if (trimmed === '') {
        return {
          session_player_hole_id: score.sessionPlayerHoleId,
          throws_count: null,
        }
      }

      const throwsCount = Number(trimmed)

      if (!Number.isInteger(throwsCount) || throwsCount <= 0) {
        throw new Error('Alla ifyllda resultat måste vara positiva heltal.')
      }

      return {
        session_player_hole_id: score.sessionPlayerHoleId,
        throws_count: throwsCount,
      }
    })

    setIsSaving(true)

    try {
      await onSaveRound({
        started_at: fromDatetimeLocal(startedAt) ?? undefined,
        ended_at: fromDatetimeLocal(endedAt),
        status,
        scores: preparedScores,
      })

      setIsEditing(false)
    } catch (err) {
      if (err instanceof Error) {
        setSaveError(err.message)
      } else {
        setSaveError('Kunde inte spara rundan.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete() {
    const confirmed = window.confirm(
      'Vill du radera rundan? Detta tar bort rundan och alla resultat i den.',
    )

    if (!confirmed) {
      return
    }

    setIsSaving(true)
    setSaveError(null)

    try {
      await onDeleteRound()
    } catch (err) {
      if (err instanceof Error) {
        setSaveError(err.message)
      } else {
        setSaveError('Kunde inte radera rundan.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return (
      <main className="content-page">
        <button className="secondary-button back-button" onClick={onBack}>
          Tillbaka
        </button>
        <p>Laddar runddetalj…</p>
      </main>
    )
  }

  if (error) {
    return (
      <main className="content-page">
        <button className="secondary-button back-button" onClick={onBack}>
          Tillbaka
        </button>
        <p className="error-message">{error}</p>
      </main>
    )
  }

  if (!round) {
    return (
      <main className="content-page">
        <button className="secondary-button back-button" onClick={onBack}>
          Tillbaka
        </button>
        <p>Ingen runda laddad.</p>
      </main>
    )
  }

  const players = [...round.players].sort(
    (first, second) => first.start_order - second.start_order,
  )

  const holes = [...(players[0]?.holes ?? [])].sort(
    (first, second) => first.sequence_number - second.sequence_number,
  )

  const holeChunks = chunkHoles(holes, 9)

  const layoutNames = Array.from(
    new Set(
      players
        .map((player) => player.layout_name)
        .filter((layoutName): layoutName is string => Boolean(layoutName)),
    ),
  )

  return (
    <main className="content-page">
      <div className="page-toolbar player-page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>

        <h2>Runddetalj</h2>

        <div className="player-view-buttons">
          {canEdit && !isEditing && (
            <button
              className="primary-button"
              onClick={() => setIsEditing(true)}
            >
              Redigera runda
            </button>
          )}

          {canDelete && !isEditing && (
            <button
              className="secondary-button danger-button"
              onClick={() => void handleDelete()}
              disabled={isSaving}
            >
              Radera runda
            </button>
          )}
        </div>
      </div>

      <section className="round-detail-header">
        <h3>{round.course_name}</h3>

        {layoutNames.length > 0 && (
          <p className="round-layout-name">{layoutNames.join(', ')}</p>
        )}

        <p>Start: {formatDateTime(round.started_at)}</p>

        {round.ended_at && <p>Slut: {formatDateTime(round.ended_at)}</p>}

        <p>Status: {roundStatusText(round.status)}</p>
      </section>

      {saveError && <p className="error-message">{saveError}</p>}

      {isEditing ? (
        <form className="round-edit-form" onSubmit={(event) => void handleSave(event)}>
          <section className="round-edit-card">
            <h3>Rundinformation</h3>

            <div className="admin-form-grid three-columns">
              <label>
                Starttid
                <input
                  type="datetime-local"
                  value={startedAt}
                  onChange={(event) => setStartedAt(event.target.value)}
                  required
                />
              </label>

              <label>
                Sluttid
                <input
                  type="datetime-local"
                  value={endedAt}
                  onChange={(event) => setEndedAt(event.target.value)}
                />
              </label>

              <label>
                Status
                <select
                  value={status}
                  onChange={(event) => setStatus(event.target.value)}
                >
                  <option value="in_progress">Pågående</option>
                  <option value="completed">Avslutad</option>
                  <option value="cancelled">Avbruten</option>
                </select>
              </label>
            </div>
          </section>

          <section className="scorecard-section">
            <h3>Resultat</h3>

            {holeChunks.map((holeChunk, chunkIndex) => (
              <EditableScorecardBlock
                key={chunkIndex}
                holeChunk={holeChunk}
                players={players}
                scoreValue={scoreValue}
                setScoreValue={setScoreValue}
              />
            ))}
          </section>

          <div className="admin-form-actions sticky-actions">
            <button className="primary-button" type="submit" disabled={isSaving}>
              {isSaving ? 'Sparar…' : 'Spara runda'}
            </button>

            <button
              className="secondary-button"
              type="button"
              onClick={() => setIsEditing(false)}
              disabled={isSaving}
            >
              Avbryt
            </button>
          </div>
        </form>
      ) : (
        <>
          <section className="round-summary-card">
            <h3>Resultat</h3>

            {players.map((player) => {
              const summary = playerSummary(player)

              return (
                <div className="round-player-summary" key={player.id}>
                  <span>{player.player_name}</span>
                  <strong>
                    {formatRelativeScore(summary.relativeToPar)} (
                    {summary.totalThrows})
                  </strong>
                </div>
              )
            })}
          </section>

          <section className="scorecard-section">
            <h3>Scorekort</h3>

            {holeChunks.map((holeChunk, chunkIndex) => (
              <ScorecardBlock
                key={chunkIndex}
                holeChunk={holeChunk}
                players={players}
              />
            ))}
          </section>
        </>
      )}
    </main>
  )
}

function EditableScorecardBlock({
  holeChunk,
  players,
  scoreValue,
  setScoreValue,
}: {
  holeChunk: RoundDetailHoleApiResponse[]
  players: RoundDetailPlayerApiResponse[]
  scoreValue: (sessionPlayerHoleId: number) => string
  setScoreValue: (sessionPlayerHoleId: number, value: string) => void
}) {
  return (
    <div className="scorecard-scroll">
      <table className="scorecard-table editable-scorecard-table">
        <thead>
          <tr>
            <th>Hål</th>
            {holeChunk.map((hole) => (
              <th key={`hole-${hole.sequence_number}`}>
                {hole.hole_number_snapshot}
              </th>
            ))}
          </tr>

          <tr>
            <th>Längd</th>
            {holeChunk.map((hole) => (
              <th key={`length-${hole.sequence_number}`}>
                {hole.length_snapshot_meters}
              </th>
            ))}
          </tr>

          <tr>
            <th>Par</th>
            {holeChunk.map((hole) => (
              <th key={`par-${hole.sequence_number}`}>{hole.par_snapshot}</th>
            ))}
          </tr>
        </thead>

        <tbody>
          {players.map((player) => (
            <tr key={player.id}>
              <td className="scorecard-player-name">{player.player_name}</td>

              {holeChunk.map((hole) => {
                const playerHole = player.holes.find(
                  (candidate) =>
                    candidate.sequence_number === hole.sequence_number,
                )

                if (!playerHole) {
                  return <td key={`${player.id}-${hole.sequence_number}`}>-</td>
                }

                return (
                  <td key={`${player.id}-${hole.sequence_number}`}>
                    <input
                      className="score-input"
                      type="number"
                      min="1"
                      value={scoreValue(playerHole.id)}
                      onChange={(event) =>
                        setScoreValue(playerHole.id, event.target.value)
                      }
                    />
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ScorecardBlock({
  holeChunk,
  players,
}: {
  holeChunk: RoundDetailHoleApiResponse[]
  players: RoundDetailPlayerApiResponse[]
}) {
  return (
    <div className="scorecard-scroll">
      <table className="scorecard-table">
        <thead>
          <tr>
            <th>Hål</th>
            {holeChunk.map((hole) => (
              <th key={`hole-${hole.sequence_number}`}>
                {hole.hole_number_snapshot}
              </th>
            ))}
          </tr>

          <tr>
            <th>Längd</th>
            {holeChunk.map((hole) => (
              <th key={`length-${hole.sequence_number}`}>
                {hole.length_snapshot_meters}
              </th>
            ))}
          </tr>

          <tr>
            <th>Par</th>
            {holeChunk.map((hole) => (
              <th key={`par-${hole.sequence_number}`}>{hole.par_snapshot}</th>
            ))}
          </tr>
        </thead>

        <tbody>
          {players.map((player) => (
            <ScorecardPlayerRow
              key={player.id}
              player={player}
              holeChunk={holeChunk}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ScorecardPlayerRow({
  player,
  holeChunk,
}: {
  player: RoundDetailPlayerApiResponse
  holeChunk: RoundDetailHoleApiResponse[]
}) {
  return (
    <tr>
      <td className="scorecard-player-name">{player.player_name}</td>

      {holeChunk.map((hole) => {
        const playerHole = player.holes.find(
          (candidate) => candidate.sequence_number === hole.sequence_number,
        )

        return (
          <td key={`${player.id}-${hole.sequence_number}`}>
            <ScoreBadge
              throwsCount={playerHole?.throws_count ?? null}
              par={playerHole?.par_snapshot ?? null}
            />
          </td>
        )
      })}
    </tr>
  )
}

function ScoreBadge({
  throwsCount,
  par,
}: {
  throwsCount: number | null
  par: number | null
}) {
  if (throwsCount === null || par === null) {
    return <span className="score-badge score-empty">-</span>
  }

  const difference = throwsCount - par

  const scoreClass =
    difference <= -1
      ? 'score-under'
      : difference === 1
        ? 'score-bogey'
        : difference >= 2
          ? 'score-double'
          : 'score-even'

  return <span className={`score-badge ${scoreClass}`}>{throwsCount}</span>
}
