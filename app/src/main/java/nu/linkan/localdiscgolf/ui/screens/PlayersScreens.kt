package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.data.local.model.PlayerHoleDetailRoundRow
import nu.linkan.localdiscgolf.data.local.model.PlayerHoleStatsRow
import nu.linkan.localdiscgolf.data.local.model.PlayerLayoutStatsRow
import nu.linkan.localdiscgolf.data.local.model.PlayerListRow
import nu.linkan.localdiscgolf.data.local.model.PlayerSessionRow
import nu.linkan.localdiscgolf.formatDateTime
import nu.linkan.localdiscgolf.formatRelativeDouble
import nu.linkan.localdiscgolf.ui.screens.formatRelativeScore
import nu.linkan.localdiscgolf.ui.dialogs.NameInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    playerRows: List<PlayerListRow>,
    onBack: () -> Unit,
    onAddPlayer: (String) -> Unit,
    onPlayerClick: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spelare") },
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
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text("Ny spelare")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Antal spelare: ${playerRows.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(playerRows) { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(player.playerId) }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = player.playerName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${player.roundCount} rundor",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
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

private fun playerSessionSyncStatusText(session: PlayerSessionRow): String? {
    if (session.hasDirtyHoles) {
        return "Ej synkad"
    }

    if (session.serverId == null) {
        return "Endast lokalt"
    }

    return when (session.syncStatus) {
        null -> null
        "synced" -> null

        "local_only" -> "Endast lokalt"
        "pending_create" -> "Ej skapad på servern"
        "pending_update" -> "Ej synkad"
        "pending_complete" -> "Avslutad lokalt, väntar på synk"
        "sync_error" -> "Synkfel"

        else -> "Ej synkad"
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${sessions.size} inlagda rundor",
                    style = MaterialTheme.typography.bodyLarge
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
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
                                    text = session.courseName + (session.layoutName?.let { " - $it" }
                                        ?: ""),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = if (session.playerCount <= 1) {
                                        "Själv"
                                    } else {
                                        "Tillsammans med ${session.playerCount - 1} andra spelare"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Start: ${formatDateTime(session.startedAt)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                val resultText =
                                    if (session.totalThrows != null && session.totalRelativeToPar != null) {
                                        "Resultat: ${formatRelativeScore(session.totalRelativeToPar)} (${session.totalThrows})"
                                    } else {
                                        "Resultat: -"
                                    }

                                val extraInfo = buildList {
                                    if (session.playedHoleCount < session.totalHoleCount) {
                                        add("${session.playedHoleCount} av ${session.totalHoleCount} hål spelade")
                                    }
                                    if (session.status != "completed") {
                                        add("Pågående")
                                    }
                                }.joinToString(" - ")

                                val resultLine = if (extraInfo.isNotBlank()) {
                                    "$resultText ($extraInfo)"
                                } else {
                                    resultText
                                }

                                Text(
                                    text = resultLine,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                val syncText = playerSessionSyncStatusText(session)

                                if (syncText != null) {
                                    Text(
                                        text = syncText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
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
fun PlayerStatsScreen(
    player: PlayerEntity,
    layoutStats: List<PlayerLayoutStatsRow>,
    holeStats: List<PlayerHoleStatsRow>,
    onBack: () -> Unit,
    onHoleClick: (Long, Int, Long?) -> Unit
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
                            .clickable { onHoleClick(row.courseId, row.holeNumber, row.holeVariantId) }
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${row.courseName} - Hål ${row.holeNumber}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val variantText = buildList {
                            if (!row.teeName.isNullOrBlank()) add("Utkast: ${row.teeName}")
                            if (!row.basketName.isNullOrBlank()) add("Korg: ${row.basketName}")
                        }.joinToString(" | ")

                        if (variantText.isNotBlank()) {
                            Text(
                                text = variantText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "Längd: ${row.lengthMeters} m, Par: ${row.parValue}",
                            style = MaterialTheme.typography.bodyMedium
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
