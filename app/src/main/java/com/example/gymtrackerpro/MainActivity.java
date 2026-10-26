
package com.example.gymtrackerpro;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GymTrackerPro";

    private GymTrackerDatabase database;
    private android.widget.TextView textTotalWorkouts;
    private android.widget.TextView textCompletedWorkouts;
    private android.widget.TextView textPlannedWorkouts;
    private android.widget.TextView textCompletionRate;
    private android.widget.TextView textWeeklyWorkoutCount;
    private android.widget.TextView textWeeklyCompletedCount;
    private android.widget.TextView textWeeklyTarget;
    private android.widget.ProgressBar progressWeeklyWorkouts;

    private static final int WEEKLY_WORKOUT_TARGET = 5;

    // Background thread for database operations
    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textTotalWorkouts = findViewById(R.id.textTotalWorkouts);
        textCompletedWorkouts = findViewById(R.id.textCompletedWorkouts);
        textPlannedWorkouts = findViewById(R.id.textPlannedWorkouts);
        textCompletionRate = findViewById(R.id.textCompletionRate);
        androidx.cardview.widget.CardView cardWorkoutExercises = findViewById(R.id.cardWorkoutExercises);
        textWeeklyWorkoutCount = findViewById(R.id.textWeeklyWorkoutCount);
        textWeeklyCompletedCount = findViewById(R.id.textWeeklyCompletedCount);
        textWeeklyTarget = findViewById(R.id.textWeeklyTarget);
        progressWeeklyWorkouts = findViewById(R.id.progressWeeklyWorkouts);

        cardWorkoutExercises.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    WorkoutExercisesActivity.class
            );
            startActivity(intent);
        });

        findViewById(R.id.cardWorkoutMusic).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            WorkoutMusicActivity.class
                    );

            startActivity(intent);
        });

        // Connect to the Room database
        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        // Test the database connection
        testDatabaseConnection();

        // Set up home screen card click listeners
        setupHomeScreenClickListeners();
    }

    // Test Room by inserting and reading a sample user
    private void testDatabaseConnection() {

        databaseExecutor.execute(() -> {

            try {
                // Get all existing users
                List<User> users =
                        database.userDao().getAllUsers();

                // Insert a sample user if the table is empty
                if (users.isEmpty()) {

                    User sampleUser = new User(
                            "Test User",
                            "test@example.com",
                            21,
                            175.0,
                            65.0,
                            "Build muscle"
                    );

                    long userId =
                            database.userDao().insertUser(sampleUser);

                    Log.d(TAG,
                            "Sample user inserted. ID: " + userId);
                }

                // Read users from the database
                List<User> savedUsers =
                        database.userDao().getAllUsers();

                Log.d(TAG, "Database connected successfully.");
                Log.d(TAG, "Number of saved users: "
                        + savedUsers.size());

                for (User user : savedUsers) {
                    Log.d(TAG,
                            "Saved user: " + user.getName());
                }

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "GymTrackerPro database connected!",
                                Toast.LENGTH_LONG
                        ).show()
                );

            } catch (Exception e) {

                Log.e(TAG, "Database test failed", e);

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Database test failed. Check Logcat.",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }
        });
    }

    // Make the four dashboard cards respond to taps

    private void setupHomeScreenClickListeners() {

        // My Workouts card
        findViewById(R.id.cardWorkouts).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            WorkoutsActivity.class
                    );

            startActivity(intent);
        });

        // Exercises card
        findViewById(R.id.cardExercises).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            ExercisesActivity.class
                    );

            startActivity(intent);
        });

        // My Fitness Goals card
        findViewById(R.id.cardGoals).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            GoalsActivity.class
                    );
            startActivity(intent);
        });

        // My Profile card

        findViewById(R.id.cardProfile).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            ProfileActivity.class
                    );

            startActivity(intent);
        });



        findViewById(R.id.cardWorkoutExercises).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            MainActivity.this,
                            WorkoutExercisesActivity.class
                    );

            startActivity(intent);
        });

    }

    private void loadWorkoutStatistics() {
        databaseExecutor.execute(() -> {
            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                int total = workouts.size();
                int completed = 0;

                for (Workout workout : workouts) {
                    if (workout.isCompleted()) {
                        completed++;
                    }
                }

                int planned = total - completed;

                int completionRate = total == 0
                        ? 0
                        : (completed * 100) / total;

                final int finalTotal = total;
                final int finalCompleted = completed;
                final int finalPlanned = planned;
                final int finalCompletionRate = completionRate;

                runOnUiThread(() -> {
                    textTotalWorkouts.setText(
                            "Total Workouts: " + finalTotal);

                    textCompletedWorkouts.setText(
                            "Completed Workouts: " + finalCompleted);

                    textPlannedWorkouts.setText(
                            "Planned Workouts: " + finalPlanned);

                    textCompletionRate.setText(
                            "Completion Rate: " + finalCompletionRate + "%");
                });


            } catch (Exception e) {
                Log.e(TAG, "Failed to load workout statistics", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (database != null
                && textTotalWorkouts != null
                && textCompletedWorkouts != null
                && textPlannedWorkouts != null
                && textCompletionRate != null) {
            loadWorkoutStatistics();
            loadWeeklyProgress();
        }
    }


    private void loadWeeklyProgress() {
        databaseExecutor.execute(() -> {
            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                Calendar calendar = Calendar.getInstance();
                int daysSinceMonday =
                        (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday);

                // Find Monday of the current week.
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                Date monday = calendar.getTime();

                // Find next Monday (exclusive end date).
                calendar.add(Calendar.DAY_OF_YEAR, 7);
                Date nextMonday = calendar.getTime();

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                int weeklyTotal = 0;
                int weeklyCompleted = 0;

                for (Workout workout : workouts) {
                    String workoutDate = workout.getWorkoutDate();

                    if (workoutDate == null || workoutDate.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        Date date = dateFormat.parse(workoutDate);

                        if (date != null
                                && !date.before(monday)
                                && date.before(nextMonday)) {
                            weeklyTotal++;

                            if (workout.isCompleted()) {
                                weeklyCompleted++;
                            }
                        }
                    } catch (java.text.ParseException ignored) {
                        Log.w(TAG, "Skipping invalid workout date: " + workoutDate);
                    }
                }

                final int finalWeeklyTotal = weeklyTotal;
                final int finalWeeklyCompleted = weeklyCompleted;

                runOnUiThread(() -> {
                    textWeeklyWorkoutCount.setText(
                            "Workouts this week: " + finalWeeklyTotal);

                    textWeeklyCompletedCount.setText(
                            "Completed this week: " + finalWeeklyCompleted);

                    int progress = Math.min(
                            finalWeeklyCompleted, WEEKLY_WORKOUT_TARGET);

                    textWeeklyTarget.setText(
                            "Weekly target: " + progress + " / "
                                    + WEEKLY_WORKOUT_TARGET);

                    progressWeeklyWorkouts.setMax(WEEKLY_WORKOUT_TARGET);
                    progressWeeklyWorkouts.setProgress(progress);
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to load weekly progress", e);
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop accepting new database tasks
        databaseExecutor.shutdown();
    }
}
