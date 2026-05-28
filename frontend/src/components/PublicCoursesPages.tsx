import type {
  PublicCourseApiResponse,
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
  isLoading,
  error,
  onBack,
  onCourseClick,
}: PublicCoursesPageProps) {
  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>
        <h2>Banor</h2>
      </div>

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
              className="public-card"
              onClick={() => onCourseClick(course)}
            >
              <div>
                <h3>{course.name}</h3>
                <p>
                  {course.hole_count} hål · {course.layout_count} aktiva layouter
                </p>
              </div>
              <span className="public-card-arrow">›</span>
            </button>
          ))}
        </section>
      )}
    </main>
  )
}

interface PublicLayoutsPageProps {
  course: PublicCourseApiResponse
  layouts: PublicLayoutApiResponse[]
  includeInactive: boolean
  isLoading: boolean
  error: string | null
  onBack: () => void
  onIncludeInactiveChange: (checked: boolean) => void
  onLayoutClick: (layout: PublicLayoutApiResponse) => void
}

export function PublicLayoutsPage({
  course,
  layouts,
  includeInactive,
  isLoading,
  error,
  onBack,
  onIncludeInactiveChange,
  onLayoutClick,
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
