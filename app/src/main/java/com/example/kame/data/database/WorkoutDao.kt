package com.example.kame.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // ========== Workouts ==========

    @Query("SELECT * FROM workouts ORDER BY lastPerformed DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkout(workoutId: String): Flow<WorkoutEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    // ========== Exercises ==========

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY `order` ASC")
    fun getExercisesForWorkout(workoutId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExercise(exerciseId: String): Flow<ExerciseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    // ========== Sessions ==========

    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun getSessionsForExercise(exerciseId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessionsForExercise(exerciseId: String, limit: Int): List<WorkoutSessionEntity>

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE exerciseId = :exerciseId")
    suspend fun deleteSessionsForExercise(exerciseId: String)

    // ========== Letzte Session für ein Workout ==========

    @Query("""
        SELECT ws.* FROM workout_sessions ws
        INNER JOIN exercises e ON ws.exerciseId = e.id
        WHERE e.workoutId = :workoutId
        ORDER BY ws.timestamp DESC
        LIMIT 1
    """)
    suspend fun getLastSessionForWorkout(workoutId: String): WorkoutSessionEntity?

    // ========== Alle letzten Sessions für ein Workout (pro Übung die neueste) ==========

    @Query("""
        SELECT ws.* FROM workout_sessions ws
        INNER JOIN exercises e ON ws.exerciseId = e.id
        WHERE e.workoutId = :workoutId
        AND ws.timestamp = (
            SELECT MAX(timestamp) 
            FROM workout_sessions ws2 
            WHERE ws2.exerciseId = ws.exerciseId
        )
    """)
    suspend fun getLatestSessionsForWorkout(workoutId: String): List<WorkoutSessionEntity>
}