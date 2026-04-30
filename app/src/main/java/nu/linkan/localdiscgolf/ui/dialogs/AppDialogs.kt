package nu.linkan.localdiscgolf.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.model.HoleVariantWithNames


@Composable
fun AddHoleDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String?, Int, Int, String?) -> Unit
) {
    var holeNumberText by remember { mutableStateOf("") }
    var nameText by remember { mutableStateOf("") }
    var lengthText by remember { mutableStateOf("") }
    var parText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nytt hål") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = holeNumberText,
                    onValueChange = { holeNumberText = it },
                    label = { Text("Hålnummer") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Namn (valfritt)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Längd i meter") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = parText,
                    onValueChange = { parText = it },
                    label = { Text("Par") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Anteckning (valfritt)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val holeNumber = holeNumberText.trim().toIntOrNull()
                    val lengthMeters = lengthText.trim().toIntOrNull()
                    val parValue = parText.trim().toIntOrNull()

                    if (holeNumber != null && lengthMeters != null && parValue != null) {
                        onConfirm(
                            holeNumber,
                            nameText.trim().ifBlank { null },
                            lengthMeters,
                            parValue,
                            notesText.trim().ifBlank { null }
                        )
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun AddHoleToLayoutDialog(
    availableHoles: List<HoleEntity>,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    alreadyIncludedCombinations: Set<Pair<Long, Long?>>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?) -> Unit
) {
    var selectedHoleId by remember { mutableStateOf<Long?>(null) }
    var selectedVariantId by remember { mutableStateOf<Long?>(null) }

    val selectedHole = availableHoles.firstOrNull { it.id == selectedHoleId }
    val availableVariants = selectedHole?.let { variantsByHole[it.id] ?: emptyList() } ?: emptyList()

    LaunchedEffect(selectedHoleId) {
        selectedVariantId = when {
            availableVariants.size == 1 -> availableVariants.first().id
            availableVariants.isEmpty() -> null
            else -> null
        }
    }

    val currentAllowed = selectedHoleId != null &&
            (selectedHoleId!! to selectedVariantId) !in alreadyIncludedCombinations

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lägg till hål i layout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Hål", style = MaterialTheme.typography.titleMedium)

                availableHoles.forEach { hole ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHoleId = hole.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedHoleId == hole.id) "●" else "○")
                        Text("Hål ${hole.holeNumber}" + (hole.name?.let { " - $it" } ?: ""))
                    }
                }

                if (selectedHole != null && availableVariants.isNotEmpty()) {
                    Text("Variant", style = MaterialTheme.typography.titleMedium)

                    availableVariants.forEach { variant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVariantId = variant.id }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (selectedVariantId == variant.id) "●" else "○")
                            Text("${variant.teeName} → ${variant.basketName} | ${variant.lengthMeters} m | par ${variant.parValue}")
                        }
                    }
                }

                if (selectedHoleId != null && !currentAllowed) {
                    Text("Den kombinationen finns redan i layouten.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedHoleId?.let { holeId ->
                        onConfirm(holeId, selectedVariantId)
                    }
                },
                enabled = selectedHoleId != null && currentAllowed
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun AddHoleVariantDialog(
    tees: List<HoleTeeEntity>,
    baskets: List<HoleBasketEntity>,
    existingCombinations: Set<Pair<Long, Long>>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Int, Int) -> Unit
) {
    var selectedTeeId by remember { mutableStateOf<Long?>(null) }
    var selectedBasketId by remember { mutableStateOf<Long?>(null) }
    var lengthText by remember { mutableStateOf("") }
    var parText by remember { mutableStateOf("") }

    val duplicate = selectedTeeId != null &&
            selectedBasketId != null &&
            (selectedTeeId!! to selectedBasketId!!) in existingCombinations

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny variant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Utkast", style = MaterialTheme.typography.titleMedium)
                tees.forEach { tee ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTeeId = tee.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedTeeId == tee.id) "●" else "○")
                        Text(tee.name)
                    }
                }

                Text("Korgplacering", style = MaterialTheme.typography.titleMedium)
                baskets.forEach { basket ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBasketId = basket.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedBasketId == basket.id) "●" else "○")
                        Text(basket.name)
                    }
                }

                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Längd i meter") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = parText,
                    onValueChange = { parText = it },
                    label = { Text("Par") },
                    singleLine = true
                )

                if (duplicate) {
                    Text("Den kombinationen finns redan.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val teeId = selectedTeeId
                    val basketId = selectedBasketId
                    val lengthMeters = lengthText.toIntOrNull()
                    val parValue = parText.toIntOrNull()

                    if (teeId != null &&
                        basketId != null &&
                        lengthMeters != null &&
                        parValue != null &&
                        !duplicate
                    ) {
                        onConfirm(teeId, basketId, lengthMeters, parValue)
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun AddLayoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny layout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Layoutnamn") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Beskrivning (valfritt)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = nameText.trim()
                    if (trimmedName.isNotEmpty()) {
                        onConfirm(
                            trimmedName,
                            descriptionText.trim().ifBlank { null }
                        )
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun EditHoleDialog(
    hole: HoleEntity,
    onDismiss: () -> Unit,
    onConfirm: (HoleEntity) -> Unit
) {
    var holeNumberText by remember(hole.id) { mutableStateOf(hole.holeNumber.toString()) }
    var nameText by remember(hole.id) { mutableStateOf(hole.name ?: "") }
    var lengthText by remember(hole.id) { mutableStateOf(hole.lengthMeters.toString()) }
    var parText by remember(hole.id) { mutableStateOf(hole.parValue.toString()) }
    var notesText by remember(hole.id) { mutableStateOf(hole.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera hål") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = holeNumberText,
                    onValueChange = { holeNumberText = it },
                    label = { Text("Hålnummer") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Namn (valfritt)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Längd i meter") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = parText,
                    onValueChange = { parText = it },
                    label = { Text("Par") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Anteckning (valfritt)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val holeNumber = holeNumberText.trim().toIntOrNull()
                    val lengthMeters = lengthText.trim().toIntOrNull()
                    val parValue = parText.trim().toIntOrNull()

                    if (holeNumber != null && lengthMeters != null && parValue != null) {
                        onConfirm(
                            hole.copy(
                                holeNumber = holeNumber,
                                name = nameText.trim().ifBlank { null },
                                lengthMeters = lengthMeters,
                                parValue = parValue,
                                notes = notesText.trim().ifBlank { null }
                            )
                        )
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun HoleVariantsDialog(
    hole: HoleEntity,
    tees: List<HoleTeeEntity>,
    baskets: List<HoleBasketEntity>,
    variants: List<HoleVariantWithNames>,
    onDismiss: () -> Unit,
    onAddTee: (String) -> Unit,
    onAddBasket: (String) -> Unit,
    onAddVariantClick: () -> Unit
) {
    var showAddTeeDialog by remember { mutableStateOf(false) }
    var showAddBasketDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Hål ${hole.holeNumber}" + (hole.name?.let { " - $it" } ?: ""))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Utkast", style = MaterialTheme.typography.titleMedium)
                if (tees.isEmpty()) {
                    Text("Inga utkast ännu.")
                } else {
                    tees.forEach { Text("• ${it.name}") }
                }

                Button(
                    onClick = { showAddTeeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nytt utkast")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Korgplaceringar", style = MaterialTheme.typography.titleMedium)
                if (baskets.isEmpty()) {
                    Text("Inga korgplaceringar ännu.")
                } else {
                    baskets.forEach { Text("• ${it.name}") }
                }

                Button(
                    onClick = { showAddBasketDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ny korgplacering")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Varianter", style = MaterialTheme.typography.titleMedium)
                if (variants.isEmpty()) {
                    Text("Inga varianter ännu.")
                } else {
                    variants.forEach { variant ->
                        Text("• ${variant.teeName} → ${variant.basketName} | ${variant.lengthMeters} m | par ${variant.parValue}")
                    }
                }

                Button(
                    onClick = onAddVariantClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = tees.isNotEmpty() && baskets.isNotEmpty()
                ) {
                    Text("Ny variant")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Stäng")
            }
        }
    )

    if (showAddTeeDialog) {
        NameInputDialog(
            title = "Nytt utkast",
            label = "Namn",
            onDismiss = { showAddTeeDialog = false },
            onConfirm = {
                onAddTee(it)
                showAddTeeDialog = false
            }
        )
    }

    if (showAddBasketDialog) {
        NameInputDialog(
            title = "Ny korgplacering",
            label = "Namn",
            onDismiss = { showAddBasketDialog = false },
            onConfirm = {
                onAddBasket(it)
                showAddBasketDialog = false
            }
        )
    }
}


@Composable
fun NameInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onConfirm(trimmed)
                    }
                }
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}