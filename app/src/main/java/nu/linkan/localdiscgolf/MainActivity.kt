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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                    observeRoundHoleStats = { playSessionId, holeId ->
                        lifecycleScope.launch {
                            playSessionDao.observeHoleStatsForPlayersInSessionOnHole(playSessionId, holeId).collectLatest { rows ->
                                roundHoleStatsByKey["$playSessionId-$holeId"] = rows
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
                    observePlayerHoleDetail = { playerId, courseId, holeNumber ->
                        lifecycleScope.launch {
                            playSessionDao.observeHoleDetailForPlayer(playerId, courseId, holeNumber).collectLatest { rows ->
                                playerHoleDetailRowsByKey["$playerId-$courseId-$holeNumber"] = rows
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
    observePlayerHoleDetail: (Long, Long, Int) -> Unit,
    roundHoleStatsByKey: Map<String, List<RoundHolePlayerStatsRow>>,
    observeRoundHoleStats: (Long, Long) -> Unit,
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
){
    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onPlayersClick = { navController.navigate("players") },
                onCoursesClick = { navController.navigate("courses") },
                onNewRoundClick = { navController.navigate("new_round") },
                onResumeRoundClick = { navController.navigate("resume_round") }
            )
        }

        composable("players") {
            PlayersScreen(
                players = players,
                onBack = { navController.popBackStack() },
                onAddPlayer = onAddPlayer,
                onPlayerClick = { playerId ->
                    navController.navigate("player/$playerId")
                }
            )
        }

        composable("courses") {
            CoursesScreen(
                courses = courses,
                onBack = { navController.popBackStack() },
                onAddCourse = onAddCourse,
                onCourseClick = { courseId ->
                    navController.navigate("course/$courseId")
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
            }

            val rows = roundSummaryRowsBySession[playSessionId] ?: emptyList()

            RoundSummaryScreen(
                title = "Runddetalj",
                rows = rows,
                onBack = { navController.popBackStack() },
                onBackToStart = null
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

            LaunchedEffect(playSessionId, holeId) {
                if (holeId != null) {
                    observeRoundHoleStats(playSessionId, holeId)
                }
            }

            val statsRows = if (holeId != null) {
                roundHoleStatsByKey["$playSessionId-$holeId"] ?: emptyList()
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
            route = "round_summary/{playSessionId}",
            arguments = listOf(
                navArgument("playSessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playSessionId = backStackEntry.arguments?.getLong("playSessionId") ?: return@composable

            LaunchedEffect(playSessionId) {
                observeRoundSummaryRows(playSessionId)
            }

            val rows = roundSummaryRowsBySession[playSessionId] ?: emptyList()

            RoundSummaryScreen(
                title = "Rundsummering",
                rows = rows,
                onBack = null,
                onBackToStart = {
                    navController.navigate("start") {
                        popUpTo("start") { inclusive = true }
                    }
                }
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
            route = "player_hole_stats/{playerId}/{courseId}/{holeNumber}",
            arguments = listOf(
                navArgument("playerId") { type = NavType.LongType },
                navArgument("courseId") { type = NavType.LongType },
                navArgument("holeNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getLong("playerId") ?: return@composable
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: return@composable
            val holeNumber = backStackEntry.arguments?.getInt("holeNumber") ?: return@composable

            LaunchedEffect(playerId, courseId, holeNumber) {
                observePlayerHoleDetail(playerId, courseId, holeNumber)
            }

            val rows = playerHoleDetailRowsByKey["$playerId-$courseId-$holeNumber"] ?: emptyList()

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
                    onHoleClick = { courseId, holeNumber ->
                        navController.navigate("player_hole_stats/$playerId/$courseId/$holeNumber")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    player: PlayerEntity,
    layoutStats: List<PlayerLayoutStatsRow>,
    holeStats: List<PlayerHoleStatsRow>,
    onBack: () -> Unit,
    onHoleClick: (Long, Int) -> Unit
) {
    var selectedCourseId by remember(player.id) { mutableStateOf<Long?>(null) }

    val availableCourses = (
            layoutStats.map { it.courseId to it.courseName } +
                    holeStats.map { it.courseId to it.courseName }
            ).distinctBy { it.first }.sortedBy { it.second }

    val filteredLayoutStats = layoutStats.filter {
        selectedCourseId == null || it.courseId == selectedCourseId
    }
    val filteredHoleStats = holeStats.filter {
        selectedCourseId == null || it.courseId == selectedCourseId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${player.name} - Statistik") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Filtrera bana",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCourseId == null) {
                        Button(onClick = { selectedCourseId = null }) {
                            Text("Alla")
                        }
                    } else {
                        OutlinedButton(onClick = { selectedCourseId = null }) {
                            Text("Alla")
                        }
                    }

                    availableCourses.forEach { (courseId, courseName) ->
                        if (selectedCourseId == courseId) {
                            Button(onClick = { selectedCourseId = courseId }) {
                                Text(courseName)
                            }
                        } else {
                            OutlinedButton(onClick = { selectedCourseId = courseId }) {
                                Text(courseName)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Per layout",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            if (filteredLayoutStats.isEmpty()) {
                item {
                    Text("Ingen layoutstatistik ännu.")
                }
            } else {
                items(filteredLayoutStats) { row ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${row.courseName} - ${row.layoutName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Rundor: ${row.roundsPlayed}")
                        Text("Bästa kast: ${row.bestThrows}")
                        Text("Medel kast: ${"%.2f".format(row.avgThrows)}")
                        Text("Bästa mot par: ${formatRelativeScore(row.bestRelativeToPar)}")
                        Text("Medel mot par: ${formatRelativeDouble(row.avgRelativeToPar)}")
                    }
                    HorizontalDivider()
                }
            }

            item {
                Text(
                    text = "Per hål",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            if (filteredHoleStats.isEmpty()) {
                item {
                    Text("Ingen hålstatistik ännu.")
                }
            } else {
                items(filteredHoleStats) { row ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHoleClick(row.courseId, row.holeNumber) }
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${row.courseName} - Hål ${row.holeNumber}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Spelat: ${row.timesPlayed} gånger")
                        Text("Bästa kast: ${row.bestThrows}")
                        Text("Medel kast: ${"%.2f".format(row.avgThrows)}")
                        Text("Birdie eller bättre: ${row.birdiesOrBetter}")
                        Text("Par: ${row.pars}")
                        Text("Bogey: ${row.bogeys}")
                        Text("Dubbelbogey: ${row.doubleBogeys}")
                        Text("Trippelbogey eller sämre: ${row.tripleBogeysOrWorse}")
                    }
                    HorizontalDivider()
                }
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
    onResumeRoundClick: () -> Unit
) {
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
fun PlayersScreen(
    players: List<PlayerEntity>,
    onBack: () -> Unit,
    onAddPlayer: (String) -> Unit,
    onPlayerClick: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spelare") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tillbaka")
            }

            Text(
                text = "Antal spelare: ${players.size}",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ny spelare")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(players) { player ->
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(player.id) }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        NameInputDialog(
            title = "Ny spelare",
            label = "Spelarnamn",
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                onAddPlayer(name)
                showDialog = false
            }
        )
    }
}

@Composable
fun PlayerListContent(
    players: List<PlayerEntity>,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Antal spelare: ${players.size}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { player ->
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    courses: List<CourseEntity>,
    onBack: () -> Unit,
    onAddCourse: (String) -> Unit,
    onCourseClick: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Banor") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tillbaka")
            }

            Text(
                text = "Antal banor: ${courses.size}",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ny bana")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(courses) { course ->
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { onCourseClick(course.id) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        NameInputDialog(
            title = "Ny bana",
            label = "Bannamn",
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                onAddCourse(name)
                showDialog = false
            }
        )
    }
}

@Composable
fun CourseListContent(
    courses: List<CourseEntity>,
    paddingValues: PaddingValues,
    onCourseClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Antal banor: ${courses.size}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(courses) { course ->
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable { onCourseClick(course.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: CourseEntity,
    holes: List<HoleEntity>,
    layouts: List<LayoutEntity>,
    teesByHole: Map<Long, List<HoleTeeEntity>>,
    basketsByHole: Map<Long, List<HoleBasketEntity>>,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    onBack: () -> Unit,
    onAddHole: (Int, String?, Int, Int, String?) -> Unit,
    onUpdateHole: (HoleEntity) -> Unit,
    onAddLayout: (String, String?) -> Unit,
    onLayoutClick: (Long) -> Unit,
    observeHoleTees: (Long) -> Unit,
    observeHoleBaskets: (Long) -> Unit,
    observeHoleVariants: (Long) -> Unit,
    onAddHoleTee: (Long, String) -> Unit,
    onAddHoleBasket: (Long, String) -> Unit,
    onAddHoleVariant: (Long, Long, Long, Int, Int) -> Unit
) {
    var showAddHoleDialog by remember { mutableStateOf(false) }
    var showAddLayoutDialog by remember { mutableStateOf(false) }
    var holeToEdit by remember { mutableStateOf<HoleEntity?>(null) }
    var holeForVariants by remember { mutableStateOf<HoleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course.name) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Hål",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                Button(
                    onClick = { showAddHoleDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nytt hål")
                }
            }

            item {
                Text(
                    text = "Antal hål: ${holes.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(holes) { hole ->
                HoleRow(
                    hole = hole,
                    onClick = {
                        observeHoleTees(hole.id)
                        observeHoleBaskets(hole.id)
                        observeHoleVariants(hole.id)
                        holeForVariants = hole
                    },
                    onEditClick = { holeToEdit = hole }
                )
                HorizontalDivider()
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "Layouter",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                Button(
                    onClick = { showAddLayoutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ny layout")
                }
            }

            item {
                Text(
                    text = "Antal layouter: ${layouts.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(layouts) { layout ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLayoutClick(layout.id) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = layout.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!layout.description.isNullOrBlank()) {
                        Text(
                            text = layout.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider()
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showAddHoleDialog) {
        AddHoleDialog(
            onDismiss = { showAddHoleDialog = false },
            onConfirm = { holeNumber, name, lengthMeters, parValue, notes ->
                onAddHole(holeNumber, name, lengthMeters, parValue, notes)
                showAddHoleDialog = false
            }
        )
    }

    if (showAddLayoutDialog) {
        AddLayoutDialog(
            onDismiss = { showAddLayoutDialog = false },
            onConfirm = { name, description ->
                onAddLayout(name, description)
                showAddLayoutDialog = false
            }
        )
    }

    holeToEdit?.let { hole ->
        EditHoleDialog(
            hole = hole,
            onDismiss = { holeToEdit = null },
            onConfirm = { updatedHole ->
                onUpdateHole(updatedHole)
                holeToEdit = null
            }
        )
    }

    holeForVariants?.let { hole ->
        HoleVariantsDialog(
            hole = hole,
            tees = teesByHole[hole.id] ?: emptyList(),
            baskets = basketsByHole[hole.id] ?: emptyList(),
            variants = variantsByHole[hole.id] ?: emptyList(),
            onDismiss = { holeForVariants = null },
            onAddTee = { name -> onAddHoleTee(hole.id, name) },
            onAddBasket = { name -> onAddHoleBasket(hole.id, name) },
            onAddVariant = { teeId, basketId, lengthMeters, parValue ->
                onAddHoleVariant(hole.id, teeId, basketId, lengthMeters, parValue)
            }
        )
    }
}

@Composable
fun AddLayoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny layout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Layoutnamn") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Beskrivning (valfritt)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = nameText.trim()
                    if (trimmedName.isNotEmpty()) {
                        onConfirm(
                            trimmedName,
                            descriptionText.trim().ifBlank { null }
                        )
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutDetailScreen(
    layout: LayoutEntity,
    availableHoles: List<HoleEntity>,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    layoutHoles: List<LayoutHoleWithHole>,
    onBack: () -> Unit,
    onAddHoleToLayout: (Long, Long?) -> Unit,
    onRemoveHoleFromLayout: (Long, Int) -> Unit,
    onMoveHoleUp: (Int) -> Unit,
    onMoveHoleDown: (Int) -> Unit
){
    var showAddHoleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(layout.name) },
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
                onClick = { showAddHoleDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text("Lägg till hål")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Antal hål i layout: ${layoutHoles.size}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layoutHoles.size) { index ->
                    val item = layoutHoles[index]

                    LayoutHoleRow(
                        item = item,
                        isFirst = index == 0,
                        isLast = index == layoutHoles.lastIndex,
                        onMoveUp = { onMoveHoleUp(index) },
                        onMoveDown = { onMoveHoleDown(index) },
                        onDelete = {
                            onRemoveHoleFromLayout(item.layoutHoleId, item.sequenceNumber)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    if (showAddHoleDialog) {
        AddHoleToLayoutDialog(
            availableHoles = availableHoles,
            variantsByHole = variantsByHole,
            alreadyIncludedCombinations = layoutHoles.map {
                it.holeId to it.holeVariantId
            }.toSet(),
            onDismiss = { showAddHoleDialog = false },
            onConfirm = { holeId, holeVariantId ->
                onAddHoleToLayout(holeId, holeVariantId)
                showAddHoleDialog = false
            }
        )
    }
}

@Composable
fun LayoutHoleRow(
    item: LayoutHoleWithHole,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${item.sequenceNumber}. Hål ${item.holeNumber}" +
                        (item.holeName?.let { " - $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Längd: ${item.lengthMeters} m, Par: ${item.parValue}",
                style = MaterialTheme.typography.bodyMedium
            )

            val variantText = buildList {
                if (!item.teeName.isNullOrBlank()) add("Utkast: ${item.teeName}")
                if (!item.basketName.isNullOrBlank()) add("Korg: ${item.basketName}")
            }.joinToString(" | ")

            if (variantText.isNotBlank()) {
                Text(
                    text = variantText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Flytta upp"
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = !isLast
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Flytta ner"
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Ta bort från layout"
            )
        }
    }
}

@Composable
fun AddHoleToLayoutDialog(
    availableHoles: List<HoleEntity>,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    alreadyIncludedCombinations: Set<Pair<Long, Long?>>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?) -> Unit
) {
    var selectedHoleId by remember { mutableStateOf<Long?>(null) }
    var selectedVariantId by remember { mutableStateOf<Long?>(null) }

    val selectedHole = availableHoles.firstOrNull { it.id == selectedHoleId }
    val availableVariants = selectedHole?.let { variantsByHole[it.id] ?: emptyList() } ?: emptyList()

    LaunchedEffect(selectedHoleId) {
        selectedVariantId = when {
            availableVariants.size == 1 -> availableVariants.first().id
            availableVariants.isEmpty() -> null
            else -> null
        }
    }

    val currentAllowed = selectedHoleId != null &&
            (selectedHoleId!! to selectedVariantId) !in alreadyIncludedCombinations

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lägg till hål i layout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Hål", style = MaterialTheme.typography.titleMedium)

                availableHoles.forEach { hole ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHoleId = hole.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedHoleId == hole.id) "●" else "○")
                        Text("Hål ${hole.holeNumber}" + (hole.name?.let { " - $it" } ?: ""))
                    }
                }

                if (selectedHole != null && availableVariants.isNotEmpty()) {
                    Text("Variant", style = MaterialTheme.typography.titleMedium)

                    availableVariants.forEach { variant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVariantId = variant.id }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (selectedVariantId == variant.id) "●" else "○")
                            Text("${variant.teeName} → ${variant.basketName} | ${variant.lengthMeters} m | par ${variant.parValue}")
                        }
                    }
                }

                if (selectedHoleId != null && !currentAllowed) {
                    Text("Den kombinationen finns redan i layouten.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedHoleId?.let { holeId ->
                        onConfirm(holeId, selectedVariantId)
                    }
                },
                enabled = selectedHoleId != null && currentAllowed
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun HoleRow(
    hole: HoleEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Hål ${hole.holeNumber}" + (hole.name?.let { " - $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Längd: ${hole.lengthMeters} m, Par: ${hole.parValue}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!hole.notes.isNullOrBlank()) {
                Text(
                    text = hole.notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Tryck för utkast/korgplaceringar",
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Redigera hål"
            )
        }
    }
}

@Composable
fun HoleVariantsDialog(
    hole: HoleEntity,
    tees: List<HoleTeeEntity>,
    baskets: List<HoleBasketEntity>,
    variants: List<HoleVariantWithNames>,
    onDismiss: () -> Unit,
    onAddTee: (String) -> Unit,
    onAddBasket: (String) -> Unit,
    onAddVariant: (Long, Long, Int, Int) -> Unit
) {
    var showAddTeeDialog by remember { mutableStateOf(false) }
    var showAddBasketDialog by remember { mutableStateOf(false) }
    var showAddVariantDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hål ${hole.holeNumber}" + (hole.name?.let { " - $it" } ?: ""))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Utkast", style = MaterialTheme.typography.titleMedium)
                if (tees.isEmpty()) {
                    Text("Inga utkast ännu.")
                } else {
                    tees.forEach { Text("• ${it.name}") }
                }

                Button(
                    onClick = { showAddTeeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nytt utkast")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Korgplaceringar", style = MaterialTheme.typography.titleMedium)
                if (baskets.isEmpty()) {
                    Text("Inga korgplaceringar ännu.")
                } else {
                    baskets.forEach { Text("• ${it.name}") }
                }

                Button(
                    onClick = { showAddBasketDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ny korgplacering")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Varianter", style = MaterialTheme.typography.titleMedium)
                if (variants.isEmpty()) {
                    Text("Inga varianter ännu.")
                } else {
                    variants.forEach { variant ->
                        Text("• ${variant.teeName} → ${variant.basketName} | ${variant.lengthMeters} m | par ${variant.parValue}")
                    }
                }

                Button(
                    onClick = { showAddVariantDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = tees.isNotEmpty() && baskets.isNotEmpty()
                ) {
                    Text("Ny variant")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Stäng")
            }
        }
    )

    if (showAddTeeDialog) {
        NameInputDialog(
            title = "Nytt utkast",
            label = "Namn",
            onDismiss = { showAddTeeDialog = false },
            onConfirm = {
                onAddTee(it)
                showAddTeeDialog = false
            }
        )
    }

    if (showAddBasketDialog) {
        NameInputDialog(
            title = "Ny korgplacering",
            label = "Namn",
            onDismiss = { showAddBasketDialog = false },
            onConfirm = {
                onAddBasket(it)
                showAddBasketDialog = false
            }
        )
    }

    if (showAddVariantDialog) {
        AddHoleVariantDialog(
            tees = tees,
            baskets = baskets,
            existingCombinations = variants.map { it.teeId to it.basketId }.toSet(),
            onDismiss = { showAddVariantDialog = false },
            onConfirm = { teeId, basketId, lengthMeters, parValue ->
                onAddVariant(teeId, basketId, lengthMeters, parValue)
                showAddVariantDialog = false
            }
        )
    }
}

@Composable
fun AddHoleVariantDialog(
    tees: List<HoleTeeEntity>,
    baskets: List<HoleBasketEntity>,
    existingCombinations: Set<Pair<Long, Long>>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Int, Int) -> Unit
) {
    var selectedTeeId by remember { mutableStateOf<Long?>(null) }
    var selectedBasketId by remember { mutableStateOf<Long?>(null) }
    var lengthText by remember { mutableStateOf("") }
    var parText by remember { mutableStateOf("") }

    val duplicate = selectedTeeId != null &&
            selectedBasketId != null &&
            (selectedTeeId!! to selectedBasketId!!) in existingCombinations

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny variant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Utkast", style = MaterialTheme.typography.titleMedium)
                tees.forEach { tee ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTeeId = tee.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedTeeId == tee.id) "●" else "○")
                        Text(tee.name)
                    }
                }

                Text("Korgplacering", style = MaterialTheme.typography.titleMedium)
                baskets.forEach { basket ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBasketId = basket.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedBasketId == basket.id) "●" else "○")
                        Text(basket.name)
                    }
                }

                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Längd i meter") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = parText,
                    onValueChange = { parText = it },
                    label = { Text("Par") },
                    singleLine = true
                )

                if (duplicate) {
                    Text("Den kombinationen finns redan.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val teeId = selectedTeeId
                    val basketId = selectedBasketId
                    val lengthMeters = lengthText.toIntOrNull()
                    val parValue = parText.toIntOrNull()

                    if (teeId != null &&
                        basketId != null &&
                        lengthMeters != null &&
                        parValue != null &&
                        !duplicate
                    ) {
                        onConfirm(teeId, basketId, lengthMeters, parValue)
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun NameInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onConfirm(trimmed)
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun AddHoleDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String?, Int, Int, String?) -> Unit
) {
    var holeNumberText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var lengthText by remember { mutableStateOf("") }
    var parText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nytt hål") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = holeNumberText,
                    onValueChange = { holeNumberText = it },
                    label = { Text("Hålnummer") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Namn (valfritt)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Längd i meter") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = parText,
                    onValueChange = { parText = it },
                    label = { Text("Par") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Anteckning (valfritt)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val holeNumber = holeNumberText.trim().toIntOrNull()
                    val lengthMeters = lengthText.trim().toIntOrNull()
                    val parValue = parText.trim().toIntOrNull()

                    if (holeNumber != null && lengthMeters != null && parValue != null) {
                        onConfirm(
                            holeNumber,
                            nameText.trim().ifBlank { null },
                            lengthMeters,
                            parValue,
                            notesText.trim().ifBlank { null }
                        )
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun EditHoleDialog(
    hole: HoleEntity,
    onDismiss: () -> Unit,
    onConfirm: (HoleEntity) -> Unit
) {
    var holeNumberText by remember(hole.id) { mutableStateOf(hole.holeNumber.toString()) }
    var nameText by remember(hole.id) { mutableStateOf(hole.name ?: "") }
    var lengthText by remember(hole.id) { mutableStateOf(hole.lengthMeters.toString()) }
    var parText by remember(hole.id) { mutableStateOf(hole.parValue.toString()) }
    var notesText by remember(hole.id) { mutableStateOf(hole.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera hål") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = holeNumberText,
                    onValueChange = { holeNumberText = it },
                    label = { Text("Hålnummer") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Namn (valfritt)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Längd i meter") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = parText,
                    onValueChange = { parText = it },
                    label = { Text("Par") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Anteckning (valfritt)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val holeNumber = holeNumberText.trim().toIntOrNull()
                    val lengthMeters = lengthText.trim().toIntOrNull()
                    val parValue = parText.trim().toIntOrNull()

                    if (holeNumber != null && lengthMeters != null && parValue != null) {
                        onConfirm(
                            hole.copy(
                                holeNumber = holeNumber,
                                name = nameText.trim().ifBlank { null },
                                lengthMeters = lengthMeters,
                                parValue = parValue,
                                notes = notesText.trim().ifBlank { null }
                            )
                        )
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
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
    rows: List<RoundSummaryHoleRow>,
    onBack: (() -> Unit)?,
    onBackToStart: (() -> Unit)?
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
            if (onBackToStart != null) {
                Button(
                    onClick = onBackToStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text("Till startsidan")
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
            val totalThrows = sorted.sumOf { it.throwsCount ?: 0 }
            val totalPar = sorted.sumOf { it.parSnapshot }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundHoleScreen(
    rows: List<RoundHolePlayerRow>,
    statsRows: List<RoundHolePlayerStatsRow>,
    sequenceNumber: Int,
    totalHoleCount: Int,
    onBack: () -> Unit,
    onPreviousHole: () -> Unit,
    onNextHole: () -> Unit,
    onSaveHoleResults: (List<Pair<Long, Int>>) -> Unit,
    onFinishRound: () -> Unit
) {
    val header = rows.firstOrNull()
    var showFinishDialog by remember { mutableStateOf(false) }

    val inputValues = remember(rows) {
        mutableStateMapOf<Long, String>().apply {
            rows.forEach { row ->
                this[row.sessionPlayerHoleId] = row.throwsCount?.toString() ?: ""
            }
        }
    }

    fun saveCurrentHole() {
        val valuesToSave = inputValues.mapNotNull { (sessionPlayerHoleId, textValue) ->
            val throwsValue = textValue.trim().toIntOrNull()
            if (throwsValue != null) {
                sessionPlayerHoleId to throwsValue
            } else {
                null
            }
        }
        onSaveHoleResults(valuesToSave)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hål $sequenceNumber") },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            saveCurrentHole()
                            onPreviousHole()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = sequenceNumber > 1
                    ) {
                        Text("Föregående")
                    }

                    Button(
                        onClick = {
                            saveCurrentHole()
                            onNextHole()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = totalHoleCount > 0 && sequenceNumber < totalHoleCount
                    ) {
                        Text("Nästa")
                    }
                }

                Button(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Avsluta runda")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (header != null) {
                Text(
                    text = "Hål ${header.holeNumberSnapshot}" +
                            (header.holeNameSnapshot?.let { " - $it" } ?: ""),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Längd: ${header.lengthSnapshotMeters} m, Par: ${header.parSnapshot}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rows) { row ->
                    val stats = statsRows.firstOrNull { it.playerId == row.playerId && it.holeId == row.holeId }

                    RoundPlayerThrowsRow(
                        row = row,
                        stats = stats,
                        value = inputValues[row.sessionPlayerHoleId] ?: "",
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                inputValues[row.sessionPlayerHoleId] = newValue
                            }
                        }
                    )
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Avsluta runda") },
            text = { Text("Vill du avsluta rundan? Aktuella resultat på hålet sparas först.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveCurrentHole()
                        showFinishDialog = false
                        onFinishRound()
                    }
                ) {
                    Text("Ja")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Nej")
                }
            }
        )
    }
}



@Composable
fun ScoreBadge(
    throwsCount: Int?,
    par: Int
) {
    val text = throwsCount?.toString() ?: "-"
    val diff = throwsCount?.minus(par)

    val backgroundColor = when {
        diff == null -> Color(0xFFE0E0E0)
        diff <= -1 -> Color(0xFF81C784)
        diff == 1 -> Color(0xFFFFCDD2)
        diff >= 2 -> Color(0xFFEF9A9A)
        else -> Color(0xFFE0E0E0)
    }

    val shape = when {
        diff != null && diff <= -1 -> CircleShape
        diff != null && diff >= 1 -> RoundedCornerShape(2.dp)
        else -> RoundedCornerShape(8.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatRelativeScore(relative: Int): String {
    return when {
        relative > 0 -> "+$relative"
        else -> relative.toString()
    }
}

@Composable
fun RoundPlayerThrowsRow(
    row: RoundHolePlayerRow,
    stats: RoundHolePlayerStatsRow?,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = row.playerName ?: "Spelare ${row.playerId}",
            style = MaterialTheme.typography.titleMedium
        )

        if (stats != null) {
            Text(
                text = "Tidigare: ${stats.timesPlayed} rundor, PB ${stats.bestThrows}, snitt ${"%.2f".format(stats.avgThrows)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Birdie+: ${stats.birdiesOrBetter}  Par: ${stats.pars}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Bogey: ${stats.bogeys}  Dubbel: ${stats.doubleBogeys}  Trippel+: ${stats.tripleBogeysOrWorse}",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                text = "Ingen tidigare statistik på hålet.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Kast") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    player: PlayerEntity,
    sessions: List<PlayerSessionRow>,
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onStatsClick: () -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    var sessionToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(player.name) },
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
                onClick = onStatsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text("Statistik")
            }
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
                    text = "Inga rundor registrerade ännu.",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSessionClick(session.playSessionId) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = session.courseName +
                                        (session.layoutName?.let { " - $it" } ?: ""),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Start: ${formatDateTime(session.startedAt)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (session.endedAt != null) {
                                Text(
                                    text = "Slut: ${formatDateTime(session.endedAt)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "Status: ${formatSessionStatus(session.status)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        IconButton(onClick = { sessionToDelete = session.playSessionId }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Ta bort runda"
                            )
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }

    sessionToDelete?.let { playSessionId ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Ta bort runda") },
            text = { Text("Vill du ta bort den här rundan? Det går inte att ångra.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(playSessionId)
                        sessionToDelete = null
                    }
                ) {
                    Text("Ta bort")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Avbryt")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    text: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun formatSessionStatus(status: String): String {
    return when (status) {
        "in_progress" -> "Pågår"
        "completed" -> "Avslutad"
        else -> status
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