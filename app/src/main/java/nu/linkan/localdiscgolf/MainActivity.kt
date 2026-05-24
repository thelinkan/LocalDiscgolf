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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
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
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleVariantEntity
import nu.linkan.localdiscgolf.data.local.model.HoleVariantWithNames
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHeaderRow
import nu.linkan.localdiscgolf.data.local.model.PlayerListRow
import nu.linkan.localdiscgolf.data.local.model.CourseListRow

import nu.linkan.localdiscgolf.ui.dialogs.AddHoleDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleToLayoutDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleVariantDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddLayoutDialog
import nu.linkan.localdiscgolf.ui.dialogs.EditHoleDialog
import nu.linkan.localdiscgolf.ui.dialogs.HoleVariantsDialog
import nu.linkan.localdiscgolf.ui.dialogs.NameInputDialog
import nu.linkan.localdiscgolf.ui.dialogs.NameInputDialog
import nu.linkan.localdiscgolf.ui.screens.PlayerDetailScreen
import nu.linkan.localdiscgolf.ui.screens.PlayerStatsScreen
import nu.linkan.localdiscgolf.ui.screens.PlayersScreen
import nu.linkan.localdiscgolf.ui.screens.CoursesScreen
import nu.linkan.localdiscgolf.ui.screens.CourseDetailScreen
import nu.linkan.localdiscgolf.ui.screens.LayoutDetailScreen
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

import nu.linkan.localdiscgolf.network.CourseApiResponse
import nu.linkan.localdiscgolf.network.LayoutApiResponse
import nu.linkan.localdiscgolf.network.LayoutHoleApiResponse
import nu.linkan.localdiscgolf.network.UserPlayersResponse
import nu.linkan.localdiscgolf.network.PlayerRoundApiResponse
import nu.linkan.localdiscgolf.network.RoundDetailApiResponse
import nu.linkan.localdiscgolf.network.CreateRoundApiRequest
import nu.linkan.localdiscgolf.network.CreateRoundPlayerApiRequest

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
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

        setContent {
            LocalDiscgolfTheme {
                val prefs = getSharedPreferences("api_settings", Context.MODE_PRIVATE)

                var apiHost by remember { mutableStateOf(prefs.getString("host", "") ?: "") }
                var apiPort by remember { mutableStateOf(prefs.getString("port", "8000") ?: "8000") }
                var authToken by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
                var loggedInUsername by remember { mutableStateOf(prefs.getString("username", "") ?: "") }

                var apiCourses by remember { mutableStateOf<List<CourseApiResponse>>(emptyList()) }
                var apiLayouts by remember { mutableStateOf<List<LayoutApiResponse>>(emptyList()) }
                var apiLayoutHoles by remember { mutableStateOf<List<LayoutHoleApiResponse>>(emptyList()) }
                var apiUserPlayers by remember { mutableStateOf<UserPlayersResponse?>(null) }
                var apiPlayerRounds by remember { mutableStateOf<List<PlayerRoundApiResponse>>(emptyList()) }
                var selectedApiPlayerName by remember { mutableStateOf("") }
                var apiRoundDetail by remember { mutableStateOf<RoundDetailApiResponse?>(null) }
                var apiNewRoundLayouts by remember { mutableStateOf<List<LayoutApiResponse>>(emptyList()) }

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
                    loggedInUsername = loggedInUsername,
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
                    onCreateApiRound = { courseId, layoutId, playerIds ->
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
                                        navController.navigate("api_round_detail")
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
                    onAddPlayer = { name ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            playerDao.insert(
                                PlayerEntity(
                                    name = name,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    onAddCourse = { name ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            courseDao.insert(
                                CourseEntity(
                                    name = name,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    observeCourseHoles = { courseId ->
                        lifecycleScope.launch {
                            holeDao.observeActiveHolesForCourse(courseId).collectLatest { holes ->
                                holesByCourse[courseId] = holes
                            }
                        }
                    },
                    onAddHole = { courseId, holeNumber, name, lengthMeters, parValue, notes ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            holeDao.insert(
                                HoleEntity(
                                    courseId = courseId,
                                    holeNumber = holeNumber,
                                    name = name,
                                    lengthMeters = lengthMeters,
                                    parValue = parValue,
                                    notes = notes,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    onUpdateHole = { holeId, courseId, holeNumber, name, lengthMeters, parValue, notes, isActive, createdAt ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            holeDao.update(
                                HoleEntity(
                                    id = holeId,
                                    courseId = courseId,
                                    holeNumber = holeNumber,
                                    name = name,
                                    lengthMeters = lengthMeters,
                                    parValue = parValue,
                                    notes = notes,
                                    isActive = isActive,
                                    createdAt = createdAt,
                                    updatedAt = now
                                )
                            )
                        }
                    },
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
                    onAddLayout = { courseId, name, description ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            layoutDao.insert(
                                LayoutEntity(
                                    courseId = courseId,
                                    name = name,
                                    description = description,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
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
                    onAddHoleToLayout = { layoutId, holeId, holeVariantId ->
                        lifecycleScope.launch {
                            val nextSequence = layoutDao.getMaxSequenceNumber(layoutId) + 1
                            layoutDao.insertLayoutHole(
                                LayoutHoleEntity(
                                    layoutId = layoutId,
                                    sequenceNumber = nextSequence,
                                    holeId = holeId,
                                    holeVariantId = holeVariantId
                                )
                            )
                        }
                    },
                    onRemoveHoleFromLayout = { layoutHoleId, deletedSequenceNumber, layoutId ->
                        lifecycleScope.launch {
                            layoutDao.deleteLayoutHoleById(layoutHoleId)
                            layoutDao.closeGapAfterDelete(layoutId, deletedSequenceNumber)
                        }
                    },
                    onMoveHoleUpInLayout = { layoutHoles, index ->
                        lifecycleScope.launch {
                            if (index > 0) {
                                val current = layoutHoles[index]
                                val previous = layoutHoles[index - 1]

                                layoutDao.swapLayoutHoleSequences(
                                    firstLayoutHoleId = current.layoutHoleId,
                                    firstSequence = current.sequenceNumber,
                                    secondLayoutHoleId = previous.layoutHoleId,
                                    secondSequence = previous.sequenceNumber
                                )
                            }
                        }
                    },
                    onMoveHoleDownInLayout = { layoutHoles, index ->
                        lifecycleScope.launch {
                            if (index < layoutHoles.lastIndex) {
                                val current = layoutHoles[index]
                                val next = layoutHoles[index + 1]

                                layoutDao.swapLayoutHoleSequences(
                                    firstLayoutHoleId = current.layoutHoleId,
                                    firstSequence = current.sequenceNumber,
                                    secondLayoutHoleId = next.layoutHoleId,
                                    secondSequence = next.sequenceNumber
                                )
                            }
                        }
                    },
                    apiPlayerRounds = apiPlayerRounds,
                    selectedApiPlayerName = selectedApiPlayerName,
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
                    onCreateRound = { courseId, startedAt, selectedPlayerIds, selectedLayoutId, onCreated ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()

                            val layoutHoles = layoutDao.getLayoutHolesOnce(selectedLayoutId)
                            val playSessionId = playSessionDao.createPlaySessionWithPlayersAndHoles(
                                playSession = PlaySessionEntity(
                                    courseId = courseId,
                                    startedAt = startedAt,
                                    createdAt = now,
                                    updatedAt = now
                                ),
                                sessionPlayers = selectedPlayerIds.mapIndexed { index, playerId ->
                                    val player = players.firstOrNull { it.id == playerId }
                                    SessionPlayerEntity(
                                        playSessionId = 0,
                                        playerId = playerId,
                                        layoutId = selectedLayoutId,
                                        displayName = player?.name,
                                        startOrder = index + 1,
                                        createdAt = now,
                                        updatedAt = now
                                    )
                                },
                                layoutHoles = layoutHoles,
                                courseId = courseId,
                                createdAt = now
                            )

                            onCreated(playSessionId)
                        }
                    },
                    onDeleteRound = { playSessionId ->
                        lifecycleScope.launch {
                            playSessionDao.deletePlaySession(playSessionId)
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
                    onAddHoleTee = { holeId, name ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            val nextSortOrder = holeDao.getMaxTeeSortOrder(holeId) + 1
                            holeDao.insertHoleTee(
                                HoleTeeEntity(
                                    holeId = holeId,
                                    name = name,
                                    sortOrder = nextSortOrder,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    onAddHoleBasket = { holeId, name ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            val nextSortOrder = holeDao.getMaxBasketSortOrder(holeId) + 1
                            holeDao.insertHoleBasket(
                                HoleBasketEntity(
                                    holeId = holeId,
                                    name = name,
                                    sortOrder = nextSortOrder,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    observePlayerSessions = { playerId ->
                        lifecycleScope.launch {
                            playSessionDao.observeSessionsForPlayer(playerId).collectLatest { rows ->
                                playerSessionsByPlayer[playerId] = rows
                            }
                        }
                    },
                    onUpdateThrowsForHole = { sessionPlayerHoleId, throwsCount ->
                        lifecycleScope.launch {
                            playSessionDao.updateThrowsForSessionPlayerHole(
                                sessionPlayerHoleId = sessionPlayerHoleId,
                                throwsCount = throwsCount,
                                updatedAt = System.currentTimeMillis()
                            )
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
                    onAddHoleVariant = { holeId, teeId, basketId, lengthMeters, parValue ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            holeDao.insertHoleVariant(
                                HoleVariantEntity(
                                    holeId = holeId,
                                    teeId = teeId,
                                    basketId = basketId,
                                    lengthMeters = lengthMeters,
                                    parValue = parValue,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    onFinishRound = { playSessionId ->
                        lifecycleScope.launch {
                            val now = System.currentTimeMillis()
                            playSessionDao.finishPlaySession(
                                playSessionId = playSessionId,
                                endedAt = now,
                                status = "completed",
                                updatedAt = now
                            )
                        }
                    },
                    onResumeRound = { playSessionId, onResolved ->
                        lifecycleScope.launch {
                            val sequenceNumber = playSessionDao.getResumeSequenceNumber(playSessionId)
                            onResolved(sequenceNumber)
                        }
                    }
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
    observeRoundHoleRows: (Long, Int) -> Unit,
    onUpdateThrowsForHole: (Long, Int) -> Unit,
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
    onCreateApiRound: (Long, Long, List<Long>) -> Unit,
){
    val coroutineScope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onPlayersClick = { navController.navigate("players") },
                onCoursesClick = { navController.navigate("courses") },
                onServerCoursesClick = { navController.navigate("api_courses") },
                onServerPlayersClick = { navController.navigate("api_players") },
                onNewRoundClick = { navController.navigate("new_round") },
                onNewServerRoundClick = { navController.navigate("api_new_round") },
                onResumeRoundClick = { navController.navigate("resume_round") },
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

        composable("api_new_round") {
            LaunchedEffect(Unit) {
                onLoadApiCourses()
                onLoadApiUserPlayers()
            }

            ApiNewRoundScreen(
                courses = apiCourses,
                layouts = apiNewRoundLayouts,
                userPlayers = apiUserPlayers,
                onBack = { navController.popBackStack() },
                onCourseSelected = { courseId ->
                    onLoadApiNewRoundLayouts(courseId)
                },
                onCreateRound = { courseId, layoutId, playerIds ->
                    onCreateApiRound(courseId, layoutId, playerIds)
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
                onPlayerClick = { playerId ->
                    val playerName =
                        apiUserPlayers?.own_player?.takeIf { it.id == playerId }?.name
                            ?: apiUserPlayers?.guest_players?.firstOrNull { it.id == playerId }?.name
                            ?: apiUserPlayers?.scoreable_players?.firstOrNull { it.id == playerId }?.name
                            ?: "Spelare"

                    onLoadApiPlayerRounds(playerId, playerName)
                    navController.navigate("api_player_rounds")
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
                onCreateRound = { courseId, startedAt, selectedPlayerIds, selectedLayoutId, onCreated ->
                    onCreateRound(
                        courseId,
                        startedAt,
                        selectedPlayerIds,
                        selectedLayoutId
                    ) { playSessionId ->
                        navController.navigate("round/$playSessionId/1")
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

            RoundHoleScreen(
                rows = rows,
                statsRows = statsRows,
                sequenceNumber = sequenceNumber,
                totalHoleCount = holeCount,
                onBack = { navController.popBackStack() },
                onPreviousHole = {
                    if (sequenceNumber > 1) {
                        navController.navigate("round/$playSessionId/${sequenceNumber - 1}")
                    }
                },
                onNextHole = {
                    if (holeCount > 0 && sequenceNumber < holeCount) {
                        navController.navigate("round/$playSessionId/${sequenceNumber + 1}")
                    }
                },
                onShowSummary = {
                    navController.navigate("round_summary_live/$playSessionId/$sequenceNumber")
                },
                onSaveHoleResults = { values ->
                    values.forEach { (sessionPlayerHoleId, throwsCount) ->
                        onUpdateThrowsForHole(sessionPlayerHoleId, throwsCount)
                    }
                },
                onFinishRound = {
                    onFinishRound(playSessionId)
                    navController.navigate("round_summary/$playSessionId") {
                        popUpTo("start")
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
                onBack = { navController.popBackStack() }
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
    onServerCoursesClick: () -> Unit,
    onNewRoundClick: () -> Unit,
    onResumeRoundClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onServerPlayersClick: () -> Unit,
    onNewServerRoundClick: () -> Unit,
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
                .padding(16.dp)
        ) {
            Column(
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
                    onClick = onServerPlayersClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Serverspelare")
                }

                Button(
                    onClick = onCoursesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Banor")
                }

                Button(
                    onClick = onServerCoursesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Serverbanor")
                }

                Button(
                    onClick = onNewRoundClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ny runda")
                }

                Button(
                    onClick = onNewServerRoundClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ny serverrunda")
                }

                Button(
                    onClick = onResumeRoundClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Återuppta runda")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            startedAt,
                            selectedPlayerIds.toList(),
                            layoutId
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