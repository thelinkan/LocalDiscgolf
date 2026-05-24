package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.network.RoundDetailApiResponse
import nu.linkan.localdiscgolf.network.RoundDetailHoleApiResponse
import nu.linkan.localdiscgolf.network.RoundDetailPlayerApiResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiRoundDetailScreen(
    round: RoundDetailApiResponse?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serverrunda") },
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
        if (round == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Ingen runddetalj laddad.")
            }
        } else {
            val sortedPlayers = round.players.sortedBy { it.start_order }
            val allHoles = sortedPlayers
                .firstOrNull()
                ?.holes
                ?.sortedBy { it.sequence_number }
                ?: emptyList()

            val holeChunks = allHoles.chunked(9)
            val layoutName = sortedPlayers.firstOrNull()?.layout_name

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
                            text = round.course_name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (!layoutName.isNullOrBlank()) {
                            Text(
                                text = layoutName,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Start: ${round.started_at}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!round.ended_at.isNullOrBlank()) {
                            Text(
                                text = "Slut: ${round.ended_at}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "Status: ${round.status}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item {
                    ApiRoundPlayerSummarySection(players = sortedPlayers)
                }

                item {
                    Text(
                        text = "Scorekort",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                items(holeChunks) { holeChunk ->
                    ApiRoundScorecardBlock(
                        holeChunk = holeChunk,
                        players = sortedPlayers
                    )
                }
            }
        }
    }
}

@Composable
fun ApiRoundPlayerSummarySection(
    players: List<RoundDetailPlayerApiResponse>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        players.forEach { player ->
            val playedHoles = player.holes
                .filter { it.throws_count != null }
                .sortedBy { it.sequence_number }

            val totalThrows = playedHoles.sumOf { it.throws_count ?: 0 }
            val totalPar = playedHoles.sumOf { it.par_snapshot }
            val relative = totalThrows - totalPar

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = player.player_name,
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
fun ApiRoundScorecardBlock(
    holeChunk: List<RoundDetailHoleApiResponse>,
    players: List<RoundDetailPlayerApiResponse>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ApiScorecardHeaderRow(
            label = "Hål",
            values = holeChunk.map { it.hole_number_snapshot.toString() }
        )

        ApiScorecardHeaderRow(
            label = "Längd",
            values = holeChunk.map { it.length_snapshot_meters.toString() }
        )

        ApiScorecardHeaderRow(
            label = "Par",
            values = holeChunk.map { it.par_snapshot.toString() }
        )

        Spacer(modifier = Modifier.size(4.dp))

        players.forEach { player ->
            val rowsForChunk = holeChunk.map { hole ->
                player.holes.firstOrNull { it.sequence_number == hole.sequence_number }
            }

            ApiScorecardPlayerRow(
                playerName = player.player_name,
                rows = rowsForChunk
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ApiScorecardHeaderRow(
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
fun ApiScorecardPlayerRow(
    playerName: String,
    rows: List<RoundDetailHoleApiResponse?>
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
                ApiCompactScoreBadge(
                    throwsCount = row?.throws_count,
                    par = row?.par_snapshot
                )
            }
        }
    }
}

@Composable
fun ApiCompactScoreBadge(
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

