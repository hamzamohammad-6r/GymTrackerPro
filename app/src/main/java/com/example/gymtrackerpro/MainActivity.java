
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

    // Executes database work away from the main UI thread
    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the shared Room database instance
        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        // Test the database on a background thread
        testDatabaseConnection();
    }

    private void testDatabaseConnection() {

        databaseExecutor.execute(() -> {

            try {
                // Check whether a user already exists
                List<User> users = database.userDao().getAllUsers();

                // Insert a sample user only if the table is empty
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

                    Log.d(TAG, "Sample user inserted. ID: " + userId);
                }

                // Read the users from the database
                List<User> savedUsers =
                        database.userDao().getAllUsers();

                Log.d(TAG, "Database connected successfully.");
                Log.d(TAG, "Number of saved users: "
                        + savedUsers.size());

                for (User user : savedUsers) {
                    Log.d(TAG, "Saved user: " + user.getName());
                }

                // Show the result on the main UI thread
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop accepting new tasks when this activity is destroyed
        databaseExecutor.shutdown();
    }
}
