
package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkoutMusicDao {

    // Add a song to a workout
    @Insert
    long insertMusic(WorkoutMusic music);

    // Get all songs assigned to one workout
    @Query("SELECT * FROM workout_music WHERE workoutId = :workoutId ORDER BY musicId DESC")
    List<WorkoutMusic> getMusicForWorkout(int workoutId);

    // Get all saved music assignments
    @Query("SELECT * FROM workout_music ORDER BY musicId DESC")
    List<WorkoutMusic> getAllMusic();

    // Update a song's details
    @Update
    int updateMusic(WorkoutMusic music);

    // Remove a song assignment
    @Delete
    int deleteMusic(WorkoutMusic music);

    // Remove all music assigned to a specific workout
    @Query("DELETE FROM workout_music WHERE workoutId = :workoutId")
    int deleteMusicForWorkout(int workoutId);
}
