import { type FormEvent, useEffect, useState } from 'react'
import {
  ApiError,
  createHoleTee,
  deleteHoleTee,
  getHoleTees,
  updateHoleTee,
  type HoleTeeApiResponse,
  type PublicCourseHoleApiResponse,
} from '../api'

interface AdminTeesSectionProps {
  hole: PublicCourseHoleApiResponse
  token: string
}

export default function AdminTeesSection({
  hole,
  token,
}: AdminTeesSectionProps) {
  const [tees, setTees] = useState<HoleTeeApiResponse[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showAddForm, setShowAddForm] = useState(false)
  const [newName, setNewName] = useState('')

  const [editingTeeId, setEditingTeeId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editSortOrder, setEditSortOrder] = useState('')
  const [editIsActive, setEditIsActive] = useState(true)

  useEffect(() => {
    void loadTees()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hole.id, includeInactive])

  async function loadTees() {
    setIsLoading(true)
    setError(null)

    try {
      const data = await getHoleTees(hole.id, includeInactive)
      setTees(data)
    } catch (err) {
      setError(errorText(err, 'Kunde inte hämta utkast.'))
    } finally {
      setIsLoading(false)
    }
  }

  function startEdit(tee: HoleTeeApiResponse) {
    setEditingTeeId(tee.id)
    setEditName(tee.name)
    setEditSortOrder(String(tee.sort_order))
    setEditIsActive(tee.is_active === 1)
  }

  function cancelEdit() {
    setEditingTeeId(null)
    setEditName('')
    setEditSortOrder('')
    setEditIsActive(true)
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (newName.trim() === '') {
      setError('Namn på utkast måste fyllas i.')
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await createHoleTee(token, hole.id, {
        name: newName.trim(),
      })

      setNewName('')
      setShowAddForm(false)
      await loadTees()
    } catch (err) {
      setError(errorText(err, 'Kunde inte skapa utkast.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (editingTeeId === null) {
      return
    }

    if (editName.trim() === '') {
      setError('Namn på utkast måste fyllas i.')
      return
    }

    const sortOrder = Number(editSortOrder)

    if (!Number.isInteger(sortOrder) || sortOrder <= 0) {
      setError('Sortering måste vara ett positivt heltal.')
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await updateHoleTee(token, editingTeeId, {
        name: editName.trim(),
        sort_order: sortOrder,
        is_active: editIsActive,
      })

      cancelEdit()
      await loadTees()
    } catch (err) {
      setError(errorText(err, 'Kunde inte uppdatera utkast.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(tee: HoleTeeApiResponse) {
    const confirmed = window.confirm(
      `Vill du ta bort eller inaktivera utkastet "${tee.name}"?\n\n` +
        'Om utkastet används i en hålvariant kommer det att inaktiveras i stället för att raderas.',
    )

    if (!confirmed) {
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await deleteHoleTee(token, tee.id)
      await loadTees()
    } catch (err) {
      setError(errorText(err, 'Kunde inte ta bort eller inaktivera utkast.'))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="nested-admin-section">
      <div className="nested-admin-header">
        <h4>Utkast</h4>

        <div className="admin-header-actions">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(event) => setIncludeInactive(event.target.checked)}
            />
            Visa inaktiva utkast
          </label>

          <button
            className="secondary-button small-button"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            {showAddForm ? 'Stäng' : 'Lägg till utkast'}
          </button>
        </div>
      </div>

      {showAddForm && (
        <form className="admin-form compact-form" onSubmit={handleCreate}>
          <label>
            Namn
            <input
              type="text"
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
              placeholder="Exempel: Standard, Gul, Blå"
              required
            />
          </label>

          <div className="admin-form-actions">
            <button className="primary-button" type="submit" disabled={isSaving}>
              {isSaving ? 'Sparar…' : 'Spara utkast'}
            </button>

            <button
              className="secondary-button"
              type="button"
              onClick={() => {
                setNewName('')
                setShowAddForm(false)
              }}
              disabled={isSaving}
            >
              Avbryt
            </button>
          </div>
        </form>
      )}

      {isLoading && <p>Laddar utkast…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && tees.length === 0 && (
        <p className="muted-text">Inga utkast finns på hålet.</p>
      )}

      {!isLoading && tees.length > 0 && (
        <div className="nested-admin-list">
          {tees.map((tee) => (
            <div
              key={tee.id}
              className={`nested-admin-row ${
                tee.is_active === 0 ? 'inactive-card' : ''
              }`}
            >
              {editingTeeId === tee.id ? (
                <form className="admin-form compact-form" onSubmit={handleUpdate}>
                  <div className="admin-form-grid two-columns">
                    <label>
                      Namn
                      <input
                        type="text"
                        value={editName}
                        onChange={(event) => setEditName(event.target.value)}
                        required
                      />
                    </label>

                    <label>
                      Sortering
                      <input
                        type="number"
                        min="1"
                        value={editSortOrder}
                        onChange={(event) =>
                          setEditSortOrder(event.target.value)
                        }
                        required
                      />
                    </label>
                  </div>

                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={editIsActive}
                      onChange={(event) =>
                        setEditIsActive(event.target.checked)
                      }
                    />
                    Aktivt utkast
                  </label>

                  <div className="admin-form-actions">
                    <button
                      className="primary-button"
                      type="submit"
                      disabled={isSaving}
                    >
                      Spara
                    </button>

                    <button
                      className="secondary-button"
                      type="button"
                      onClick={cancelEdit}
                      disabled={isSaving}
                    >
                      Avbryt
                    </button>
                  </div>
                </form>
              ) : (
                <>
                  <div>
                    <strong>{tee.name}</strong>
                    <p>Sortering: {tee.sort_order}</p>
                  </div>

                  <div className="admin-row-actions">
                    {tee.is_active === 0 && (
                      <span className="inactive-badge">Inaktiv</span>
                    )}

                    <button
                      className="icon-action-button"
                      title="Redigera utkast"
                      onClick={() => startEdit(tee)}
                      disabled={isSaving}
                    >
                      ✎
                    </button>

                    <button
                      className="icon-action-button danger"
                      title="Ta bort eller inaktivera utkast"
                      onClick={() => void handleDelete(tee)}
                      disabled={isSaving}
                    >
                      🗑
                    </button>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function errorText(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return `${fallback} ${error.responseBody}`
  }

  return fallback
}