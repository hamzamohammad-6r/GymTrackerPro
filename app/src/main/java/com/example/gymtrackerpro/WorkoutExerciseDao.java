package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkoutExerciseDao {

    // CREATE: Add an exercise to a workout
    @Insert
    long insertWorkoutExercise(WorkoutExercise workoutExercise);

    // READ: Retrieve all workout-exercise records
    @Query("SELECT * FROM workout_exercises ORDER BY workoutExerciseId DESC")
    List<WorkoutExercise> getAllWorkoutExercises();

    // READ: Retrieve exercises belonging to one workout
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId")
    List<WorkoutExercise> getExercisesForWorkout(int workoutId);

    // READ: Retrieve workout records containing a particular exercise
    @Query("SELECT * FROM workout_exercises WHERE exerciseId = :exerciseId")
    List<WorkoutExercise> getWorkoutsForExercise(int exerciseId);

    // READ: Find a record by its ID
    @Query("SELECT * FROM workout_exercises WHERE workoutExerciseId = :id LIMIT 1")
    WorkoutExercise getWorkoutExerciseById(int id);

    // UPDATE: Change sets, repetitions, weight or notes
    @Update
    int updateWorkoutExercise(WorkoutExercise workoutExercise);

    // DELETE: Remove an exercise record from a workout
    @Delete
    int deleteWorkoutExercise(WorkoutExercise workoutExercise);

    // DELETE: Remove all exercise records for one workout
    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    int deleteExercisesForWorkout(int workoutId);

    // READ: How many exercises are assigned to a workout
    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId = :workoutId")
    int countExercisesForWorkout(int workoutId);
}
