package nu.linkan.localdiscgolf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity
import nu.linkan.localdiscgolf.data.local.model.CourseListRow
import nu.linkan.localdiscgolf.data.local.model.HoleVariantWithNames
import nu.linkan.localdiscgolf.data.local.model.LayoutHoleWithHole
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleToLayoutDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddHoleVariantDialog
import nu.linkan.localdiscgolf.ui.dialogs.AddLayoutDialog
import nu.linkan.localdiscgolf.ui.dialogs.EditHoleDialog
import nu.linkan.localdiscgolf.ui.dialogs.HoleVariantsDialog
import nu.linkan.localdiscgolf.ui.dialogs.NameInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    courseRows: List<CourseListRow>,
    onBack: () -> Unit,
    onAddCourse: (String) -> Unit,
    onCourseClick: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Banor") },
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
            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text("Ny bana")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Antal banor: ${courseRows.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(courseRows) { course ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCourseClick(course.courseId) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = course.courseName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${course.holeCount} hål, ${course.layoutCount} layouter",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showDialog) {
        NameInputDialog(
            title = "Ny bana",
            label = "Bannamn",
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                onAddCourse(name)
                showDialog = false
            }
        )
    }
}

@Composable
fun LayoutHoleRow(
    item: LayoutHoleWithHole,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${item.sequenceNumber}. Hål ${item.holeNumber}" +
                        (item.holeName?.let { " - $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Längd: ${item.lengthMeters} m, Par: ${item.parValue}",
                style = MaterialTheme.typography.bodyMedium
            )

            val variantText = buildList {
                if (!item.teeName.isNullOrBlank()) add("Utkast: ${item.teeName}")
                if (!item.basketName.isNullOrBlank()) add("Korg: ${item.basketName}")
            }.joinToString(" | ")

            if (variantText.isNotBlank()) {
                Text(
                    text = variantText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column {
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Flytta upp"
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = !isLast
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Flytta ner"
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Ta bort från layout"
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: CourseEntity,
    holes: List<HoleEntity>,
    layouts: List<LayoutEntity>,
    teesByHole: Map<Long, List<HoleTeeEntity>>,
    basketsByHole: Map<Long, List<HoleBasketEntity>>,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    onBack: () -> Unit,
    onAddHole: (Int, String?, Int, Int, String?) -> Unit,
    onUpdateHole: (HoleEntity) -> Unit,
    onAddLayout: (String, String?) -> Unit,
    onLayoutClick: (Long) -> Unit,
    observeHoleTees: (Long) -> Unit,
    observeHoleBaskets: (Long) -> Unit,
    observeHoleVariants: (Long) -> Unit,
    onAddHoleTee: (Long, String) -> Unit,
    onAddHoleBasket: (Long, String) -> Unit,
    onAddHoleVariant: (Long, Long, Long, Int, Int) -> Unit
) {
    var showAddHoleDialog by remember { mutableStateOf(false) }
    var showAddLayoutDialog by remember { mutableStateOf(false) }
    var holeToEdit by remember { mutableStateOf<HoleEntity?>(null) }
    var holeForVariants by remember { mutableStateOf<HoleEntity?>(null) }
    var holeForNewVariant by remember { mutableStateOf<HoleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course.name) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Hål",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                Button(
                    onClick = { showAddHoleDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nytt hål")
                }
            }

            item {
                Text(
                    text = "Antal hål: ${holes.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(holes) { hole ->
                HoleRow(
                    hole = hole,
                    onClick = {
                        observeHoleTees(hole.id)
                        observeHoleBaskets(hole.id)
                        observeHoleVariants(hole.id)
                        holeForVariants = hole
                    },
                    onEditClick = { holeToEdit = hole }
                )
                HorizontalDivider()
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "Layouter",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                Button(
                    onClick = { showAddLayoutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ny layout")
                }
            }

            item {
                Text(
                    text = "Antal layouter: ${layouts.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            items(layouts) { layout ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLayoutClick(layout.id) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = layout.name,
                        style = MaterialTheme.typography.titleMedium
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

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showAddHoleDialog) {
        AddHoleDialog(
            onDismiss = { showAddHoleDialog = false },
            onConfirm = { holeNumber, name, lengthMeters, parValue, notes ->
                onAddHole(holeNumber, name, lengthMeters, parValue, notes)
                showAddHoleDialog = false
            }
        )
    }

    if (showAddLayoutDialog) {
        AddLayoutDialog(
            onDismiss = { showAddLayoutDialog = false },
            onConfirm = { name, description ->
                onAddLayout(name, description)
                showAddLayoutDialog = false
            }
        )
    }

    holeToEdit?.let { hole ->
        EditHoleDialog(
            hole = hole,
            onDismiss = { holeToEdit = null },
            onConfirm = { updatedHole ->
                onUpdateHole(updatedHole)
                holeToEdit = null
            }
        )
    }

    holeForVariants?.let { hole ->
        HoleVariantsDialog(
            hole = hole,
            tees = teesByHole[hole.id] ?: emptyList(),
            baskets = basketsByHole[hole.id] ?: emptyList(),
            variants = variantsByHole[hole.id] ?: emptyList(),
            onDismiss = { holeForVariants = null },
            onAddTee = { name -> onAddHoleTee(hole.id, name) },
            onAddBasket = { name -> onAddHoleBasket(hole.id, name) },
            onAddVariantClick = {
                holeForVariants = null
                holeForNewVariant = hole
            }
        )
    }

    holeForNewVariant?.let { hole ->
        AddHoleVariantDialog(
            tees = teesByHole[hole.id] ?: emptyList(),
            baskets = basketsByHole[hole.id] ?: emptyList(),
            existingCombinations = (variantsByHole[hole.id] ?: emptyList())
                .map { it.teeId to it.basketId }
                .toSet(),
            onDismiss = { holeForNewVariant = null },
            onConfirm = { teeId, basketId, lengthMeters, parValue ->
                onAddHoleVariant(hole.id, teeId, basketId, lengthMeters, parValue)
                holeForNewVariant = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutDetailScreen(
    layout: LayoutEntity,
    availableHoles: List<HoleEntity>,
    variantsByHole: Map<Long, List<HoleVariantWithNames>>,
    layoutHoles: List<LayoutHoleWithHole>,
    onBack: () -> Unit,
    onAddHoleToLayout: (Long, Long?) -> Unit,
    onRemoveHoleFromLayout: (Long, Int) -> Unit,
    onMoveHoleUp: (Int) -> Unit,
    onMoveHoleDown: (Int) -> Unit
){
    var showAddHoleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(layout.name) },
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
            Button(
                onClick = { showAddHoleDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Text("Lägg till hål")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Antal hål i layout: ${layoutHoles.size}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layoutHoles.size) { index ->
                    val item = layoutHoles[index]

                    LayoutHoleRow(
                        item = item,
                        isFirst = index == 0,
                        isLast = index == layoutHoles.lastIndex,
                        onMoveUp = { onMoveHoleUp(index) },
                        onMoveDown = { onMoveHoleDown(index) },
                        onDelete = {
                            onRemoveHoleFromLayout(item.layoutHoleId, item.sequenceNumber)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    if (showAddHoleDialog) {
        AddHoleToLayoutDialog(
            availableHoles = availableHoles,
            variantsByHole = variantsByHole,
            alreadyIncludedCombinations = layoutHoles.map {
                it.holeId to it.holeVariantId
            }.toSet(),
            onDismiss = { showAddHoleDialog = false },
            onConfirm = { holeId, holeVariantId ->
                onAddHoleToLayout(holeId, holeVariantId)
                showAddHoleDialog = false
            }
        )
    }
}

@Composable
fun HoleRow(
    hole: HoleEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Hål ${hole.holeNumber}" + (hole.name?.let { " - $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Längd: ${hole.lengthMeters} m, Par: ${hole.parValue}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!hole.notes.isNullOrBlank()) {
                Text(
                    text = hole.notes,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Tryck för utkast/korgplaceringar",
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Redigera hål"
            )
        }
    }
}

