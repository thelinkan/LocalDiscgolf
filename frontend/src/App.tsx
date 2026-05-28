import { type FormEvent, useEffect, useState } from 'react'
import './App.css'
import {
  ApiError,
  flagIsTrue,
  getCourses,
  getMe,
  getPlayerHoleStats,
  getPlayerLayoutStats,
  getUserPlayers,
  login,
  type CourseApiResponse,
  type MeResponse,
  type PlayerHoleStatsApiResponse,
  type PlayerLayoutStatsApiResponse,
  type UserPlayersResponse,
} from './api'
import PlayersPage, {
  type SelectablePlayer,
} from './components/PlayersPage'
import PlayerStatsPage from './components/PlayerStatsPage'

const TOKEN_STORAGE_KEY = 'discgolf_access_token'

type AuthStatus =
  | 'checking'
  | 'logged_out'
  | 'logged_in'
  | 'server_unavailable'

  type AppView = 'home' | 'players' | 'player_stats'

function App() {
  const [authStatus, setAuthStatus] = useState<AuthStatus>('checking')
  const [user, setUser] = useState<MeResponse | null>(null)
  const [storedToken, setStoredToken] = useState<string | null>(null)

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isLoggingIn, setIsLoggingIn] = useState(false)
  const [loginError, setLoginError] = useState<string | null>(null)

  const [view, setView] = useState<AppView>('home')

  const [playersData, setPlayersData] = useState<UserPlayersResponse | null>(null)
  const [isLoadingPlayers, setIsLoadingPlayers] = useState(false)
  const [playersError, setPlayersError] = useState<string | null>(null)

  const [selectedPlayer, setSelectedPlayer] =
    useState<SelectablePlayer | null>(null)

  const [courses, setCourses] = useState<CourseApiResponse[]>([])
  const [selectedCourseId, setSelectedCourseId] = useState<number | null>(null)

  const [layoutStats, setLayoutStats] =
    useState<PlayerLayoutStatsApiResponse[]>([])

  const [holeStats, setHoleStats] =
    useState<PlayerHoleStatsApiResponse[]>([])

  const [isLoadingStats, setIsLoadingStats] = useState(false)
  const [statsError, setStatsError] = useState<string | null>(null)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY)

    if (!token) {
      setAuthStatus('logged_out')
      return
    }

    setStoredToken(token)

    const validateStoredToken = async () => {
      try {
        const me = await getMe(token)
        setUser(me)
        setAuthStatus('logged_in')
      } catch (error) {
        if (error instanceof ApiError && error.statusCode === 401) {
          localStorage.removeItem(TOKEN_STORAGE_KEY)
          setStoredToken(null)
          setUser(null)
          setAuthStatus('logged_out')
        } else {
          setAuthStatus('server_unavailable')
        }
      }
    }

    void validateStoredToken()
  }, [])

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setLoginError(null)
    setIsLoggingIn(true)

    try {
      const loginResponse = await login(username.trim(), password)

      localStorage.setItem(TOKEN_STORAGE_KEY, loginResponse.access_token)
      setStoredToken(loginResponse.access_token)

      const me = await getMe(loginResponse.access_token)

      setUser(me)
      setPassword('')
      setAuthStatus('logged_in')
    } catch (error) {
      if (error instanceof ApiError && error.statusCode === 401) {
        setLoginError('Fel användarnamn eller lösenord.')
      } else {
        setLoginError('Kunde inte logga in. Kontrollera att servern är tillgänglig.')
      }
    } finally {
      setIsLoggingIn(false)
    }
  }

  async function retrySavedLogin() {
    if (!storedToken) {
      setAuthStatus('logged_out')
      return
    }

    setAuthStatus('checking')

    try {
      const me = await getMe(storedToken)
      setUser(me)
      setAuthStatus('logged_in')
    } catch (error) {
      if (error instanceof ApiError && error.statusCode === 401) {
        handleLogout()
      } else {
        setAuthStatus('server_unavailable')
      }
    }
  }

  function handleLogout() {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setStoredToken(null)
    setUser(null)
    setUsername('')
    setPassword('')
    setLoginError(null)

    setPlayersData(null)
    setSelectedPlayer(null)
    setCourses([])
    setSelectedCourseId(null)
    setLayoutStats([])
    setHoleStats([])
    setStatsError(null)
    setView('home')
    setAuthStatus('logged_out')
  }

  if (authStatus === 'checking') {
    return (
      <div className="page centered">
        <div className="status-card">
          <h1>LocalDiscgolf</h1>
          <p>Kontrollerar inloggning…</p>
        </div>
      </div>
    )
  }

  if (authStatus === 'server_unavailable') {
    return (
      <div className="page centered">
        <div className="status-card">
          <h1>LocalDiscgolf</h1>
          <p className="error-message">
            Kunde inte kontrollera din sparade inloggning mot servern.
          </p>

          <div className="button-column">
            <button className="primary-button" onClick={() => void retrySavedLogin()}>
              Försök igen
            </button>

            <button className="secondary-button" onClick={handleLogout}>
              Logga ut lokalt
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (authStatus === 'logged_out') {
    return (
      <div className="page centered">
        <main className="login-card">
          <header className="login-header">
            <p className="eyebrow">Discgolf</p>
            <h1>LocalDiscgolf</h1>
            <p className="muted-text">
              Logga in för att se rundor och statistik.
            </p>
          </header>

          <form className="login-form" onSubmit={handleLogin}>
            <label>
              Användarnamn
              <input
                type="text"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
              />
            </label>

            <label>
              Lösenord
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
              />
            </label>

            {loginError && <p className="error-message">{loginError}</p>}

            <button
              className="primary-button"
              type="submit"
              disabled={isLoggingIn || username.trim() === '' || password === ''}
            >
              {isLoggingIn ? 'Loggar in…' : 'Logga in'}
            </button>
          </form>
        </main>
      </div>
    )
  }

  async function openPlayers() {
    if (!storedToken || !user) {
      return
    }

    setView('players')
    setIsLoadingPlayers(true)
    setPlayersError(null)

    try {
      const data = await getUserPlayers(storedToken, user.username)
      setPlayersData(data)
    } catch (error) {
      setPlayersError(apiErrorText(error, 'Kunde inte hämta spelare.'))
    } finally {
      setIsLoadingPlayers(false)
    }
  }

  async function openPlayerStats(player: SelectablePlayer) {
    if (!storedToken) {
      return
    }

    setSelectedPlayer(player)
    setSelectedCourseId(null)
    setView('player_stats')
    setIsLoadingStats(true)
    setStatsError(null)

    try {
      const [courseData, layoutData, holeData] = await Promise.all([
        getCourses(storedToken),
        getPlayerLayoutStats(storedToken, player.id, null),
        getPlayerHoleStats(storedToken, player.id, null),
      ])

      setCourses(courseData)
      setLayoutStats(layoutData)
      setHoleStats(holeData)
    } catch (error) {
      setStatsError(apiErrorText(error, 'Kunde inte hämta statistik.'))
    } finally {
      setIsLoadingStats(false)
    }
  }

  async function changeStatsCourse(courseId: number | null) {
    if (!storedToken || !selectedPlayer) {
      return
    }

    setSelectedCourseId(courseId)
    setIsLoadingStats(true)
    setStatsError(null)

    try {
      const [layoutData, holeData] = await Promise.all([
        getPlayerLayoutStats(storedToken, selectedPlayer.id, courseId),
        getPlayerHoleStats(storedToken, selectedPlayer.id, courseId),
      ])

      setLayoutStats(layoutData)
      setHoleStats(holeData)
    } catch (error) {
      setStatsError(apiErrorText(error, 'Kunde inte hämta statistik.'))
    } finally {
      setIsLoadingStats(false)
    }
  }

  return (
    <div className="page">
      <header className="app-header">
        <div>
          <p className="eyebrow">Discgolf</p>
          <h1>LocalDiscgolf</h1>
        </div>

        <div className="user-controls">
          <span>Inloggad som <strong>{user?.username}</strong></span>
          <button className="secondary-button small-button" onClick={handleLogout}>
            Logga ut
          </button>
        </div>
      </header>

      {view === 'home' && (
      <main className="home-content">
        {user && flagIsTrue(user.must_change_password) && (
          <section className="warning-card">
            Du använder ett tillfälligt lösenord. Funktion för lösenordsbyte läggs till senare.
          </section>
        )}

        <section className="welcome-card">
          <h2>Välkommen, {user?.username}</h2>
          <p>
            Webbgränssnittet är anslutet till Discgolf-API:t.
          </p>
        </section>

        <section className="feature-grid">
          <article className="feature-card">
            <h2>Spelare</h2>
            <p>Visa tillgängliga spelare och deras statistik.</p>
            <button
              className="primary-button"
              onClick={() => void openPlayers()}
            >
              Öppna spelare
            </button>
          </article>

          <article className="feature-card">
            <h2>Statistik</h2>
            <p>Mer avancerad analys kommer senare att byggas här.</p>
            <button className="primary-button" disabled>
              Öppna statistik
            </button>
          </article>
        </section>
      </main>
      )}
      {view === 'players' && (
        <PlayersPage
          data={playersData}
          isLoading={isLoadingPlayers}
          error={playersError}
          onBack={() => setView('home')}
          onSelectPlayer={(player) => void openPlayerStats(player)}
        />
      )}

      {view === 'player_stats' && selectedPlayer && (
        <PlayerStatsPage
          player={selectedPlayer}
          courses={courses}
          selectedCourseId={selectedCourseId}
          layoutStats={layoutStats}
          holeStats={holeStats}
          isLoading={isLoadingStats}
          error={statsError}
          onBack={() => setView('players')}
          onCourseSelected={(courseId) => void changeStatsCourse(courseId)}
        />
      )}
    </div>
  )
}

function apiErrorText(error: unknown, defaultMessage: string): string {
  if (error instanceof ApiError && error.statusCode === 401) {
    return 'Din inloggning är inte längre giltig. Logga ut och logga in igen.'
  }

  return defaultMessage
}

export default App
