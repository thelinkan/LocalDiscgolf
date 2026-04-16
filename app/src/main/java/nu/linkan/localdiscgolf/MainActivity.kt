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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
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
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = DatabaseProvider.getDatabase(this)
        val playerDao = db.playerDao()
        val courseDao = db.courseDao()
        val holeDao = db.holeDao()
        val layoutDao = db.layoutDao()

        setContent {
            LocalDiscgolfTheme {
                val navController = rememberNavController()

                var players by remember { mutableStateOf<List<PlayerEntity>>(emptyList()) }
                var courses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }
                val holesByCourse = remember { mutableStateMapOf<Long, List<HoleEntity>>() }
                val layoutsByCourse = remember { mutableStateMapOf<Long, List<LayoutEntity>>() }
                val layoutHolesByLayout = remember { mutableStateMapOf<Long, List<LayoutHoleWithHole>>() }

                LaunchedEffect(Unit) {
                    launch {
                        playerDao.observeActivePlayers().collectLatest { players = it }
                    }
                    launch {
                        courseDao.observeActiveCourses().collectLatest { courseList ->
                            courses = courseList
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
                    observeCourseLayouts = { courseId ->
                        lifecycleScope.launch {
                            layoutDao.observeActiveLayoutsForCourse(courseId).collectLatest { layouts ->
                                layoutsByCourse[courseId] = layouts
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
                    onAddHoleToLayout = { layoutId, holeId ->
                        lifecycleScope.launch {
                            val nextSequence = layoutDao.getMaxSequenceNumber(layoutId) + 1
                            layoutDao.insertLayoutHole(
                                LayoutHoleEntity(
                                    layoutId = layoutId,
                                    sequenceNumber = nextSequence,
                                    holeId = holeId
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
    onAddPlayer: (String) -> Unit,
    onAddCourse: (String) -> Unit,
    observeCourseHoles: (Long) -> Unit,
    onAddHole: (Long, Int, String?, Int, Int, String?) -> Unit,
    onUpdateHole: (Long, Long, Int, String?, Int, Int, String?, Boolean, Long) -> Unit,
    observeCourseLayouts: (Long) -> Unit,
    onAddLayout: (Long, String, String?) -> Unit,
    observeLayoutHoles: (Long) -> Unit,
    onAddHoleToLayout: (Long, Long) -> Unit,
    onRemoveHoleFromLayout: (Long, Int, Long) -> Unit,
    onMoveHoleUpInLayout: (List<LayoutHoleWithHole>, Int) -> Unit,
    onMoveHoleDownInLayout: (List<LayoutHoleWithHole>, Int) -> Unit
){
    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") {
            StartScreen(
                onPlayersClick = { navController.navigate("players") },
                onCoursesClick = { navController.navigate("courses") },
                onNewRoundClick = { navController.navigate("new_round_placeholder") }
            )
        }

        composable("players") {
            PlayersScreen(
                players = players,
                onBack = { navController.popBackStack() },
                onAddPlayer = onAddPlayer
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
                    layoutHoles = layoutHoles,
                    onBack = { navController.popBackStack() },
                    onAddHoleToLayout = { holeId ->
                        onAddHoleToLayout(layoutId, holeId)
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

        composable("new_round_placeholder") {
            PlaceholderScreen(
                title = "Ny runda",
                text = "Den här funktionen kommer senare.",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onPlayersClick: () -> Unit,
    onCoursesClick: () -> Unit,
    onNewRoundClick: () -> Unit
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    players: List<PlayerEntity>,
    onBack: () -> Unit,
    onAddPlayer: (String) -> Unit
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
                        style = MaterialTheme.typography.bodyLarge
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
    onBack: () -> Unit,
    onAddHole: (Int, String?, Int, Int, String?) -> Unit,
    onUpdateHole: (HoleEntity) -> Unit,
    onAddLayout: (String, String?) -> Unit,
    onLayoutClick: (Long) -> Unit
) {
    var showAddHoleDialog by remember { mutableStateOf(false) }
    var showAddLayoutDialog by remember { mutableStateOf(false) }
    var holeToEdit by remember { mutableStateOf<HoleEntity?>(null) }

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
    layoutHoles: List<LayoutHoleWithHole>,
    onBack: () -> Unit,
    onAddHoleToLayout: (Long) -> Unit,
    onRemoveHoleFromLayout: (Long, Int) -> Unit,
    onMoveHoleUp: (Int) -> Unit,
    onMoveHoleDown: (Int) -> Unit
) {
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
            alreadyIncludedHoleIds = layoutHoles.map { it.holeId }.toSet(),
            onDismiss = { showAddHoleDialog = false },
            onConfirm = { holeId ->
                onAddHoleToLayout(holeId)
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
    alreadyIncludedHoleIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val selectableHoles = availableHoles.filter { it.id !in alreadyIncludedHoleIds }
    var selectedHoleId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lägg till hål i layout") },
        text = {
            if (selectableHoles.isEmpty()) {
                Text("Alla hål på banan finns redan i layouten.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableHoles.forEach { hole ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedHoleId = hole.id }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (selectedHoleId == hole.id) "●" else "○",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Hål ${hole.holeNumber}" +
                                        (hole.name?.let { " - $it" } ?: "") +
                                        " (${hole.lengthMeters} m, par ${hole.parValue})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedHoleId?.let(onConfirm)
                },
                enabled = selectedHoleId != null && selectableHoles.isNotEmpty()
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
    onEditClick: () -> Unit
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