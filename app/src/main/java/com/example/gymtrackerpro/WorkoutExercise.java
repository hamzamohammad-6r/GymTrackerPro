package com.example.gymtrackerpro;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "workout_exercises",
        foreignKeys = {
                @ForeignKey(
                        entity = Workout.class,
                        parentColumns = "workoutId",
                        childColumns = "workoutId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Exercise.class,
                        parentColumns = "exerciseId",
                        childColumns = "exerciseId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("workoutId"),
                @Index("exerciseId")
        }
)
public class WorkoutExercise {

    @PrimaryKey(autoGenerate = true)
    private int workoutExerciseId;

    private int workoutId;
    private int exerciseId;
    private int sets;
    private int repetitions;
    private double weight;
    private String notes;

    // Constructor
    public WorkoutExercise(
            int workoutId,
            int exerciseId,
            int sets,
            int repetitions,
            double weight,
            String notes) {

        this.workoutId = workoutId;
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.repetitions = repetitions;
        this.weight = weight;
        this.notes = notes;
    }

    // Getters
    public int getWorkoutExerciseId() {
        return workoutExerciseId;
    }

    public int getWorkoutId() {
        return workoutId;
    }

    public int getExerciseId() {
        return exerciseId;
    }

    public int getSets() {
        return sets;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public double getWeight() {
        return weight;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setWorkoutExerciseId(int workoutExerciseId) {
        this.workoutExerciseId = workoutExerciseId;
    }

    public void setWorkoutId(int workoutId) {
        this.workoutId = workoutId;
    }

    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
