import { type FormEvent, useEffect, useState } from 'react'
import {
  ApiError,
  createHoleVariant,
  deleteHoleVariant,
  getHoleBaskets,
  getHoleTees,
  getHoleVariants,
  updateHoleVariant,
  type HoleBasketApiResponse,
  type HoleTeeApiResponse,
  type HoleVariantApiResponse,
  type PublicCourseHoleApiResponse,
} from '../api'

interface AdminVariantsSectionProps {
  hole: PublicCourseHoleApiResponse
  token: string
}

export default function AdminVariantsSection({
  hole,
  token,
}: AdminVariantsSectionProps) {
  const [variants, setVariants] = useState<HoleVariantApiResponse[]>([])
  const [tees, setTees] = useState<HoleTeeApiResponse[]>([])
  const [baskets, setBaskets] = useState<HoleBasketApiResponse[]>([])

  const [includeInactive, setIncludeInactive] = useState(false)
  const [includeInactivePositions, setIncludeInactivePositions] = useState(false)

  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [showAddForm, setShowAddForm] = useState(false)

  const [newTeeId, setNewTeeId] = useState('')
  const [newBasketId, setNewBasketId] = useState('')
  const [newLengthMeters, setNewLengthMeters] = useState('')
  const [newPar, setNewPar] = useState('3')

  const [editingVariantId, setEditingVariantId] = useState<number | null>(null)
  const [editTeeId, setEditTeeId] = useState('')
  const [editBasketId, setEditBasketId] = useState('')
  const [editLengthMeters, setEditLengthMeters] = useState('')
  const [editPar, setEditPar] = useState('')
  const [editIsActive, setEditIsActive] = useState(true)

  useEffect(() => {
    void loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hole.id, includeInactive, includeInactivePositions])

  async function loadData() {
    setIsLoading(true)
    setError(null)

    try {
      const [variantData, teeData, basketData] = await Promise.all([
        getHoleVariants(hole.id, includeInactive),
        getHoleTees(hole.id, includeInactivePositions),
        getHoleBaskets(hole.id, includeInactivePositions),
      ])

      setVariants(variantData)
      setTees(teeData)
      setBaskets(basketData)
    } catch (err) {
      setError(errorText(err, 'Kunde inte hämta hålvarianter.'))
    } finally {
      setIsLoading(false)
    }
  }

  function activeTees() {
    return tees.filter((tee) => tee.is_active === 1)
  }

  function activeBaskets() {
    return baskets.filter((basket) => basket.is_active === 1)
  }

  function resetNewForm() {
    setNewTeeId('')
    setNewBasketId('')
    setNewLengthMeters('')
    setNewPar('3')
  }

  function startEdit(variant: HoleVariantApiResponse) {
    setEditingVariantId(variant.id)
    setEditTeeId(String(variant.tee_id))
    setEditBasketId(String(variant.basket_id))
    setEditLengthMeters(String(variant.length_meters))
    setEditPar(String(variant.par_value))
    setEditIsActive(variant.is_active === 1)
  }

  function cancelEdit() {
    setEditingVariantId(null)
    setEditTeeId('')
    setEditBasketId('')
    setEditLengthMeters('')
    setEditPar('')
    setEditIsActive(true)
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const parsed = parseVariantForm(newTeeId, newBasketId, newLengthMeters, newPar)

    if (!parsed.ok) {
      setError(parsed.message)
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await createHoleVariant(token, hole.id, {
        tee_id: parsed.teeId,
        basket_id: parsed.basketId,
        length_meters: parsed.lengthMeters,
        par_value: parsed.parValue,
      })

      resetNewForm()
      setShowAddForm(false)
      await loadData()
    } catch (err) {
      setError(errorText(err, 'Kunde inte skapa hålvariant.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (editingVariantId === null) {
      return
    }

    const parsed = parseVariantForm(
      editTeeId,
      editBasketId,
      editLengthMeters,
      editPar,
    )

    if (!parsed.ok) {
      setError(parsed.message)
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await updateHoleVariant(token, editingVariantId, {
        tee_id: parsed.teeId,
        basket_id: parsed.basketId,
        length_meters: parsed.lengthMeters,
        par_value: parsed.parValue,
        is_active: editIsActive,
      })

      cancelEdit()
      await loadData()
    } catch (err) {
      setError(errorText(err, 'Kunde inte uppdatera hålvariant.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(variant: HoleVariantApiResponse) {
    const confirmed = window.confirm(
      `Vill du ta bort eller inaktivera hålvarianten ${variant.tee_name ?? '?'} → ${variant.basket_name ?? '?'}?\n\n` +
        'Om varianten används i layout eller rundhistorik kommer den att inaktiveras i stället för att raderas.',
    )

    if (!confirmed) {
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await deleteHoleVariant(token, variant.id)
      await loadData()
    } catch (err) {
      setError(errorText(err, 'Kunde inte ta bort eller inaktivera hålvariant.'))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="nested-admin-section">
      <div className="nested-admin-header">
        <h4>Hålvarianter</h4>

        <div className="admin-header-actions">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(event) => setIncludeInactive(event.target.checked)}
            />
            Visa inaktiva varianter
          </label>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactivePositions}
              onChange={(event) =>
                setIncludeInactivePositions(event.target.checked)
              }
            />
            Visa inaktiva utkast/korgar
          </label>

          <button
            className="secondary-button small-button"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            {showAddForm ? 'Stäng' : 'Skapa hålvariant'}
          </button>
        </div>
      </div>

      {showAddForm && (
        <VariantForm
          title="Skapa hålvariant"
          tees={activeTees()}
          baskets={activeBaskets()}
          teeId={newTeeId}
          basketId={newBasketId}
          lengthMeters={newLengthMeters}
          par={newPar}
          isActive={true}
          showActiveField={false}
          isSaving={isSaving}
          submitText="Spara hålvariant"
          onSubmit={handleCreate}
          onCancel={() => {
            resetNewForm()
            setShowAddForm(false)
          }}
          onTeeIdChange={setNewTeeId}
          onBasketIdChange={setNewBasketId}
          onLengthMetersChange={setNewLengthMeters}
          onParChange={setNewPar}
          onIsActiveChange={() => {}}
        />
      )}

      {isLoading && <p>Laddar hålvarianter…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && variants.length === 0 && (
        <p className="muted-text">Inga hålvarianter finns på hålet.</p>
      )}

      {!isLoading && variants.length > 0 && (
        <div className="nested-admin-list">
          {variants.map((variant) => (
            <div
              key={variant.id}
              className={`nested-admin-row ${
                variant.is_active === 0 ? 'inactive-card' : ''
              }`}
            >
              {editingVariantId === variant.id ? (
                <VariantForm
                  title="Redigera hålvariant"
                  tees={tees}
                  baskets={baskets}
                  teeId={editTeeId}
                  basketId={editBasketId}
                  lengthMeters={editLengthMeters}
                  par={editPar}
                  isActive={editIsActive}
                  showActiveField
                  isSaving={isSaving}
                  submitText="Spara"
                  onSubmit={handleUpdate}
                  onCancel={cancelEdit}
                  onTeeIdChange={setEditTeeId}
                  onBasketIdChange={setEditBasketId}
                  onLengthMetersChange={setEditLengthMeters}
                  onParChange={setEditPar}
                  onIsActiveChange={setEditIsActive}
                />
              ) : (
                <>
                  <div>
                    <strong>
                      {variant.tee_name ?? 'Utkast ?'} →{' '}
                      {variant.basket_name ?? 'Korg ?'}
                    </strong>
                    <p>
                      {variant.length_meters} m · Par {variant.par_value}
                    </p>
                  </div>

                  <div className="admin-row-actions">
                    {variant.is_active === 0 && (
                      <span className="inactive-badge">Inaktiv</span>
                    )}

                    <button
                      className="icon-action-button"
                      title="Redigera hålvariant"
                      onClick={() => startEdit(variant)}
                      disabled={isSaving}
                    >
                      ✎
                    </button>

                    <button
                      className="icon-action-button danger"
                      title="Ta bort eller inaktivera hålvariant"
                      onClick={() => void handleDelete(variant)}
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

interface VariantFormProps {
  title: string
  tees: HoleTeeApiResponse[]
  baskets: HoleBasketApiResponse[]
  teeId: string
  basketId: string
  lengthMeters: string
  par: string
  isActive: boolean
  showActiveField: boolean
  isSaving: boolean
  submitText: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
  onTeeIdChange: (value: string) => void
  onBasketIdChange: (value: string) => void
  onLengthMetersChange: (value: string) => void
  onParChange: (value: string) => void
  onIsActiveChange: (value: boolean) => void
}

function VariantForm({
  title,
  tees,
  baskets,
  teeId,
  basketId,
  lengthMeters,
  par,
  isActive,
  showActiveField,
  isSaving,
  submitText,
  onSubmit,
  onCancel,
  onTeeIdChange,
  onBasketIdChange,
  onLengthMetersChange,
  onParChange,
  onIsActiveChange,
}: VariantFormProps) {
  return (
    <form className="admin-form compact-form" onSubmit={onSubmit}>
      <h4>{title}</h4>

      <div className="admin-form-grid two-columns">
        <label>
          Utkast
          <select
            value={teeId}
            onChange={(event) => onTeeIdChange(event.target.value)}
            required
          >
            <option value="">Välj utkast</option>
            {tees.map((tee) => (
              <option key={tee.id} value={tee.id}>
                {tee.name}
                {tee.is_active === 0 ? ' (inaktiv)' : ''}
              </option>
            ))}
          </select>
        </label>

        <label>
          Korg
          <select
            value={basketId}
            onChange={(event) => onBasketIdChange(event.target.value)}
            required
          >
            <option value="">Välj korg</option>
            {baskets.map((basket) => (
              <option key={basket.id} value={basket.id}>
                {basket.name}
                {basket.is_active === 0 ? ' (inaktiv)' : ''}
              </option>
            ))}
          </select>
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
      </div>

      {showActiveField && (
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={isActive}
            onChange={(event) => onIsActiveChange(event.target.checked)}
          />
          Aktiv hålvariant
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

function parseVariantForm(
  teeIdText: string,
  basketIdText: string,
  lengthMetersText: string,
  parText: string,
):
  | {
      ok: true
      teeId: number
      basketId: number
      lengthMeters: number
      parValue: number
    }
  | {
      ok: false
      message: string
    } {
  const teeId = Number(teeIdText)
  const basketId = Number(basketIdText)
  const lengthMeters = Number(lengthMetersText)
  const parValue = Number(parText)

  if (!Number.isInteger(teeId) || teeId <= 0) {
    return { ok: false, message: 'Välj ett utkast.' }
  }

  if (!Number.isInteger(basketId) || basketId <= 0) {
    return { ok: false, message: 'Välj en korg.' }
  }

  if (!Number.isInteger(lengthMeters) || lengthMeters <= 0) {
    return { ok: false, message: 'Längd måste vara ett positivt heltal.' }
  }

  if (!Number.isInteger(parValue) || parValue <= 0) {
    return { ok: false, message: 'Par måste vara ett positivt heltal.' }
  }

  return {
    ok: true,
    teeId,
    basketId,
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