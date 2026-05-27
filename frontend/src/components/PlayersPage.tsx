import type {
  PlayerApiResponse,
  ScoreablePlayerApiResponse,
  UserPlayersResponse,
} from '../api'

export interface SelectablePlayer {
  id: number
  name: string
  roundCount: number
  subtitle: string
}

interface PlayersPageProps {
  data: UserPlayersResponse | null
  isLoading: boolean
  error: string | null
  onBack: () => void
  onSelectPlayer: (player: SelectablePlayer) => void
}

function mapPlayer(
  player: PlayerApiResponse | ScoreablePlayerApiResponse,
  subtitle: string,
): SelectablePlayer {
  return {
    id: player.id,
    name: player.name,
    roundCount: player.round_count,
    subtitle,
  }
}

export default function PlayersPage({
  data,
  isLoading,
  error,
  onBack,
  onSelectPlayer,
}: PlayersPageProps) {
  const ownPlayer = data?.own_player
    ? mapPlayer(data.own_player, 'Egen spelare')
    : null

  const guestPlayers =
    data?.guest_players.map((player) =>
      mapPlayer(player, 'Gästspelare'),
    ) ?? []

  const excludedIds = new Set<number>([
    ...(ownPlayer ? [ownPlayer.id] : []),
    ...guestPlayers.map((player) => player.id),
  ])

  const scoreablePlayers =
    data?.scoreable_players
      .filter((player) => !excludedIds.has(player.id))
      .map((player) => {
        const subtitle =
          player.permission_level === 'auto_approve'
            ? 'Autogodkänd'
            : player.permission_level === 'propose'
              ? 'Kräver godkännande'
              : player.permission_level

        return mapPlayer(player, subtitle)
      }) ?? []

  return (
    <main className="content-page">
      <div className="page-toolbar">
        <button className="secondary-button" onClick={onBack}>
          Tillbaka
        </button>
        <h2>Spelare</h2>
      </div>

      {isLoading && <p>Laddar spelare…</p>}

      {error && <p className="error-message">{error}</p>}

      {!isLoading && !error && data && (
        <div className="player-sections">
          <PlayerSection
            title="Egen spelare"
            players={ownPlayer ? [ownPlayer] : []}
            emptyText="Ingen egen spelare hittades."
            onSelectPlayer={onSelectPlayer}
          />

          <PlayerSection
            title="Gästspelare"
            players={guestPlayers}
            emptyText="Inga gästspelare."
            onSelectPlayer={onSelectPlayer}
          />

          <PlayerSection
            title="Spelare du får scorea för"
            players={scoreablePlayers}
            emptyText="Inga andra spelare."
            onSelectPlayer={onSelectPlayer}
          />
        </div>
      )}
    </main>
  )
}

interface PlayerSectionProps {
  title: string
  players: SelectablePlayer[]
  emptyText: string
  onSelectPlayer: (player: SelectablePlayer) => void
}

function PlayerSection({
  title,
  players,
  emptyText,
  onSelectPlayer,
}: PlayerSectionProps) {
  return (
    <section className="list-card">
      <h3>{title}</h3>

      {players.length === 0 ? (
        <p className="muted-text">{emptyText}</p>
      ) : (
        <div className="player-list">
          {players.map((player) => (
            <button
              key={player.id}
              className="player-row"
              onClick={() => onSelectPlayer(player)}
            >
              <span>
                <strong>{player.name}</strong>
                <small>{player.subtitle}</small>
              </span>

              <span className="round-count">
                {player.roundCount} rundor
              </span>
            </button>
          ))}
        </div>
      )}
    </section>
  )
}