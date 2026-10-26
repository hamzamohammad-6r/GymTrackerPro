
package com.example.gymtrackerpro;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GymTrackerPro";

    private GymTrackerDatabase database;

    // Background thread for database operations
    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop accepting new database tasks
        databaseExecutor.shutdown();
    }
}
