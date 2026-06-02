import { type FormEvent, useEffect, useState } from 'react'
import {
  ApiError,
  createHole,
  deleteHole,
  getCourseHoles,
  updateHole,
  type PublicCourseApiResponse,
  type PublicCourseHoleApiResponse,
} from '../api'
import AdminTeesSection from './AdminTeesSection'
import AdminBasketsSection from './AdminBasketsSection'

interface AdminHolesSectionProps {
  course: PublicCourseApiResponse
  token: string
  onCourseChanged: () => void
}

export default function AdminHolesSection({
  course,
  token,
  onCourseChanged,
}: AdminHolesSectionProps) {
  const [holes, setHoles] = useState<PublicCourseHoleApiResponse[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showAddForm, setShowAddForm] = useState(false)

  const [newHoleNumber, setNewHoleNumber] = useState('')
  const [newHoleName, setNewHoleName] = useState('')
  const [newHoleLengthMeters, setNewHoleLengthMeters] = useState('')
  const [newHolePar, setNewHolePar] = useState('3')
  const [newHoleNotes, setNewHoleNotes] = useState('')

  const [editingHoleId, setEditingHoleId] = useState<number | null>(null)
  const [editHoleNumber, setEditHoleNumber] = useState('')
  const [editHoleName, setEditHoleName] = useState('')
  const [editHoleLengthMeters, setEditHoleLengthMeters] = useState('')
  const [editHolePar, setEditHolePar] = useState('')
  const [editHoleNotes, setEditHoleNotes] = useState('')
  const [editHoleIsActive, setEditHoleIsActive] = useState(true)

  useEffect(() => {
    void loadHoles()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [course.id, includeInactive])

  async function loadHoles() {
    setIsLoading(true)
    setError(null)

    try {
      const data = await getCourseHoles(course.id, includeInactive)
      setHoles(data)
    } catch (err) {
      setError(errorText(err, 'Kunde inte hämta hål.'))
    } finally {
      setIsLoading(false)
    }
  }

  function resetNewHoleForm() {
    setNewHoleNumber('')
    setNewHoleName('')
    setNewHoleLengthMeters('')
    setNewHolePar('3')
    setNewHoleNotes('')
  }

  function startEditHole(hole: PublicCourseHoleApiResponse) {
    setEditingHoleId(hole.id)
    setEditHoleNumber(String(hole.hole_number))
    setEditHoleName(hole.name ?? '')
    setEditHoleLengthMeters(String(hole.length_meters))
    setEditHolePar(String(hole.par_value))
    setEditHoleNotes(hole.notes ?? '')
    setEditHoleIsActive(hole.is_active === 1)
  }

  function cancelEditHole() {
    setEditingHoleId(null)
    setEditHoleNumber('')
    setEditHoleName('')
    setEditHoleLengthMeters('')
    setEditHolePar('')
    setEditHoleNotes('')
    setEditHoleIsActive(true)
  }

  async function handleCreateHole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const parsed = parseHoleForm(
      newHoleNumber,
      newHoleLengthMeters,
      newHolePar,
    )

    if (!parsed.ok) {
      setError(parsed.message)
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await createHole(token, course.id, {
        hole_number: parsed.holeNumber,
        name: newHoleName.trim() ? newHoleName.trim() : null,
        length_meters: parsed.lengthMeters,
        par_value: parsed.parValue,
        notes: newHoleNotes.trim() ? newHoleNotes.trim() : null,
      })

      resetNewHoleForm()
      setShowAddForm(false)
      await loadHoles()
      onCourseChanged()
    } catch (err) {
      setError(errorText(err, 'Kunde inte skapa hål.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleUpdateHole(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (editingHoleId === null) {
      return
    }

    const parsed = parseHoleForm(
      editHoleNumber,
      editHoleLengthMeters,
      editHolePar,
    )

    if (!parsed.ok) {
      setError(parsed.message)
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await updateHole(token, editingHoleId, {
        hole_number: parsed.holeNumber,
        name: editHoleName.trim() ? editHoleName.trim() : '',
        length_meters: parsed.lengthMeters,
        par_value: parsed.parValue,
        notes: editHoleNotes.trim() ? editHoleNotes.trim() : '',
        is_active: editHoleIsActive,
      })

      cancelEditHole()
      await loadHoles()
      onCourseChanged()
    } catch (err) {
      setError(errorText(err, 'Kunde inte uppdatera hål.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDeleteHole(hole: PublicCourseHoleApiResponse) {
    const confirmed = window.confirm(
      `Vill du ta bort eller inaktivera hål ${hole.hole_number}?\n\n` +
        'Om hålet används i layout eller rundhistorik kommer det att inaktiveras i stället för att raderas.',
    )

    if (!confirmed) {
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await deleteHole(token, hole.id)
      await loadHoles()
      onCourseChanged()
    } catch (err) {
      setError(errorText(err, 'Kunde inte ta bort eller inaktivera hålet.'))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="admin-course-section">
      <div className="admin-section-header">
        <h3>Hål</h3>

        <div className="admin-header-actions">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(event) => setIncludeInactive(event.target.checked)}
            />
            Visa inaktiva hål
          </label>

          <button
            className="primary-button"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            {showAddForm ? 'Stäng formulär' : 'Lägg till hål'}
          </button>
        </div>
      </div>

      {showAddForm && (
        <HoleForm
          title="Lägg till hål"
          holeNumber={newHoleNumber}
          name={newHoleName}
          lengthMeters={newHoleLengthMeters}
          par={newHolePar}
          notes={newHoleNotes}
          isActive={true}
          showActiveField={false}
          isSaving={isSaving}
          submitText="Spara hål"
          onSubmit={handleCreateHole}
          onCancel={() => {
            resetNewHoleForm()
            setShowAddForm(false)
          }}
          onHoleNumberChange={setNewHoleNumber}
          onNameChange={setNewHoleName}
          onLengthMetersChange={setNewHoleLengthMeters}
          onParChange={setNewHolePar}
          onNotesChange={setNewHoleNotes}
          onIsActiveChange={() => {}}
        />
      )}

      {isLoading && <p>Laddar hål…</p>}

      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && holes.length === 0 && (
        <p className="muted-text">Inga hål finns på banan ännu.</p>
      )}

      {!isLoading && holes.length > 0 && (
        <div className="admin-hole-list">
          {holes.map((hole) => (
            <div
              key={hole.id}
              className={`admin-hole-row ${
                hole.is_active === 0 ? 'inactive-card' : ''
              }`}
            >
              {editingHoleId === hole.id ? (
                <HoleForm
                  title={`Redigera hål ${hole.hole_number}`}
                  holeNumber={editHoleNumber}
                  name={editHoleName}
                  lengthMeters={editHoleLengthMeters}
                  par={editHolePar}
                  notes={editHoleNotes}
                  isActive={editHoleIsActive}
                  showActiveField
                  isSaving={isSaving}
                  submitText="Spara ändringar"
                  onSubmit={handleUpdateHole}
                  onCancel={cancelEditHole}
                  onHoleNumberChange={setEditHoleNumber}
                  onNameChange={setEditHoleName}
                  onLengthMetersChange={setEditHoleLengthMeters}
                  onParChange={setEditHolePar}
                  onNotesChange={setEditHoleNotes}
                  onIsActiveChange={setEditHoleIsActive}
                />
              ) : (
                <div className="admin-hole-content">
                    <div className="admin-hole-main-row">
                        <div>
                        <strong>
                            Hål {hole.hole_number}
                            {hole.name ? ` - ${hole.name}` : ''}
                        </strong>
                        <p>
                            {hole.length_meters} m · Par {hole.par_value}
                        </p>
                        {hole.notes && (
                            <p className="public-description">{hole.notes}</p>
                        )}
                        </div>

                        <div className="admin-row-actions">
                        {hole.is_active === 0 && (
                            <span className="inactive-badge">Inaktiv</span>
                        )}

                        <button
                            className="icon-action-button"
                            title="Redigera hål"
                            onClick={() => startEditHole(hole)}
                            disabled={isSaving}
                        >
                            ✎
                        </button>

                        <button
                            className="icon-action-button danger"
                            title="Radera eller inaktivera hål"
                            onClick={() => void handleDeleteHole(hole)}
                            disabled={isSaving}
                        >
                            🗑
                        </button>
                        </div>
                    </div>

                    <AdminTeesSection
                        hole={hole}
                        token={token}
                    />

                    <AdminBasketsSection
                        hole={hole}
                        token={token}
                    />
                    </div>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

interface HoleFormProps {
  title: string
  holeNumber: string
  name: string
  lengthMeters: string
  par: string
  notes: string
  isActive: boolean
  showActiveField: boolean
  isSaving: boolean
  submitText: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
  onHoleNumberChange: (value: string) => void
  onNameChange: (value: string) => void
  onLengthMetersChange: (value: string) => void
  onParChange: (value: string) => void
  onNotesChange: (value: string) => void
  onIsActiveChange: (value: boolean) => void
}

function HoleForm({
  title,
  holeNumber,
  name,
  lengthMeters,
  par,
  notes,
  isActive,
  showActiveField,
  isSaving,
  submitText,
  onSubmit,
  onCancel,
  onHoleNumberChange,
  onNameChange,
  onLengthMetersChange,
  onParChange,
  onNotesChange,
  onIsActiveChange,
}: HoleFormProps) {
  return (
    <form className="admin-form full-width" onSubmit={onSubmit}>
      <h4>{title}</h4>

      <div className="admin-form-grid">
        <label>
          Hålnummer
          <input
            type="number"
            min="1"
            value={holeNumber}
            onChange={(event) => onHoleNumberChange(event.target.value)}
            required
          />
        </label>

        <label>
          Längd, meter
          <input
            type="number"
            min="1"
            value={lengthMeters}
            onChange={(event) => onLengthMetersChange(event.target.value)}
            required
          />
        </label>

        <label>
          Par
          <input
            type="number"
            min="1"
            value={par}
            onChange={(event) => onParChange(event.target.value)}
            required
          />
        </label>

        <label>
          Namn
          <input
            type="text"
            value={name}
            onChange={(event) => onNameChange(event.target.value)}
            placeholder="Kan lämnas tomt"
          />
        </label>
      </div>

      <label>
        Anteckning
        <textarea
          value={notes}
          onChange={(event) => onNotesChange(event.target.value)}
          rows={3}
          placeholder="Kan lämnas tomt"
        />
      </label>

      {showActiveField && (
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={isActive}
            onChange={(event) => onIsActiveChange(event.target.checked)}
          />
          Aktivt hål
        </label>
      )}

      <div className="admin-form-actions">
        <button className="primary-button" type="submit" disabled={isSaving}>
          {isSaving ? 'Sparar…' : submitText}
        </button>

        <button
          className="secondary-button"
          type="button"
          onClick={onCancel}
          disabled={isSaving}
        >
          Avbryt
        </button>
      </div>
    </form>
  )
}

function parseHoleForm(
  holeNumberText: string,
  lengthMetersText: string,
  parText: string,
):
  | {
      ok: true
      holeNumber: number
      lengthMeters: number
      parValue: number
    }
  | {
      ok: false
      message: string
    } {
  const holeNumber = Number(holeNumberText)
  const lengthMeters = Number(lengthMetersText)
  const parValue = Number(parText)

  if (!Number.isInteger(holeNumber) || holeNumber <= 0) {
    return { ok: false, message: 'Hålnummer måste vara ett positivt heltal.' }
  }

  if (!Number.isInteger(lengthMeters) || lengthMeters <= 0) {
    return { ok: false, message: 'Längd måste vara ett positivt heltal.' }
  }

  if (!Number.isInteger(parValue) || parValue <= 0) {
    return { ok: false, message: 'Par måste vara ett positivt heltal.' }
  }

  return {
    ok: true,
    holeNumber,
    lengthMeters,
    parValue,
  }
}

function errorText(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} ${error.responseBody}`
  }

  return fallback
}