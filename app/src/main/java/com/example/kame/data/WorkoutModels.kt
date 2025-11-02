package com.example.kame.data

// Data classes für JSON-Serialisierung
data class PlannedSet(
    val setNumber: Int,
    val targetReps: Int,
    val targetWeight: Float
)

data class CompletedSet(
    val setNumber: Int,
    val reps: Int,
    val weight: Float,
    val completed: Boolean
)

data class WorkoutSession(
    val timestamp: String,
    val sets: List<CompletedSet>,
    val notes: String? = null
)

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val order: Int,
    val sets: List<PlannedSet>
)

data class Workout(
    val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val exerciseCount: Int,
    val createdAt: String,
    val lastPerformed: String?
)