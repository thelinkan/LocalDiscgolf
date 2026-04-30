package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.data.local.model.RoundHolePlayerRow
import nu.linkan.localdiscgolf.data.local.model.RoundHolePlayerStatsRow
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHeaderRow
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHoleRow

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
    onShowSummary: () -> Unit,
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
                    onClick = {
                        saveCurrentHole()
                        onShowSummary()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Visa rundsummering")
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
                val variantText = buildList {
                    if (!header?.teeNameSnapshot.isNullOrBlank()) add("Utkast: ${header?.teeNameSnapshot}")
                    if (!header?.basketNameSnapshot.isNullOrBlank()) add("Korg: ${header?.basketNameSnapshot}")
                }.joinToString(" | ")

                if (variantText.isNotBlank()) {
                    Text(
                        text = variantText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
fun RoundPlayerThrowsRow(
    row: RoundHolePlayerRow,
    stats: RoundHolePlayerStatsRow?,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val playerText = row.playerName ?: "Spelare ${row.playerId}"

        if (row.sequenceNumber > 1) {
            Text(
                text = "$playerText, ${formatRelativeScore(row.previousRelativeToPar)} (${row.previousThrowsTotal})",
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Text(
                text = playerText,
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (stats != null) {
            Text(
                text = "Tidigare: ${stats.timesPlayed} rundor, PB ${stats.bestThrows}, snitt ${"%.2f".format(stats.avgThrows)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Birdie+: ${stats.birdiesOrBetter}  Par: ${stats.pars} Bogey: ${stats.bogeys}  Dubbel: ${stats.doubleBogeys}  Trippel+: ${stats.tripleBogeysOrWorse}",
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


