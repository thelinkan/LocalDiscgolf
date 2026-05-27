package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPlayerDetailScreen(
    playerName: String,
    roundCount: Int,
    onBack: () -> Unit,
    onRoundsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playerName) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "$roundCount rundor",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = onRoundsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rundor")
            }

            Button(
                onClick = onStatsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Statistik")
            }
        }
    }
}