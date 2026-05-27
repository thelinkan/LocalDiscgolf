package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.network.CourseApiResponse
import nu.linkan.localdiscgolf.network.PlayerHoleStatsApiResponse
import nu.linkan.localdiscgolf.network.PlayerLayoutStatsApiResponse
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPlayerStatsScreen(
    playerName: String,
    courses: List<CourseApiResponse>,
    selectedCourseId: Long?,
    layoutStats: List<PlayerLayoutStatsApiResponse>,
    holeStats: List<PlayerHoleStatsApiResponse>,
    onBack: () -> Unit,
    onCourseSelected: (Long?) -> Unit
) {
    var showCourseMenu by remember { mutableStateOf(false) }

    val selectedCourseName = courses
        .firstOrNull { it.id == selectedCourseId }
        ?.name
        ?: "Alla banor"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$playerName - Statistik") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Bana",
                    style = MaterialTheme.typography.titleMedium
                )

                Box {
                    OutlinedButton(
                        onClick = { showCourseMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedCourseName)
                    }

                    DropdownMenu(
                        expanded = showCourseMenu,
                        onDismissRequest = { showCourseMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alla banor") },
                            onClick = {
                                showCourseMenu = false
                                onCourseSelected(null)
                            }
                        )

                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = {
                                    showCourseMenu = false
                                    onCourseSelected(course.id)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Per layout",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (layoutStats.isEmpty()) {
                item {
                    Text("Ingen layoutstatistik ännu.")
                }
            } else {
                items(layoutStats) { stat ->
                    LayoutStatsRow(stat)
                    HorizontalDivider()
                }
            }

            item {
                Text(
                    text = "Per hål",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (holeStats.isEmpty()) {
                item {
                    Text("Ingen hålstatistik ännu.")
                }
            } else {
                items(holeStats) { stat ->
                    HoleStatsRow(stat)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LayoutStatsRow(
    stat: PlayerLayoutStatsApiResponse
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${stat.course_name} - ${stat.layout_name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Par ${stat.total_par}, ${stat.hole_count} hål, ${stat.total_length_meters} meter"
        )

        Text("Antal rundor: ${stat.round_count}")

        Text(
            text = "Personbästa: ${stat.personal_best_throws} " +
                    "(${formatSignedInt(stat.personal_best_relative_to_par)})"
        )

        Text(
            text = "Snitt: ${formatOneDecimal(stat.average_throws)} " +
                    "(${formatSignedOneDecimal(stat.average_relative_to_par)})"
        )

        if (
            stat.last_10_average_throws != null &&
            stat.last_10_average_relative_to_par != null
        ) {
            Text(
                text = "Snitt 10 senaste: ${formatOneDecimal(stat.last_10_average_throws)} " +
                        "(${formatSignedOneDecimal(stat.last_10_average_relative_to_par)})"
            )
        }
    }
}

@Composable
private fun HoleStatsRow(
    stat: PlayerHoleStatsApiResponse
) {
    val title = if (stat.hole_name.isNullOrBlank()) {
        "${stat.course_name} - Hål ${stat.hole_number}"
    } else {
        "${stat.course_name} - Hål ${stat.hole_number} - ${stat.hole_name}"
    }

    val lastTenText = stat.last_10_average_throws?.let {
        " (senaste 10: ${formatTwoDecimals(it)})"
    }.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Utkast: ${stat.tee_name ?: "-"} | Korg: ${stat.basket_name ?: "-"}"
        )

        Text(
            text = "Längd: ${stat.length_meters} m, Par ${stat.par_value}"
        )

        Text(
            text = "Spelat: ${stat.played_count} gånger"
        )

        Text(
            text = "Personbästa: ${stat.personal_best_throws} | " +
                    "Svit: ${formatSignedInt(stat.streak)}"
        )

        Text(
            text = "Medel kast: ${formatTwoDecimals(stat.average_throws)}$lastTenText"
        )

        Text(
            text = "Birdie+: ${stat.birdie_or_better_count} | " +
                    "Par: ${stat.par_count} | " +
                    "Bogey: ${stat.bogey_count} | " +
                    "Dubbelbogey: ${stat.double_bogey_count} | " +
                    "Trippelbogey+: ${stat.triple_bogey_or_worse_count}"
        )
    }
}

private val swedishLocale = Locale("sv", "SE")

private fun formatOneDecimal(value: Double): String {
    return String.format(swedishLocale, "%.1f", value)
}

private fun formatTwoDecimals(value: Double): String {
    return String.format(swedishLocale, "%.2f", value)
}

private fun formatSignedInt(value: Int): String {
    return if (value > 0) "+$value" else value.toString()
}

private fun formatSignedOneDecimal(value: Double): String {
    val valueText = formatOneDecimal(value)
    return if (value > 0) "+$valueText" else valueText
}