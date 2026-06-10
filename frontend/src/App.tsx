import { useEffect, useState } from 'react'
import type React from 'react'
import './App.css'
import {
  ApiError,
  flagIsTrue,
  getCourses,
  getMe,
  getPlayerHoleStats,
  getPlayerLayoutStats,
  getPlayerRounds,
  getRoundDetail,
  getUserPlayers,
  getPublicCourses,
  getPublicCourseLayouts ,
  getPublicLayoutHoles,
  createCourse,
  deleteCourse,
  updateCourse,
  createRound,
  updateRound,
  deleteRound,
  approveSessionPlayer,
  getPendingApprovals,
  getStatsOverviewYears,
  getStatsOverviewActivity,
  getStatsOverviewScoreDistribution,
  changePassword,
  login,
  type CourseApiResponse,
  type MeResponse,
  type PlayerHoleStatsApiResponse,
  type PlayerLayoutStatsApiResponse,
  type PlayerRoundApiResponse,
  type RoundDetailApiResponse,
  type UserPlayersResponse,
  type PublicCourseApiResponse,
  type PublicLayoutApiResponse,
  type PublicLayoutHoleApiResponse,
  type CreateRoundRequest,
  type RoundUpdateRequest,
  type PendingApprovalApiResponse,
  type StatsActivityGroupBy,
  type StatsOverviewActivityResponse,
  type StatsOverviewScoreDistributionResponse,
  type StatsOverviewYearResponse,
} from './api'
import PlayersPage, {
  type SelectablePlayer,
} from './components/PlayersPage'
import PlayerStatsPage from './components/PlayerStatsPage'
import PlayerRoundsPage from './components/PlayerRoundsPage'
import NewRoundPage from './components/NewRoundPage'
import RoundDetailPage from './components/RoundDetailPage'
import {
  PublicCoursesPage,
  PublicLayoutDetailPage,
  PublicLayoutsPage,
} from './components/PublicCoursesPages'
import PendingApprovalsPage from './components/PendingApprovalsPage'
import SettingsPage from './components/SettingsPage'


const TOKEN_STORAGE_KEY = 'discgolf_access_token'

type AuthStatus =
  | 'checking'
  | 'logged_out'
  | 'logged_in'
  | 'server_unavailable'

type AppView =
  | 'home'
  | 'players'
  | 'player_stats'
  | 'player_rounds'
  | 'round_detail'
  | 'new_round'
  | 'pending_approvals'
  | 'public_courses'
  | 'public_layouts'
  | 'public_layout_detail'
  | 'settings'

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

  const [playerRounds, setPlayerRounds] =
    useState<PlayerRoundApiResponse[]>([])

  const [isLoadingRounds, setIsLoadingRounds] = useState(false)
  const [roundsError, setRoundsError] = useState<string | null>(null)

  const [selectedRound, setSelectedRound] =
    useState<RoundDetailApiResponse | null>(null)

  const [isLoadingRoundDetail, setIsLoadingRoundDetail] = useState(false)
  const [roundDetailError, setRoundDetailError] = useState<string | null>(null)

  const [publicCourses, setPublicCourses] =
    useState<PublicCourseApiResponse[]>([])

  const [selectedPublicCourse, setSelectedPublicCourse] =
    useState<PublicCourseApiResponse | null>(null)

  const [publicLayouts, setPublicLayouts] =
    useState<PublicLayoutApiResponse[]>([])

  const [includeInactiveLayouts, setIncludeInactiveLayouts] = useState(false)

  const [selectedPublicLayout, setSelectedPublicLayout] =
    useState<PublicLayoutApiResponse | null>(null)

  const [publicLayoutHoles, setPublicLayoutHoles] =
    useState<PublicLayoutHoleApiResponse[]>([])

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [settingsError, setSettingsError] = useState<string | null>(null)
  const [settingsSuccess, setSettingsSuccess] = useState<string | null>(null)

  const [isLoadingPublicData, setIsLoadingPublicData] = useState(false)
  const [publicDataError, setPublicDataError] = useState<string | null>(null)

  const [includeInactiveCourses, setIncludeInactiveCourses] = useState(false)

  const [statsOverviewYears, setStatsOverviewYears] =
    useState<StatsOverviewYearResponse[]>([])

  const [statsOverviewActivity, setStatsOverviewActivity] =
    useState<StatsOverviewActivityResponse[]>([])

  const [statsOverviewScoreDistribution, setStatsOverviewScoreDistribution] =
    useState<StatsOverviewScoreDistributionResponse | null>(null)

  const [selectedStatsYear, setSelectedStatsYear] = useState<number | null>(null)
  const [statsActivityGroupBy, setStatsActivityGroupBy] =
    useState<StatsActivityGroupBy>('month')

  const [newRoundCourses, setNewRoundCourses] =
    useState<PublicCourseApiResponse[]>([])

  const [newRoundLayouts, setNewRoundLayouts] =
    useState<PublicLayoutApiResponse[]>([])

  const [isLoadingNewRoundCourses, setIsLoadingNewRoundCourses] = useState(false)
  const [isLoadingNewRoundLayouts, setIsLoadingNewRoundLayouts] = useState(false)
  const [isSavingNewRound, setIsSavingNewRound] = useState(false)
  const [newRoundError, setNewRoundError] = useState<string | null>(null)
  const [openCreatedRoundInEditMode, setOpenCreatedRoundInEditMode] =
    useState(false)
  const [pendingApprovals, setPendingApprovals] = useState<PendingApprovalApiResponse[]>([])
  const [isLoadingPendingApprovals, setIsLoadingPendingApprovals] = useState(false)
  const [pendingApprovalsError, setPendingApprovalsError] = useState<string | null>(null)

  const isAdmin = user?.role === 'admin'

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

  useEffect(() => {
    if (authStatus === 'logged_in' && storedToken) {
      void loadPendingApprovals()
    }
  }, [authStatus, storedToken])

  async function handleLogin(event: React.FormEvent<HTMLFormElement>) {
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

  function openSettings() {
    setSettingsError(null)
    setSettingsSuccess(null)
    setCurrentPassword('')
    setNewPassword('')
    setConfirmPassword('')
    setView('settings')
  }

  async function loadPendingApprovals() {
    if (!storedToken) {
      return
    }

    setIsLoadingPendingApprovals(true)
    setPendingApprovalsError(null)

    try {
      const approvals = await getPendingApprovals(storedToken)
      setPendingApprovals(approvals)
    } catch (error) {
      setPendingApprovalsError(
        apiErrorText(error, 'Kunde inte hämta rundor att godkänna.'),
      )
    } finally {
      setIsLoadingPendingApprovals(false)
    }
  }

  async function openPendingApprovals() {
    setView('pending_approvals')
    await loadPendingApprovals()
  }

  async function handleApproveSessionPlayer(sessionPlayerId: number) {
    if (!storedToken) {
      return
    }

    try {
      await approveSessionPlayer(storedToken, sessionPlayerId)
      await loadPendingApprovals()
    } catch (error) {
      setPendingApprovalsError(
        apiErrorText(error, 'Kunde inte godkänna rundan.'),
      )
    }
  }

  async function handleChangePassword(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (newPassword.trim() === '' || currentPassword.trim() === '') {
      setSettingsError('Fyll i både nuvarande och nytt lösenord.')
      return
    }

    if (newPassword !== confirmPassword) {
      setSettingsError('De nya lösenorden matchar inte.')
      return
    }

    if (!storedToken) {
      setSettingsError('Du måste logga in igen.')
      return
    }

    setIsChangingPassword(true)
    setSettingsError(null)
    setSettingsSuccess(null)

    try {
      await changePassword(
        storedToken,
        user?.username ?? '',
        currentPassword,
        newPassword,
      )
      setSettingsSuccess('Lösenordet har ändrats.')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setUser((existing) =>
        existing
          ? {
              ...existing,
              must_change_password: 0,
            }
          : existing,
      )
    } catch (error) {
      setSettingsError(apiErrorText(error, 'Kunde inte byta lösenord.'))
    } finally {
      setIsChangingPassword(false)
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

    setPlayerRounds([])
    setRoundsError(null)
    setSelectedRound(null)
    setRoundDetailError(null)

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
    if (view === 'public_courses') {
      return (
        <div className="page">
          <PublicSiteHeader
            onHome={() => setView('home')}
            onLogin={() => setView('home')}
          />

          <PublicCoursesPage
            courses={publicCourses}
            isAdmin={false}
            includeInactive={false}
            isLoading={isLoadingPublicData}
            error={publicDataError}
            onBack={() => setView('home')}
            onCourseClick={(course) => void openPublicLayouts(course)}
            onIncludeInactiveChange={() => {}}
            onAddCourse={() => {}}
            onEditCourse={() => {}}
            onDeleteCourse={() => {}}
          />
        </div>
      )
    }

    if (view === 'public_layouts' && selectedPublicCourse) {
      return (
        <div className="page">
          <PublicSiteHeader
            onHome={() => setView('home')}
            onLogin={() => setView('home')}
          />

          <PublicLayoutsPage
            course={selectedPublicCourse}
            layouts={publicLayouts}
            includeInactive={includeInactiveLayouts}
            isAdmin={false}
            adminToken={null}
            isLoading={isLoadingPublicData}
            error={publicDataError}
            onBack={() => setView('public_courses')}
            onIncludeInactiveChange={(checked) =>
              void changeIncludeInactiveLayouts(checked)
            }
            onLayoutClick={(layout) => void openPublicLayoutDetail(layout)}
            onCourseChanged={() => {}}
          />
        </div>
      )
    }

    if (
      view === 'public_layout_detail' &&
      selectedPublicCourse &&
      selectedPublicLayout
    ) {
      return (
        <div className="page">
          <PublicSiteHeader
            onHome={() => setView('home')}
            onLogin={() => setView('home')}
          />

          <PublicLayoutDetailPage
            course={selectedPublicCourse}
            layout={selectedPublicLayout}
            holes={publicLayoutHoles}
            isLoading={isLoadingPublicData}
            error={publicDataError}
            onBack={() => setView('public_layouts')}
          />
        </div>
      )
    }

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

          <div className="public-login-link">
            <button
              className="secondary-button"
              onClick={() => void openPublicCourses()}
            >
              Visa banor och layouter
            </button>
          </div>
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
      const [courseData, layoutData, holeData, overviewYears] =
        await Promise.all([
          getCourses(storedToken),
          getPlayerLayoutStats(storedToken, player.id, null),
          getPlayerHoleStats(storedToken, player.id, null),
          getStatsOverviewYears(storedToken, player.id),
        ])

      setCourses(courseData)
      setLayoutStats(layoutData)
      setHoleStats(holeData)
      setStatsOverviewYears(overviewYears)

      const defaultYear =
        overviewYears.length > 0
          ? overviewYears[0].year
          : new Date().getFullYear()

      setSelectedStatsYear(defaultYear)
      setStatsActivityGroupBy('month')

      const [activityData, distributionData] = await Promise.all([
        getStatsOverviewActivity(
          storedToken,
          player.id,
          defaultYear,
          'month',
        ),
        getStatsOverviewScoreDistribution(
          storedToken,
          player.id,
          defaultYear,
        ),
      ])

      setStatsOverviewActivity(activityData)
      setStatsOverviewScoreDistribution(distributionData)
    } catch (error) {
      setStatsError(apiErrorText(error, 'Kunde inte hämta statistik.'))
    } finally {
      setIsLoadingStats(false)
    }
  }

  async function openPlayerRounds(player: SelectablePlayer) {
    if (!storedToken) {
      return
    }

    setSelectedPlayer(player)
    setView('player_rounds')
    setIsLoadingRounds(true)
    setRoundsError(null)

    try {
      const rounds = await getPlayerRounds(storedToken, player.id)
      setPlayerRounds(rounds)
    } catch (error) {
      setRoundsError(apiErrorText(error, 'Kunde inte hämta rundor.'))
    } finally {
      setIsLoadingRounds(false)
    }
  }

  async function changeStatsYear(year: number) {
    if (!storedToken || !selectedPlayer) {
      return
    }

    setSelectedStatsYear(year)
    setIsLoadingStats(true)
    setStatsError(null)

    try {
      const [activityData, distributionData] = await Promise.all([
        getStatsOverviewActivity(
          storedToken,
          selectedPlayer.id,
          year,
          statsActivityGroupBy,
        ),
        getStatsOverviewScoreDistribution(
          storedToken,
          selectedPlayer.id,
          year,
        ),
      ])

      setStatsOverviewActivity(activityData)
      setStatsOverviewScoreDistribution(distributionData)
    } catch (error) {
      setStatsError(apiErrorText(error, 'Kunde inte hämta översiktsstatistik.'))
    } finally {
      setIsLoadingStats(false)
    }
  }

  async function changeStatsActivityGroupBy(groupBy: StatsActivityGroupBy) {
    if (!storedToken || !selectedPlayer || selectedStatsYear === null) {
      return
    }

    setStatsActivityGroupBy(groupBy)
    setIsLoadingStats(true)
    setStatsError(null)

    try {
      const activityData = await getStatsOverviewActivity(
        storedToken,
        selectedPlayer.id,
        selectedStatsYear,
        groupBy,
      )

      setStatsOverviewActivity(activityData)
    } catch (error) {
      setStatsError(apiErrorText(error, 'Kunde inte hämta aktivitetsstatistik.'))
    } finally {
      setIsLoadingStats(false)
    }
  }

  async function openRoundDetail(roundId: number) {
    if (!storedToken) {
      return
    }

    setOpenCreatedRoundInEditMode(false)
    setView('round_detail')
    setSelectedRound(null)
    setIsLoadingRoundDetail(true)
    setRoundDetailError(null)

    try {
      const detail = await getRoundDetail(storedToken, roundId)
      setSelectedRound(detail)
    } catch (error) {
      setRoundDetailError(apiErrorText(error, 'Kunde inte hämta runddetalj.'))
    } finally {
      setIsLoadingRoundDetail(false)
    }
  }

  async function openPublicCourses(includeInactive = includeInactiveCourses) {
    setView('public_courses')
    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      const courses = await getPublicCourses(includeInactive)
      setPublicCourses(courses)
    } catch (error) {
      setPublicDataError(apiErrorText(error, 'Kunde inte hämta banor.'))
    } finally {
      setIsLoadingPublicData(false)
    }
  }

  async function changeIncludeInactiveCourses(checked: boolean) {
    setIncludeInactiveCourses(checked)
    await openPublicCourses(checked)
  }

  async function handleAddCourse() {
    if (!storedToken || !isAdmin) {
      return
    }

    const name = window.prompt('Namn på ny bana:')

    if (!name || name.trim() === '') {
      return
    }

    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      await createCourse(storedToken, {
        name: name.trim(),
      })

      await openPublicCourses(includeInactiveCourses)
    } catch (error) {
      setPublicDataError(apiErrorText(error, 'Kunde inte skapa bana.'))
    } finally {
      setIsLoadingPublicData(false)
    }
  }


  async function handleEditCourse(course: PublicCourseApiResponse) {
    if (!storedToken || !isAdmin) {
      return
    }

    const newName = window.prompt('Nytt namn på banan:', course.name)

    if (!newName || newName.trim() === '') {
      return
    }

    const shouldBeActive = window.confirm(
      'Ska banan vara aktiv?\n\nOK = aktiv\nAvbryt = inaktiv',
    )

    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      await updateCourse(storedToken, course.id, {
        name: newName.trim(),
        is_active: shouldBeActive,
      })

      await openPublicCourses(includeInactiveCourses)
    } catch (error) {
      setPublicDataError(apiErrorText(error, 'Kunde inte uppdatera bana.'))
    } finally {
      setIsLoadingPublicData(false)
    }
  }


  async function handleDeleteCourse(course: PublicCourseApiResponse) {
    if (!storedToken || !isAdmin) {
      return
    }

    const confirmed = window.confirm(
      `Vill du ta bort banan "${course.name}"?\n\nDet går bara om banan inte har några hål.`,
    )

    if (!confirmed) {
      return
    }

    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      await deleteCourse(storedToken, course.id)
      await openPublicCourses(includeInactiveCourses)
    } catch (error) {
      setPublicDataError(
        apiErrorText(
          error,
          'Kunde inte ta bort bana. Banor med hål kan inte tas bort.',
        ),
      )
    } finally {
      setIsLoadingPublicData(false)
    }
  }

  async function openPublicLayouts(course: PublicCourseApiResponse) {
    setSelectedPublicCourse(course)
    setIncludeInactiveLayouts(false)
    setView('public_layouts')
    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      const layouts = await getPublicCourseLayouts(course.id, false)
      setPublicLayouts(layouts)
    } catch (error) {
      setPublicDataError(apiErrorText(error, 'Kunde inte hämta layouter.'))
    } finally {
      setIsLoadingPublicData(false)
    }
  }

  async function changeIncludeInactiveLayouts(includeInactive: boolean) {
    if (!selectedPublicCourse) {
      return
    }

    setIncludeInactiveLayouts(includeInactive)
    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      const layouts = await getPublicCourseLayouts(
        selectedPublicCourse.id,
        includeInactive,
      )
      setPublicLayouts(layouts)
    } catch (error) {
      setPublicDataError(apiErrorText(error, 'Kunde inte hämta layouter.'))
    } finally {
      setIsLoadingPublicData(false)
    }
  }

  async function openPublicLayoutDetail(layout: PublicLayoutApiResponse) {
    setSelectedPublicLayout(layout)
    setView('public_layout_detail')
    setIsLoadingPublicData(true)
    setPublicDataError(null)

    try {
      const holes = await getPublicLayoutHoles(layout.id)
      setPublicLayoutHoles(holes)
    } catch (error) {
      setPublicDataError(apiErrorText(error, 'Kunde inte hämta layouthål.'))
    } finally {
      setIsLoadingPublicData(false)
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

  function canEditSelectedRound(): boolean {
    if (!user || !selectedRound) {
      return false
    }

    if (user.role === 'admin') {
      return true
    }

    if (user.id === selectedRound.created_by_user_id) {
      return true
    }

    return selectedRound.players.some(
      (player) =>
        player.added_by_user_id === user.id &&
        player.approval_state !== 'approved',
    )
  }

  function canDeleteSelectedRound(): boolean {
    if (!user || !selectedRound) {
      return false
    }

    return user.role === 'admin' || user.id === selectedRound.created_by_user_id
  }

  async function handleSaveRoundUpdate(request: RoundUpdateRequest) {
    if (!storedToken || !selectedRound) {
      return
    }

    const updatedRound = await updateRound(storedToken, selectedRound.id, request)
    setSelectedRound(updatedRound)

    if (selectedPlayer) {
      const rounds = await getPlayerRounds(storedToken, selectedPlayer.id)
      setPlayerRounds(rounds)
    }
  }

  async function handleDeleteSelectedRound() {
    if (!storedToken || !selectedRound || !selectedPlayer) {
      return
    }

    await deleteRound(storedToken, selectedRound.id)

    const rounds = await getPlayerRounds(storedToken, selectedPlayer.id)
    setPlayerRounds(rounds)
    setSelectedRound(null)
    setView('player_rounds')
  }

  function selectablePlayersForNewRound(): SelectablePlayer[] {
    if (!playersData) {
      return []
    }

    const result: SelectablePlayer[] = []
    const seenPlayerIds = new Set<number>()

    function addPlayer(player: {
      id: number
      name: string
      round_count: number
      permission_level?: string
    }, subtitle: string) {
      if (seenPlayerIds.has(player.id)) {
        return
      }

      seenPlayerIds.add(player.id)

      result.push({
        id: player.id,
        name: player.name,
        roundCount: player.round_count,
        subtitle,
      })
    }

    if (playersData.own_player) {
      addPlayer(playersData.own_player, 'Egen spelare')
    }

    for (const player of playersData.guest_players) {
      addPlayer(player, 'Gästspelare')
    }

    for (const player of playersData.scoreable_players) {
      addPlayer(
        player,
        player.permission_level === 'auto_approve'
          ? 'Kan scoreas direkt'
          : 'Kan läggas in för godkännande',
      )
    }

    return result
  }

  async function openNewRound() {
    if (!storedToken || !user) {
      return
    }

    setView('new_round')
    setNewRoundError(null)
    setNewRoundLayouts([])
    setIsLoadingNewRoundCourses(true)

    try {
      const [courses, userPlayers] = await Promise.all([
        getPublicCourses(false),
        getUserPlayers(storedToken, user.username),
      ])

      setNewRoundCourses(courses)
      setPlayersData(userPlayers)
    } catch (error) {
      setNewRoundError(apiErrorText(error, 'Kunde inte ladda ny runda.'))
    } finally {
      setIsLoadingNewRoundCourses(false)
    }
  }

  async function loadNewRoundLayouts(courseId: number) {
    setIsLoadingNewRoundLayouts(true)
    setNewRoundError(null)

    try {
      const layouts = await getPublicCourseLayouts(courseId, false)
      setNewRoundLayouts(layouts)
    } catch (error) {
      setNewRoundError(apiErrorText(error, 'Kunde inte hämta layouter.'))
    } finally {
      setIsLoadingNewRoundLayouts(false)
    }
  }

  async function handleCreateNewRound(request: {
    courseId: number
    layoutId: number
    startedAt: string
    playerIds: number[]
  }) {
    if (!storedToken) {
      return
    }

    const requestBody: CreateRoundRequest = {
      course_id: request.courseId,
      started_at: request.startedAt,
      players: request.playerIds.map((playerId) => ({
        player_id: playerId,
        layout_id: request.layoutId,
      })),
    }

    setIsSavingNewRound(true)
    setNewRoundError(null)

    try {
      const createdRound = await createRound(storedToken, requestBody)

      setSelectedRound(createdRound)
      setOpenCreatedRoundInEditMode(true)
      setView('round_detail')

      if (selectedPlayer) {
        const rounds = await getPlayerRounds(storedToken, selectedPlayer.id)
        setPlayerRounds(rounds)
      }
    } catch (error) {
      setNewRoundError(apiErrorText(error, 'Kunde inte skapa runda.'))
    } finally {
      setIsSavingNewRound(false)
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
          <button className="secondary-button small-button" onClick={openSettings}>
            Inställningar
          </button>
          <button className="secondary-button small-button" onClick={handleLogout}>
            Logga ut
          </button>
        </div>
      </header>

      {user && flagIsTrue(user.must_change_password) && view !== 'settings' && (
        <div className="warning-card warning-banner">
          Ditt konto kräver att du ändrar lösenord. <button className="link-button" type="button" onClick={openSettings}>Gå till inställningar</button>
        </div>
      )}

      {view === 'home' && (
      <main className="home-content">
        <section className="welcome-card">
          <h2>Välkommen, {user?.username}</h2>
          <p>
            Webbgränssnittet är anslutet till Discgolf-API:t.
          </p>
        </section>

        {pendingApprovals.length > 0 && (
          <section className="warning-card">
            Du har {pendingApprovals.length} resultat att godkänna.
            {' '}
            <button className="link-button" onClick={() => void openPendingApprovals()}>
              Visa
            </button>
          </section>
        )}

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

          <article className="feature-card">
            <h2>Banor</h2>
            <p>Visa banor, layouter och hålinformation.</p>
            <button
              className="primary-button"
              onClick={() => void openPublicCourses()}
            >
              Öppna banor
            </button>
          </article>
        </section>
      </main>
      )}
      {view === 'settings' && user && (
        <SettingsPage
          username={user.username}
          currentPassword={currentPassword}
          newPassword={newPassword}
          confirmPassword={confirmPassword}
          isSubmitting={isChangingPassword}
          errorMessage={settingsError}
          successMessage={settingsSuccess}
          onBack={() => setView('home')}
          onCurrentPasswordChange={setCurrentPassword}
          onNewPasswordChange={setNewPassword}
          onConfirmPasswordChange={setConfirmPassword}
          onSubmit={handleChangePassword}
        />
      )}
      {view === 'players' && (
        <PlayersPage
          data={playersData}
          isLoading={isLoadingPlayers}
          error={playersError}
          onBack={() => setView('home')}
          onSelectPlayer={(player) => void openPlayerRounds(player)}
        />
      )}

      {view === 'player_stats' && selectedPlayer && (
        <PlayerStatsPage
          player={selectedPlayer}
          courses={courses}
          selectedCourseId={selectedCourseId}
          layoutStats={layoutStats}
          holeStats={holeStats}
          overviewYears={statsOverviewYears}
          selectedYear={selectedStatsYear}
          activityGroupBy={statsActivityGroupBy}
          activity={statsOverviewActivity}
          scoreDistribution={statsOverviewScoreDistribution}
          isLoading={isLoadingStats}
          error={statsError}
          onBack={() => setView('players')}
          onRoundsClick={() => void openPlayerRounds(selectedPlayer)}
          onCourseSelected={(courseId) => void changeStatsCourse(courseId)}
          onYearSelected={(year) => void changeStatsYear(year)}
          onActivityGroupBySelected={(groupBy) =>
            void changeStatsActivityGroupBy(groupBy)
          }
        />
      )}

      {view === 'player_rounds' && selectedPlayer && (
        <PlayerRoundsPage
          player={selectedPlayer}
          rounds={playerRounds}
          isLoading={isLoadingRounds}
          error={roundsError}
          onBack={() => setView('players')}
          onStatsClick={() => void openPlayerStats(selectedPlayer)}
          onNewRoundClick={() => void openNewRound()}
          onRoundClick={(roundId) => void openRoundDetail(roundId)}
        />
      )}

      {view === 'new_round' && (
        <NewRoundPage
          courses={newRoundCourses}
          layouts={newRoundLayouts}
          players={selectablePlayersForNewRound()}
          preselectedPlayerId={selectedPlayer?.id ?? null}
          isLoadingCourses={isLoadingNewRoundCourses}
          isLoadingLayouts={isLoadingNewRoundLayouts}
          isSaving={isSavingNewRound}
          error={newRoundError}
          onBack={() => setView(selectedPlayer ? 'player_rounds' : 'home')}
          onCourseSelected={(courseId) => void loadNewRoundLayouts(courseId)}
          onSubmit={(request) => void handleCreateNewRound(request)}
        />
      )}

      {view === 'round_detail' && (
        <RoundDetailPage
          round={selectedRound}
          currentUserId={user?.id ?? null}
          currentUserRole={user?.role ?? null}
          canEdit={canEditSelectedRound()}
          canDelete={canDeleteSelectedRound()}
          startInEditMode={openCreatedRoundInEditMode}
          isLoading={isLoadingRoundDetail}
          error={roundDetailError}
          onBack={() => setView('player_rounds')}
          onSaveRound={handleSaveRoundUpdate}
          onDeleteRound={handleDeleteSelectedRound}
          onEditModeConsumed={() => setOpenCreatedRoundInEditMode(false)}
        />
      )}

      {view === 'public_courses' && (
        <PublicCoursesPage
          courses={publicCourses}
          isAdmin={isAdmin}
          includeInactive={includeInactiveCourses}
          isLoading={isLoadingPublicData}
          error={publicDataError}
          onBack={() => setView('home')}
          onCourseClick={(course) => void openPublicLayouts(course)}
          onIncludeInactiveChange={(checked) =>
            void changeIncludeInactiveCourses(checked)
          }
          onAddCourse={() => void handleAddCourse()}
          onEditCourse={(course) => void handleEditCourse(course)}
          onDeleteCourse={(course) => void handleDeleteCourse(course)}
        />
      )}

      {view === 'public_layouts' && selectedPublicCourse && (
        <PublicLayoutsPage
          course={selectedPublicCourse}
          layouts={publicLayouts}
          includeInactive={includeInactiveLayouts}
          isAdmin={isAdmin}
          adminToken={isAdmin ? storedToken : null}
          isLoading={isLoadingPublicData}
          error={publicDataError}
          onBack={() => setView('public_courses')}
          onIncludeInactiveChange={(checked) =>
            void changeIncludeInactiveLayouts(checked)
          }
          onLayoutClick={(layout) => void openPublicLayoutDetail(layout)}
          onCourseChanged={() => {
            void openPublicCourses(includeInactiveCourses)
          }}
        />
      )}

      {view === 'public_layout_detail' &&
        selectedPublicCourse &&
        selectedPublicLayout && (
          <PublicLayoutDetailPage
            course={selectedPublicCourse}
            layout={selectedPublicLayout}
            holes={publicLayoutHoles}
            isLoading={isLoadingPublicData}
            error={publicDataError}
            onBack={() => setView('public_layouts')}
          />
        )}

        {view === 'pending_approvals' && (
          <PendingApprovalsPage
            approvals={pendingApprovals}
            isLoading={isLoadingPendingApprovals}
            error={pendingApprovalsError}
            onBack={() => setView('home')}
            onOpenRound={(roundId) => void openRoundDetail(roundId)}
            onApprove={(sessionPlayerId) =>
              void handleApproveSessionPlayer(sessionPlayerId)
            }
          />
        )}

    </div>
  )
}

function PublicSiteHeader({
  onHome,
  onLogin,
}: {
  onHome: () => void
  onLogin: () => void
}) {
  return (
    <header className="app-header">
      <button className="brand-button" onClick={onHome}>
        <p className="eyebrow">Discgolf</p>
        <h1>LocalDiscgolf</h1>
      </button>

      <button className="secondary-button small-button" onClick={onLogin}>
        Logga in
      </button>
    </header>
  )
}

function apiErrorText(error: unknown, defaultMessage: string): string {
  if (error instanceof ApiError) {
    if (error.statusCode === 401) {
      return 'Din inloggning är inte längre giltig. Logga ut och logga in igen.'
    }

    if (error.responseBody) {
      try {
        const parsed = JSON.parse(error.responseBody)
        if (
          parsed &&
          typeof parsed === 'object' &&
          parsed !== null
        ) {
          const parsedObj = parsed as { detail?: unknown; message?: unknown }
          if (
            typeof parsedObj.detail === 'string' &&
            parsedObj.detail.trim() !== ''
          ) {
            return parsedObj.detail
          }
          if (
            typeof parsedObj.message === 'string' &&
            parsedObj.message.trim() !== ''
          ) {
            return parsedObj.message
          }
        }
      } catch {
        return error.responseBody
      }

      return error.responseBody
    }
  }

  return defaultMessage
}

export default App
