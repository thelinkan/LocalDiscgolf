import type {
  RoundDetailApiResponse,
  RoundDetailHoleApiResponse,
  RoundDetailPlayerApiResponse,
} from '../api'

interface RoundDetailPageProps {
  round: RoundDetailApiResponse | null
  isLoading: boolean
  error: string | null
  onBack: () => void
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}

function formatRelativeScore(value: number): string {
  return value > 0 ? `+${value}` : `${value}`
}

function roundStatusText(status: string): string {
  if (status === 'completed') {
    return 'Avslutad'
  }

  if (status === 'in_progress') {
    return 'Pågående'
  }

  return status
}

function chunkHoles(
  holes: RoundDetailHoleApiResponse[],
  size: number,
): RoundDetailHoleApiResponse[][] {
  const result: RoundDetailHoleApiResponse[][] = []

  for (let index = 0; index < holes.length; index += size) {
    result.push(holes.slice(index, index + size))
  }

  return result
}

function playerSummary(player: RoundDetailPlayerApiResponse) {
  const playedHoles = player.holes.filter((hole) => hole.throws_count !== null)

  const totalThrows = playedHoles.reduce(
    (sum, hole) => sum + (hole.throws_count ?? 0),
    0,
  )

  const totalPar = playedHoles.reduce(
    (sum, hole) => sum + hole.par_snapshot,
    0,
  )

  return {
    totalThrows,
    relativeToPar: totalThrows - totalPar,
  }
}

export default function RoundDetailPage({
  round,
  isLoading,
  error,
  onBack,
}: RoundDetailPageProps) {
  if (isLoading) {
    return (
      <main className="content-page">
        <button className="secondary-button back-button" onClick={onBack}>
          Tillbaka
        </button>
        <p>Laddar runddetalj…</p>
      </main>
    )
  }

  if (error) {
    return (
      <main className="content-page">
        <button className="secondary-button back-button" onClick={onBack}>
          Tillbaka
        </button>
        <p className="error-message">{error}</p>
      </main>
    )
  }

  if (!round) {
    return (
      <main className="content-page">
        <button className="secondary-button back-button" onClick={onBack}>
          Tillbaka
        </button>
        <p>Ingen runda laddad.</p>
      </main>
    )
  }

  const players = [...round.players].sort(
    (first, second) => first.start_order - second.start_order,
  )

  const holes = [...(players[0]?.holes ?? [])].sort(
    (first, second) => first.sequence_number - second.sequence_number,
  )

  const holeChunks = chunkHoles(holes, 9)

  const layoutNames = Array.from(
    new Set(
      players
        .map((player) => player.layout_name)
        .filter((layoutName): layoutName is string => Boolean(layoutName)),
    ),
  )

  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>
        <h2>Runddetalj</h2>
      </div>

      <section className="round-detail-header">
        <h3>{round.course_name}</h3>

        {layoutNames.length > 0 && (
          <p className="round-layout-name">{layoutNames.join(', ')}</p>
        )}

        <p>Start: {formatDateTime(round.started_at)}</p>

        {round.ended_at && (
          <p>Slut: {formatDateTime(round.ended_at)}</p>
        )}

        <p>Status: {roundStatusText(round.status)}</p>
      </section>

      <section className="round-summary-card">
        <h3>Resultat</h3>

        {players.map((player) => {
          const summary = playerSummary(player)

          return (
            <div className="round-player-summary" key={player.id}>
              <span>{player.player_name}</span>
              <strong>
                {formatRelativeScore(summary.relativeToPar)} ({summary.totalThrows})
              </strong>
            </div>
          )
        })}
      </section>

      <section className="scorecard-section">
        <h3>Scorekort</h3>

        {holeChunks.map((holeChunk, chunkIndex) => (
          <ScorecardBlock
            key={chunkIndex}
            holeChunk={holeChunk}
            players={players}
          />
        ))}
      </section>
    </main>
  )
}

function ScorecardBlock({
  holeChunk,
  players,
}: {
  holeChunk: RoundDetailHoleApiResponse[]
  players: RoundDetailPlayerApiResponse[]
}) {
  return (
    <div className="scorecard-scroll">
      <table className="scorecard-table">
        <thead>
          <tr>
            <th>Hål</th>
            {holeChunk.map((hole) => (
              <th key={`hole-${hole.sequence_number}`}>
                {hole.hole_number_snapshot}
              </th>
            ))}
          </tr>

          <tr>
            <th>Längd</th>
            {holeChunk.map((hole) => (
              <th key={`length-${hole.sequence_number}`}>
                {hole.length_snapshot_meters}
              </th>
            ))}
          </tr>

          <tr>
            <th>Par</th>
            {holeChunk.map((hole) => (
              <th key={`par-${hole.sequence_number}`}>
                {hole.par_snapshot}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {players.map((player) => (
            <ScorecardPlayerRow
              key={player.id}
              player={player}
              holeChunk={holeChunk}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ScorecardPlayerRow({
  player,
  holeChunk,
}: {
  player: RoundDetailPlayerApiResponse
  holeChunk: RoundDetailHoleApiResponse[]
}) {
  return (
    <tr>
      <td className="scorecard-player-name">{player.player_name}</td>

      {holeChunk.map((hole) => {
        const playerHole = player.holes.find(
          (candidate) => candidate.sequence_number === hole.sequence_number,
        )

        return (
          <td key={`${player.id}-${hole.sequence_number}`}>
            <ScoreBadge
              throwsCount={playerHole?.throws_count ?? null}
              par={playerHole?.par_snapshot ?? null}
            />
          </td>
        )
      })}
    </tr>
  )
}

function ScoreBadge({
  throwsCount,
  par,
}: {
  throwsCount: number | null
  par: number | null
}) {
  if (throwsCount === null || par === null) {
    return <span className="score-badge score-empty">-</span>
  }

  const difference = throwsCount - par

  const scoreClass =
    difference <= -1
      ? 'score-under'
      : difference === 1
        ? 'score-bogey'
        : difference >= 2
          ? 'score-double'
          : 'score-even'

  return (
    <span className={`score-badge ${scoreClass}`}>
      {throwsCount}
    </span>
  )
}
