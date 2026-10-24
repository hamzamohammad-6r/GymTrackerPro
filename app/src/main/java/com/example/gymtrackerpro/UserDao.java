package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {

    // CREATE: Insert a new user
    @Insert
    long insertUser(User user);

    // READ: Retrieve all users
    @Query("SELECT * FROM users ORDER BY userId DESC")
    List<User> getAllUsers();

    // READ: Retrieve one user by ID
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    User getUserById(int userId);

    // READ: Find a user by email
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    // UPDATE: Update an existing user
    @Update
    int updateUser(User user);

    // DELETE: Delete a user
    @Delete
    int deleteUser(User user);

    // DELETE: Delete all users
    @Query("DELETE FROM users")
    int deleteAllUsers();
}
