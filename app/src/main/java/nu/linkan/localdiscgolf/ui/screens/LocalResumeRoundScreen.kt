package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Tillbaka"
            )
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
    val statusText = syncStatusText(round)

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
            Text(
                text = round.courseName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Start: ${formatDateTime(round.startedAt)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Fortsätt på hål ${round.currentSequenceNumber ?: 1}",
                style = MaterialTheme.typography.bodySmall
            )

            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        round.syncStatus == "sync_error" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
        }
    }
}

private fun syncStatusText(round: LocalResumeRoundListItem): String? {
    if (round.hasDirtyHoles) {
        return "Ändringar väntar på synk"
    }

    return when (round.syncStatus) {
        "synced" -> null
        null -> null

        "local_only" -> "Endast lokalt"
        "pending_create" -> "Väntar på att skapas på servern"
        "pending_update" -> "Ändringar väntar på synk"
        "pending_complete" -> "Avslutad lokalt, väntar på synk"
        "sync_error" -> "Synkfel"

        else -> "Ej synkad"
    }
}

private fun formatDateTime(value: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(value))
}