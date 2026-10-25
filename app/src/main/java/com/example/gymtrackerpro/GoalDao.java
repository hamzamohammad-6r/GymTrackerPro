
package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GoalDao {

    // CREATE: Add a new fitness goal
    @Insert
    long insertGoal(Goal goal);

    // READ: Retrieve all fitness goals
    @Query("SELECT * FROM goals ORDER BY goalId DESC")
    List<Goal> getAllGoals();

    // READ: Find a goal by its ID
    @Query("SELECT * FROM goals WHERE goalId = :goalId LIMIT 1")
    Goal getGoalById(int goalId);

    // READ: Retrieve goals belonging to a particular user
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY goalId DESC")
    List<Goal> getGoalsForUser(int userId);

    // UPDATE: Modify an existing fitness goal
    @Update
    int updateGoal(Goal goal);

    // DELETE: Remove a fitness goal
    @Delete
    int deleteGoal(Goal goal);

    // DELETE: Remove all goals belonging to a user
    @Query("DELETE FROM goals WHERE userId = :userId")
    int deleteGoalsForUser(int userId);
}
