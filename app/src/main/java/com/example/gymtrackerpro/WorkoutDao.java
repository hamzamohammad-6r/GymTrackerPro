package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkoutDao {

    // CREATE: Insert a new workout
    @Insert
    long insertWorkout(Workout workout);

    // READ: Retrieve all workouts
    @Query("SELECT * FROM workouts ORDER BY workoutDate DESC")
    List<Workout> getAllWorkouts();

    // READ: Retrieve a workout by its ID
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId LIMIT 1")
    Workout getWorkoutById(int workoutId);

    // READ: Retrieve workouts belonging to a user
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY workoutDate DESC")
    List<Workout> getWorkoutsForUser(int userId);

    // UPDATE: Modify an existing workout
    @Update
    int updateWorkout(Workout workout);

    // DELETE: Remove a workout
    @Delete
    int deleteWorkout(Workout workout);

    // DELETE: Remove all workouts belonging to a user
    @Query("DELETE FROM workouts WHERE userId = :userId")
    int deleteWorkoutsForUser(int userId);

    // UPDATE: Mark a workout as completed or planned
    @Query("UPDATE workouts SET completed = :completed WHERE workoutId = :workoutId")
    int updateWorkoutCompletion(int workoutId, boolean completed);

    // READ: Next workout still to do. Upcoming/today first (soonest date),
    // then overdue ones (oldest first). Dates are stored as yyyy-MM-dd.
    @Query("SELECT * FROM workouts WHERE completed = 0 " +
            "ORDER BY (workoutDate < :today) ASC, workoutDate ASC LIMIT 1")
    Workout getNextPlannedWorkout(String today);


}
