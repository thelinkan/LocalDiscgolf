import type {
  PublicCourseApiResponse,
  PublicCourseHoleApiResponse,
  PublicLayoutApiResponse,
  PublicLayoutHoleApiResponse,
} from '../api'

interface PublicCoursesPageProps {
  courses: PublicCourseApiResponse[]
  isLoading: boolean
  error: string | null
  onBack: () => void
  onCourseClick: (course: PublicCourseApiResponse) => void
}

export function PublicCoursesPage({
  courses,
  isAdmin,
  includeInactive,
  isLoading,
  error,
  onBack,
  onCourseClick,
  onIncludeInactiveChange,
  onAddCourse,
  onEditCourse,
  onDeleteCourse,
}: PublicCoursesPageProps) {
  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>

        <h2>Banor</h2>
      </div>

      {isAdmin && (
        <section className="admin-toolbar-card">
          <button className="primary-button" onClick={onAddCourse}>
            Lägg till bana
          </button>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(event) =>
                onIncludeInactiveChange(event.target.checked)
              }
            />
            Visa inaktiva banor
          </label>
        </section>
      )}

      {isLoading && <p>Laddar banor…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && courses.length === 0 && (
        <section className="list-card">
          <p className="muted-text">Inga banor hittades.</p>
        </section>
      )}

      {!isLoading && !error && courses.length > 0 && (
        <section className="public-list">
          {courses.map((course) => (
            <button
              key={course.id}
              className={`public-card ${
                course.is_active === 0 ? 'inactive-card' : ''
              }`}
              onClick={() => onCourseClick(course)}
            >
              <div>
                <div className="layout-title-row">
                  <h3>{course.name}</h3>

                  {course.is_active === 0 && (
                    <span className="inactive-badge">Inaktiv</span>
                  )}
                </div>

                <p>
                  {course.hole_count} hål · {course.layout_count} aktiva layouter
                </p>
              </div>

              <div className="public-card-actions">
                {isAdmin && (
                  <>
                    <span
                      role="button"
                      tabIndex={0}
                      className="icon-action-button"
                      title="Redigera bana"
                      onClick={(event) => {
                        event.stopPropagation()
                        onEditCourse(course)
                      }}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.stopPropagation()
                          onEditCourse(course)
                        }
                      }}
                    >
                      ✎
                    </span>

                    <span
                      role="button"
                      tabIndex={0}
                      className="icon-action-button danger"
                      title="Ta bort bana"
                      onClick={(event) => {
                        event.stopPropagation()
                        onDeleteCourse(course)
                      }}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.stopPropagation()
                          onDeleteCourse(course)
                        }
                      }}
                    >
                      🗑
                    </span>
                  </>
                )}

                <span className="public-card-arrow">›</span>
              </div>
            </button>
          ))}
        </section>
      )}
    </main>
  )
}

interface PublicCoursesPageProps {
  courses: PublicCourseApiResponse[]
  isAdmin: boolean
  includeInactive: boolean
  isLoading: boolean
  error: string | null
  onBack: () => void
  onCourseClick: (course: PublicCourseApiResponse) => void
  onIncludeInactiveChange: (checked: boolean) => void
  onAddCourse: () => void
  onEditCourse: (course: PublicCourseApiResponse) => void
  onDeleteCourse: (course: PublicCourseApiResponse) => void
}

interface PublicLayoutsPageProps {
  course: PublicCourseApiResponse
  layouts: PublicLayoutApiResponse[]
  includeInactive: boolean
  isAdmin: boolean
  holes: PublicCourseHoleApiResponse[]
  isLoadingHoles: boolean
  holesError: string | null
  isLoading: boolean
  error: string | null
  onBack: () => void
  onIncludeInactiveChange: (checked: boolean) => void
  onLayoutClick: (layout: PublicLayoutApiResponse) => void
  onAddHole: () => void
}

export function PublicLayoutsPage({
  course,
  layouts,
  includeInactive,
  isAdmin,
  holes,
  isLoadingHoles,
  holesError,
  isLoading,
  error,
  onBack,
  onIncludeInactiveChange,
  onLayoutClick,
  onAddHole,
}: PublicLayoutsPageProps) {
  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>

        <div>
          <h2>{course.name}</h2>
          <p className="muted-text">Layouter</p>
        </div>
      </div>

      <section className="public-options-card">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(event) => onIncludeInactiveChange(event.target.checked)}
          />
          Visa inaktiva layouter
        </label>
      </section>

      {isLoading && <p>Laddar layouter…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && layouts.length === 0 && (
        <section className="list-card">
          <p className="muted-text">Inga layouter hittades.</p>
        </section>
      )}

      {!isLoading && !error && layouts.length > 0 && (
        <section className="public-list">
          {layouts.map((layout) => (
            <button
              key={layout.id}
              className={`public-card ${layout.is_active === 0 ? 'inactive-card' : ''}`}
              onClick={() => onLayoutClick(layout)}
            >
              <div>
                <div className="layout-title-row">
                  <h3>{layout.name}</h3>
                  {layout.is_active === 0 && (
                    <span className="inactive-badge">Inaktiv</span>
                  )}
                </div>

                <p>
                  Par {layout.total_par} · {layout.hole_count} hål ·{' '}
                  {layout.total_length_meters} meter
                </p>

                {layout.description && (
                  <p className="public-description">{layout.description}</p>
                )}
              </div>

              <span className="public-card-arrow">›</span>
            </button>
          ))}
        </section>
      )}

      {isAdmin && (
        <section className="admin-course-section">
          <div className="admin-section-header">
            <h3>Hål</h3>
            <button className="primary-button" onClick={onAddHole}>
              Lägg till hål
            </button>
          </div>

          {isLoadingHoles && <p>Laddar hål…</p>}

          {holesError && <p className="error-message">{holesError}</p>}

          {!isLoadingHoles && !holesError && holes.length === 0 && (
            <p className="muted-text">Inga hål finns på banan ännu.</p>
          )}

          {!isLoadingHoles && !holesError && holes.length > 0 && (
            <div className="admin-hole-list">
              {holes.map((hole) => (
                <div
                  key={hole.id}
                  className={`admin-hole-row ${
                    hole.is_active === 0 ? 'inactive-card' : ''
                  }`}
                >
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

                  {hole.is_active === 0 && (
                    <span className="inactive-badge">Inaktiv</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      )}
    </main>
  )
}

interface PublicLayoutDetailPageProps {
  course: PublicCourseApiResponse
  layout: PublicLayoutApiResponse
  holes: PublicLayoutHoleApiResponse[]
  isLoading: boolean
  error: string | null
  onBack: () => void
}

export function PublicLayoutDetailPage({
  course,
  layout,
  holes,
  isLoading,
  error,
  onBack,
}: PublicLayoutDetailPageProps) {
  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>

        <div>
          <h2>{layout.name}</h2>
          <p className="muted-text">{course.name}</p>
        </div>
      </div>

      <section className="layout-summary-card">
        <div className="layout-title-row">
          <h3>{course.name} - {layout.name}</h3>
          {layout.is_active === 0 && (
            <span className="inactive-badge">Inaktiv</span>
          )}
        </div>

        <p>
          Par {layout.total_par}, {layout.hole_count} hål,{' '}
          {layout.total_length_meters} meter
        </p>

        {layout.description && <p>{layout.description}</p>}
      </section>

      {isLoading && <p>Laddar hål…</p>}
      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && (
        <section className="hole-list-card">
          <h3>Hål</h3>

          {holes.map((hole) => (
            <div className="layout-hole-row" key={hole.sequence_number}>
              <div>
                <h4>
                  Hål {hole.hole_number}
                  {hole.hole_name ? ` - ${hole.hole_name}` : ''}
                </h4>
                <p>
                  Utkast: {hole.tee_name ?? '-'} | Korg: {hole.basket_name ?? '-'}
                </p>
              </div>

              <div className="hole-values">
                <strong>{hole.length_meters} m</strong>
                <span>Par {hole.par_value}</span>
              </div>
            </div>
          ))}
        </section>
      )}
    </main>
  )
}
