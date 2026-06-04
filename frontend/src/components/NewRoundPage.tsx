import { type FormEvent, useEffect, useState } from 'react'
import type {
  PublicCourseApiResponse,
  PublicLayoutApiResponse,
} from '../api'
import type { SelectablePlayer } from './PlayersPage'

interface NewRoundPageProps {
  courses: PublicCourseApiResponse[]
  layouts: PublicLayoutApiResponse[]
  players: SelectablePlayer[]
  preselectedPlayerId: number | null
  isLoadingCourses: boolean
  isLoadingLayouts: boolean
  isSaving: boolean
  error: string | null
  onBack: () => void
  onCourseSelected: (courseId: number) => void
  onSubmit: (request: {
    courseId: number
    layoutId: number
    startedAt: string
    playerIds: number[]
  }) => void
}

function currentDatetimeLocal(): string {
  const now = new Date()
  const offsetMs = now.getTimezoneOffset() * 60 * 1000
  return new Date(now.getTime() - offsetMs).toISOString().slice(0, 16)
}

export default function NewRoundPage({
  courses,
  layouts,
  players,
  preselectedPlayerId,
  isLoadingCourses,
  isLoadingLayouts,
  isSaving,
  error,
  onBack,
  onCourseSelected,
  onSubmit,
}: NewRoundPageProps) {
  const [courseId, setCourseId] = useState('')
  const [layoutId, setLayoutId] = useState('')
  const [startedAt, setStartedAt] = useState(currentDatetimeLocal())
  const [selectedPlayerIds, setSelectedPlayerIds] = useState<number[]>([])
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    if (preselectedPlayerId !== null) {
      setSelectedPlayerIds((current) =>
        current.includes(preselectedPlayerId)
          ? current
          : [preselectedPlayerId, ...current],
      )
    }
  }, [preselectedPlayerId])

  function handleCourseChange(value: string) {
    setCourseId(value)
    setLayoutId('')

    const parsedCourseId = Number(value)

    if (Number.isInteger(parsedCourseId) && parsedCourseId > 0) {
      onCourseSelected(parsedCourseId)
    }
  }

  function togglePlayer(playerId: number) {
    setSelectedPlayerIds((current) =>
      current.includes(playerId)
        ? current.filter((id) => id !== playerId)
        : [...current, playerId],
    )
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)

    const parsedCourseId = Number(courseId)
    const parsedLayoutId = Number(layoutId)

    if (!Number.isInteger(parsedCourseId) || parsedCourseId <= 0) {
      setFormError('Välj bana.')
      return
    }

    if (!Number.isInteger(parsedLayoutId) || parsedLayoutId <= 0) {
      setFormError('Välj layout.')
      return
    }

    if (selectedPlayerIds.length === 0) {
      setFormError('Välj minst en spelare.')
      return
    }

    if (startedAt.trim() === '') {
      setFormError('Välj starttid.')
      return
    }

    onSubmit({
      courseId: parsedCourseId,
      layoutId: parsedLayoutId,
      startedAt,
      playerIds: selectedPlayerIds,
    })
  }

  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>
        <h2>Ny runda</h2>
      </div>

      <form className="round-edit-form" onSubmit={handleSubmit}>
        <section className="round-edit-card">
          <h3>Rundinformation</h3>

          <div className="admin-form-grid three-columns">
            <label>
              Bana
              <select
                value={courseId}
                onChange={(event) => handleCourseChange(event.target.value)}
                required
              >
                <option value="">Välj bana</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Layout
              <select
                value={layoutId}
                onChange={(event) => setLayoutId(event.target.value)}
                disabled={courseId === '' || isLoadingLayouts}
                required
              >
                <option value="">
                  {isLoadingLayouts ? 'Laddar layouter…' : 'Välj layout'}
                </option>
                {layouts.map((layout) => (
                  <option key={layout.id} value={layout.id}>
                    {layout.name} · {layout.hole_count} hål · par{' '}
                    {layout.total_par}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Starttid
              <input
                type="datetime-local"
                value={startedAt}
                onChange={(event) => setStartedAt(event.target.value)}
                required
              />
            </label>
          </div>
        </section>

        <section className="round-edit-card">
          <h3>Spelare</h3>

          {players.length === 0 && (
            <p className="muted-text">Inga spelare finns att välja.</p>
          )}

          {players.length > 0 && (
            <div className="new-round-player-list">
              {players.map((player) => (
                <label className="player-checkbox-row" key={player.id}>
                  <input
                    type="checkbox"
                    checked={selectedPlayerIds.includes(player.id)}
                    onChange={() => togglePlayer(player.id)}
                  />
                  <span>
                    <strong>{player.name}</strong>
                    {player.subtitle && (
                      <span className="muted-text"> · {player.subtitle}</span>
                    )}
                  </span>
                </label>
              ))}
            </div>
          )}
        </section>

        {(formError || error) && (
          <p className="error-message">{formError ?? error}</p>
        )}

        <div className="admin-form-actions sticky-actions">
          <button
            className="primary-button"
            type="submit"
            disabled={isSaving || isLoadingCourses}
          >
            {isSaving ? 'Skapar…' : 'Skapa runda'}
          </button>

          <button
            className="secondary-button"
            type="button"
            onClick={onBack}
            disabled={isSaving}
          >
            Avbryt
          </button>
        </div>
      </form>
    </main>
  )
}