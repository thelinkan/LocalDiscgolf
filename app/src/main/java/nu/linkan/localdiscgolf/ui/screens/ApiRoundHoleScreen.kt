package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.network.CurrentRoundApiResponse
import nu.linkan.localdiscgolf.network.CurrentRoundHoleScoreApiResponse
import nu.linkan.localdiscgolf.network.CurrentRoundSummaryApiResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiRoundHoleScreen(
    currentRound: CurrentRoundApiResponse?,
    onBack: () -> Unit,
    onPreviousHole: (() -> Unit)?,
    onNextHole: (() -> Unit)?,
    onShowSummary: () -> Unit,
    onSaveHole: (Int, List<Pair<Long, Int?>>) -> Unit,
    onFinishRound: () -> Unit
) {
    val round = currentRound
    val header = round?.current_hole
    var showFinishDialog by remember { mutableStateOf(false) }

    val inputValues = remember(round?.current_hole?.sequence_number, round?.current_hole?.scores) {
        mutableStateMapOf<Long, String>().apply {
            round?.current_hole?.scores?.forEach { score ->
                this[score.player_id] = score.throws_count?.toString() ?: ""
            }
        }
    }

    fun saveCurrentHole() {
        val sequenceNumber = round?.current_hole?.sequence_number ?: return
        val valuesToSave = round.current_hole.scores.map { score ->
            val throwsValue = inputValues[score.player_id]?.trim()?.toIntOrNull()
            score.player_id to throwsValue
        }
        onSaveHole(sequenceNumber, valuesToSave)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (header != null) {
                            "Hål ${header.sequence_number}"
                        } else {
                            "Serverrunda"
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
        },
        bottomBar = {
            if (round != null) {
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
                                onPreviousHole?.invoke()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = onPreviousHole != null
                        ) {
                            Text("Föregående")
                        }

                        Button(
                            onClick = {
                                saveCurrentHole()
                                onNextHole?.invoke()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = onNextHole != null
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
        }
    ) { innerPadding ->
        if (round == null || header == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Ingen serverrunda laddad.")
            }
        } else {
            val variantText = buildList {
                if (!header.tee_name.isNullOrBlank()) add("Utkast: ${header.tee_name}")
                if (!header.basket_name.isNullOrBlank()) add("Korg: ${header.basket_name}")
            }.joinToString(" | ")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hål ${header.hole_number}" +
                            (header.hole_name?.let { " - $it" } ?: ""),
                    style = MaterialTheme.typography.headlineSmall
                )

                if (variantText.isNotBlank()) {
                    Text(
                        text = variantText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Längd: ${header.length_meters} m, Par: ${header.par_value}",
                    style = MaterialTheme.typography.bodyLarge
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(header.scores.sortedBy { it.start_order }) { score ->
                        val summary = round.summary_to_previous_hole
                            .firstOrNull { it.player_id == score.player_id }

                        ApiRoundPlayerThrowsRow(
                            score = score,
                            summary = summary,
                            value = inputValues[score.player_id] ?: "",
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    inputValues[score.player_id] = newValue
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFinishDialog && round != null) {
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
fun ApiRoundPlayerThrowsRow(
    score: CurrentRoundHoleScoreApiResponse,
    summary: CurrentRoundSummaryApiResponse?,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val playerText = score.player_name

        if (summary != null && summary.played_holes > 0) {
            Text(
                text = "$playerText, ${formatRelativeScore(summary.relative_to_par)} (${summary.total_throws})",
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Text(
                text = playerText,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(
            text = "Tidigare: 0 rundor, PB 0, snitt 0.00",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Birdie+: 0  Par: 0  Bogey: 0  Dubbel: 0  Trippel+: 0",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Kast") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

