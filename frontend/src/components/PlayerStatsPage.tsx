import type {
  CourseApiResponse,
  PlayerHoleStatsApiResponse,
  PlayerLayoutStatsApiResponse,
  StatsActivityGroupBy,
  StatsOverviewActivityResponse,
  StatsOverviewScoreDistributionResponse,
  StatsOverviewYearResponse,
} from '../api'
import type { SelectablePlayer } from './PlayersPage'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

interface PlayerStatsPageProps {
  player: SelectablePlayer
  courses: CourseApiResponse[]
  selectedCourseId: number | null
  layoutStats: PlayerLayoutStatsApiResponse[]
  holeStats: PlayerHoleStatsApiResponse[]
  overviewYears: StatsOverviewYearResponse[]
  selectedYear: number | null
  activityGroupBy: StatsActivityGroupBy
  activity: StatsOverviewActivityResponse[]
  scoreDistribution: StatsOverviewScoreDistributionResponse | null
  isLoading: boolean
  error: string | null
  onBack: () => void
  onCourseSelected: (courseId: number | null) => void
  onRoundsClick: () => void
  onYearSelected: (year: number) => void
  onActivityGroupBySelected: (groupBy: StatsActivityGroupBy) => void
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
  overviewYears,
  selectedYear,
  activityGroupBy,
  activity,
  scoreDistribution,
  onYearSelected,
  onActivityGroupBySelected,
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
          <div className="stats-section-heading">
            <div>
              <h3>Översikt</h3>
              <p className="muted-text">
                Sammanfattning av färdiga och godkända rundor.
              </p>
            </div>

            <div className="stats-controls">
              <label>
                År
                <select
                  value={selectedYear ?? ''}
                  onChange={(event) => onYearSelected(Number(event.target.value))}
                  disabled={overviewYears.length === 0}
                >
                  {overviewYears.map((row) => (
                    <option key={row.year} value={row.year}>
                      {row.year}
                    </option>
                  ))}
                </select>
              </label>

              <div className="segmented-control" aria-label="Diagramperiod">
                <button
                  type="button"
                  className={activityGroupBy === 'month' ? 'active' : ''}
                  onClick={() => onActivityGroupBySelected('month')}
                >
                  Månad
                </button>
                <button
                  type="button"
                  className={activityGroupBy === 'week' ? 'active' : ''}
                  onClick={() => onActivityGroupBySelected('week')}
                >
                  Vecka
                </button>
              </div>
            </div>
          </div>

          {overviewYears.length === 0 ? (
            <p className="muted-text">Ingen översiktsstatistik ännu.</p>
          ) : (
            <>
              <div className="table-wrapper">
                <table className="stats-overview-table">
                  <thead>
                    <tr>
                      <th>Årtal</th>
                      <th>Antal banor</th>
                      <th>Antal rundor</th>
                      <th>Antal kast</th>
                      <th>Antal hål</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overviewYears.map((row) => (
                      <tr key={row.year}>
                        <td>{row.year}</td>
                        <td>{integer.format(row.course_count)}</td>
                        <td>{integer.format(row.round_count)}</td>
                        <td>{integer.format(row.throw_count)}</td>
                        <td>{integer.format(row.hole_count)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="stats-chart-grid">
                <article className="stats-chart-card">
                  <h4>Aktivitet {selectedYear}</h4>

                  {activity.length === 0 ? (
                    <p className="muted-text">Ingen aktivitet för valt år.</p>
                  ) : (
                    <div className="chart-container">
                      <ResponsiveContainer width="100%" height={300}>
                        <LineChart
                          data={activity.map((row) => ({
                            ...row,
                            label: formatPeriodLabel(row, activityGroupBy),
                          }))}
                          margin={{ top: 10, right: 20, bottom: 10, left: 0 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="label" />
                          <YAxis
                            yAxisId="rounds"
                            allowDecimals={false}
                          />
                          <YAxis
                            yAxisId="holes"
                            orientation="right"
                            allowDecimals={false}
                          />
                          <Tooltip />
                          <Legend />
                          <Line
                            yAxisId="rounds"
                            type="monotone"
                            dataKey="round_count"
                            name="Rundor"
                            stroke="#2563eb"
                            strokeWidth={2}
                            dot={false}
                          />
                          <Line
                            yAxisId="holes"
                            type="monotone"
                            dataKey="hole_count"
                            name="Hål"
                            stroke="#16a34a"
                            strokeWidth={2}
                            dot={false}
                          />
                        </LineChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </article>

                <article className="stats-chart-card">
                  <h4>Scorefördelning {selectedYear}</h4>

                  {scoreDistributionItems(scoreDistribution).length === 0 ? (
                    <p className="muted-text">Ingen scorefördelning för valt år.</p>
                  ) : (
                    <>
                      <div className="chart-container">
                        <ResponsiveContainer width="100%" height={300}>
                          <PieChart>
                            <Tooltip />
                            <Legend />
                            <Pie
                              data={scoreDistributionItems(scoreDistribution)}
                              dataKey="value"
                              nameKey="label"
                              outerRadius={105}
                              label
                            />
                          </PieChart>
                        </ResponsiveContainer>
                      </div>

                      {scoreDistribution && (
                        <p className="muted-text">
                          Totalt {integer.format(scoreDistribution.total_holes)} hål.
                        </p>
                      )}
                    </>
                  )}
                </article>
              </div>
            </>
          )}
        </section>

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
const integer = new Intl.NumberFormat('sv-SE', {
  maximumFractionDigits: 0,
})

function formatPeriodLabel(
  row: StatsOverviewActivityResponse,
  groupBy: StatsActivityGroupBy,
): string {
  if (groupBy === 'week') {
    return row.period
  }

  const [year, month] = row.period.split('-')
  return `${month}/${year.slice(2)}`
}

function scoreDistributionItems(
  scoreDistribution: StatsOverviewScoreDistributionResponse | null,
) {
  if (!scoreDistribution) {
    return []
  }

  const distribution = scoreDistribution.distribution

  return [
    {
      key: 'albatross_or_better',
      label: 'Albatross+',
      value: distribution.albatross_or_better,
    },
    {
      key: 'eagle',
      label: 'Eagle',
      value: distribution.eagle,
    },
    {
      key: 'birdie',
      label: 'Birdie',
      value: distribution.birdie,
    },
    {
      key: 'par',
      label: 'Par',
      value: distribution.par,
    },
    {
      key: 'bogey',
      label: 'Bogey',
      value: distribution.bogey,
    },
    {
      key: 'double_bogey',
      label: 'Dubbelbogey',
      value: distribution.double_bogey,
    },
    {
      key: 'triple_bogey',
      label: 'Trippelbogey',
      value: distribution.triple_bogey,
    },
    {
      key: 'quadruple_bogey',
      label: 'Fyrbogey',
      value: distribution.quadruple_bogey,
    },
    {
      key: 'five_bogey_or_worse',
      label: '5-bogey+',
      value: distribution.five_bogey_or_worse,
    },
  ].filter((item) => item.value > 0)
}