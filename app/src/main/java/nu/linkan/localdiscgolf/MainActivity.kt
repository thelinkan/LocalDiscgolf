package nu.linkan.localdiscgolf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import nu.linkan.localdiscgolf.data.local.DatabaseProvider
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.ui.theme.LocalDiscgolfTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = DatabaseProvider.getDatabase(this)
        val playerDao = db.playerDao()
        val courseDao = db.courseDao()

        setContent {
            LocalDiscgolfTheme {
                val navController = rememberNavController()

                var players by remember { mutableStateOf<List<PlayerEntity>>(emptyList()) }
                var courses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }

                LaunchedEffect(Unit) {
                    launch {
                        playerDao.observeActivePlayers().collectLatest { players = it }
                    }
                    launch {
                        courseDao.observeActiveCourses().collectLatest { courses = it }
                    }
                }

                AppNavHost(
                    navController = navController,
                    players = players,
                    courses = courses,
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
    onAddPlayer: (String) -> Unit,
    onAddCourse: (String) -> Unit
) {
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
                onAddCourse = onAddCourse
            )
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
                title = { Text("Spelare åäö") },
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
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Ny spelare")
            }
        }
    ) { innerPadding ->
        PlayerListContent(
            players = players,
            paddingValues = innerPadding
        )
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
    onAddCourse: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Banor") },
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
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Ny bana")
            }
        }
    ) { innerPadding ->
        CourseListContent(
            courses = courses,
            paddingValues = innerPadding
        )
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
    paddingValues: PaddingValues
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
                    modifier = Modifier.clickable { }
                )
            }
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
        title = {
            Text(title)
        },
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