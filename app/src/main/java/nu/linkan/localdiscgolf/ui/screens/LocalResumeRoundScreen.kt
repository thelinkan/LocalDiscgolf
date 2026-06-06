package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.data.local.model.LocalResumeRoundListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocalResumeRoundScreen(
    rounds: List<LocalResumeRoundListItem>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRoundClick: (playSessionId: Long, sequenceNumber: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) {
            Text("Tillbaka")
        }

        Text(
            text = "Återuppta runda",
            style = MaterialTheme.typography.headlineSmall
        )

        when {
            isLoading -> {
                Text("Laddar rundor...")
            }

            error != null -> {
                Text(
                    text = "Kunde inte hämta rundor: $error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            rounds.isEmpty() -> {
                Text("Det finns inga pågående rundor.")
            }

            else -> {
                rounds.forEach { round ->
                    ResumeRoundCard(
                        round = round,
                        onClick = {
                            onRoundClick(
                                round.id,
                                round.currentSequenceNumber ?: 1
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeRoundCard(
    round: LocalResumeRoundListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = round.courseName,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = syncStatusText(round),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Start: ${formatDateTime(round.startedAt)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Fortsätt på hål ${round.currentSequenceNumber ?: 1}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun syncStatusText(round: LocalResumeRoundListItem): String {
    return when {
        round.serverId == null -> "Endast lokalt"
        round.hasDirtyHoles -> "Ändringar väntar"
        round.syncStatus == "synced" -> "Synkad"
        round.syncStatus == "pending_update" -> "Ändringar väntar"
        round.syncStatus == "pending_complete" -> "Avslut väntar"
        round.syncStatus == "sync_error" -> "Synkfel"
        else -> round.syncStatus
    }
}

private fun formatDateTime(value: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(value))
}