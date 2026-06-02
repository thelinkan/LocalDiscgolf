import { type FormEvent, useEffect, useState } from 'react'
import {
  ApiError,
  createHoleBasket,
  deleteHoleBasket,
  getHoleBaskets,
  updateHoleBasket,
  type HoleBasketApiResponse,
  type PublicCourseHoleApiResponse,
} from '../api'

interface AdminBasketsSectionProps {
  hole: PublicCourseHoleApiResponse
  token: string
}

export default function AdminBasketsSection({
  hole,
  token,
}: AdminBasketsSectionProps) {
  const [baskets, setBaskets] = useState<HoleBasketApiResponse[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showAddForm, setShowAddForm] = useState(false)
  const [newName, setNewName] = useState('')

  const [editingBasketId, setEditingBasketId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editSortOrder, setEditSortOrder] = useState('')
  const [editIsActive, setEditIsActive] = useState(true)

  useEffect(() => {
    void loadBaskets()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hole.id, includeInactive])

  async function loadBaskets() {
    setIsLoading(true)
    setError(null)

    try {
      const data = await getHoleBaskets(hole.id, includeInactive)
      setBaskets(data)
    } catch (err) {
      setError(errorText(err, 'Kunde inte hämta korgar.'))
    } finally {
      setIsLoading(false)
    }
  }

  function startEdit(basket: HoleBasketApiResponse) {
    setEditingBasketId(basket.id)
    setEditName(basket.name)
    setEditSortOrder(String(basket.sort_order))
    setEditIsActive(basket.is_active === 1)
  }

  function cancelEdit() {
    setEditingBasketId(null)
    setEditName('')
    setEditSortOrder('')
    setEditIsActive(true)
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (newName.trim() === '') {
      setError('Namn på korg måste fyllas i.')
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await createHoleBasket(token, hole.id, {
        name: newName.trim(),
      })

      setNewName('')
      setShowAddForm(false)
      await loadBaskets()
    } catch (err) {
      setError(errorText(err, 'Kunde inte skapa korg.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (editingBasketId === null) {
      return
    }

    if (editName.trim() === '') {
      setError('Namn på korg måste fyllas i.')
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
      await updateHoleBasket(token, editingBasketId, {
        name: editName.trim(),
        sort_order: sortOrder,
        is_active: editIsActive,
      })

      cancelEdit()
      await loadBaskets()
    } catch (err) {
      setError(errorText(err, 'Kunde inte uppdatera korg.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(basket: HoleBasketApiResponse) {
    const confirmed = window.confirm(
      `Vill du ta bort eller inaktivera korgen "${basket.name}"?\n\n` +
        'Om korgen används i en hålvariant kommer den att inaktiveras i stället för att raderas.',
    )

    if (!confirmed) {
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await deleteHoleBasket(token, basket.id)
      await loadBaskets()
    } catch (err) {
      setError(errorText(err, 'Kunde inte ta bort eller inaktivera korg.'))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="nested-admin-section">
      <div className="nested-admin-header">
        <h4>Korgar</h4>

        <div className="admin-header-actions">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(event) => setIncludeInactive(event.target.checked)}
            />
            Visa inaktiva korgar
          </label>

          <button
            className="secondary-button small-button"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            {showAddForm ? 'Stäng' : 'Lägg till korg'}
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
              placeholder="Exempel: Standard, Lång, Kort"
              required
            />
          </label>

          <div className="admin-form-actions">
            <button className="primary-button" type="submit" disabled={isSaving}>
              {isSaving ? 'Sparar…' : 'Spara korg'}
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

      {isLoading && <p>Laddar korgar…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && baskets.length === 0 && (
        <p className="muted-text">Inga korgar finns på hålet.</p>
      )}

      {!isLoading && baskets.length > 0 && (
        <div className="nested-admin-list">
          {baskets.map((basket) => (
            <div
              key={basket.id}
              className={`nested-admin-row ${
                basket.is_active === 0 ? 'inactive-card' : ''
              }`}
            >
              {editingBasketId === basket.id ? (
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
                    Aktiv korg
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
                    <strong>{basket.name}</strong>
                    <p>Sortering: {basket.sort_order}</p>
                  </div>

                  <div className="admin-row-actions">
                    {basket.is_active === 0 && (
                      <span className="inactive-badge">Inaktiv</span>
                    )}

                    <button
                      className="icon-action-button"
                      title="Redigera korg"
                      onClick={() => startEdit(basket)}
                      disabled={isSaving}
                    >
                      ✎
                    </button>

                    <button
                      className="icon-action-button danger"
                      title="Ta bort eller inaktivera korg"
                      onClick={() => void handleDelete(basket)}
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