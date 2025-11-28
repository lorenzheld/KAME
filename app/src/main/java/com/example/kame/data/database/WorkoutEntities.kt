package com.example.kame.data.database

import androidx.room.*
import com.example.kame.data.*

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroups: List<String>,
    val exerciseCount: Int,
    val createdAt: String,
    val lastPerformed: String?
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val name: String,
    val muscleGroups: List<String>,
    val order: Int,
    val sets: List<PlannedSet>
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.NO_ACTION  // ✅ CHANGED: Kein CASCADE!
        )
    ],
    indices = [Index("exerciseId")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val timestamp: String,
    val sets: List<CompletedSet>,
    val notes: String?
)