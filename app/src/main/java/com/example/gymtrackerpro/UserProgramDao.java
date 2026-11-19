package com.example.gymtrackerpro;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserProgramDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrReplace(UserProgram userProgram);

    @Query("SELECT * FROM user_programs WHERE userId = :userId LIMIT 1")
    UserProgram getProgramForUser(int userId);

    @Update
    int updateProgram(UserProgram userProgram);

    @Query("UPDATE user_programs SET active = :active WHERE userId = :userId")
    int updateActiveStatus(int userId, boolean active);
}
