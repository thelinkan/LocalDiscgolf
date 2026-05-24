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
import nu.linkan.localdiscgolf.network.PlayerApiResponse
import nu.linkan.localdiscgolf.network.ScoreablePlayerApiResponse
import nu.linkan.localdiscgolf.network.UserPlayersResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPlayersScreen(
    data: UserPlayersResponse?,
    onBack: () -> Unit,
    onPlayerClick: (Long) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serverspelare") },
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
        if (data == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Inga serverspelare laddade.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Egen spelare",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    val own = data.own_player
                    if (own == null) {
                        Text("Ingen egen spelare hittades.")
                    } else {
                        PlayerRow(
                            name = own.name,
                            subtitle = "Egen spelare",
                            onClick = { onPlayerClick(own.id) }
                        )
                    }
                }

                item {
                    HorizontalDivider()
                    Text(
                        text = "Gästspelare",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (data.guest_players.isEmpty()) {
                    item { Text("Inga gästspelare.") }
                } else {
                    items(data.guest_players) { player ->
                        PlayerRow(
                            name = player.name,
                            subtitle = "Gästspelare",
                            onClick = { onPlayerClick(player.id) }
                        )
                    }
                }

                item {
                    HorizontalDivider()
                    Text(
                        text = "Spelare du får scorea för",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (data.scoreable_players.isEmpty()) {
                    item { Text("Inga spelare.") }
                } else {
                    items(data.scoreable_players) { player ->
                        val permissionText = when (player.permission_level) {
                            "auto_approve" -> "Autogodkänd"
                            "propose" -> "Kräver godkännande"
                            else -> player.permission_level
                        }

                        PlayerRow(
                            name = player.name,
                            subtitle = permissionText,
                            onClick = { onPlayerClick(player.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    name: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}