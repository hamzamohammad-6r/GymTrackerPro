
package com.example.gymtrackerpro;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "workout_music",
        foreignKeys = {
                @ForeignKey(
                        entity = Workout.class,
                        parentColumns = "workoutId",
                        childColumns = "workoutId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("workoutId")}
)
public class WorkoutMusic {

    @PrimaryKey(autoGenerate = true)
    private int musicId;

    private int workoutId;
    private String songTitle;
    private String artist;
    private String songUri;

    public WorkoutMusic(
            int workoutId,
            String songTitle,
            String artist,
            String songUri
    ) {
        this.workoutId = workoutId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.songUri = songUri;
    }

    public int getMusicId() {
        return musicId;
    }

    public void setMusicId(int musicId) {
        this.musicId = musicId;
    }

    public int getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(int workoutId) {
        this.workoutId = workoutId;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSongUri() {
        return songUri;
    }

    public void setSongUri(String songUri) {
        this.songUri = songUri;
    }
}
