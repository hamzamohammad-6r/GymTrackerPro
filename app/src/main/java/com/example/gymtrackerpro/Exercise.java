package com.example.gymtrackerpro;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exercises")
public class Exercise {

    @PrimaryKey(autoGenerate = true)
    private int exerciseId;

    private String exerciseName;
    private String muscleGroup;

    // Constructor
    public Exercise(String exerciseName, String muscleGroup) {
        this.exerciseName = exerciseName;
        this.muscleGroup = muscleGroup;
    }

    // Getters
    public int getExerciseId() {
        return exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    // Setters
    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public void setMuscleGroup(String muscleGroup) {
        this.muscleGroup = muscleGroup;
    }
}
