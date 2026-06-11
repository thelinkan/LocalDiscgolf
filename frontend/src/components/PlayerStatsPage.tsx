import type {
  CourseApiResponse,
  PlayerHoleStatsApiResponse,
  PlayerLayoutStatsApiResponse,
  LayoutResultMetric,
  LayoutRoundResultApiResponse,
  LayoutScoreDistributionApiResponse,
  LayoutStatsYearFilter,
  LayoutHoleDifficultyApiResponse,
  StatsActivityGroupBy,
  StatsOverviewActivityResponse,
  StatsOverviewScoreDistributionResponse,
  StatsOverviewYearResponse,
} from '../api'
import type { SelectablePlayer } from './PlayersPage'
import { useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import {
  CartesianGrid,
  Cell,
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
import type {
  NameType,
  Payload,
  ValueType,
} from 'recharts/types/component/DefaultTooltipContent'

interface PlayerStatsPageProps {
  player: SelectablePlayer
  courses: CourseApiResponse[]
  selectedCourseId: number | null
  layoutStats: PlayerLayoutStatsApiResponse[]
  holeStats: PlayerHoleStatsApiResponse[]

  selectedLayoutStat: PlayerLayoutStatsApiResponse | null
  selectedLayoutYear: LayoutStatsYearFilter
  includeLongerRounds: boolean
  layoutResultMetric: LayoutResultMetric
  layoutRoundResults: LayoutRoundResultApiResponse[]
  layoutScoreDistribution: LayoutScoreDistributionApiResponse | null
  layoutHoleDifficulty: LayoutHoleDifficultyApiResponse[]
  isLoadingSelectedLayoutStats: boolean
  selectedLayoutStatsError: string | null


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

  onLayoutSelected: (stat: PlayerLayoutStatsApiResponse) => void
  onLayoutYearSelected: (year: LayoutStatsYearFilter) => void
  onIncludeLongerRoundsChanged: (checked: boolean) => void
  onLayoutResultMetricChanged: (metric: LayoutResultMetric) => void  
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

  selectedLayoutStat,
  selectedLayoutYear,
  includeLongerRounds,
  layoutResultMetric,
  layoutRoundResults,
  layoutScoreDistribution,
  layoutHoleDifficulty,
  isLoadingSelectedLayoutStats,
  selectedLayoutStatsError,

  overviewYears,
  selectedYear,
  activityGroupBy,
  activity,
  scoreDistribution,
  isLoading,
  error,
  onBack,
  onRoundsClick,
  onCourseSelected,
  onYearSelected,
  onActivityGroupBySelected,

  onLayoutSelected,
  onLayoutYearSelected,
  onIncludeLongerRoundsChanged,
  onLayoutResultMetricChanged,
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
                            >
                              {scoreDistributionItems(scoreDistribution).map((entry) => (
                                <Cell
                                  key={entry.key}
                                  fill={scoreDistributionColor(entry.key)}
                                />
                              ))}
                            </Pie>
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

          {selectedLayoutStat && (
            <LayoutDetailStatsSection
              stat={selectedLayoutStat}
              selectedYear={selectedLayoutYear}
              includeLongerRounds={includeLongerRounds}
              resultMetric={layoutResultMetric}
              roundResults={layoutRoundResults}
              scoreDistribution={layoutScoreDistribution}
              holeDifficulty={layoutHoleDifficulty}
              isLoading={isLoadingSelectedLayoutStats}
              error={selectedLayoutStatsError}
              onYearSelected={onLayoutYearSelected}
              onIncludeLongerRoundsChanged={onIncludeLongerRoundsChanged}
              onResultMetricChanged={onLayoutResultMetricChanged}
            />
          )}


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
                    onClick={() => onLayoutSelected(stat)}
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

function scoreDistributionColor(key: string): string {
  switch (key) {
    case 'albatross_or_better':
      return '#14532d' // mycket mörkgrön

    case 'eagle':
      return '#15803d' // mörkgrön

    case 'birdie':
      return '#86efac' // ljusgrön

    case 'par':
      return '#9ca3af' // grå

    case 'bogey':
      return '#fecaca' // ljusröd

    case 'double_bogey':
      return '#f87171' // röd

    case 'triple_bogey':
      return '#dc2626' // tydligt röd

    case 'quadruple_bogey':
      return '#991b1b' // mörkröd

    case 'five_bogey_or_worse':
      return '#450a0a' // mycket mörkröd

    default:
      return '#d1d5db'
  }
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('sv-SE', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value))
}

function averageOfLast(
  rows: LayoutRoundResultApiResponse[],
  count: number,
  field: 'throws' | 'relative_to_par',
): number | null {
  if (rows.length < count) {
    return null
  }

  const lastRows = rows.slice(-count)
  const sum = lastRows.reduce((total, row) => total + row[field], 0)

  return sum / count
}

function bestRound(rows: LayoutRoundResultApiResponse[]) {
  if (rows.length === 0) {
    return null
  }

  return [...rows].sort((a, b) => {
    if (a.relative_to_par !== b.relative_to_par) {
      return a.relative_to_par - b.relative_to_par
    }

    return a.throws - b.throws
  })[0]
}

function layoutSummary(rows: LayoutRoundResultApiResponse[]) {
  if (rows.length === 0) {
    return null
  }

  const throwSum = rows.reduce((total, row) => total + row.throws, 0)
  const relativeSum = rows.reduce(
    (total, row) => total + row.relative_to_par,
    0,
  )
  const best = bestRound(rows)

  return {
    roundCount: rows.length,
    best,
    averageThrows: throwSum / rows.length,
    averageRelativeToPar: relativeSum / rows.length,
    last5Throws: averageOfLast(rows, 5, 'throws'),
    last5Relative: averageOfLast(rows, 5, 'relative_to_par'),
    last10Throws: averageOfLast(rows, 10, 'throws'),
    last10Relative: averageOfLast(rows, 10, 'relative_to_par'),
    last20Throws: averageOfLast(rows, 20, 'throws'),
    last20Relative: averageOfLast(rows, 20, 'relative_to_par'),
  }
}

function LayoutResultDot(props: any) {
  const { cx, cy, payload } = props

  if (cx === undefined || cy === undefined) {
    return null
  }

  if (payload?.is_longer_round) {
    return (
      <circle
        cx={cx}
        cy={cy}
        r={5}
        fill="#f97316"
        stroke="#7c2d12"
        strokeWidth={1}
      />
    )
  }

  return <circle cx={cx} cy={cy} r={3} fill="#2563eb" />
}

type HoleDifficultySortKey =
  | 'sequence_number'
  | 'par'
  | 'length_meters'
  | 'my_average_relative_to_par'
  | 'my_rank'
  | 'global_average_relative_to_par'
  | 'global_rank'
  | 'my_count'
  | 'global_count'

type SortDirection = 'asc' | 'desc'

interface HoleDifficultySortState {
  key: HoleDifficultySortKey
  direction: SortDirection
}

function compareNullableNumbers(
  left: number | null | undefined,
  right: number | null | undefined,
  direction: SortDirection,
): number {
  const leftMissing = left === null || left === undefined
  const rightMissing = right === null || right === undefined

  if (leftMissing && rightMissing) {
    return 0
  }

  if (leftMissing) {
    return 1
  }

  if (rightMissing) {
    return -1
  }

  return direction === 'asc' ? left - right : right - left
}

function sortHoleDifficultyRows(
  rows: LayoutHoleDifficultyApiResponse[],
  sortState: HoleDifficultySortState,
): LayoutHoleDifficultyApiResponse[] {
  return [...rows].sort((left, right) => {
    const comparison = compareNullableNumbers(
      left[sortState.key],
      right[sortState.key],
      sortState.direction,
    )

    if (comparison !== 0) {
      return comparison
    }

    return left.sequence_number - right.sequence_number
  })
}

function nextSortState(
  current: HoleDifficultySortState,
  key: HoleDifficultySortKey,
): HoleDifficultySortState {
  if (current.key !== key) {
    return {
      key,
      direction:
        key === 'sequence_number' ||
        key === 'par' ||
        key === 'length_meters'
          ? 'asc'
          : 'desc',
    }
  }

  return {
    key,
    direction: current.direction === 'asc' ? 'desc' : 'asc',
  }
}

function sortIndicator(
  sortState: HoleDifficultySortState,
  key: HoleDifficultySortKey,
): string {
  if (sortState.key !== key) {
    return ''
  }

  return sortState.direction === 'asc' ? ' ↑' : ' ↓'
}

function formatNullableSignedDecimal(value: number | null): string {
  if (value === null) {
    return '—'
  }

  return signedDecimal(value)
}

function formatNullableDecimal(value: number | null): string {
  if (value === null) {
    return '—'
  }

  return decimalOne.format(value)
}

function formatRank(rank: number | null, total: number): string {
  if (rank === null || total === 0) {
    return '—'
  }

  return `${rank} av ${total}`
}

function holeDifficultyTitle(row: LayoutHoleDifficultyApiResponse): string {
  const parts = [`Hål ${row.hole_number}`]

  if (row.hole_name) {
    parts.push(row.hole_name)
  }

  const variantParts = [row.tee_name, row.basket_name].filter(Boolean)

  if (variantParts.length > 0) {
    parts.push(`(${variantParts.join(' → ')})`)
  }

  return parts.join(' ')
}

function LayoutDetailStatsSection({
  stat,
  selectedYear,
  includeLongerRounds,
  resultMetric,
  roundResults,
  scoreDistribution,
  holeDifficulty,
  isLoading,
  error,
  onYearSelected,
  onIncludeLongerRoundsChanged,
  onResultMetricChanged,
}: {
  stat: PlayerLayoutStatsApiResponse
  selectedYear: LayoutStatsYearFilter
  includeLongerRounds: boolean
  resultMetric: LayoutResultMetric
  roundResults: LayoutRoundResultApiResponse[]
  scoreDistribution: LayoutScoreDistributionApiResponse | null
  holeDifficulty: LayoutHoleDifficultyApiResponse[]
  isLoading: boolean
  error: string | null
  onYearSelected: (year: LayoutStatsYearFilter) => void
  onIncludeLongerRoundsChanged: (checked: boolean) => void
  onResultMetricChanged: (metric: LayoutResultMetric) => void
}) {
  const summary = layoutSummary(roundResults)
  const scoreItems = scoreDistributionItems(scoreDistribution)

  const [holeDifficultySort, setHoleDifficultySort] =
    useState<HoleDifficultySortState>({
      key: 'sequence_number',
      direction: 'asc',
    })

  const sortedHoleDifficulty = useMemo(
    () => sortHoleDifficultyRows(holeDifficulty, holeDifficultySort),
    [holeDifficulty, holeDifficultySort],
  )

  function handleHoleDifficultySort(key: HoleDifficultySortKey) {
    setHoleDifficultySort((current) => nextSortState(current, key))
  }

  const years = Array.from(
    new Set(roundResults.map((row) => new Date(row.started_at).getFullYear())),
  ).sort((a, b) => b - a)

  const chartData = roundResults.map((row, index) => ({
    ...row,
    round_index: index + 1,
    label: formatDate(row.started_at),
    result_value:
      resultMetric === 'throws'
        ? row.throws
        : row.relative_to_par,
    cumulative_average_value:
      resultMetric === 'throws'
        ? row.cumulative_average_throws
        : row.cumulative_average_relative_to_par,
  }))

  return (
    <section className="stats-section selected-layout-section">
      <div className="stats-section-heading">
        <div>
          <h3>
            {stat.course_name} - {stat.layout_name}
          </h3>
          <p className="muted-text">
            Par {stat.total_par}, {stat.hole_count} hål,{' '}
            {integer.format(stat.total_length_meters)} meter
          </p>
        </div>

        <div className="layout-stats-controls">
          <label>
            År
            <select
              value={selectedYear ?? ''}
              onChange={(event) => {
                const value = event.target.value
                onYearSelected(value === '' ? null : Number(value))
              }}
            >
              <option value="">Alla år</option>
              {years.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </label>

          <label>
            Diagram
            <select
              value={resultMetric}
              onChange={(event) =>
                onResultMetricChanged(event.target.value as LayoutResultMetric)
              }
            >
              <option value="relative_to_par">Jämfört med par</option>
              <option value="throws">Antal kast</option>
            </select>
          </label>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={includeLongerRounds}
              onChange={(event) =>
                onIncludeLongerRoundsChanged(event.target.checked)
              }
            />
            Ta med längre rundor
          </label>
        </div>
      </div>

      {isLoading && <p>Laddar layoutstatistik…</p>}

      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && (
        <>
          {summary === null ? (
            <p className="muted-text">
              Ingen statistik finns för valt urval.
            </p>
          ) : (
            <>
              <div className="layout-summary-grid">
                <article className="layout-summary-card">
                  <span>Antal rundor</span>
                  <strong>{summary.roundCount}</strong>
                </article>

                <article className="layout-summary-card">
                  <span>Personbästa</span>
                  <strong>
                    {summary.best?.throws}{' '}
                    ({signedInteger(summary.best?.relative_to_par ?? 0)})
                  </strong>
                </article>

                <article className="layout-summary-card">
                  <span>Snitt</span>
                  <strong>
                    {decimalOne.format(summary.averageThrows)}{' '}
                    ({signedDecimal(summary.averageRelativeToPar)})
                  </strong>
                </article>

                {summary.last5Throws !== null &&
                  summary.last5Relative !== null && (
                    <article className="layout-summary-card">
                      <span>Snitt 5 senaste</span>
                      <strong>
                        {decimalOne.format(summary.last5Throws)}{' '}
                        ({signedDecimal(summary.last5Relative)})
                      </strong>
                    </article>
                  )}

                {summary.last10Throws !== null &&
                  summary.last10Relative !== null && (
                    <article className="layout-summary-card">
                      <span>Snitt 10 senaste</span>
                      <strong>
                        {decimalOne.format(summary.last10Throws)}{' '}
                        ({signedDecimal(summary.last10Relative)})
                      </strong>
                    </article>
                  )}

                {summary.last20Throws !== null &&
                  summary.last20Relative !== null && (
                    <article className="layout-summary-card">
                      <span>Snitt 20 senaste</span>
                      <strong>
                        {decimalOne.format(summary.last20Throws)}{' '}
                        ({signedDecimal(summary.last20Relative)})
                      </strong>
                    </article>
                  )}
              </div>

              <div className="stats-chart-grid">
                <article className="stats-chart-card">
                  <div className="chart-card-heading">
                    <h4>
                      Resultat per runda
                    </h4>
                    {includeLongerRounds && (
                      <p className="muted-text">
                        Orange punkt = längre runda där endast layoutens hål
                        räknas.
                      </p>
                    )}
                  </div>

                  <div className="chart-container">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart
                        data={chartData}
                        margin={{ top: 10, right: 20, bottom: 10, left: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="label" />
                        <YAxis allowDecimals />
                        <Tooltip
                          formatter={(
                            value: ValueType | undefined,
                            name: NameType | undefined,
                          ): [ReactNode, NameType] => {
                            const label: NameType =
                              name === 'result_value'
                                ? resultMetric === 'throws'
                                  ? 'Kast'
                                  : 'Jämfört med par'
                                : 'Kumulativt snitt'

                            return [value ?? '', label]
                          }}
                          labelFormatter={(
                            label: ReactNode,
                            payload: ReadonlyArray<Payload<ValueType, NameType>>,
                          ): ReactNode => {
                            const row = payload?.[0]?.payload as
                              | (LayoutRoundResultApiResponse & {
                                  is_longer_round?: boolean
                                  source_hole_count?: number
                                })
                              | undefined

                            if (!row) {
                              return label
                            }

                            const extraText = row.is_longer_round
                              ? `, längre runda (${row.source_hole_count} hål totalt)`
                              : ''

                            return `${label}${extraText}`
                          }}
                        />
                        <Legend />
                        <Line
                          type="monotone"
                          dataKey="result_value"
                          name={
                            resultMetric === 'throws'
                              ? 'Kast'
                              : 'Jämfört med par'
                          }
                          stroke="#2563eb"
                          strokeWidth={2}
                          dot={<LayoutResultDot />}
                          activeDot={{ r: 6 }}
                        />
                        <Line
                          type="monotone"
                          dataKey="cumulative_average_value"
                          name="Kumulativt snitt"
                          stroke="#111827"
                          strokeWidth={2}
                          strokeDasharray="6 4"
                          dot={false}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </article>

                <article className="stats-chart-card">
                  <h4>Scorefördelning</h4>

                  {scoreItems.length === 0 ? (
                    <p className="muted-text">
                      Ingen scorefördelning för valt urval.
                    </p>
                  ) : (
                    <>
                      <div className="chart-container">
                        <ResponsiveContainer width="100%" height="100%">
                          <PieChart>
                            <Tooltip />
                            <Legend />
                            <Pie
                              data={scoreItems}
                              dataKey="value"
                              nameKey="label"
                              outerRadius={105}
                              label={({
                              name,
                              value,
                            }: {
                              name?: string | number
                              value?: string | number
                            }) => `${name}: ${value}`}
                            >
                              {scoreItems.map((entry) => (
                                <Cell
                                  key={entry.key}
                                  fill={scoreDistributionColor(entry.key)}
                                />
                              ))}
                            </Pie>
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

              <div className="table-wrapper layout-rounds-table-wrapper">
                <table className="stats-overview-table">
                  <thead>
                    <tr>
                      <th>Datum</th>
                      <th>Kast</th>
                      <th>Par</th>
                      <th>Resultat</th>
                      <th>Hål</th>
                      <th>Typ</th>
                      <th>Kumulativt snitt</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roundResults.map((row) => (
                      <tr key={row.round_id}>
                        <td>{formatDate(row.started_at)}</td>
                        <td>{row.throws}</td>
                        <td>{row.par}</td>
                        <td>{signedInteger(row.relative_to_par)}</td>
                        <td>
                          {row.hole_count}
                          {row.is_longer_round &&
                            row.source_hole_count !== undefined && (
                              <> av {row.source_hole_count}</>
                            )}
                        </td>
                        <td>
                          {row.is_longer_round ? 'Längre runda' : 'Exakt layout'}
                        </td>
                        <td>
                          {resultMetric === 'throws'
                            ? decimalOne.format(row.cumulative_average_throws)
                            : signedDecimal(
                                row.cumulative_average_relative_to_par,
                              )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="table-wrapper layout-hole-difficulty-table-wrapper">
                <div className="table-heading-row">
                  <div>
                    <h4>Hålsvårighet</h4>
                    <p className="muted-text">
                      Snittet beräknas på alla spelade förekomster av respektive hålvariant.
                      Rankingen gäller bara hålvarianterna i den valda layouten.
                    </p>
                  </div>
                </div>

                {sortedHoleDifficulty.length === 0 ? (
                  <p className="muted-text">
                    Ingen hålsvårighetsstatistik finns för valt urval.
                  </p>
                ) : (
                  <table className="stats-overview-table sortable-stats-table">
                    <thead>
                      <tr>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('sequence_number')}
                          >
                            Hål{sortIndicator(holeDifficultySort, 'sequence_number')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('par')}
                          >
                            Par{sortIndicator(holeDifficultySort, 'par')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('length_meters')}
                          >
                            Längd{sortIndicator(holeDifficultySort, 'length_meters')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() =>
                              handleHoleDifficultySort('my_average_relative_to_par')
                            }
                          >
                            Mitt snitt
                            {sortIndicator(holeDifficultySort, 'my_average_relative_to_par')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('my_rank')}
                          >
                            Min rank{sortIndicator(holeDifficultySort, 'my_rank')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() =>
                              handleHoleDifficultySort('global_average_relative_to_par')
                            }
                          >
                            Globalt snitt
                            {sortIndicator(
                              holeDifficultySort,
                              'global_average_relative_to_par',
                            )}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('global_rank')}
                          >
                            Global rank{sortIndicator(holeDifficultySort, 'global_rank')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('my_count')}
                          >
                            Mina rundor{sortIndicator(holeDifficultySort, 'my_count')}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            onClick={() => handleHoleDifficultySort('global_count')}
                          >
                            Alla rundor{sortIndicator(holeDifficultySort, 'global_count')}
                          </button>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {sortedHoleDifficulty.map((row) => (
                        <tr key={row.hole_variant_id}>
                          <td>{holeDifficultyTitle(row)}</td>
                          <td>{row.par}</td>
                          <td>{integer.format(row.length_meters)} m</td>
                          <td>
                            {formatNullableSignedDecimal(
                              row.my_average_relative_to_par,
                            )}
                            {row.my_average_throws !== null && (
                              <span className="secondary-table-value">
                                {' '}
                                ({formatNullableDecimal(row.my_average_throws)} kast)
                              </span>
                            )}
                          </td>
                          <td>{formatRank(row.my_rank, row.my_rank_total)}</td>
                          <td>
                            {formatNullableSignedDecimal(
                              row.global_average_relative_to_par,
                            )}
                            {row.global_average_throws !== null && (
                              <span className="secondary-table-value">
                                {' '}
                                ({formatNullableDecimal(row.global_average_throws)} kast)
                              </span>
                            )}
                          </td>
                          <td>{formatRank(row.global_rank, row.global_rank_total)}</td>
                          <td>{integer.format(row.my_count)}</td>
                          <td>{integer.format(row.global_count)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </>
          )}
        </>
      )}
    </section>
  )
}

function LayoutStatsCard({
  stat,
  onClick,
}: {
  stat: PlayerLayoutStatsApiResponse
  onClick: () => void
}) {
  return (
    <article
      className="stats-card clickable-stats-card"
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onClick()
        }
      }}
    >
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