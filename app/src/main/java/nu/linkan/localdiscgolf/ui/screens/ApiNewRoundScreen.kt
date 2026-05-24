package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.network.CourseApiResponse
import nu.linkan.localdiscgolf.network.LayoutApiResponse
import nu.linkan.localdiscgolf.network.PlayerApiResponse
import nu.linkan.localdiscgolf.network.ScoreablePlayerApiResponse
import nu.linkan.localdiscgolf.network.UserPlayersResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SelectableServerPlayer(
    val id: Long,
    val name: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiNewRoundScreen(
    courses: List<CourseApiResponse>,
    layouts: List<LayoutApiResponse>,
    userPlayers: UserPlayersResponse?,
    onBack: () -> Unit,
    onCourseSelected: (Long) -> Unit,
    onCreateRound: (Long, Long, List<Long>) -> Unit
) {
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var selectedLayoutId by remember { mutableStateOf<Long?>(null) }
    var selectedPlayerIds by remember { mutableStateOf(setOf<Long>()) }

    val selectablePlayers = remember(userPlayers) {
        buildList {
            userPlayers?.own_player?.let {
                add(SelectableServerPlayer(it.id, it.name, "Egen spelare"))
            }

            userPlayers?.guest_players?.forEach {
                add(SelectableServerPlayer(it.id, it.name, "Gästspelare"))
            }

            userPlayers?.scoreable_players?.forEach {
                if (none { existing -> existing.id == it.id }) {
                    val subtitle = when (it.permission_level) {
                        "auto_approve" -> "Autogodkänd"
                        "propose" -> "Kräver godkännande"
                        else -> it.permission_level
                    }
                    add(SelectableServerPlayer(it.id, it.name, subtitle))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ny serverrunda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
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
                        onCreateRound(courseId, layoutId, selectedPlayerIds.toList())
                    }
                },
                enabled = selectedCourseId != null &&
                        selectedLayoutId != null &&
                        selectedPlayerIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text("Starta serverrunda")
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
                RowChoice(
                    selected = selectedCourseId == course.id,
                    text = course.name,
                    subtext = "${course.hole_count} hål • ${course.layout_count} layouter",
                    onClick = {
                        selectedCourseId = course.id
                        selectedLayoutId = null
                        onCourseSelected(course.id)
                    }
                )
            }

            item {
                HorizontalDivider()
                Text("Layout", style = MaterialTheme.typography.titleMedium)
            }

            items(layouts) { layout ->
                RowChoice(
                    selected = selectedLayoutId == layout.id,
                    text = layout.name,
                    subtext = "${layout.hole_count} hål • par ${layout.total_par} • ${layout.total_length_meters} m",
                    onClick = {
                        selectedLayoutId = layout.id
                    }
                )
            }

            item {
                HorizontalDivider()
                Text("Spelare", style = MaterialTheme.typography.titleMedium)
            }

            items(selectablePlayers) { player ->
                val isSelected = player.id in selectedPlayerIds
                RowChoice(
                    selected = isSelected,
                    text = player.name,
                    subtext = player.subtitle,
                    onClick = {
                        selectedPlayerIds = if (isSelected) {
                            selectedPlayerIds - player.id
                        } else {
                            selectedPlayerIds + player.id
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowChoice(
    selected: Boolean,
    text: String,
    subtext: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = (if (selected) "● " else "○ ") + text,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}