
package com.example.gymtrackerpro;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                User.class,
                Workout.class,
                Exercise.class,
                WorkoutExercise.class,
                Goal.class
        },
        version = 1,
        exportSchema = false
)
public abstract class GymTrackerDatabase extends RoomDatabase {

    private static volatile GymTrackerDatabase INSTANCE;

    // Connect the database to the User DAO
    public abstract UserDao userDao();

    // Connect the database to the Workout DAO
    public abstract WorkoutDao workoutDao();

    // Connect the database to the Exercise DAO
    public abstract ExerciseDao exerciseDao();

    // Connect the database to the WorkoutExercise DAO
    public abstract WorkoutExerciseDao workoutExerciseDao();

    // Connect the database to the Goal DAO
    public abstract GoalDao goalDao();

    // Create or return the single database instance
    public static GymTrackerDatabase getDatabase(Context context) {

        if (INSTANCE == null) {
            synchronized (GymTrackerDatabase.class) {

                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            GymTrackerDatabase.class,
                            "gymtracker_database"
                    ).build();
                }
            }
        }

        return INSTANCE;
    }
}
