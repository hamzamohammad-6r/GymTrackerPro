package com.example.gymtrackerpro;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "goals",
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "userId",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("userId")}
)
public class Goal {

    @PrimaryKey(autoGenerate = true)
    private int goalId;

    private int userId;
    private String goalType;
    private double targetValue;
    private double currentValue;
    private String targetDate;

    // Constructor
    public Goal(
            int userId,
            String goalType,
            double targetValue,
            double currentValue,
            String targetDate) {

        this.userId = userId;
        this.goalType = goalType;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.targetDate = targetDate;
    }

    // Getters
    public int getGoalId() {
        return goalId;
    }

    public int getUserId() {
        return userId;
    }

    public String getGoalType() {
        return goalType;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public String getTargetDate() {
        return targetDate;
    }

    // Setters
    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }
}
