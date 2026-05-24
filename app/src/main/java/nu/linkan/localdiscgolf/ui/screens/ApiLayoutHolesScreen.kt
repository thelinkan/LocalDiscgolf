package nu.linkan.localdiscgolf.ui.screens

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
import nu.linkan.localdiscgolf.network.LayoutHoleApiResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLayoutHolesScreen(
    layoutName: String,
    holes: List<LayoutHoleApiResponse>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(layoutName) },
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
        if (holes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Inga hål hittades för layouten.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(holes) { hole ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        val title = buildString {
                            append("Hål ${hole.hole_number}")
                            if (!hole.hole_name.isNullOrBlank()) {
                                append(" - ${hole.hole_name}")
                            }
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium
                        )

                        val teeText = hole.tee_name ?: "-"
                        val basketText = hole.basket_name ?: "-"

                        Text(
                            text = "$teeText → $basketText",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "${hole.length_meters} m • par ${hole.par_value}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}