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
import nu.linkan.localdiscgolf.network.LayoutApiResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiCourseLayoutsScreen(
    courseName: String,
    layouts: List<LayoutApiResponse>,
    onBack: () -> Unit,
    onLayoutClick: (Long) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(courseName) },
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
        if (layouts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Inga layouter hittades.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layouts) { layout ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLayoutClick(layout.id) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = layout.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "${layout.hole_count} hål • par ${layout.total_par} • ${layout.total_length_meters} m",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!layout.description.isNullOrBlank()) {
                            Text(
                                text = layout.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}