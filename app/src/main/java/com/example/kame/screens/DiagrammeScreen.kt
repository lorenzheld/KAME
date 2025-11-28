package com.example.kame.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kame.R
import com.example.kame.data.database.WorkoutDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

// Datenmodell für den Graphen
data class ChartDataPoint(
    val dateLabel: String,
    val weight: Float,
    val fullDate: LocalDateTime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagrammeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { WorkoutDatabase.getDatabase(context) }
    val dao = database.workoutDao()

    // State
    var allExerciseNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedExercise by remember { mutableStateOf("Keine Übung") }
    var chartData by remember { mutableStateOf<List<ChartDataPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 1. Alle verfügbaren Übungs-Namen laden
    LaunchedEffect(Unit) {
        val workouts = dao.getAllWorkouts().first()
        val names = mutableSetOf<String>()

        workouts.forEach { workout ->
            val exercises = dao.getExercisesForWorkout(workout.id).first()
            exercises.forEach { names.add(it.name) }
        }

        val sortedNames = names.sorted()
        allExerciseNames = sortedNames
        if (sortedNames.isNotEmpty()) {
            selectedExercise = sortedNames.first()
        }
        isLoading = false
    }

    // 2. Daten für die ausgewählte Übung laden
    LaunchedEffect(selectedExercise) {
        if (selectedExercise == "Keine Übung") return@LaunchedEffect

        // Wir müssen alle Exercise-IDs finden, die diesen Namen haben (könnte in mehreren Plänen vorkommen)
        val workouts = dao.getAllWorkouts().first()
        val relevantExerciseIds = mutableListOf<String>()

        workouts.forEach { workout ->
            val exercises = dao.getExercisesForWorkout(workout.id).first()
            exercises.filter { it.name == selectedExercise }.forEach { relevantExerciseIds.add(it.id) }
        }

        // Jetzt alle Sessions für diese IDs holen
        val rawPoints = mutableListOf<ChartDataPoint>()

        relevantExerciseIds.forEach { exId ->
            val sessions = dao.getSessionsForExercise(exId).first()
            sessions.forEach { session ->
                // Wir nehmen das MAXIMALE Gewicht dieses Trainings als Datenpunkt
                val maxWeightInSession = session.sets
                    .filter { it.completed }
                    .maxOfOrNull { it.weight } ?: 0f

                if (maxWeightInSession > 0) {
                    val date = LocalDateTime.parse(session.timestamp, DateTimeFormatter.ISO_DATE_TIME)
                    // Format: dd.MM (z.B. 27.10)
                    val label = "${date.dayOfMonth}.${date.monthValue}"
                    rawPoints.add(ChartDataPoint(label, maxWeightInSession, date))
                }
            }
        }

        // Sortieren nach Datum und in State speichern
        chartData = rawPoints.sortedBy { it.fullDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Progress", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Back navigation */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = { Box(modifier = Modifier.size(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Exercise Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exercise", style = MaterialTheme.typography.titleMedium)

                    if (allExerciseNames.isNotEmpty()) {
                        ExerciseDropdown(
                            options = allExerciseNames,
                            selectedOption = selectedExercise,
                            onOptionSelected = { selectedExercise = it }
                        )
                    } else {
                        Text("Keine Übungen gefunden", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Stats & Chart Area
            if (chartData.isNotEmpty()) {
                val currentWeight = chartData.last().weight
                val maxWeight = chartData.maxOf { it.weight }
                val startWeight = chartData.first().weight

                // Berechnung der Veränderung
                val diff = currentWeight - startWeight
                val sign = if (diff > 0) "+" else ""
                val percentString = "$sign${"%.1f".format(diff)} kg"

                item {
                    ChartCard(
                        data = chartData,
                        title = "$selectedExercise: Max Gewicht",
                        currentValue = "${currentWeight.toInt()} kg",
                        percentChange = percentString
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Start",
                            value = "${startWeight.toInt()} kg",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Maximum",
                            value = "${maxWeight.toInt()} kg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                item {
                    EmptyStateChartCard()
                }
            }
        }
    }
}

@Composable
fun ExerciseDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedOption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChartCard(
    data: List<ChartDataPoint>,
    title: String,
    currentValue: String,
    percentChange: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = currentValue,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Seit Beginn",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = percentChange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentChange.startsWith("+")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LineChart(
                data = data,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

@Composable
fun LineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (data.isEmpty()) return@Canvas

            // Berechne Skalierung
            val maxWeight = data.maxOf { it.weight }
            val minWeight = data.minOf { it.weight }
            // FIX: Verhindere Division durch 0, falls min == max
            val weightDiff = maxWeight - minWeight
            val range = if (weightDiff == 0f) 1f else weightDiff

            // Layout Parameter
            val paddingBottom = 60f
            val paddingTop = 20f
            val paddingHorizontal = 40f

            val chartWidth = size.width - (paddingHorizontal * 2)
            val chartHeight = size.height - paddingBottom - paddingTop

            // X-Schrittweite
            val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

            val path = Path()

            data.forEachIndexed { index, point ->
                val x = paddingHorizontal + index * stepX

                // Y-Position berechnen (0 ist oben!)
                val normalizedY = (point.weight - minWeight) / range
                // Umkehren, da Canvas 0 oben hat:
                val y = (size.height - paddingBottom) - (normalizedY * chartHeight)

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Punkt zeichnen
                drawCircle(
                    color = primaryColor,
                    radius = 8f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 5f)
            )
        }

        // Labels zeichnen (einfache Version für X-Achse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Zeige maximal 5 Labels an, um Überlappung zu vermeiden
            val labelCount = min(5, data.size)
            if (data.isNotEmpty()) {
                val step = (data.size - 1).toFloat() / (labelCount - 1).coerceAtLeast(1)

                for (i in 0 until labelCount) {
                    val index = (i * step).toInt().coerceAtMost(data.lastIndex)
                    Text(
                        text = data[index].dateLabel,
                        fontSize = 10.sp,
                        color = onSurfaceColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyStateChartCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bar_chart),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keine Daten", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Absolviere Workouts, um hier Diagramme zu sehen.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}