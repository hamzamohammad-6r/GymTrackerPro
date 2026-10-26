
package com.example.gymtrackerpro;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutsActivity extends AppCompatActivity {

    private static final String TAG = "GymTrackerPro";

    private static final String LOCAL_USER_EMAIL =
            "gymtrackerpro@local.test";

    private GymTrackerDatabase database;

    private EditText editWorkoutName;
    private EditText editWorkoutDate;
    private TextView textWorkoutList;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workouts);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        editWorkoutName = findViewById(R.id.editWorkoutName);
        editWorkoutDate = findViewById(R.id.editWorkoutDate);
        textWorkoutList = findViewById(R.id.textWorkoutList);

        Button buttonSaveWorkout =
                findViewById(R.id.buttonSaveWorkout);

        Button buttonBackHome =
                findViewById(R.id.buttonBackHome);

        // Open the calendar when the date field is tapped.
        editWorkoutDate.setOnClickListener(v -> showDatePicker());

        // Save a new workout.
        buttonSaveWorkout.setOnClickListener(v -> saveWorkout());

        // Return to the home screen.
        buttonBackHome.setOnClickListener(v -> finish());

        // Display previously saved workouts.
        loadWorkouts();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Use the currently selected date if one exists.
        String currentDate = editWorkoutDate.getText()
                .toString().trim();

        if (currentDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                int year = Integer.parseInt(
                        currentDate.substring(0, 4)
                );
                int month = Integer.parseInt(
                        currentDate.substring(5, 7)
                ) - 1;
                int day = Integer.parseInt(
                        currentDate.substring(8, 10)
                );

                calendar.set(year, month, day);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Could not read selected date", e);
            }
        }

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {
                            String selectedDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    year,
                                    month + 1,
                                    dayOfMonth
                            );

                            editWorkoutDate.setText(selectedDate);
                            editWorkoutDate.setError(null);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.show();
    }

    private int getOrCreateUserId() {
        User user = database.userDao()
                .getUserByEmail(LOCAL_USER_EMAIL);

        if (user == null) {
            User newUser = new User(
                    "GymTrackerPro User",
                    LOCAL_USER_EMAIL,
                    0,
                    0.0,
                    0.0,
                    "General fitness"
            );

            long newUserId =
                    database.userDao().insertUser(newUser);

            return (int) newUserId;
        }

        return user.getUserId();
    }

    private void saveWorkout() {
        String workoutName =
                editWorkoutName.getText().toString().trim();

        String workoutDate =
                editWorkoutDate.getText().toString().trim();

        if (workoutName.isEmpty()) {
            editWorkoutName.setError(
                    "Enter a workout name"
            );
            editWorkoutName.requestFocus();
            return;
        }

        if (workoutDate.isEmpty()
                || !workoutDate.matches(
                "\\d{4}-\\d{2}-\\d{2}"
        )) {
            editWorkoutDate.setError(
                    "Please select a date from the calendar"
            );
            Toast.makeText(
                    this,
                    "Select a workout date first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                int userId = getOrCreateUserId();

                Workout workout = new Workout(
                        userId,
                        workoutName,
                        workoutDate
                );

                database.workoutDao().insertWorkout(workout);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    editWorkoutName.setText("");
                    editWorkoutDate.setText("");

                    Toast.makeText(
                            WorkoutsActivity.this,
                            "Workout saved successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadWorkouts();
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to save workout", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            WorkoutsActivity.this,
                            "Could not save workout. Check Logcat.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void loadWorkouts() {
        databaseExecutor.execute(() -> {
            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                StringBuilder displayText = new StringBuilder();

                if (workouts.isEmpty()) {
                    displayText.append(
                            "No workouts saved yet. "
                                    + "Add your first workout above."
                    );
                } else {
                    for (Workout workout : workouts) {
                        displayText
                                .append(workout.getWorkoutName())
                                .append("\nDate: ")
                                .append(workout.getWorkoutDate())
                                .append("\n\n");
                    }
                }

                String result = displayText.toString();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textWorkoutList.setText(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load workouts", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textWorkoutList.setText(
                            "Could not load workouts. "
                                    + "Check Logcat."
                    );
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}
