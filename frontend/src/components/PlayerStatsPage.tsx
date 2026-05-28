import type {
  CourseApiResponse,
  PlayerHoleStatsApiResponse,
  PlayerLayoutStatsApiResponse,
} from '../api'
import type { SelectablePlayer } from './PlayersPage'

interface PlayerStatsPageProps {
  player: SelectablePlayer
  courses: CourseApiResponse[]
  selectedCourseId: number | null
  layoutStats: PlayerLayoutStatsApiResponse[]
  holeStats: PlayerHoleStatsApiResponse[]
  isLoading: boolean
  error: string | null
  onBack: () => void
  onCourseSelected: (courseId: number | null) => void
  onRoundsClick: () => void
}

const decimalOne = new Intl.NumberFormat('sv-SE', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

const decimalTwo = new Intl.NumberFormat('sv-SE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function signedInteger(value: number): string {
  return value > 0 ? `+${value}` : `${value}`
}

function signedDecimal(value: number): string {
  const formatted = decimalOne.format(value)
  return value > 0 ? `+${formatted}` : formatted
}

export default function PlayerStatsPage({
  player,
  courses,
  selectedCourseId,
  layoutStats,
  holeStats,
  isLoading,
  error,
  onBack,
  onRoundsClick,
  onCourseSelected,
}: PlayerStatsPageProps) {
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
          <button className="secondary-button" onClick={onRoundsClick}>
            Rundor
          </button>
          <button className="primary-button">
            Statistik
          </button>
        </div>
      </div>
      <section className="filter-card">
        <label htmlFor="course-filter">Bana</label>
        <select
          id="course-filter"
          value={selectedCourseId ?? ''}
          onChange={(event) => {
            const value = event.target.value
            onCourseSelected(value === '' ? null : Number(value))
          }}
        >
          <option value="">Alla banor</option>
          {courses.map((course) => (
            <option key={course.id} value={course.id}>
              {course.name}
            </option>
          ))}
        </select>
      </section>

      {isLoading && <p>Laddar statistik…</p>}

      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && (
        <>
          <section className="stats-section">
            <h3>Per layout</h3>

            {layoutStats.length === 0 ? (
              <p className="muted-text">Ingen layoutstatistik ännu.</p>
            ) : (
              <div className="stats-grid">
                {layoutStats.map((stat) => (
                  <LayoutStatsCard
                    key={stat.layout_id}
                    stat={stat}
                  />
                ))}
              </div>
            )}
          </section>

          <section className="stats-section">
            <h3>Per hål</h3>

            {holeStats.length === 0 ? (
              <p className="muted-text">Ingen hålstatistik ännu.</p>
            ) : (
              <div className="stats-grid">
                {holeStats.map((stat) => (
                  <HoleStatsCard
                    key={`${stat.course_id}-${stat.hole_id}-${stat.hole_variant_id ?? 0}`}
                    stat={stat}
                  />
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </main>
  )
}

function LayoutStatsCard({
  stat,
}: {
  stat: PlayerLayoutStatsApiResponse
}) {
  return (
    <article className="stats-card">
      <h4>
        {stat.course_name} - {stat.layout_name}
      </h4>

      <p>
        Par {stat.total_par}, {stat.hole_count} hål,{' '}
        {stat.total_length_meters} meter
      </p>

      <dl className="stats-values">
        <div>
          <dt>Antal rundor</dt>
          <dd>{stat.round_count}</dd>
        </div>

        <div>
          <dt>Personbästa</dt>
          <dd>
            {stat.personal_best_throws}{' '}
            ({signedInteger(stat.personal_best_relative_to_par)})
          </dd>
        </div>

        <div>
          <dt>Snitt</dt>
          <dd>
            {decimalOne.format(stat.average_throws)}{' '}
            ({signedDecimal(stat.average_relative_to_par)})
          </dd>
        </div>

        {stat.last_10_average_throws !== null &&
          stat.last_10_average_relative_to_par !== null && (
            <div>
              <dt>Snitt 10 senaste</dt>
              <dd>
                {decimalOne.format(stat.last_10_average_throws)}{' '}
                ({signedDecimal(stat.last_10_average_relative_to_par)})
              </dd>
            </div>
          )}
      </dl>
    </article>
  )
}

function HoleStatsCard({
  stat,
}: {
  stat: PlayerHoleStatsApiResponse
}) {
  const holeTitle = stat.hole_name
    ? `${stat.course_name} - Hål ${stat.hole_number} - ${stat.hole_name}`
    : `${stat.course_name} - Hål ${stat.hole_number}`

  return (
    <article className="stats-card">
      <h4>{holeTitle}</h4>

      <p>
        Utkast: {stat.tee_name ?? '-'} | Korg: {stat.basket_name ?? '-'}
      </p>

      <p>
        Längd: {stat.length_meters} m, Par {stat.par_value}
      </p>

      <dl className="stats-values">
        <div>
          <dt>Spelat</dt>
          <dd>{stat.played_count} gånger</dd>
        </div>

        <div>
          <dt>Personbästa</dt>
          <dd>
            {stat.personal_best_throws} | Svit: {signedInteger(stat.streak)}
          </dd>
        </div>

        <div>
          <dt>Medel kast</dt>
          <dd>
            {decimalTwo.format(stat.average_throws)}
            {stat.last_10_average_throws !== null && (
              <> (senaste 10: {decimalTwo.format(stat.last_10_average_throws)})</>
            )}
          </dd>
        </div>
      </dl>

      <p className="distribution-line">
        Birdie+: {stat.birdie_or_better_count} | Par: {stat.par_count} |
        {' '}Bogey: {stat.bogey_count} | Dubbelbogey: {stat.double_bogey_count} |
        {' '}Trippelbogey+: {stat.triple_bogey_or_worse_count}
      </p>
    </article>
  )
}
