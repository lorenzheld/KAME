package com.example.kame.screens

import android.util.Log
import androidx.activity.compose.BackHandler
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
import com.example.kame.R
import com.example.kame.data.*
import com.example.kame.data.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    var sessionTimestamp by remember { mutableStateOf<String?>(null) }
    val sessionIds = remember { mutableStateMapOf<String, Long>() }

    BackHandler {
        showCancelDialog = true
    }

    // ---------------------------------------------------------
    // WICHTIGER FIX IM LADE-LOGIK BLOCK
    // ---------------------------------------------------------
    LaunchedEffect(workoutId, resumeSession) {
        try {
            workout = dao.getWorkout(workoutId).first()
            val exercises = dao.getExercisesForWorkout(workoutId).first()

            // 1. Leere States erstellen
            val initialStates = exercises.mapIndexed { index, exercise ->
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

            // 2. Bestimme den KORREKTEN Zeitstempel
            var masterTimestamp: String? = null

            // Wir suchen zuerst über ALLE Übungen hinweg nach dem allerneusten Eintrag
            if (resumeSession) {
                var latestSessionFound: WorkoutSessionEntity? = null

                for (ex in exercises) {
                    val sessions = dao.getSessionsForExercise(ex.id).first()
                    val first = sessions.firstOrNull()
                    if (first != null) {
                        // Wir suchen den aktuellsten Eintrag über alle Übungen hinweg
                        if (latestSessionFound == null || first.timestamp > latestSessionFound!!.timestamp) {
                            latestSessionFound = first
                        }
                    }
                }

                // Prüfen: Haben wir was gefunden UND ist es NICHT beendet?
                if (latestSessionFound != null && latestSessionFound.notes != "FINISHED") {
                    masterTimestamp = latestSessionFound.timestamp
                    Log.d("ActiveWorkout", "Resuming VALID session: $masterTimestamp")
                } else {
                    Log.d("ActiveWorkout", "Latest session was FINISHED or null. Starting NEW.")
                }
            }

            // Wenn wir keinen gültigen alten Zeitstempel haben, erstellen wir einen neuen
            if (masterTimestamp == null) {
                masterTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            }

            // Speichern für später
            sessionTimestamp = masterTimestamp

            // 3. Daten in die UI laden - ABER NUR wenn der Zeitstempel passt!
            val loadedStates = initialStates.map { state ->
                val sessions = dao.getSessionsForExercise(state.exercise.id).first()
                val lastSession = sessions.firstOrNull()

                // CHECK: Stimmt der Zeitstempel mit unserem Master-Zeitstempel überein?
                if (lastSession != null && lastSession.timestamp == masterTimestamp) {

                    // Ja, das gehört zu DIESEM Workout
                    sessionIds[state.exercise.id] = lastSession.id

                    state.sets.forEach { setUiState ->
                        val savedSet = lastSession.sets.find { it.setNumber == setUiState.setNumber }
                        if (savedSet != null && savedSet.completed) {
                            setUiState.actualReps = savedSet.reps.toString()
                            setUiState.actualWeight = savedSet.weight.toString()
                            setUiState.isCompleted = true
                        }
                    }
                } else {
                    // Nein, das ist entweder null oder ein ALTOS Workout (Zeitstempel passt nicht).
                    // Wir laden NICHTS, lassen die Felder leer und ID auf 0 (für neuen Insert).
                    // Das verhindert, dass alte Daten ("2/3 fertig") hier auftauchen.
                }
                state
            }

            exerciseStates = loadedStates
            isLoading = false
        } catch (e: Exception) {
            Log.e("ActiveWorkout", "ERROR loading data", e)
            isLoading = false
        }
    }

    // Auto-Save
    DisposableEffect(Unit) {
        onDispose {
            if (!hasSavedManually && sessionTimestamp != null) {
                val hasCompletedSets = exerciseStates.any { it.completedCount > 0 }
                if (hasCompletedSets) {
                    scope.launch {
                        saveWorkoutSession(dao, exerciseStates, workoutId, sessionTimestamp!!, sessionIds, isFinished = false)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = workout?.name ?: "Laden...",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!isLoading && exerciseStates.isNotEmpty()) {
                            Text(
                                text = "${currentExerciseIndex + 1} / ${exerciseStates.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                modifier = Modifier.fillMaxSize().padding(padding),
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
                            exerciseStates.forEachIndexed { i, state ->
                                if (i != index) state.isExpanded = false
                            }
                            exerciseState.isExpanded = !exerciseState.isExpanded
                        },
                        onSetComplete = { setIndex ->
                            val set = exerciseState.sets[setIndex]
                            set.isCompleted = !set.isCompleted

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

                item {
                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
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

        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Workout beenden?") },
                text = { Text("Möchtest du das Workout speichern und beenden?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFinishDialog = false
                            hasSavedManually = true
                            scope.launch {
                                if (sessionTimestamp != null) {
                                    saveWorkoutSession(dao, exerciseStates, workoutId, sessionTimestamp!!, sessionIds, isFinished = true)
                                }
                                onFinish()
                            }
                        }
                    ) { Text("Speichern & Beenden") }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishDialog = false }) { Text("Abbrechen") }
                }
            )
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Training abbrechen?") },
                text = { Text("Dein bisheriger Fortschritt wird gespeichert.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            hasSavedManually = true
                            scope.launch {
                                if (sessionTimestamp != null) {
                                    saveWorkoutSession(dao, exerciseStates, workoutId, sessionTimestamp!!, sessionIds, isFinished = false)
                                }
                                onFinish()
                            }
                        }
                    ) { Text("Speichern & Beenden") }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) { Text("Zurück") }
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

            AnimatedVisibility(visible = exerciseState.isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set", Modifier.width(40.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Ziel", Modifier.width(70.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Gewicht (kg)", Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Wdh.", Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    exerciseState.sets.forEachIndexed { index, setState ->
                        SetRow(setState = setState, onComplete = { onSetComplete(index) })
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
        Text("${setState.setNumber}", Modifier.width(40.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("${setState.targetReps}×${setState.targetWeight.toInt()}kg", Modifier.width(70.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        var weightFocused by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = setState.actualWeight,
            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) setState.actualWeight = it },
            modifier = Modifier.width(100.dp).height(56.dp).onFocusChanged { weightFocused = it.isFocused },
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
            enabled = !setState.isCompleted
        )

        var repsFocused by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = setState.actualReps,
            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d+$"))) setState.actualReps = it },
            modifier = Modifier.width(80.dp).height(56.dp).onFocusChanged { repsFocused = it.isFocused },
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            enabled = !setState.isCompleted
        )

        IconButton(
            onClick = onComplete,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(if (setState.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
        ) {
            Icon(Icons.Default.Check, "Complete", tint = if (setState.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

suspend fun saveWorkoutSession(
    dao: WorkoutDao,
    exerciseStates: List<ExerciseState>,
    workoutId: String,
    timestamp: String,
    sessionIds: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Long>,
    isFinished: Boolean
) {
    try {
        Log.d("ActiveWorkout", "=== SAVING SESSION (Finished: $isFinished) ===")
        val existingExercises = try { dao.getExercisesForWorkout(workoutId).first().map { it.id }.toSet() } catch(e: Exception) { emptySet() }

        exerciseStates.forEach { exerciseState ->
            if (!existingExercises.contains(exerciseState.exercise.id)) return@forEach

            val completedSets = exerciseState.sets.mapNotNull { setState ->
                if (setState.isCompleted && setState.actualReps.isNotBlank() && setState.actualWeight.isNotBlank()) {
                    CompletedSet(
                        setNumber = setState.setNumber,
                        reps = setState.actualReps.toIntOrNull() ?: setState.targetReps,
                        weight = setState.actualWeight.toFloatOrNull() ?: setState.targetWeight,
                        completed = true
                    )
                } else null
            }

            if (completedSets.isNotEmpty()) {
                val existingId = sessionIds[exerciseState.exercise.id] ?: 0L

                val session = WorkoutSessionEntity(
                    id = existingId,
                    exerciseId = exerciseState.exercise.id,
                    timestamp = timestamp,
                    sets = completedSets,
                    notes = if (isFinished) "FINISHED" else null
                )

                val newId = dao.insertSession(session)
                sessionIds[exerciseState.exercise.id] = newId
            }
        }

        val workout = dao.getWorkout(workoutId).first()
        workout?.let { dao.updateWorkout(it.copy(lastPerformed = timestamp)) }

        Log.d("ActiveWorkout", "=== SAVED SUCCESSFULLY ===")
    } catch (e: Exception) {
        Log.e("ActiveWorkout", "ERROR saving", e)
    }
}