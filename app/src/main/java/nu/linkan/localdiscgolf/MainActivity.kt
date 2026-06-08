package nu.linkan.localdiscgolf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nu.linkan.localdiscgolf.data.local.DatabaseProvider
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.ui.theme.LocalDiscgolfTheme
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutHoleEntity
import nu.linkan.localdiscgolf.data.local.model.LayoutHoleWithHole
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.model.RoundHolePlayerRow
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHoleRow
import nu.linkan.localdiscgolf.data.local.model.InProgressSessionRow
import nu.linkan.localdiscgolf.data.local.model.PlayerSessionRow
import nu.linkan.localdiscgolf.data.local.model.PlayerHoleStatsRow
import nu.linkan.localdiscgolf.data.local.model.PlayerLayoutStatsRow
import nu.linkan.localdiscgolf.data.local.model.PlayerHoleDetailRoundRow
import nu.linkan.localdiscgolf.data.local.model.RoundHolePlayerStatsRow
import nu.linkan.localdiscgolf.data.local.model.LocalResumeRoundListItem
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleVariantEntity
import nu.linkan.localdiscgolf.data.local.model.HoleVariantWithNames
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHeaderRow
import nu.linkan.localdiscgolf.data.local.model.PlayerListRow
import nu.linkan.localdiscgolf.data.local.model.CourseListRow
import nu.linkan.localdiscgolf.data.local.repository.LocalRoundCreationRepository
import nu.linkan.localdiscgolf.data.local.repository.LocalResumeRoundRepository
import nu.linkan.localdiscgolf.data.sync.ReferenceSyncRepository
import nu.linkan.localdiscgolf.data.sync.RoundSyncRepository

import nu.linkan.localdiscgolf.ui.dialogs.AddHoleDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleToLayoutDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleVariantDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddLayoutDialog
import nu.linkan.localdiscgolf.ui.dialogs.EditHoleDialog
import nu.linkan.localdiscgolf.ui.dialogs.HoleVariantsDialog
import nu.linkan.localdiscgolf.ui.dialogs.NameInputDialog
import nu.linkan.localdiscgolf.ui.screens.PlayerDetailScreen
import nu.linkan.localdiscgolf.ui.screens.PlayerStatsScreen
import nu.linkan.localdiscgolf.ui.screens.PlayersScreen
import nu.linkan.localdiscgolf.ui.screens.CoursesScreen
import nu.linkan.localdiscgolf.ui.screens.CourseDetailScreen
import nu.linkan.localdiscgolf.ui.screens.LayoutDetailScreen
import nu.linkan.localdiscgolf.ui.screens.LocalResumeRoundScreen
import nu.linkan.localdiscgolf.ui.components.HoleRow
import nu.linkan.localdiscgolf.ui.screens.RoundHoleScreen
import nu.linkan.localdiscgolf.ui.screens.formatRelativeScore
import nu.linkan.localdiscgolf.ui.screens.ScoreBadge
import nu.linkan.localdiscgolf.ui.screens.ApiCoursesScreen
import nu.linkan.localdiscgolf.ui.screens.ApiCourseLayoutsScreen
import nu.linkan.localdiscgolf.ui.screens.ApiLayoutHolesScreen
import nu.linkan.localdiscgolf.ui.screens.ApiPlayersScreen
import nu.linkan.localdiscgolf.ui.screens.LoginScreen
import nu.linkan.localdiscgolf.ui.screens.SettingsScreen
import nu.linkan.localdiscgolf.ui.screens.ApiPlayerRoundsScreen
import nu.linkan.localdiscgolf.ui.screens.ApiRoundDetailScreen
import nu.linkan.localdiscgolf.ui.screens.ApiNewRoundScreen
import nu.linkan.localdiscgolf.ui.screens.ApiRoundHoleScreen
import nu.linkan.localdiscgolf.ui.screens.ApiResumeRoundScreen
import nu.linkan.localdiscgolf.ui.screens.ApiPlayerDetailScreen
import nu.linkan.localdiscgolf.ui.screens.ApiPlayerStatsScreen

import nu.linkan.localdiscgolf.network.CourseApiResponse
import nu.linkan.localdiscgolf.network.LayoutApiResponse
import nu.linkan.localdiscgolf.network.LayoutHoleApiResponse
import nu.linkan.localdiscgolf.network.UserPlayersResponse
import nu.linkan.localdiscgolf.network.PlayerRoundApiResponse
import nu.linkan.localdiscgolf.network.RoundDetailApiResponse
import nu.linkan.localdiscgolf.network.CreateRoundApiRequest
import nu.linkan.localdiscgolf.network.CreateRoundPlayerApiRequest
import nu.linkan.localdiscgolf.network.CurrentRoundApiResponse
import nu.linkan.localdiscgolf.network.UpdateHoleApiRequest
import nu.linkan.localdiscgolf.network.UpdateHoleScoreApiRequest
import nu.linkan.localdiscgolf.network.ApiHttpException
import nu.linkan.localdiscgolf.network.MeResponse
import nu.linkan.localdiscgolf.network.CompleteRoundApiRequest
import nu.linkan.localdiscgolf.network.InProgressServerRoundApiResponse
import nu.linkan.localdiscgolf.network.PlayerLayoutStatsApiResponse
import nu.linkan.localdiscgolf.network.PlayerHoleStatsApiResponse

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.withContext
import nu.linkan.localdiscgolf.network.ApiClient

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.text.get

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = DatabaseProvider.getDatabase(this)
        val playerDao = db.playerDao()
        val courseDao = db.courseDao()
        val holeDao = db.holeDao()
        val layoutDao = db.layoutDao()
        val playSessionDao = db.playSessionDao()

        val referenceSyncRepository = ReferenceSyncRepository(
            referenceSyncDao = db.referenceSyncDao()
        )

        val localRoundCreationRepository = LocalRoundCreationRepository(
            localRoundCreationDao = db.localRoundCreationDao()
        )

        val roundSyncRepository = RoundSyncRepository(
            roundSyncDao = db.roundSyncDao()
        )

        val localResumeRoundRepository = LocalResumeRoundRepository(
            localResumeRoundDao = db.localResumeRoundDao()
        )

        setContent {
            LocalDiscgolfTheme {
                val prefs = getSharedPreferences("api_settings", Context.MODE_PRIVATE)

                var apiHost by remember { mutableStateOf(prefs.getString("host", "") ?: "") }
                var apiPort by remember { mutableStateOf(prefs.getString("port", "8000") ?: "8000") }
                var authToken by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
                var loggedInUsername by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
                var isCheckingSavedLogin by remember { mutableStateOf(authToken.isNotBlank()) }

                var apiCourses by remember { mutableStateOf<List<CourseApiResponse>>(emptyList()) }
                var apiLayouts by remember { mutableStateOf<List<LayoutApiResponse>>(emptyList()) }
                var apiLayoutHoles by remember { mutableStateOf<List<LayoutHoleApiResponse>>(emptyList()) }
                var apiUserPlayers by remember { mutableStateOf<UserPlayersResponse?>(null) }
                var apiPlayerRounds by remember { mutableStateOf<List<PlayerRoundApiResponse>>(emptyList()) }
                var selectedApiPlayerName by remember { mutableStateOf("") }
                var apiRoundDetail by remember { mutableStateOf<RoundDetailApiResponse?>(null) }
                var apiNewRoundLayouts by remember { mutableStateOf<List<LayoutApiResponse>>(emptyList()) }
                var apiCurrentRound by remember { mutableStateOf<CurrentRoundApiResponse?>(null) }
                var apiInProgressRounds by remember {
                    mutableStateOf<List<InProgressServerRoundApiResponse>>(emptyList())
                }

                var selectedApiPlayerId by remember { mutableStateOf<Long?>(null) }
                var selectedApiPlayerRoundCount by remember { mutableStateOf(0) }

                var apiLayoutStats by remember {
                    mutableStateOf<List<PlayerLayoutStatsApiResponse>>(emptyList())
                }

                var apiHoleStats by remember {
                    mutableStateOf<List<PlayerHoleStatsApiResponse>>(emptyList())
                }

                var apiStatsCourseId by remember { mutableStateOf<Long?>(null) }

                val navController = rememberNavController()

                var players by remember { mutableStateOf<List<PlayerEntity>>(emptyList()) }
                var courses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }
                val holesByCourse = remember { mutableStateMapOf<Long, List<HoleEntity>>() }
                val layoutsByCourse = remember { mutableStateMapOf<Long, List<LayoutEntity>>() }
                val layoutHolesByLayout = remember { mutableStateMapOf<Long, List<LayoutHoleWithHole>>() }
                val roundHoleRowsByKey = remember { mutableStateMapOf<String, List<RoundHolePlayerRow>>() }
                val holeCountBySession = remember { mutableStateMapOf<Long, Int>() }
                val roundSummaryRowsBySession = remember { mutableStateMapOf<Long, List<RoundSummaryHoleRow>>() }
                val inProgressSessions = remember { mutableStateOf<List<InProgressSessionRow>>(emptyList()) }
                val playerSessionsByPlayer = remember { mutableStateMapOf<Long, List<PlayerSessionRow>>() }
                val playerLayoutStatsByPlayer = remember { mutableStateMapOf<Long, List<PlayerLayoutStatsRow>>() }
                val playerHoleStatsByPlayer = remember { mutableStateMapOf<Long, List<PlayerHoleStatsRow>>() }
                val playerHoleDetailRowsByKey = remember { mutableStateMapOf<String, List<PlayerHoleDetailRoundRow>>() }
                val roundHoleStatsByKey = remember { mutableStateMapOf<String, List<RoundHolePlayerStatsRow>>() }
                val teesByHole = remember { mutableStateMapOf<Long, List<HoleTeeEntity>>() }
                val basketsByHole = remember { mutableStateMapOf<Long, List<HoleBasketEntity>>() }
                val variantsByHole = remember { mutableStateMapOf<Long, List<HoleVariantWithNames>>() }
                val roundSummaryHeaderBySession = remember { mutableStateMapOf<Long, RoundSummaryHeaderRow?>() }
                var playerListRows by remember { mutableStateOf<List<PlayerListRow>>(emptyList()) }
                var courseListRows by remember { mutableStateOf<List<CourseListRow>>(emptyList()) }

                LaunchedEffect(Unit) {
                    if (authToken.isBlank()) {
                        isCheckingSavedLogin = false
                        return@LaunchedEffect
                    }

                    if (apiHost.isBlank() || apiPort.isBlank()) {
                        isCheckingSavedLogin = false
                        return@LaunchedEffect
                    }

                    val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                    val result: Result<MeResponse> = withContext(Dispatchers.IO) {
                        ApiClient.getMe(baseUrl, authToken)
                    }

                    result.fold(
                        onSuccess = { me ->
                            loggedInUsername = me.username

                            prefs.edit()
                                .putString("username", me.username)
                                .apply()
                        },
                        onFailure = { error ->
                            if (error is ApiHttpException && error.statusCode == 401) {
                                prefs.edit()
                                    .remove("token")
                                    .remove("username")
                                    .apply()

                                authToken = ""
                                loggedInUsername = ""

                                Toast.makeText(
                                    this@MainActivity,
                                    "Din inloggning har gått ut. Logga in igen.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Kunde inte kontrollera inloggningen mot servern.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    isCheckingSavedLogin = false
                }
                LaunchedEffect(Unit) {
                    launch {
                        playerDao.observeActivePlayers().collectLatest { players = it }
                    }
                    launch {
                        courseDao.observeActiveCourses().collectLatest { courseList ->
                            courses = courseList
                        }
                    }
                    launch {
                        playSessionDao.observeInProgressSessions().collectLatest {
                            inProgressSessions.value = it
                        }
                    }
                    launch {
                        playerDao.observePlayerListRows().collectLatest { playerListRows = it }
                    }
                    launch {
                        courseDao.observeCourseListRows().collectLatest { courseListRows = it }
                    }
                }

                AppNavHost(
                    navController = navController,
                    players = players,
                    courses = courses,
                    holesByCourse = holesByCourse,
                    layoutsByCourse = layoutsByCourse,
                    layoutHolesByLayout = layoutHolesByLayout,
                    roundHoleRowsByKey = roundHoleRowsByKey,
                    holeCountBySession = holeCountBySession,
                    roundSummaryRowsBySession = roundSummaryRowsBySession,
                    inProgressSessions = inProgressSessions.value,
                    playerSessionsByPlayer = playerSessionsByPlayer,
                    playerLayoutStatsByPlayer = playerLayoutStatsByPlayer,
                    playerHoleStatsByPlayer = playerHoleStatsByPlayer,
                    playerHoleDetailRowsByKey = playerHoleDetailRowsByKey,
                    roundHoleStatsByKey = roundHoleStatsByKey,
                    teesByHole = teesByHole,
                    basketsByHole = basketsByHole,
                    variantsByHole = variantsByHole,
                    roundSummaryHeaderBySession = roundSummaryHeaderBySession,
                    playerListRows = playerListRows,
                    courseListRows = courseListRows,
                    apiHost = apiHost,
                    apiPort = apiPort,
                    authToken = authToken,
                    loggedInUsername = if (isCheckingSavedLogin) "" else loggedInUsername,
                    localRoundCreationRepository = localRoundCreationRepository,
                    localResumeRoundRepository = localResumeRoundRepository,
                    onSyncReferenceData = {
                        if (
                            apiHost.isBlank() ||
                            apiPort.isBlank() ||
                            authToken.isBlank() ||
                            loggedInUsername.isBlank()
                        ) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val result = withContext(Dispatchers.IO) {
                                    referenceSyncRepository.syncReferenceData(
                                        baseUrl = baseUrl,
                                        token = authToken,
                                        username = loggedInUsername
                                    )
                                }

                                result.fold(
                                    onSuccess = {
                                        val players = withContext(Dispatchers.IO) {
                                            db.cachedRoundSetupDao().getPlayersForRound()
                                        }

                                        val courses = withContext(Dispatchers.IO) {
                                            db.cachedRoundSetupDao().getCoursesForRound()
                                        }

                                        println("Cached players for round: ${players.size}")
                                        println("Cached courses for round: ${courses.size}")

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Synk klar. Spelare: ${players.size}, banor: ${courses.size}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Synk misslyckades: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onApiHostChange = { apiHost = it },
                    onApiPortChange = { apiPort = it },
                    onAuthTokenChange = { authToken = it },
                    onLoggedInUsernameChange = { loggedInUsername = it },
                    prefs = prefs,
                    activity = this,
                    apiCourses = apiCourses,
                    onLoadApiCourses = {
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getCourses(baseUrl, authToken)
                                }

                                result.fold(
                                    onSuccess = { courses ->
                                        apiCourses = courses
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta serverbanor: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onLogout = {
                        prefs.edit()
                            .remove("token")
                            .remove("username")
                            .apply()

                        authToken = ""
                        loggedInUsername = ""
                        isCheckingSavedLogin = false

                        Toast.makeText(this, "Utloggad", Toast.LENGTH_SHORT).show()
                    },
                    apiLayouts = apiLayouts,
                    onLoadApiLayouts = { courseId ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getCourseLayouts(baseUrl, authToken, courseId)
                                }

                                result.fold(
                                    onSuccess = { layouts ->
                                        apiLayouts = layouts
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta layouter: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    apiLayoutHoles = apiLayoutHoles,
                    onLoadApiLayoutHoles = { layoutId ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val result: Result<List<LayoutHoleApiResponse>> = withContext(Dispatchers.IO) {
                                    ApiClient.getLayoutHoles(baseUrl, authToken, layoutId)
                                }

                                result.fold(
                                    onSuccess = { holes: List<LayoutHoleApiResponse> ->
                                        apiLayoutHoles = holes
                                    },
                                    onFailure = { error: Throwable ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta layouthål: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    apiUserPlayers = apiUserPlayers,
                    onLoadApiUserPlayers = {
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank() || loggedInUsername.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getUserPlayers(baseUrl, authToken, loggedInUsername)
                                }

                                result.fold(
                                    onSuccess = { response ->
                                        apiUserPlayers = response
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta serverspelare: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    selectedApiPlayerId = selectedApiPlayerId,
                    selectedApiPlayerName = selectedApiPlayerName,
                    selectedApiPlayerRoundCount = selectedApiPlayerRoundCount,

                    onSelectApiPlayer = { playerId, playerName, roundCount ->
                        selectedApiPlayerId = playerId
                        selectedApiPlayerName = playerName
                        selectedApiPlayerRoundCount = roundCount
                    },

                    apiLayoutStats = apiLayoutStats,
                    apiHoleStats = apiHoleStats,
                    apiStatsCourseId = apiStatsCourseId,

                    onLoadApiPlayerStats = { playerId, courseId ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            apiStatsCourseId = courseId
                            apiLayoutStats = emptyList()
                            apiHoleStats = emptyList()

                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val layoutResult: Result<List<PlayerLayoutStatsApiResponse>> =
                                    withContext(Dispatchers.IO) {
                                        ApiClient.getPlayerLayoutStats(
                                            baseUrl = baseUrl,
                                            token = authToken,
                                            playerId = playerId,
                                            courseId = courseId
                                        )
                                    }

                                val holeResult: Result<List<PlayerHoleStatsApiResponse>> =
                                    withContext(Dispatchers.IO) {
                                        ApiClient.getPlayerHoleStats(
                                            baseUrl = baseUrl,
                                            token = authToken,
                                            playerId = playerId,
                                            courseId = courseId
                                        )
                                    }

                                layoutResult.fold(
                                    onSuccess = { stats ->
                                        apiLayoutStats = stats
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta layoutstatistik: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )

                                holeResult.fold(
                                    onSuccess = { stats ->
                                        apiHoleStats = stats
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta hålstatistik: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    apiRoundDetail = apiRoundDetail,
                    onLoadApiRoundDetail = { roundId ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getRoundDetail(baseUrl, authToken, roundId)
                                }

                                result.fold(
                                    onSuccess = { detail ->
                                        apiRoundDetail = detail
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta runddetalj: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    apiNewRoundLayouts = apiNewRoundLayouts,
                    onLoadApiNewRoundLayouts = { courseId ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getCourseLayouts(baseUrl, authToken, courseId)
                                }

                                result.fold(
                                    onSuccess = { layouts ->
                                        apiNewRoundLayouts = layouts
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta layouter: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onCreateApiRound = { courseId, layoutId, playerIds, onCreated ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val request = CreateRoundApiRequest(
                                    course_id = courseId,
                                    started_at = java.time.OffsetDateTime.now().toString(),
                                    players = playerIds.map { playerId ->
                                        CreateRoundPlayerApiRequest(
                                            player_id = playerId,
                                            layout_id = layoutId
                                        )
                                    }
                                )

                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.createRound(baseUrl, authToken, request)
                                }

                                result.fold(
                                    onSuccess = { round ->
                                        apiRoundDetail = round

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Serverrunda skapad",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        onCreated(round.id)
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte skapa serverrunda: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    apiCurrentRound = apiCurrentRound,
                    onLoadApiRoundHole = { roundId, sequenceNumber ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                apiCurrentRound = null

                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result: Result<CurrentRoundApiResponse> = withContext(Dispatchers.IO) {
                                    ApiClient.getRoundHole(
                                        baseUrl = baseUrl,
                                        token = authToken,
                                        roundId = roundId,
                                        sequenceNumber = sequenceNumber
                                    )
                                }

                                result.fold(
                                    onSuccess = { round ->
                                        apiCurrentRound = round
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta hålet: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onSaveApiHole = { roundId, sequenceNumber, values, onSaved ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val request = UpdateHoleApiRequest(
                                    scores = values.map { (playerId, throwsCount) ->
                                        UpdateHoleScoreApiRequest(
                                            player_id = playerId,
                                            throws_count = throwsCount
                                        )
                                    }
                                )

                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.updateRoundHole(
                                        baseUrl = baseUrl,
                                        token = authToken,
                                        roundId = roundId,
                                        sequenceNumber = sequenceNumber,
                                        requestBody = request
                                    )
                                }

                                result.fold(
                                    onSuccess = {
                                        onSaved()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte spara hålet: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onCompleteApiRound = { roundId, onCompleted ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val request = CompleteRoundApiRequest(
                                    ended_at = java.time.OffsetDateTime.now().toString()
                                )

                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.completeRound(
                                        baseUrl = baseUrl,
                                        token = authToken,
                                        roundId = roundId,
                                        requestBody = request
                                    )
                                }

                                result.fold(
                                    onSuccess = { completedRound ->
                                        apiRoundDetail = completedRound

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Rundan avslutad",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        onCompleted()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte avsluta rundan: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    apiInProgressRounds = apiInProgressRounds,

                    onLoadApiInProgressRounds = {
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getMyInProgressRounds(baseUrl, authToken)
                                }

                                result.fold(
                                    onSuccess = { rounds ->
                                        apiInProgressRounds = rounds
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta pågående rundor: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onSyncRounds = {
                        if (apiHost.isBlank() || apiPort.isBlank() || authToken.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val result = withContext(Dispatchers.IO) {
                                    roundSyncRepository.syncPendingRounds(
                                        baseUrl = baseUrl,
                                        token = authToken
                                    )
                                }

                                result.fold(
                                    onSuccess = { syncedCount ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Rundsynk klar. Synkade rundor: $syncedCount",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Rundsynk misslyckades: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onResumeApiRound = { roundId, onResolved ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Logga in och ange server först",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getCurrentRound(baseUrl, authToken, roundId)
                                }

                                result.fold(
                                    onSuccess = { currentRound ->
                                        apiCurrentRound = currentRound
                                        onResolved(currentRound.progress.current_sequence_number)
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte återuppta rundan: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onAddPlayer = {
                        Toast.makeText(
                            this@MainActivity,
                            "Lokal spelaredigering är borttagen",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onAddCourse = {
                        Toast.makeText(
                            this@MainActivity,
                            "Lokal banredigering är borttagen",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    observeCourseHoles = { courseId ->
                        lifecycleScope.launch {
                            holeDao.observeActiveHolesForCourse(courseId).collectLatest { holes ->
                                holesByCourse[courseId] = holes
                            }
                        }
                    },
                    onAddHole = { _, _, _, _, _, _ -> },
                    onUpdateHole = { _, _, _, _, _, _, _, _, _ -> },
                    observeSessionHoleCount = { playSessionId ->
                        lifecycleScope.launch {
                            holeCountBySession[playSessionId] = playSessionDao.getHoleCountForSession(playSessionId)
                        }
                    },
                    observeRoundSummaryRows = { playSessionId ->
                        lifecycleScope.launch {
                            playSessionDao.observeRoundSummaryHoleRows(playSessionId).collectLatest { rows ->
                                roundSummaryRowsBySession[playSessionId] = rows
                            }
                        }
                    },
                    observeCourseLayouts = { courseId ->
                        lifecycleScope.launch {
                            layoutDao.observeActiveLayoutsForCourse(courseId).collectLatest { layouts ->
                                layoutsByCourse[courseId] = layouts
                            }
                        }
                    },
                    observeRoundHoleStats = { playSessionId, holeId, holeVariantId ->
                        lifecycleScope.launch {
                            playSessionDao.observeHoleStatsForPlayersInSessionOnHole(
                                playSessionId,
                                holeId,
                                holeVariantId
                            ).collectLatest { rows ->
                                roundHoleStatsByKey["$playSessionId-$holeId-$holeVariantId"] = rows
                            }
                        }
                    },
                    onAddLayout = { _, _, _ -> },
                    observeLayoutHoles = { layoutId ->
                        lifecycleScope.launch {
                            layoutDao.observeLayoutHoles(layoutId).collectLatest { layoutHoles ->
                                layoutHolesByLayout[layoutId] = layoutHoles
                            }
                        }
                    },
                    observeRoundSummaryHeader = { playSessionId ->
                        lifecycleScope.launch {
                            playSessionDao.observeRoundSummaryHeader(playSessionId).collectLatest { header ->
                                roundSummaryHeaderBySession[playSessionId] = header
                            }
                        }
                    },
                    onAddHoleToLayout = { _, _, _ -> },
                    onRemoveHoleFromLayout = { _, _, _ -> },
                    onMoveHoleUpInLayout = { _, _ -> },
                    onMoveHoleDownInLayout = { _, _ -> },
                    apiPlayerRounds = apiPlayerRounds,
                    onLoadApiPlayerRounds = { playerId, playerName ->
                        if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                            Toast.makeText(this@MainActivity, "Logga in och ange server först", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.getPlayerRounds(baseUrl, authToken, playerId)
                                }

                                result.fold(
                                    onSuccess = { rounds ->
                                        selectedApiPlayerName = playerName
                                        apiPlayerRounds = rounds
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Kunde inte hämta spelarens rundor: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onCreateRound = { courseId, layoutId, playerIds, startedAt, onCreated ->
                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                localRoundCreationRepository.createLocalRoundFromCache(
                                    courseId = courseId,
                                    layoutId = layoutId,
                                    playerIds = playerIds,
                                    startedAt = startedAt
                                )
                            }

                            result.fold(
                                onSuccess = { playSessionId ->
                                    val syncResult = if (
                                        apiHost.isNotBlank() &&
                                        apiPort.isNotBlank() &&
                                        authToken.isNotBlank()
                                    ) {
                                        withContext(Dispatchers.IO) {
                                            val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                            roundSyncRepository.syncPendingRounds(
                                                baseUrl = baseUrl,
                                                token = authToken
                                            )
                                        }
                                    } else {
                                        null
                                    }

                                    if (syncResult == null) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Runda skapad lokalt",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        syncResult.fold(
                                            onSuccess = { syncedCount ->
                                                if (syncedCount > 0) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Runda skapad och synkad",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Runda skapad lokalt. Synkas senare.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onFailure = { error ->
                                                println("Autosynk efter skapad runda misslyckades: ${error.message}")

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Runda skapad lokalt. Synkas senare.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }

                                    onCreated(playSessionId)
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Kunde inte skapa lokal runda: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    observeRoundHoleRows = { playSessionId, sequenceNumber ->
                        lifecycleScope.launch {
                            playSessionDao.observeRoundHoleRows(playSessionId, sequenceNumber).collectLatest { rows ->
                                roundHoleRowsByKey["$playSessionId-$sequenceNumber"] = rows
                            }
                        }
                    },
                    observePlayerHoleDetail = { playerId, courseId, holeNumber, holeVariantId ->
                        lifecycleScope.launch {
                            playSessionDao.observeHoleDetailForPlayer(
                                playerId,
                                courseId,
                                holeNumber,
                                holeVariantId
                            ).collectLatest { rows ->
                                playerHoleDetailRowsByKey["$playerId-$courseId-$holeNumber-$holeVariantId"] = rows
                            }
                        }
                    },
                    observeHoleTees = { holeId ->
                        lifecycleScope.launch {
                            holeDao.observeActiveTeesForHole(holeId).collectLatest { tees ->
                                teesByHole[holeId] = tees
                            }
                        }
                    },
                    observeHoleBaskets = { holeId ->
                        lifecycleScope.launch {
                            holeDao.observeActiveBasketsForHole(holeId).collectLatest { baskets ->
                                basketsByHole[holeId] = baskets
                            }
                        }
                    },
                    onAddHoleTee = { _, _ -> },
                    onAddHoleBasket = { _, _ -> },
                    observePlayerSessions = { playerId ->
                        lifecycleScope.launch {
                            playSessionDao.observeSessionsForPlayer(playerId).collectLatest { rows ->
                                playerSessionsByPlayer[playerId] = rows
                            }
                        }
                    },
                    onUpdateCurrentSequenceNumber = { playSessionId, sequenceNumber ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                playSessionDao.updateCurrentSequenceNumber(
                                    playSessionId = playSessionId,
                                    sequenceNumber = sequenceNumber,
                                    updatedAt = System.currentTimeMillis()
                                )
                            }
                        }
                    },
                    onUpdateThrowsForHole = { playSessionId, sessionPlayerHoleId, throwsCount ->
                        lifecycleScope.launch {
                            val updatedAt = System.currentTimeMillis()

                            withContext(Dispatchers.IO) {
                                playSessionDao.updateThrowsAndMarkSessionDirty(
                                    playSessionId = playSessionId,
                                    sessionPlayerHoleId = sessionPlayerHoleId,
                                    throwsCount = throwsCount,
                                    updatedAt = updatedAt
                                )

                                if (apiHost.isNotBlank() && apiPort.isNotBlank() && authToken.isNotBlank()) {
                                    roundSyncRepository.syncPendingRounds(
                                        baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort),
                                        token = authToken
                                    )
                                }

                                val pending = db.roundSyncDao().getPendingRoundsForSync()

                                println("Pending rounds for sync: ${pending.size}")
                                pending.forEach {
                                    println(
                                        "Pending round: id=${it.id}, serverId=${it.serverId}, status=${it.status}, syncStatus=${it.syncStatus}"
                                    )
                                }
                            }
                        }
                    },
                    observePlayerLayoutStats = { playerId ->
                        lifecycleScope.launch {
                            playSessionDao.observeLayoutStatsForPlayer(playerId).collectLatest { rows ->
                                playerLayoutStatsByPlayer[playerId] = rows
                            }
                        }
                    },
                    observePlayerHoleStats = { playerId ->
                        lifecycleScope.launch {
                            playSessionDao.observeHoleStatsForPlayer(playerId).collectLatest { rows ->
                                playerHoleStatsByPlayer[playerId] = rows
                            }
                        }
                    },
                    observeHoleVariants = { holeId ->
                        lifecycleScope.launch {
                            holeDao.observeActiveVariantsForHole(holeId).collectLatest { variants ->
                                variantsByHole[holeId] = variants
                            }
                        }
                    },
                    onAddHoleVariant = { _, _, _, _, _ -> },
                    onDeleteRound = { _ -> },
                    onFinishRound = { playSessionId ->
                        lifecycleScope.launch {
                            val endedAt = System.currentTimeMillis()

                            withContext(Dispatchers.IO) {
                                playSessionDao.finishPlaySession(
                                    playSessionId = playSessionId,
                                    endedAt = endedAt,
                                    status = "completed",
                                    updatedAt = endedAt
                                )
                            }

                            val syncResult = if (
                                apiHost.isNotBlank() &&
                                apiPort.isNotBlank() &&
                                authToken.isNotBlank()
                            ) {
                                withContext(Dispatchers.IO) {
                                    val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                                    roundSyncRepository.syncPendingRounds(
                                        baseUrl = baseUrl,
                                        token = authToken
                                    )
                                }
                            } else {
                                null
                            }

                            if (syncResult == null) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Rundan avslutad lokalt. Synkas senare.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                syncResult.fold(
                                    onSuccess = { syncedCount ->
                                        if (syncedCount > 0) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Rundan avslutad och synkad",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Rundan avslutad lokalt. Synkas senare.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onFailure = { error ->
                                        println("Autosynk efter avslutad runda misslyckades: ${error.message}")

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Rundan avslutad lokalt. Synkas senare.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    onResumeRound = { _, _ -> }
                )
            }
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    players: List<PlayerEntity>,
    courses: List<CourseEntity>,
    holesByCourse: Map<Long, List<HoleEntity>>,
    layoutsByCourse: Map<Long, List<LayoutEntity>>,
    layoutHolesByLayout: Map<Long, List<LayoutHoleWithHole>>,
    roundHoleRowsByKey: Map<String, List<RoundHolePlayerRow>>,
    holeCountBySession: Map<Long, Int>,
    roundSummaryRowsBySession: Map<Long, List<RoundSummaryHoleRow>>,
    onAddPlayer: (String) -> Unit,
    onAddCourse: (String) -> Unit,
    observeCourseHoles: (Long) -> Unit,
    onAddHole: (Long, Int, String?, Int, Int, String?) -> Unit,
    onUpdateHole: (Long, Long, Int, String?, Int, Int, String?, Boolean, Long) -> Unit,
    observeCourseLayouts: (Long) -> Unit,
    onAddLayout: (Long, String, String?) -> Unit,
    observeLayoutHoles: (Long) -> Unit,
    onAddHoleToLayout: (Long, Long, Long?) -> Unit,
    onRemoveHoleFromLayout: (Long, Int, Long) -> Unit,
    onMoveHoleUpInLayout: (List<LayoutHoleWithHole>, Int) -> Unit,
    onMoveHoleDownInLayout: (List<LayoutHoleWithHole>, Int) -> Unit,
    onCreateRound: (Long, Long, List<Long>, Long, (Long) -> Unit) -> Unit,
    localRoundCreationRepository: LocalRoundCreationRepository,
    localResumeRoundRepository: LocalResumeRoundRepository,
    observeRoundHoleRows: (Long, Int) -> Unit,
    onUpdateCurrentSequenceNumber: (Long, Int) -> Unit,
    onUpdateThrowsForHole: (Long, Long, Int?) -> Unit,
    onFinishRound: (Long) -> Unit,
    observeSessionHoleCount: (Long) -> Unit,
    observeRoundSummaryRows: (Long) -> Unit,
    inProgressSessions: List<InProgressSessionRow>,
    onResumeRound: (Long, (Int) -> Unit) -> Unit,
    playerSessionsByPlayer: Map<Long, List<PlayerSessionRow>>,
    observePlayerSessions: (Long) -> Unit,
    playerLayoutStatsByPlayer: Map<Long, List<PlayerLayoutStatsRow>>,
    playerHoleStatsByPlayer: Map<Long, List<PlayerHoleStatsRow>>,
    observePlayerLayoutStats: (Long) -> Unit,
    observePlayerHoleStats: (Long) -> Unit,
    playerHoleDetailRowsByKey: Map<String, List<PlayerHoleDetailRoundRow>>,
    observePlayerHoleDetail: (Long, Long, Int, Long?) -> Unit,
    roundHoleStatsByKey: Map<String, List<RoundHolePlayerStatsRow>>,
    observeRoundHoleStats: (Long, Long, Long?) -> Unit,
    onDeleteRound: (Long) -> Unit,
    teesByHole: Map<Long, List<HoleTeeEntity>>,
    basketsByHole: Map<Long, List<HoleBasketEntity>>,
    observeHoleTees: (Long) -> Unit,
    observeHoleBaskets: (Long) -> Unit,
    onAddHoleTee: (Long, String) -> Unit,
    onAddHoleBasket: (Long, String) -> Unit,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    observeHoleVariants: (Long) -> Unit,
    onAddHoleVariant: (Long, Long, Long, Int, Int) -> Unit,
    roundSummaryHeaderBySession: Map<Long, RoundSummaryHeaderRow?>,
    observeRoundSummaryHeader: (Long) -> Unit,
    playerListRows: List<PlayerListRow>,
    courseListRows: List<CourseListRow>,
    apiHost: String,
    apiPort: String,
    authToken: String,
    loggedInUsername: String,
    onLogout: () -> Unit,
    onApiHostChange: (String) -> Unit,
    onApiPortChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onSyncReferenceData: () -> Unit,
    onSyncRounds: () -> Unit,
    onLoggedInUsernameChange: (String) -> Unit,
    prefs: android.content.SharedPreferences,
    activity: ComponentActivity,
    apiCourses: List<CourseApiResponse>,
    onLoadApiCourses: () -> Unit,
    apiLayouts: List<LayoutApiResponse>,
    onLoadApiLayouts: (Long) -> Unit,
    apiLayoutHoles: List<LayoutHoleApiResponse>,
    onLoadApiLayoutHoles: (Long) -> Unit,
    apiUserPlayers: UserPlayersResponse?,
    onLoadApiUserPlayers: () -> Unit,
    apiPlayerRounds: List<PlayerRoundApiResponse>,
    selectedApiPlayerName: String,
    onLoadApiPlayerRounds: (Long, String) -> Unit,
    apiRoundDetail: RoundDetailApiResponse?,
    onLoadApiRoundDetail: (Long) -> Unit,
    apiNewRoundLayouts: List<LayoutApiResponse>,
    onLoadApiNewRoundLayouts: (Long) -> Unit,
    onCreateApiRound: (Long, Long, List<Long>, (Long) -> Unit) -> Unit,
    apiCurrentRound: CurrentRoundApiResponse?,
    onLoadApiRoundHole: (Long, Int) -> Unit,
    onSaveApiHole: (Long, Int, List<Pair<Long, Int?>>, () -> Unit) -> Unit,
    onCompleteApiRound: (Long, () -> Unit) -> Unit,
    apiInProgressRounds: List<InProgressServerRoundApiResponse>,
    onLoadApiInProgressRounds: () -> Unit,
    onResumeApiRound: (Long, (Int) -> Unit) -> Unit,
    selectedApiPlayerId: Long?,
    selectedApiPlayerRoundCount: Int,
    onSelectApiPlayer: (Long, String, Int) -> Unit,

    apiLayoutStats: List<PlayerLayoutStatsApiResponse>,
    apiHoleStats: List<PlayerHoleStatsApiResponse>,
    apiStatsCourseId: Long?,
    onLoadApiPlayerStats: (Long, Long?) -> Unit,
){
    val coroutineScope = rememberCoroutineScope()

    var localResumeRounds by remember {
        mutableStateOf<List<LocalResumeRoundListItem>>(emptyList())
    }

    var isLoadingLocalResumeRounds by remember {
        mutableStateOf(false)
    }

    var localResumeRoundsError by remember {
        mutableStateOf<String?>(null)
    }

    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onPlayersClick = { navController.navigate("api_players") },
                onCoursesClick = { navController.navigate("api_courses") },
                onNewRoundClick = { navController.navigate("api_new_round") },
                onResumeRoundClick = { navController.navigate("api_resume_round") },
                onSyncClick = onSyncReferenceData,
                onSyncRoundsClick = onSyncRounds,
                onSettingsClick = { navController.navigate("settings") },
                onLoginClick = { navController.navigate("login") },
                onLogoutClick = onLogout,
                loggedInUsername = loggedInUsername.ifBlank { null }
            )
        }

        composable("settings") {
            SettingsScreen(
                currentHost = apiHost,
                currentPort = apiPort,
                onBack = { navController.popBackStack() },
                onSave = { host, port ->
                    prefs.edit()
                        .putString("host", host)
                        .putString("port", port)
                        .apply()

                    onApiHostChange(host)
                    onApiPortChange(port)

                    Toast.makeText(activity, "Inställningar sparade", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }

        composable("login") {
            LoginScreen(
                host = apiHost,
                port = apiPort,
                onBack = { navController.popBackStack() },
                onLogin = { username, password ->
                    coroutineScope.launch {
                        val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)

                        val loginResult = withContext(Dispatchers.IO) {
                            ApiClient.login(baseUrl, username, password)
                        }

                        loginResult.fold(
                            onSuccess = { loginResponse ->
                                val meResult = withContext(Dispatchers.IO) {
                                    ApiClient.getMe(baseUrl, loginResponse.access_token)
                                }

                                meResult.fold(
                                    onSuccess = {
                                        val coursesResult = withContext(Dispatchers.IO) {
                                            ApiClient.getCourses(baseUrl, loginResponse.access_token)
                                        }

                                        prefs.edit()
                                            .putString("token", loginResponse.access_token)
                                            .putString("username", loginResponse.username)
                                            .apply()

                                        onAuthTokenChange(loginResponse.access_token)
                                        onLoggedInUsernameChange(loginResponse.username)

                                        val message = when {
                                            loginResponse.must_change_password ->
                                                "Inloggad. Lösenordsbyte krävs."
                                            coursesResult.isSuccess ->
                                                "Inloggad som ${loginResponse.username}. Banor hämtade."
                                            else ->
                                                "Inloggad som ${loginResponse.username}, men kunde inte hämta banor."
                                        }

                                        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                                        navController.popBackStack()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            activity,
                                            "Login lyckades men /me misslyckades: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            },
                            onFailure = { error ->
                                Toast.makeText(
                                    activity,
                                    "Inloggning misslyckades: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            )
        }

        composable("players") {
            PlayersScreen(
                playerRows = playerListRows,
                onBack = { navController.popBackStack() },
                onAddPlayer = onAddPlayer,
                onPlayerClick = { playerId ->
                    navController.navigate("player/$playerId")
                }
            )
        }

        composable("api_player_rounds") {
            ApiPlayerRoundsScreen(
                playerName = selectedApiPlayerName,
                rounds = apiPlayerRounds,
                onBack = { navController.popBackStack() },
                onRoundClick = { roundId ->
                    onLoadApiRoundDetail(roundId)
                    navController.navigate("api_round_detail")
                }
            )
        }

        composable("api_round_detail") {
            ApiRoundDetailScreen(
                round = apiRoundDetail,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "api_round_hole/{roundId}/{sequenceNumber}",
            arguments = listOf(
                navArgument("roundId") { type = NavType.LongType },
                navArgument("sequenceNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val roundId = backStackEntry.arguments?.getLong("roundId") ?: return@composable
            val sequenceNumber = backStackEntry.arguments?.getInt("sequenceNumber") ?: return@composable

            var holeStatsByPlayerId by remember(roundId, sequenceNumber) {
                mutableStateOf<Map<Long, PlayerHoleStatsApiResponse>>(emptyMap())
            }

            var isLoadingHoleStats by remember(roundId, sequenceNumber) {
                mutableStateOf(false)
            }

            LaunchedEffect(roundId, sequenceNumber) {
                onLoadApiRoundHole(roundId, sequenceNumber)
            }

            LaunchedEffect(
                apiCurrentRound?.round?.id,
                apiCurrentRound?.round?.course_id,
                apiCurrentRound?.current_hole?.hole_variant_id,
                apiCurrentRound?.current_hole?.sequence_number
            ) {
                val loadedRound = apiCurrentRound ?: return@LaunchedEffect

                if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                    holeStatsByPlayerId = emptyMap()
                    return@LaunchedEffect
                }

                isLoadingHoleStats = true

                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                val currentHole = loadedRound.current_hole
                val newStatsByPlayerId = mutableMapOf<Long, PlayerHoleStatsApiResponse>()

                loadedRound.current_hole.scores.forEach { score ->
                    val result = withContext(Dispatchers.IO) {
                        ApiClient.getPlayerHoleStats(
                            baseUrl = baseUrl,
                            token = authToken,
                            playerId = score.player_id,
                            courseId = loadedRound.round.course_id
                        )
                    }

                    result.onSuccess { statsRows ->
                        val matchingStats = statsRows.firstOrNull { stats ->
                            stats.hole_variant_id == currentHole.hole_variant_id
                        } ?: statsRows.firstOrNull { stats ->
                            stats.hole_id == currentHole.hole_id &&
                                    stats.tee_name == currentHole.tee_name &&
                                    stats.basket_name == currentHole.basket_name
                        }

                        if (matchingStats != null) {
                            newStatsByPlayerId[score.player_id] = matchingStats
                        }
                    }
                }

                holeStatsByPlayerId = newStatsByPlayerId
                isLoadingHoleStats = false
            }

            val totalHoles = apiCurrentRound?.progress?.total_holes ?: 0

            ApiRoundHoleScreen(
                currentRound = apiCurrentRound,
                holeStatsByPlayerId = holeStatsByPlayerId,
                isLoadingHoleStats = isLoadingHoleStats,
                onBack = { navController.popBackStack() },

                onPreviousHole = if (sequenceNumber > 1) {
                    { values ->
                        onSaveApiHole(roundId, sequenceNumber, values) {
                            navController.navigate(
                                "api_round_hole/$roundId/${sequenceNumber - 1}"
                            )
                        }
                    }
                } else {
                    null
                },

                onNextHole = if (totalHoles > 0 && sequenceNumber < totalHoles) {
                    { values ->
                        onSaveApiHole(roundId, sequenceNumber, values) {
                            navController.navigate(
                                "api_round_hole/$roundId/${sequenceNumber + 1}"
                            )
                        }
                    }
                } else {
                    null
                },

                onShowSummary = { values ->
                    onSaveApiHole(roundId, sequenceNumber, values) {
                        onLoadApiRoundDetail(roundId)
                        navController.navigate("api_round_detail")
                    }
                },

                onFinishRound = { values ->
                    onSaveApiHole(roundId, sequenceNumber, values) {
                        onCompleteApiRound(roundId) {
                            navController.navigate("api_round_detail") {
                                popUpTo("api_round_hole/$roundId/$sequenceNumber") {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }
            )
        }

        composable("api_new_round") {
            NewRoundScreen(
                players = players,
                courses = courses,
                layoutsByCourse = layoutsByCourse,
                onBack = { navController.popBackStack() },
                observeCourseLayouts = observeCourseLayouts,
                onCreateRound = { courseId, layoutId, playerIds, startedAt, onCreated ->
                    onCreateRound(courseId, layoutId, playerIds, startedAt) { playSessionId ->
                        onCreated(playSessionId)

                        navController.navigate("round/$playSessionId/1") {
                            popUpTo("api_new_round") {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }

        composable("api_resume_round") {
            LaunchedEffect(Unit) {
                isLoadingLocalResumeRounds = true
                localResumeRoundsError = null

                val baseUrl = if (apiHost.isBlank() || apiPort.isBlank()) {
                    null
                } else {
                    ApiClient.buildBaseUrl(apiHost, apiPort)
                }

                val result = withContext(Dispatchers.IO) {
                    localResumeRoundRepository.getResumeRounds(
                        baseUrl = baseUrl,
                        token = authToken.ifBlank { null }
                    )
                }

                result.fold(
                    onSuccess = { rounds ->
                        localResumeRounds = rounds
                    },
                    onFailure = { error ->
                        localResumeRoundsError = error.message
                    }
                )

                isLoadingLocalResumeRounds = false
            }

            LocalResumeRoundScreen(
                rounds = localResumeRounds,
                isLoading = isLoadingLocalResumeRounds,
                error = localResumeRoundsError,
                onBack = { navController.popBackStack() },
                onRoundClick = { playSessionId, sequenceNumber ->
                    navController.navigate("round/$playSessionId/$sequenceNumber")
                }
            )
        }

        composable("courses") {
            CoursesScreen(
                courseRows = courseListRows,
                onBack = { navController.popBackStack() },
                onAddCourse = onAddCourse,
                onCourseClick = { courseId ->
                    navController.navigate("course/$courseId")
                }
            )
        }

        composable("api_courses") {
            LaunchedEffect(Unit) {
                onLoadApiCourses()
            }

            ApiCoursesScreen(
                courses = apiCourses,
                onBack = { navController.popBackStack() },
                onCourseClick = { courseId ->
                    navController.navigate("api_course/$courseId")
                }
            )
        }

        composable(
            route = "api_course/{courseId}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            val course = apiCourses.firstOrNull { it.id == courseId }

            LaunchedEffect(courseId) {
                onLoadApiLayouts(courseId)
            }

            ApiCourseLayoutsScreen(
                courseName = course?.name ?: "Serverlayouter",
                layouts = apiLayouts,
                onBack = { navController.popBackStack() },
                onLayoutClick = { layoutId ->
                    navController.navigate("api_layout/$layoutId")
                }
            )
        }

        composable(
            route = "api_layout/{layoutId}",
            arguments = listOf(
                navArgument("layoutId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val layoutId = backStackEntry.arguments?.getLong("layoutId") ?: return@composable
            val layout = apiLayouts.firstOrNull { it.id == layoutId }

            LaunchedEffect(layoutId) {
                onLoadApiLayoutHoles(layoutId)
            }

            ApiLayoutHolesScreen(
                layoutName = layout?.name ?: "Layout",
                holes = apiLayoutHoles,
                onBack = { navController.popBackStack() }
            )
        }

        composable("api_players") {
            LaunchedEffect(Unit) {
                onLoadApiUserPlayers()
            }

            ApiPlayersScreen(
                data = apiUserPlayers,
                onBack = { navController.popBackStack() },
                onPlayerClick = { playerId, playerName, roundCount ->
                    onSelectApiPlayer(playerId, playerName, roundCount)
                    navController.navigate("api_player_detail")
                }
            )
        }

        composable("api_player_detail") {
            val playerId = selectedApiPlayerId

            ApiPlayerDetailScreen(
                playerName = selectedApiPlayerName,
                roundCount = selectedApiPlayerRoundCount,
                onBack = { navController.popBackStack() },
                onRoundsClick = {
                    if (playerId != null) {
                        onLoadApiPlayerRounds(playerId, selectedApiPlayerName)
                        navController.navigate("api_player_rounds")
                    }
                },
                onStatsClick = {
                    if (playerId != null) {
                        onLoadApiCourses()
                        onLoadApiPlayerStats(playerId, null)
                        navController.navigate("api_player_stats")
                    }
                }
            )
        }

        composable("api_player_stats") {
            val playerId = selectedApiPlayerId

            ApiPlayerStatsScreen(
                playerName = selectedApiPlayerName,
                courses = apiCourses,
                selectedCourseId = apiStatsCourseId,
                layoutStats = apiLayoutStats,
                holeStats = apiHoleStats,
                onBack = { navController.popBackStack() },
                onCourseSelected = { courseId ->
                    if (playerId != null) {
                        onLoadApiPlayerStats(playerId, courseId)
                    }
                }
            )
        }

        composable(
            route = "course/{courseId}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            val course = courses.firstOrNull { it.id == courseId }
            val holes = holesByCourse[courseId] ?: emptyList()
            val layouts = layoutsByCourse[courseId] ?: emptyList()

            LaunchedEffect(courseId) {
                observeCourseHoles(courseId)
                observeCourseLayouts(courseId)
            }

            if (course != null) {
                CourseDetailScreen(
                    course = course,
                    holes = holes,
                    layouts = layouts,
                    teesByHole = teesByHole,
                    basketsByHole = basketsByHole,
                    variantsByHole = variantsByHole,
                    onBack = { navController.popBackStack() },
                    onAddHole = { holeNumber, name, lengthMeters, parValue, notes ->
                        onAddHole(courseId, holeNumber, name, lengthMeters, parValue, notes)
                    },
                    onUpdateHole = { hole ->
                        onUpdateHole(
                            hole.id,
                            hole.courseId,
                            hole.holeNumber,
                            hole.name,
                            hole.lengthMeters,
                            hole.parValue,
                            hole.notes,
                            hole.isActive,
                            hole.createdAt
                        )
                    },
                    onAddLayout = { name, description ->
                        onAddLayout(courseId, name, description)
                    },
                    onLayoutClick = { layoutId ->
                        navController.navigate("layout/$layoutId")
                    },
                    observeHoleTees = observeHoleTees,
                    observeHoleBaskets = observeHoleBaskets,
                    observeHoleVariants = observeHoleVariants,
                    onAddHoleTee = onAddHoleTee,
                    onAddHoleBasket = onAddHoleBasket,
                    onAddHoleVariant = onAddHoleVariant
                )
            }
        }

        composable(
            route = "player/{playerId}",
            arguments = listOf(
                navArgument("playerId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getLong("playerId") ?: return@composable
            val player = players.firstOrNull { it.id == playerId }
            val sessions = playerSessionsByPlayer[playerId] ?: emptyList()

            LaunchedEffect(playerId) {
                observePlayerSessions(playerId)
            }

            if (player != null) {
                PlayerDetailScreen(
                    player = player,
                    sessions = sessions,
                    onBack = { navController.popBackStack() },
                    onSessionClick = { playSessionId ->
                        navController.navigate("session/$playSessionId")
                    },
                    onStatsClick = {
                        navController.navigate("player_stats/$playerId")
                    },
                    onDeleteSession = { playSessionId ->
                        onDeleteRound(playSessionId)
                    }
                )
            }
        }

        composable(
            route = "layout/{layoutId}",
            arguments = listOf(
                navArgument("layoutId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val layoutId = backStackEntry.arguments?.getLong("layoutId") ?: return@composable
            val layoutHoles = layoutHolesByLayout[layoutId] ?: emptyList()

            LaunchedEffect(layoutId) {
                observeLayoutHoles(layoutId)
            }

            val allLayouts = layoutsByCourse.values.flatten()
            val currentLayout = allLayouts.firstOrNull { it.id == layoutId }

            if (currentLayout != null) {
                val courseHoles = holesByCourse[currentLayout.courseId] ?: emptyList()

                LaunchedEffect(currentLayout.courseId) {
                    observeCourseHoles(currentLayout.courseId)
                }

                LayoutDetailScreen(
                    layout = currentLayout,
                    availableHoles = courseHoles,
                    variantsByHole = variantsByHole,
                    layoutHoles = layoutHoles,
                    onBack = { navController.popBackStack() },
                    onAddHoleToLayout = { holeId, holeVariantId ->
                        onAddHoleToLayout(layoutId, holeId, holeVariantId)
                    },
                    onRemoveHoleFromLayout = { layoutHoleId, sequenceNumber ->
                        onRemoveHoleFromLayout(layoutHoleId, sequenceNumber, layoutId)
                    },
                    onMoveHoleUp = { index ->
                        onMoveHoleUpInLayout(layoutHoles, index)
                    },
                    onMoveHoleDown = { index ->
                        onMoveHoleDownInLayout(layoutHoles, index)
                    }
                )
            }
        }

        composable(
            route = "session/{playSessionId}",
            arguments = listOf(
                navArgument("playSessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playSessionId = backStackEntry.arguments?.getLong("playSessionId") ?: return@composable

            LaunchedEffect(playSessionId) {
                observeRoundSummaryRows(playSessionId)
                observeRoundSummaryHeader(playSessionId)
            }

            val rows = roundSummaryRowsBySession[playSessionId] ?: emptyList()
            val header = roundSummaryHeaderBySession[playSessionId]

            RoundSummaryScreen(
                title = "Runddetalj",
                courseName = header?.courseName ?: "",
                layoutName = header?.layoutName,
                startedAt = header?.startedAt,
                rows = rows,
                onBack = { navController.popBackStack() },
                onBackToStart = null,
                onBackToRound = null
            )
        }

        composable("new_round") {
            NewRoundScreen(
                players = players,
                courses = courses,
                layoutsByCourse = layoutsByCourse,
                onBack = { navController.popBackStack() },
                observeCourseLayouts = observeCourseLayouts,
                onCreateRound = { courseId, layoutId, selectedPlayerIds, startedAt, onCreated ->
                    onCreateRound(
                        courseId,
                        layoutId,
                        selectedPlayerIds,
                        startedAt
                    ) { playSessionId ->
                        onCreated(playSessionId)

                        navController.navigate("round/$playSessionId/1") {
                            popUpTo("api_new_round") {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }

        composable(
            route = "round/{playSessionId}/{sequenceNumber}",
            arguments = listOf(
                navArgument("playSessionId") { type = NavType.LongType },
                navArgument("sequenceNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val playSessionId = backStackEntry.arguments?.getLong("playSessionId") ?: return@composable
            val sequenceNumber = backStackEntry.arguments?.getInt("sequenceNumber") ?: return@composable

            LaunchedEffect(playSessionId, sequenceNumber) {
                observeRoundHoleRows(playSessionId, sequenceNumber)
                observeSessionHoleCount(playSessionId)
            }

            val rows = roundHoleRowsByKey["$playSessionId-$sequenceNumber"] ?: emptyList()
            val holeCount = holeCountBySession[playSessionId] ?: 0

            var localHoleStatsByPlayerId by remember(playSessionId, sequenceNumber) {
                mutableStateOf<Map<Long, PlayerHoleStatsApiResponse>>(emptyMap())
            }

            var isLoadingLocalHoleStats by remember(playSessionId, sequenceNumber) {
                mutableStateOf(false)
            }

            val holeId = rows.firstOrNull()?.holeId
            val holeVariantId = rows.firstOrNull()?.holeVariantId

            LaunchedEffect(playSessionId, holeId, holeVariantId) {
                if (holeId != null) {
                    observeRoundHoleStats(playSessionId, holeId, holeVariantId)
                }
            }

            val statsRows = if (holeId != null) {
                roundHoleStatsByKey["$playSessionId-$holeId-$holeVariantId"] ?: emptyList()
            } else {
                emptyList()
            }

            LaunchedEffect(playSessionId, sequenceNumber, rows) {
                if (authToken.isBlank() || apiHost.isBlank() || apiPort.isBlank()) {
                    localHoleStatsByPlayerId = emptyMap()
                    return@LaunchedEffect
                }

                if (rows.isEmpty()) {
                    localHoleStatsByPlayerId = emptyMap()
                    return@LaunchedEffect
                }

                isLoadingLocalHoleStats = true

                val baseUrl = ApiClient.buildBaseUrl(apiHost, apiPort)
                val newStatsByPlayerId = mutableMapOf<Long, PlayerHoleStatsApiResponse>()

                rows.forEach { row ->
                    val serverPlayerId = row.serverPlayerId
                    val serverCourseId = row.serverCourseId
                    val serverHoleVariantId = row.serverHoleVariantId

                    if (
                        serverPlayerId != null &&
                        serverCourseId != null &&
                        serverHoleVariantId != null
                    ) {
                        val result = withContext(Dispatchers.IO) {
                            ApiClient.getPlayerHoleStats(
                                baseUrl = baseUrl,
                                token = authToken,
                                playerId = serverPlayerId,
                                courseId = serverCourseId
                            )
                        }

                        result.onSuccess { statsRows ->
                            val matchingStats = statsRows.firstOrNull { stats ->
                                stats.hole_variant_id == serverHoleVariantId
                            }

                            if (matchingStats != null) {
                                newStatsByPlayerId[row.playerId] = matchingStats
                            }
                        }
                    }
                }

                localHoleStatsByPlayerId = newStatsByPlayerId
                isLoadingLocalHoleStats = false
            }
            RoundHoleScreen(
                rows = rows,
                statsRows = statsRows,
                holeStatsByPlayerId = localHoleStatsByPlayerId,
                isLoadingHoleStats = isLoadingLocalHoleStats,
                sequenceNumber = sequenceNumber,
                totalHoleCount = holeCount,
                onBack = {
                    navController.navigate("start") {
                        popUpTo("start") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onPreviousHole = {
                    val previousSequenceNumber = sequenceNumber - 1

                    onUpdateCurrentSequenceNumber(
                        playSessionId,
                        previousSequenceNumber
                    )

                    navController.navigate("round/$playSessionId/$previousSequenceNumber")
                },
                onNextHole = {
                    val nextSequenceNumber = sequenceNumber + 1

                    onUpdateCurrentSequenceNumber(
                        playSessionId,
                        nextSequenceNumber
                    )

                    navController.navigate("round/$playSessionId/$nextSequenceNumber")
                },
                onShowSummary = {
                    navController.navigate("round_summary_live/$playSessionId/$sequenceNumber")
                },
                onSaveHoleResults = { values ->
                    values.forEach { (sessionPlayerHoleId, throwsCount) ->
                        onUpdateThrowsForHole(
                            playSessionId,
                            sessionPlayerHoleId,
                            throwsCount
                        )
                    }

                    onUpdateCurrentSequenceNumber(
                        playSessionId,
                        sequenceNumber
                    )
                },
                onFinishRound = {
                    onFinishRound(playSessionId)

                    navController.navigate("round_summary/$playSessionId") {
                        popUpTo("round/$playSessionId/$sequenceNumber") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = "round_summary_live/{playSessionId}/{sequenceNumber}",
            arguments = listOf(
                navArgument("playSessionId") { type = NavType.LongType },
                navArgument("sequenceNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val playSessionId = backStackEntry.arguments?.getLong("playSessionId") ?: return@composable
            val sequenceNumber = backStackEntry.arguments?.getInt("sequenceNumber") ?: return@composable

            LaunchedEffect(playSessionId) {
                observeRoundSummaryRows(playSessionId)
                observeRoundSummaryHeader(playSessionId)
            }

            val rows = roundSummaryRowsBySession[playSessionId] ?: emptyList()
            val header = roundSummaryHeaderBySession[playSessionId]

            RoundSummaryScreen(
                title = "Rundsummering",
                courseName = header?.courseName ?: "",
                layoutName = header?.layoutName,
                startedAt = header?.startedAt,
                rows = rows,
                onBack = {
                    navController.popBackStack()
                },
                onBackToStart = null,
                onBackToRound = {
                    navController.navigate("round/$playSessionId/$sequenceNumber") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "round_summary/{playSessionId}",
            arguments = listOf(
                navArgument("playSessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playSessionId = backStackEntry.arguments?.getLong("playSessionId") ?: return@composable

            LaunchedEffect(playSessionId) {
                observeRoundSummaryRows(playSessionId)
                observeRoundSummaryHeader(playSessionId)
            }

            val rows = roundSummaryRowsBySession[playSessionId] ?: emptyList()
            val header = roundSummaryHeaderBySession[playSessionId]

            RoundSummaryScreen(
                title = "Rundsummering",
                courseName = header?.courseName ?: "",
                layoutName = header?.layoutName,
                startedAt = header?.startedAt,
                rows = rows,
                onBack = null,
                onBackToStart = {
                    navController.navigate("start") {
                        popUpTo("start") { inclusive = true }
                    }
                },
                onBackToRound = null
            )
        }

        composable("resume_round") {
            ResumeRoundScreen(
                sessions = inProgressSessions,
                onBack = { navController.popBackStack() },
                onResume = { playSessionId ->
                    onResumeRound(playSessionId) { sequenceNumber ->
                        navController.navigate("round/$playSessionId/$sequenceNumber")
                    }
                }
            )
        }

        composable(
            route = "player_hole_stats/{playerId}/{courseId}/{holeNumber}/{holeVariantId}",
            arguments = listOf(
                navArgument("playerId") { type = NavType.LongType },
                navArgument("courseId") { type = NavType.LongType },
                navArgument("holeNumber") { type = NavType.IntType },
                navArgument("holeVariantId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getLong("playerId") ?: return@composable
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            val holeNumber = backStackEntry.arguments?.getInt("holeNumber") ?: return@composable
            val holeVariantIdRaw = backStackEntry.arguments?.getLong("holeVariantId") ?: 0L
            val holeVariantId = if (holeVariantIdRaw == 0L) null else holeVariantIdRaw

            LaunchedEffect(playerId, courseId, holeNumber, holeVariantId) {
                observePlayerHoleDetail(playerId, courseId, holeNumber, holeVariantId)
            }

            val rows = playerHoleDetailRowsByKey["$playerId-$courseId-$holeNumber-$holeVariantId"] ?: emptyList()

            PlayerHoleDetailScreen(
                rows = rows,
                onBack = {
                    navController.navigate("start") {
                        popUpTo("start") {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = "player_stats/{playerId}",
            arguments = listOf(
                navArgument("playerId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getLong("playerId") ?: return@composable
            val player = players.firstOrNull { it.id == playerId }
            val layoutStats = playerLayoutStatsByPlayer[playerId] ?: emptyList()
            val holeStats = playerHoleStatsByPlayer[playerId] ?: emptyList()

            LaunchedEffect(playerId) {
                observePlayerLayoutStats(playerId)
                observePlayerHoleStats(playerId)
            }

            if (player != null) {
                PlayerStatsScreen(
                    player = player,
                    layoutStats = layoutStats,
                    holeStats = holeStats,
                    onBack = { navController.popBackStack() },
                    onHoleClick = { courseId, holeNumber, holeVariantId ->
                        val variantPart = holeVariantId ?: 0L
                        navController.navigate("player_hole_stats/$playerId/$courseId/$holeNumber/$variantPart")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onPlayersClick: () -> Unit,
    onCoursesClick: () -> Unit,
    onNewRoundClick: () -> Unit,
    onResumeRoundClick: () -> Unit,
    onSyncClick: () -> Unit,
    onSyncRoundsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    loggedInUsername: String?
) {
    val isLoggedIn = !loggedInUsername.isNullOrBlank()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LocalDiscgolf") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoggedIn) {
                Text(
                    text = "Inloggad som $loggedInUsername",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "Inte inloggad",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Button(
                onClick = onPlayersClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spelare")
            }

            Button(
                onClick = onCoursesClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Banor")
            }

            Button(
                onClick = onNewRoundClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ny runda")
            }

            Button(
                onClick = onResumeRoundClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Återuppta runda")
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onSyncRoundsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Synka rundor")
            }

            OutlinedButton(
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Synka data")
            }

            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inställningar")
            }

            if (isLoggedIn) {
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logga ut")
                }
            } else {
                OutlinedButton(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logga in")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerHoleDetailScreen(
    rows: List<PlayerHoleDetailRoundRow>,
    onBack: () -> Unit
) {
    val header = rows.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (header != null) {
                            "${header.courseName} - Hål ${header.holeNumber}"
                        } else {
                            "Håldetalj"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tillbaka"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Ingen hålstatistik ännu.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val avg = rows.map { it.throwsCount }.average()
                    val best = rows.minOf { it.throwsCount }
                    val par = header?.parSnapshot ?: 0

                    Text(
                        text = "Par $par",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Spelat: ${rows.size} gånger")
                    Text("Bästa kast: $best")
                    Text("Medel kast: ${"%.2f".format(avg)}")
                }

                items(rows) { row ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = formatDateTime(row.startedAt),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Layout: ${row.layoutName ?: "-"}")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Resultat:")
                            ScoreBadge(
                                throwsCount = row.throwsCount,
                                par = row.parSnapshot
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRoundScreen(
    players: List<PlayerEntity>,
    courses: List<CourseEntity>,
    layoutsByCourse: Map<Long, List<LayoutEntity>>,
    onBack: () -> Unit,
    observeCourseLayouts: (Long) -> Unit,
    onCreateRound: (Long, Long, List<Long>, Long, (Long) -> Unit) -> Unit,
) {
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedLayoutId by remember { mutableStateOf<Long?>(null) }
    var selectedPlayerIds by remember { mutableStateOf(setOf<Long>()) }

    val now = remember { java.time.LocalDateTime.now() }

    var selectedDate by remember {
        mutableStateOf(now.toLocalDate())
    }
    var selectedHour by remember {
        mutableStateOf(now.hour)
    }
    var selectedMinute by remember {
        mutableStateOf(now.minute)
    }

    LaunchedEffect(selectedCourseId) {
        selectedCourseId?.let { observeCourseLayouts(it) }
        selectedLayoutId = null
    }

    val availableLayouts = selectedCourseId?.let { layoutsByCourse[it] } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ny runda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tillbaka"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val courseId = selectedCourseId
                    val layoutId = selectedLayoutId
                    if (courseId != null && layoutId != null && selectedPlayerIds.isNotEmpty()) {
                        val startedAt = java.time.LocalDateTime
                            .of(selectedDate, java.time.LocalTime.of(selectedHour, selectedMinute))
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                        onCreateRound(
                            courseId,
                            layoutId,
                            selectedPlayerIds.toList(),
                            startedAt
                        ) { playSessionId ->
                            // lämnas tom här om navigation sker i AppNavHost
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                enabled = selectedCourseId != null &&
                        selectedLayoutId != null &&
                        selectedPlayerIds.isNotEmpty()
            ) {
                Text("Starta runda")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Bana", style = MaterialTheme.typography.titleMedium)
            }

            items(courses) { course ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCourseId = course.id }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (selectedCourseId == course.id) "●" else "○")
                    Text(course.name)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Layout", style = MaterialTheme.typography.titleMedium)
            }

            items(availableLayouts) { layout ->
                println(
                    "Layout in NewRoundScreen: id=${layout.id}, serverId=${layout.serverId}, name=${layout.name}"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLayoutId = layout.id }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (selectedLayoutId == layout.id) "●" else "○")
                    Text(layout.name)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Datum", style = MaterialTheme.typography.titleMedium)
                DateRow(
                    selectedDate = selectedDate,
                    onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
                    onNextDay = { selectedDate = selectedDate.plusDays(1) }
                )
            }

            item {
                Text("Starttid", style = MaterialTheme.typography.titleMedium)
                TimeRow(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourDecrease = { selectedHour = (selectedHour + 23) % 24 },
                    onHourIncrease = { selectedHour = (selectedHour + 1) % 24 },
                    onMinuteDecrease = { selectedMinute = (selectedMinute + 59) % 60 },
                    onMinuteIncrease = { selectedMinute = (selectedMinute + 1) % 60 }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Spelare", style = MaterialTheme.typography.titleMedium)
            }

            items(players) { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlayerIds =
                                if (player.id in selectedPlayerIds) {
                                    selectedPlayerIds - player.id
                                } else {
                                    selectedPlayerIds + player.id
                                }
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (player.id in selectedPlayerIds) "☑" else "☐")
                    Text(player.name)
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSummaryScreen(
    title: String,
    courseName: String,
    layoutName: String?,
    startedAt: Long?,
    rows: List<RoundSummaryHoleRow>,
    onBack: (() -> Unit)?,
    onBackToStart: (() -> Unit)?,
    onBackToRound: (() -> Unit)?
) {
    val grouped = rows
        .groupBy { it.playerId }
        .values
        .sortedBy { playerRows -> playerRows.firstOrNull()?.startOrder ?: Int.MAX_VALUE }

    val allHoles = rows
        .distinctBy { it.sequenceNumber }
        .sortedBy { it.sequenceNumber }

    val holeChunks = allHoles.chunked(9)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tillbaka"
                            )
                        }
                    }
                } else {
                    {}
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onBackToRound != null) {
                    Button(
                        onClick = onBackToRound,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tillbaka till rundan")
                    }
                }

                if (onBackToStart != null) {
                    Button(
                        onClick = onBackToStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Till startsidan")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (!layoutName.isNullOrBlank()) {
                        Text(
                            text = layoutName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (startedAt != null) {
                        Text(
                            text = "Start: ${formatDateTime(startedAt)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                RoundPlayerSummarySection(grouped = grouped)
            }

            item {
                Text(
                    text = "Scorekort",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            items(holeChunks) { holeChunk ->
                RoundScorecardBlock(
                    holeChunk = holeChunk,
                    groupedPlayerRows = grouped
                )
            }
        }
    }
}

@Composable
fun RoundPlayerSummarySection(
    grouped: List<List<RoundSummaryHoleRow>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        grouped.forEach { playerRows ->
            val sorted = playerRows.sortedBy { it.sequenceNumber }
            val playerName = sorted.firstOrNull()?.playerName ?: "Spelare"
            val playedRows = sorted.filter { it.throwsCount != null }

            val totalThrows = playedRows.sumOf { it.throwsCount ?: 0 }
            val totalPar = playedRows.sumOf { it.parSnapshot }
            val relative = totalThrows - totalPar

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${formatRelativeScore(relative)} ($totalThrows)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RoundScorecardBlock(
    holeChunk: List<RoundSummaryHoleRow>,
    groupedPlayerRows: List<List<RoundSummaryHoleRow>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ScorecardHeaderRow(
            label = "Hål",
            values = holeChunk.map { it.holeNumberSnapshot.toString() }
        )

        ScorecardHeaderRow(
            label = "Längd",
            values = holeChunk.map { it.lengthSnapshotMeters.toString() }
        )

        ScorecardHeaderRow(
            label = "Par",
            values = holeChunk.map { it.parSnapshot.toString() }
        )

        Spacer(modifier = Modifier.height(4.dp))

        groupedPlayerRows.forEach { playerRows ->
            val sortedPlayerRows = playerRows.sortedBy { it.sequenceNumber }
            val rowsForChunk = holeChunk.map { hole ->
                sortedPlayerRows.firstOrNull { it.sequenceNumber == hole.sequenceNumber }
            }

            ScorecardPlayerRow(
                playerName = sortedPlayerRows.firstOrNull()?.playerName ?: "Spelare",
                rows = rowsForChunk
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ScorecardHeaderRow(
    label: String,
    values: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(2.2f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        values.forEach { value ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScorecardPlayerRow(
    playerName: String,
    rows: List<RoundSummaryHoleRow?>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playerName,
            modifier = Modifier.weight(2.2f),
            style = MaterialTheme.typography.bodyMedium
        )

        rows.forEach { row ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CompactScoreBadge(
                    throwsCount = row?.throwsCount,
                    par = row?.parSnapshot
                )
            }
        }
    }
}

@Composable
fun CompactScoreBadge(
    throwsCount: Int?,
    par: Int?
) {
    val text = throwsCount?.toString() ?: "-"
    val diff = if (throwsCount != null && par != null) throwsCount - par else null

    val backgroundColor = when {
        diff == null -> Color.Transparent
        diff <= -1 -> Color(0xFF81C784)
        diff == 1 -> Color(0xFFFFCCBC)
        diff >= 2 -> Color(0xFFFF8A65)
        else -> Color.Transparent
    }

    val shape = when {
        diff != null && diff <= -1 -> CircleShape
        diff != null && diff >= 1 -> RoundedCornerShape(2.dp)
        else -> RoundedCornerShape(0.dp)
    }

    val useBackground = diff != null && diff != 0

    Box(
        modifier = Modifier
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .then(
                if (useBackground) {
                    Modifier.background(backgroundColor, shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DateRow(
    selectedDate: java.time.LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onPreviousDay) {
            Text("-")
        }

        Text(
            text = selectedDate.toString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        Button(onClick = onNextDay) {
            Text("+")
        }
    }
}

@Composable
fun TimeRow(
    hour: Int,
    minute: Int,
    onHourDecrease: () -> Unit,
    onHourIncrease: () -> Unit,
    onMinuteDecrease: () -> Unit,
    onMinuteIncrease: () -> Unit
) {
    val timeText = String.format("%02d:%02d", hour, minute)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.headlineSmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onHourDecrease) { Text("Tim -") }
            Button(onClick = onHourIncrease) { Text("Tim +") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMinuteDecrease) { Text("Min -") }
            Button(onClick = onMinuteIncrease) { Text("Min +") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeRoundScreen(
    sessions: List<InProgressSessionRow>,
    onBack: () -> Unit,
    onResume: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Återuppta runda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tillbaka"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Det finns inga pågående rundor.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResume(session.playSessionId) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = session.courseName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formatDateTime(session.startedAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}


fun formatDateTime(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

fun formatRelativeDouble(value: Double): String {
    return when {
        value > 0 -> "+${"%.2f".format(value)}"
        else -> "%.2f".format(value)
    }
}