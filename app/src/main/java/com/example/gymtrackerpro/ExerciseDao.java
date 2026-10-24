package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExerciseDao {

    // CREATE: Add a new exercise
    @Insert
    long insertExercise(Exercise exercise);

    // READ: Retrieve all exercises
    @Query("SELECT * FROM exercises ORDER BY exerciseName ASC")
    List<Exercise> getAllExercises();

    // READ: Find an exercise by its ID
    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId LIMIT 1")
    Exercise getExerciseById(int exerciseId);

    // READ: Find exercises by muscle group
    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY exerciseName ASC")
    List<Exercise> getExercisesByMuscleGroup(String muscleGroup);

    // READ: Search exercises by name
    @Query("SELECT * FROM exercises WHERE exerciseName LIKE '%' || :searchText || '%' ORDER BY exerciseName ASC")
    List<Exercise> searchExercises(String searchText);

    // UPDATE: Modify an existing exercise
    @Update
    int updateExercise(Exercise exercise);

    // DELETE: Remove an exercise
    @Delete
    int deleteExercise(Exercise exercise);

    // DELETE: Remove all exercises
    @Query("DELETE FROM exercises")
    int deleteAllExercises();
}
