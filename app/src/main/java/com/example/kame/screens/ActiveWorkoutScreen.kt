package com.example.kame.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.kame.data.*
import com.example.kame.data.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// State für einen einzelnen Satz
@Stable
class SetState(
    val setNumber: Int,
    val targetReps: Int,
    val targetWeight: Float,
    actualReps: String = "",
    actualWeight: String = "",
    isCompleted: Boolean = false
) {
    var actualReps by mutableStateOf(actualReps)
    var actualWeight by mutableStateOf(actualWeight)
    var isCompleted by mutableStateOf(isCompleted)
}

// State für eine Übung
@Stable
class ExerciseState(
    val exercise: ExerciseEntity,
    val sets: List<SetState>,
    isExpanded: Boolean = true
) {
    var isExpanded by mutableStateOf(isExpanded)

    val completedCount: Int
        get() = sets.count { it.isCompleted }

    val isFullyCompleted: Boolean
        get() = sets.all { it.isCompleted }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workoutId: String,
    resumeSession: Boolean = false,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = lifecycleOwner.lifecycleScope
    val database = remember { WorkoutDatabase.getDatabase(context) }
    val dao = database.workoutDao()

    var workout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var exerciseStates by remember { mutableStateOf<List<ExerciseState>>(emptyList()) }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var hasSavedManually by remember { mutableStateOf(false) }

    // Daten laden
    LaunchedEffect(workoutId, resumeSession) {
        try {
            Log.d("ActiveWorkout", "Loading workout: $workoutId, resume: $resumeSession")

            workout = dao.getWorkout(workoutId).first()
            Log.d("ActiveWorkout", "Workout: ${workout?.name}")

            val exercises = dao.getExercisesForWorkout(workoutId).first()
            Log.d("ActiveWorkout", "Exercises: ${exercises.size}")

            // IMMER frisch starten (Resume temporär deaktiviert)
            exerciseStates = exercises.mapIndexed { index, exercise ->
                ExerciseState(
                    exercise = exercise,
                    sets = exercise.sets.map { plannedSet ->
                        SetState(
                            setNumber = plannedSet.setNumber,
                            targetReps = plannedSet.targetReps,
                            targetWeight = plannedSet.targetWeight,
                            actualReps = plannedSet.targetReps.toString(),
                            actualWeight = plannedSet.targetWeight.toString(),
                            isCompleted = false
                        )
                    },
                    isExpanded = index == 0
                )
            }

            Log.d("ActiveWorkout", "States created: ${exerciseStates.size}")
            isLoading = false
            Log.d("ActiveWorkout", "DONE")
        } catch (e: Exception) {
            Log.e("ActiveWorkout", "ERROR", e)
            isLoading = false
        }
    }

    // Auto-Save beim Verlassen
    DisposableEffect(Unit) {
        onDispose {
            // NUR auto-save wenn NICHT manuell gespeichert wurde
            if (!hasSavedManually) {
                val hasCompletedSets = exerciseStates.any { exerciseState ->
                    exerciseState.sets.any { it.isCompleted }
                }

                if (hasCompletedSets) {
                    scope.launch {
                        try {
                            Log.d("ActiveWorkout", "Auto-saving on dispose...")
                            saveWorkoutSession(dao, exerciseStates, workoutId)
                            Log.d("ActiveWorkout", "Auto-save successful")
                        } catch (e: Exception) {
                            Log.e("ActiveWorkout", "Error auto-saving", e)
                        }
                    }
                } else {
                    Log.d("ActiveWorkout", "No completed sets, skipping auto-save")
                }
            } else {
                Log.d("ActiveWorkout", "Already saved manually, skipping auto-save")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = workout?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${currentExerciseIndex + 1} / ${exerciseStates.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Close, "Abbrechen")
                    }
                },
                actions = {
                    IconButton(onClick = { showFinishDialog = true }) {
                        Icon(Icons.Default.Check, "Beenden")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading || exerciseStates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(exerciseStates, key = { _, ex -> ex.exercise.id }) { index, exerciseState ->
                    ExerciseCard(
                        exerciseState = exerciseState,
                        isActive = index == currentExerciseIndex,
                        onToggleExpand = {
                            // Alle anderen Fenster schließen
                            exerciseStates.forEachIndexed { i, state ->
                                if (i != index) {
                                    state.isExpanded = false
                                }
                            }
                            exerciseState.isExpanded = !exerciseState.isExpanded
                        },
                        onSetComplete = { setIndex ->
                            val set = exerciseState.sets[setIndex]
                            set.isCompleted = !set.isCompleted

                            // Auto-advance wenn alle Sätze fertig
                            if (exerciseState.isFullyCompleted &&
                                index == currentExerciseIndex &&
                                currentExerciseIndex < exerciseStates.size - 1) {

                                exerciseState.isExpanded = false
                                currentExerciseIndex++
                                exerciseStates[currentExerciseIndex].isExpanded = true
                            }
                        }
                    )
                }

                // Finish Button
                item {
                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Workout beenden", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cancel Dialog
        if (showCancelDialog) {
            val completedSets = remember(exerciseStates) {
                exerciseStates.sumOf { it.completedCount }
            }
            val totalSets = remember(exerciseStates) {
                exerciseStates.sumOf { it.sets.size }
            }

            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Workout abbrechen?") },
                text = {
                    if (completedSets > 0) {
                        Text("Du hast $completedSets von $totalSets Sätzen abgeschlossen.\n\nDein Fortschritt wird automatisch gespeichert.")
                    } else {
                        Text("Du hast noch keine Sätze abgeschlossen.\n\nMöchtest du wirklich abbrechen?")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            if (completedSets > 0) {
                                hasSavedManually = true
                                scope.launch {
                                    saveWorkoutSession(dao, exerciseStates, workoutId)
                                }
                            }
                            onFinish()
                        }
                    ) {
                        Text(if (completedSets > 0) "Speichern & Beenden" else "Ja, abbrechen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Zurück zum Training")
                    }
                }
            )
        }

        // Finish Dialog
        if (showFinishDialog) {
            val completedSets = remember(exerciseStates) {
                exerciseStates.sumOf { it.completedCount }
            }
            val totalSets = remember(exerciseStates) {
                exerciseStates.sumOf { it.sets.size }
            }

            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Workout beenden?") },
                text = {
                    Text("Du hast $completedSets von $totalSets Sätzen abgeschlossen.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFinishDialog = false
                            hasSavedManually = true
                            scope.launch {
                                saveWorkoutSession(dao, exerciseStates, workoutId)
                            }
                            onFinish()
                        }
                    ) {
                        Text("Speichern & Beenden")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}

@Composable
fun ExerciseCard(
    exerciseState: ExerciseState,
    isActive: Boolean,
    onToggleExpand: () -> Unit,
    onSetComplete: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseState.exercise.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = exerciseState.exercise.muscleGroups.joinToString(", "),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Completion Badge
                val completedSets = exerciseState.completedCount
                val totalSets = exerciseState.sets.size

                Surface(
                    shape = CircleShape,
                    color = if (completedSets == totalSets)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "$completedSets/$totalSets",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (completedSets == totalSets)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (exerciseState.isExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle"
                    )
                }
            }

            // Sets
            AnimatedVisibility(visible = exerciseState.isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Set",
                            modifier = Modifier.width(40.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Ziel",
                            modifier = Modifier.width(70.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Gewicht (kg)",
                            modifier = Modifier.width(100.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Wdh.",
                            modifier = Modifier.width(80.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    exerciseState.sets.forEachIndexed { index, setState ->
                        SetRow(
                            setState = setState,
                            onComplete = { onSetComplete(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetRow(
    setState: SetState,
    onComplete: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number
        Text(
            text = "${setState.setNumber}",
            modifier = Modifier.width(40.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Target
        Text(
            text = "${setState.targetReps}×${setState.targetWeight.toInt()}kg",
            modifier = Modifier.width(70.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Weight Input
        var weightFocused by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = setState.actualWeight,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    setState.actualWeight = newValue
                }
            },
            modifier = Modifier
                .width(100.dp)
                .height(56.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !weightFocused) {
                        weightFocused = true
                    } else if (!focusState.isFocused) {
                        weightFocused = false
                    }
                },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }
            ),
            enabled = !setState.isCompleted,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Reps Input
        var repsFocused by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = setState.actualReps,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                    setState.actualReps = newValue
                }
            },
            modifier = Modifier
                .width(80.dp)
                .height(56.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !repsFocused) {
                        repsFocused = true
                    } else if (!focusState.isFocused) {
                        repsFocused = false
                    }
                },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            enabled = !setState.isCompleted,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Check Button
        IconButton(
            onClick = onComplete,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (setState.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surface
                )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete",
                tint = if (setState.isCompleted)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Session in DB speichern
suspend fun saveWorkoutSession(
    dao: WorkoutDao,
    exerciseStates: List<ExerciseState>,
    workoutId: String
) {
    try {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        Log.d("ActiveWorkout", "=== START SAVING SESSION ===")
        Log.d("ActiveWorkout", "WorkoutId: $workoutId")
        Log.d("ActiveWorkout", "Timestamp: $now")

        var totalSavedSets = 0

        exerciseStates.forEach { exerciseState ->
            Log.d("ActiveWorkout", "Processing exercise: ${exerciseState.exercise.name}")
            Log.d("ActiveWorkout", "Exercise ID: ${exerciseState.exercise.id}")

            // Prüfe ob Exercise in DB existiert
            val exerciseExists = try {
                val exercises = dao.getExercisesForWorkout(workoutId).first()
                val exists = exercises.any { it.id == exerciseState.exercise.id }
                Log.d("ActiveWorkout", "Exercise exists in DB: $exists")
                exists
            } catch (e: Exception) {
                Log.e("ActiveWorkout", "Error checking exercise existence", e)
                false
            }

            if (!exerciseExists) {
                Log.e("ActiveWorkout", "ERROR: Exercise ${exerciseState.exercise.id} does NOT exist in DB!")
                return@forEach // Skip this exercise
            }

            val completedSets = exerciseState.sets.mapNotNull { setState ->
                if (setState.isCompleted &&
                    setState.actualReps.isNotBlank() &&
                    setState.actualWeight.isNotBlank()) {
                    CompletedSet(
                        setNumber = setState.setNumber,
                        reps = setState.actualReps.toIntOrNull() ?: setState.targetReps,
                        weight = setState.actualWeight.toFloatOrNull() ?: setState.targetWeight,
                        completed = true
                    )
                } else null
            }

            if (completedSets.isNotEmpty()) {
                Log.d("ActiveWorkout", "Saving ${completedSets.size} sets for exercise: ${exerciseState.exercise.name}")

                val session = WorkoutSessionEntity(
                    exerciseId = exerciseState.exercise.id,
                    timestamp = now,
                    sets = completedSets,
                    notes = null
                )

                Log.d("ActiveWorkout", "Session entity: $session")

                try {
                    dao.insertSession(session)
                    Log.d("ActiveWorkout", "✅ Session inserted successfully!")
                    totalSavedSets += completedSets.size
                } catch (e: Exception) {
                    Log.e("ActiveWorkout", "❌ ERROR inserting session", e)
                    throw e
                }
            } else {
                Log.d("ActiveWorkout", "No completed sets for this exercise, skipping")
            }
        }

        Log.d("ActiveWorkout", "Total saved sets: $totalSavedSets")

        // Update lastPerformed nur wenn mindestens ein Satz gespeichert wurde
        if (totalSavedSets > 0) {
            val workout = dao.getWorkout(workoutId).first()
            Log.d("ActiveWorkout", "Workout from DB: $workout")
            workout?.let {
                dao.updateWorkout(it.copy(lastPerformed = now))  // ✅ CHANGED: updateWorkout statt insertWorkout!
                Log.d("ActiveWorkout", "✅ Updated lastPerformed for workout")
            }
        }

        Log.d("ActiveWorkout", "=== END SAVING SESSION ===")
    } catch (e: Exception) {
        Log.e("ActiveWorkout", "=== ERROR in saveWorkoutSession ===", e)
        throw e
    }
}