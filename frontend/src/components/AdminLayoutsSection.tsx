import { type FormEvent, useEffect, useState } from 'react'
import {
  ApiError,
  createLayout,
  deleteLayout,
  getCourseHoleVariants,
  getPublicCourseLayouts,
  getPublicLayoutHoles,
  updateLayout,
  type CourseHoleVariantApiResponse,
  type PublicCourseApiResponse,
  type PublicLayoutApiResponse,
  type PublicLayoutHoleApiResponse,
} from '../api'

interface AdminLayoutsSectionProps {
  course: PublicCourseApiResponse
  token: string
  onCourseChanged: () => void
}

interface EditableLayoutHole {
  localId: string
  holeVariantId: string
}

export default function AdminLayoutsSection({
  course,
  token,
  onCourseChanged,
}: AdminLayoutsSectionProps) {
  const [layouts, setLayouts] = useState<PublicLayoutApiResponse[]>([])
  const [variants, setVariants] = useState<CourseHoleVariantApiResponse[]>([])
  const [includeInactiveLayouts, setIncludeInactiveLayouts] = useState(false)
  const [includeInactiveVariants, setIncludeInactiveVariants] = useState(false)

  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showAddForm, setShowAddForm] = useState(false)

  const [editingLayoutId, setEditingLayoutId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [isActive, setIsActive] = useState(true)
  const [layoutHoles, setLayoutHoles] = useState<EditableLayoutHole[]>([])

  useEffect(() => {
    void loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [course.id, includeInactiveLayouts, includeInactiveVariants])

  async function loadData() {
    setIsLoading(true)
    setError(null)

    try {
      const [layoutData, variantData] = await Promise.all([
        getPublicCourseLayouts(course.id, includeInactiveLayouts),
        getCourseHoleVariants(course.id, includeInactiveVariants),
      ])

      setLayouts(layoutData)
      setVariants(variantData)
    } catch (err) {
      setError(errorText(err, 'Kunde inte hämta layouter.'))
    } finally {
      setIsLoading(false)
    }
  }

  function resetForm() {
    setEditingLayoutId(null)
    setName('')
    setDescription('')
    setIsActive(true)
    setLayoutHoles([])
  }

  function addLayoutHole() {
    const firstVariant = variants.find(
      (variant) => variant.hole_is_active === 1 && variant.variant_is_active === 1,
    )

    setLayoutHoles((current) => [
      ...current,
      {
        localId: crypto.randomUUID(),
        holeVariantId: firstVariant ? String(firstVariant.hole_variant_id) : '',
      },
    ])
  }

  function updateLayoutHole(localId: string, holeVariantId: string) {
    setLayoutHoles((current) =>
      current.map((row) =>
        row.localId === localId ? { ...row, holeVariantId } : row,
      ),
    )
  }

  function removeLayoutHole(localId: string) {
    setLayoutHoles((current) =>
      current.filter((row) => row.localId !== localId),
    )
  }

  function moveLayoutHole(localId: string, direction: -1 | 1) {
    setLayoutHoles((current) => {
      const index = current.findIndex((row) => row.localId === localId)

      if (index < 0) {
        return current
      }

      const newIndex = index + direction

      if (newIndex < 0 || newIndex >= current.length) {
        return current
      }

      const copy = [...current]
      const [item] = copy.splice(index, 1)
      copy.splice(newIndex, 0, item)

      return copy
    })
  }

  async function startEdit(layout: PublicLayoutApiResponse) {
    setIsLoading(true)
    setError(null)

    try {
      const holes = await getPublicLayoutHoles(layout.id)

      setEditingLayoutId(layout.id)
      setShowAddForm(true)
      setName(layout.name)
      setDescription(layout.description ?? '')
      setIsActive(layout.is_active === 1)
      setLayoutHoles(
        holes.map((hole) => ({
          localId: crypto.randomUUID(),
          holeVariantId: hole.hole_variant_id ? String(hole.hole_variant_id) : '',
        })),
      )
    } catch (err) {
      setError(errorText(err, 'Kunde inte hämta layoutens hål.'))
    } finally {
      setIsLoading(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (name.trim() === '') {
      setError('Layoutnamn måste fyllas i.')
      return
    }

    if (layoutHoles.length === 0) {
      setError('Layouten måste innehålla minst ett hål.')
      return
    }

    const preparedHoles = layoutHoles.map((row, index) => {
      const variantId = Number(row.holeVariantId)
      const variant = variants.find(
        (candidate) => candidate.hole_variant_id === variantId,
      )

      if (!variant) {
        throw new Error('En vald hålvariant hittades inte.')
      }

      return {
        hole_id: variant.hole_id,
        hole_variant_id: variant.hole_variant_id,
        sequence_number: index + 1,
      }
    })

    setIsSaving(true)
    setError(null)

    try {
      if (editingLayoutId === null) {
        await createLayout(token, course.id, {
          name: name.trim(),
          description: description.trim() ? description.trim() : null,
          holes: preparedHoles,
        })
      } else {
        await updateLayout(token, editingLayoutId, {
          name: name.trim(),
          description: description.trim() ? description.trim() : null,
          is_active: isActive,
          holes: preparedHoles,
        })
      }

      resetForm()
      setShowAddForm(false)
      await loadData()
      onCourseChanged()
    } catch (err) {
      setError(errorText(err, 'Kunde inte spara layout.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(layout: PublicLayoutApiResponse) {
    const confirmed = window.confirm(
      `Vill du ta bort eller inaktivera layouten "${layout.name}"?\n\n` +
        'Om layouten har använts i rundor kommer den att inaktiveras i stället för att raderas.',
    )

    if (!confirmed) {
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await deleteLayout(token, layout.id)
      await loadData()
      onCourseChanged()
    } catch (err) {
      setError(errorText(err, 'Kunde inte ta bort eller inaktivera layout.'))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="admin-course-section">
      <div className="admin-section-header">
        <h3>Layouter</h3>

        <div className="admin-header-actions">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactiveLayouts}
              onChange={(event) => setIncludeInactiveLayouts(event.target.checked)}
            />
            Visa inaktiva layouter
          </label>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactiveVariants}
              onChange={(event) => setIncludeInactiveVariants(event.target.checked)}
            />
            Visa inaktiva hålvarianter
          </label>

          <button
            className="primary-button"
            onClick={() => {
              if (showAddForm) {
                resetForm()
                setShowAddForm(false)
              } else {
                resetForm()
                setShowAddForm(true)
              }
            }}
          >
            {showAddForm ? 'Stäng formulär' : 'Skapa layout'}
          </button>
        </div>
      </div>

      {showAddForm && (
        <form className="admin-form" onSubmit={handleSubmit}>
          <h4>{editingLayoutId === null ? 'Skapa layout' : 'Redigera layout'}</h4>

          <div className="admin-form-grid two-columns">
            <label>
              Namn
              <input
                type="text"
                value={name}
                onChange={(event) => setName(event.target.value)}
                required
              />
            </label>

            {editingLayoutId !== null && (
              <label className="checkbox-label form-checkbox">
                <input
                  type="checkbox"
                  checked={isActive}
                  onChange={(event) => setIsActive(event.target.checked)}
                />
                Aktiv layout
              </label>
            )}
          </div>

          <label>
            Beskrivning
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={3}
              placeholder="Kan lämnas tom"
            />
          </label>

          <div className="layout-builder">
            <div className="admin-section-header compact-header">
              <h4>Hål i layouten</h4>
              <button
                className="secondary-button small-button"
                type="button"
                onClick={addLayoutHole}
                disabled={variants.length === 0}
              >
                Lägg till hålvariant
              </button>
            </div>

            {variants.length === 0 && (
              <p className="muted-text">
                Det finns inga valbara hålvarianter på banan.
              </p>
            )}

            {layoutHoles.map((row, index) => (
              <div className="layout-builder-row" key={row.localId}>
                <span className="layout-sequence-number">{index + 1}</span>

                <select
                  value={row.holeVariantId}
                  onChange={(event) =>
                    updateLayoutHole(row.localId, event.target.value)
                  }
                  required
                >
                  <option value="">Välj hålvariant</option>
                  {variants.map((variant) => (
                    <option
                      key={variant.hole_variant_id}
                      value={variant.hole_variant_id}
                    >
                      Hål {variant.hole_number}
                      {variant.hole_name ? ` - ${variant.hole_name}` : ''}
                      {' | '}
                      {variant.tee_name ?? 'Utkast ?'} →{' '}
                      {variant.basket_name ?? 'Korg ?'}
                      {' | '}
                      {variant.length_meters} m, par {variant.par_value}
                      {variant.variant_is_active === 0 ||
                      variant.hole_is_active === 0
                        ? ' (inaktiv)'
                        : ''}
                    </option>
                  ))}
                </select>

                <button
                  className="secondary-button small-button"
                  type="button"
                  onClick={() => moveLayoutHole(row.localId, -1)}
                  disabled={index === 0}
                >
                  ↑
                </button>

                <button
                  className="secondary-button small-button"
                  type="button"
                  onClick={() => moveLayoutHole(row.localId, 1)}
                  disabled={index === layoutHoles.length - 1}
                >
                  ↓
                </button>

                <button
                  className="icon-action-button danger"
                  type="button"
                  title="Ta bort från layout"
                  onClick={() => removeLayoutHole(row.localId)}
                >
                  🗑
                </button>
              </div>
            ))}
          </div>

          <div className="admin-form-actions">
            <button className="primary-button" type="submit" disabled={isSaving}>
              {isSaving ? 'Sparar…' : 'Spara layout'}
            </button>

            <button
              className="secondary-button"
              type="button"
              onClick={() => {
                resetForm()
                setShowAddForm(false)
              }}
              disabled={isSaving}
            >
              Avbryt
            </button>
          </div>
        </form>
      )}

      {isLoading && <p>Laddar layouter…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && layouts.length === 0 && (
        <p className="muted-text">Inga layouter finns på banan.</p>
      )}

      {!isLoading && layouts.length > 0 && (
        <div className="admin-hole-list">
          {layouts.map((layout) => (
            <div
              key={layout.id}
              className={`admin-hole-row ${
                layout.is_active === 0 ? 'inactive-card' : ''
              }`}
            >
              <div>
                <strong>{layout.name}</strong>
                <p>
                  Par {layout.total_par} · {layout.hole_count} hål ·{' '}
                  {layout.total_length_meters} m
                </p>
                {layout.description && (
                  <p className="public-description">{layout.description}</p>
                )}
              </div>

              <div className="admin-row-actions">
                {layout.is_active === 0 && (
                  <span className="inactive-badge">Inaktiv</span>
                )}

                <button
                  className="icon-action-button"
                  title="Redigera layout"
                  onClick={() => void startEdit(layout)}
                  disabled={isSaving}
                >
                  ✎
                </button>

                <button
                  className="icon-action-button danger"
                  title="Ta bort eller inaktivera layout"
                  onClick={() => void handleDelete(layout)}
                  disabled={isSaving}
                >
                  🗑
                </button>
              </div>
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

  if (error instanceof Error) {
    return `${fallback} ${error.message}`
  }

  return fallback
}