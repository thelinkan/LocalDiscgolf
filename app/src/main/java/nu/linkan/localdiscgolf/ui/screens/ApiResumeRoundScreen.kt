package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.network.InProgressServerRoundApiResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiResumeRoundScreen(
    rounds: List<InProgressServerRoundApiResponse>,
    onBack: () -> Unit,
    onResume: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Återuppta serverrunda") },
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
        if (rounds.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Det finns inga pågående serverrundor.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rounds) { round ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResume(round.id) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (round.layout_name.isNullOrBlank()) {
                                round.course_name
                            } else {
                                "${round.course_name} - ${round.layout_name}"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )

                        val playerText = when (round.player_count) {
                            1 -> "Själv"
                            2 -> "Tillsammans med 1 annan spelare"
                            else -> "Tillsammans med ${round.player_count - 1} andra spelare"
                        }

                        Text(
                            text = playerText,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Start: ${round.started_at}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}