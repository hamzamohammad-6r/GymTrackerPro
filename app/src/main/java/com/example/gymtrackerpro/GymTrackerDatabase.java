package com.example.gymtrackerpro;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                User.class,
                Workout.class,
                Exercise.class,
                WorkoutExercise.class,
                Goal.class,
                WorkoutMusic.class,
                UserProgram.class
        },
        version = 5,
        exportSchema = false
)
public abstract class GymTrackerDatabase extends RoomDatabase {

    private static volatile GymTrackerDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_music` (" +
                            "`musicId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`workoutId` INTEGER NOT NULL, " +
                            "`songTitle` TEXT, " +
                            "`artist` TEXT, " +
                            "`songUri` TEXT, " +
                            "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`workoutId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_music_workoutId` " +
                            "ON `workout_music` (`workoutId`)"
            );
        }
    };
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(
                @NonNull SupportSQLiteDatabase database
        ) {
            database.execSQL(
                    "ALTER TABLE workouts ADD COLUMN dayOfWeek TEXT"
            );
        }
    };

    static final Migration MIGRATION_3_4 =
            new Migration(3, 4) {
                @Override
                public void migrate(
                        SupportSQLiteDatabase database) {

                    database.execSQL(
                            "ALTER TABLE workouts "
                                    + "ADD COLUMN completed INTEGER "
                                    + "NOT NULL DEFAULT 0"
                    );
                }
            };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_programs` (" +
                            "`userProgramId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`userId` INTEGER NOT NULL, " +
                            "`programId` TEXT, " +
                            "`startDate` TEXT, " +
                            "`active` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`userId`) REFERENCES `users`(`userId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_user_programs_userId` " +
                            "ON `user_programs` (`userId`)"
            );
        }
    };

    public abstract UserDao userDao();

    public abstract WorkoutDao workoutDao();

    public abstract ExerciseDao exerciseDao();

    public abstract WorkoutExerciseDao workoutExerciseDao();

    public abstract GoalDao goalDao();

    public abstract WorkoutMusicDao workoutMusicDao();

    public abstract UserProgramDao userProgramDao();

    public static GymTrackerDatabase getDatabase(Context context) {

        if (INSTANCE == null) {
            synchronized (GymTrackerDatabase.class) {

                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    GymTrackerDatabase.class,
                                    "gymtracker_database"
                            )
                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5
                            )
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
