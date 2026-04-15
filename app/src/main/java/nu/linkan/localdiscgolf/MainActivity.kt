package nu.linkan.localdiscgolf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
                var players by remember { mutableStateOf<List<PlayerEntity>>(emptyList()) }
                var courses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }

                LaunchedEffect(Unit) {
                    launch {
                        playerDao.observeActivePlayers().collectLatest { playerList ->
                            players = playerList
                        }
                    }
                    launch {
                        courseDao.observeActiveCourses().collectLatest { courseList ->
                            courses = courseList
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainTestScreen(
                        players = players,
                        courses = courses,
                        paddingValues = innerPadding,
                        onAddTestPlayer = {
                            lifecycleScope.launch {
                                val now = System.currentTimeMillis()
                                val testName = "Spelare ${now % 100000}"

                                playerDao.insert(
                                    PlayerEntity(
                                        name = testName,
                                        isMe = false,
                                        isActive = true,
                                        createdAt = now,
                                        updatedAt = now
                                    )
                                )
                            }
                        },
                        onAddTestCourse = {
                            lifecycleScope.launch {
                                val now = System.currentTimeMillis()
                                val testName = "Bana ${now % 100000}"

                                courseDao.insert(
                                    CourseEntity(
                                        name = testName,
                                        location = null,
                                        notes = null,
                                        isActive = true,
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
}

@Composable
fun MainTestScreen(
    players: List<PlayerEntity>,
    courses: List<CourseEntity>,
    paddingValues: PaddingValues,
    onAddTestPlayer: () -> Unit,
    onAddTestCourse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Test av Room",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = onAddTestPlayer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lägg till testspelare")
        }

        Text(
            text = "Antal spelare: ${players.size}",
            style = MaterialTheme.typography.bodyLarge
        )

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { player ->
                Text(
                    text = "${player.id}: ${player.name}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        HorizontalDivider()

        Button(
            onClick = onAddTestCourse,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lägg till testbana")
        }

        Text(
            text = "Antal banor: ${courses.size}",
            style = MaterialTheme.typography.bodyLarge
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(courses) { course ->
                Text(
                    text = "${course.id}: ${course.name}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}