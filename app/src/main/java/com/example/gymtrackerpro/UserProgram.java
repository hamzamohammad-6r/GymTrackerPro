package com.example.gymtrackerpro;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "user_programs",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = {"userId"}, unique = true)
)
public class UserProgram {

    @PrimaryKey(autoGenerate = true)
    private int userProgramId;

    private int userId;
    private String programId;
    private String startDate;
    private boolean active;

    public UserProgram(int userId, String programId,
                       String startDate, boolean active) {
        this.userId = userId;
        this.programId = programId;
        this.startDate = startDate;
        this.active = active;
    }

    public int getUserProgramId() {
        return userProgramId;
    }

    public int getUserId() {
        return userId;
    }

    public String getProgramId() {
        return programId;
    }

    public String getStartDate() {
        return startDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setUserProgramId(int userProgramId) {
        this.userProgramId = userProgramId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
