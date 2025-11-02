package com.example.kame.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kame.R
import com.example.kame.ui.theme.KAMETheme
import kotlin.math.max
import kotlin.math.min

data class ChartDataPoint(
    val week: String,
    val weight: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagrammeScreen(modifier: Modifier = Modifier) {
    var selectedExercise by remember { mutableStateOf("Bankdrücken") }
    var selectedChartType by remember { mutableStateOf("Weight (Line Chart)") }
    var selectedTimeRange by remember { mutableStateOf("30 Tage") }

    // Beispiel-Daten
    val chartData = listOf(
        ChartDataPoint("W1", 87f),
        ChartDataPoint("W2", 90f),
        ChartDataPoint("W3", 92f),
        ChartDataPoint("W4", 95f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.size(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
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
                    Text(
                        text = "Exercise",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    DropdownSelector(
                        value = selectedExercise,
                        onValueChange = { selectedExercise = it }
                    )
                }
            }

            // Chart Type Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Chart Type",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    DropdownSelector(
                        value = selectedChartType,
                        onValueChange = { selectedChartType = it }
                    )
                }
            }

            // Time Range Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeRangeButton(
                        text = "7 Tage",
                        selected = selectedTimeRange == "7 Tage",
                        onClick = { selectedTimeRange = "7 Tage" },
                        modifier = Modifier.weight(1f)
                    )
                    TimeRangeButton(
                        text = "30 Tage",
                        selected = selectedTimeRange == "30 Tage",
                        onClick = { selectedTimeRange = "30 Tage" },
                        modifier = Modifier.weight(1f)
                    )
                    TimeRangeButton(
                        text = "90 Tage",
                        selected = selectedTimeRange == "90 Tage",
                        onClick = { selectedTimeRange = "90 Tage" },
                        modifier = Modifier.weight(1f)
                    )
                    TimeRangeButton(
                        text = "Gesamt",
                        selected = selectedTimeRange == "Gesamt",
                        onClick = { selectedTimeRange = "Gesamt" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Main Chart Card
            item {
                ChartCard(data = chartData)
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Durchschnitt",
                        value = "91 kg",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Maximum",
                        value = "95 kg",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Second Chart with different data
            item {
                val secondChartData = listOf(
                    ChartDataPoint("W1", 75f),
                    ChartDataPoint("W2", 78f),
                    ChartDataPoint("W3", 76f),
                    ChartDataPoint("W4", 80f)
                )
                ChartCard(
                    data = secondChartData,
                    title = "Kniebeugen: Gewicht - Letzte 30 Tage",
                    currentValue = "80kg",
                    percentChange = "+6.7%"
                )
            }

            // Empty State Card
            item {
                EmptyStateChartCard()
            }
        }
    }
}

@Composable
fun DropdownSelector(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { /* TODO: Show dropdown */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimeRangeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ChartCard(
    data: List<ChartDataPoint>,
    title: String = "Bankdrücken: Gewicht - Letzte 30 Tage",
    currentValue: String = "95kg",
    percentChange: String = "+9.2%"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last 30 Days",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = percentChange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line Chart
            LineChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (data.isEmpty()) return@Canvas

            val maxWeight = data.maxOf { it.weight }
            val minWeight = data.minOf { it.weight }
            val range = maxWeight - minWeight
            val padding = 40f

            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2
            val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)

            // Draw line
            val path = Path()
            data.forEachIndexed { index, point ->
                val x = padding + index * stepX
                val normalizedY = if (range > 0) (point.weight - minWeight) / range else 0.5f
                val y = size.height - padding - (normalizedY * chartHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f)
            )

            // Draw points
            data.forEachIndexed { index, point ->
                val x = padding + index * stepX
                val normalizedY = if (range > 0) (point.weight - minWeight) / range else 0.5f
                val y = size.height - padding - (normalizedY * chartHeight)

                drawCircle(
                    color = primaryColor,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }

        // Week labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { point ->
                Text(
                    text = point.week,
                    fontSize = 12.sp,
                    color = onSurfaceColor.copy(alpha = 0.5f)
                )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateChartCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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

            Text(
                text = "No Data Available",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "When no data is found for the selected filters, this message will be shown.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiagrammeScreenPreview() {
    KAMETheme {
        DiagrammeScreen()
    }
}