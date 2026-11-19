package com.example.gymtrackerpro;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutHistory";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private TextView textHistorySummary;
    private TextView textWorkoutHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiFx.applyEdgeToEdge(this);

        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(R.layout.activity_workout_history);
        TextView buttonBackArrow = findViewById(R.id.buttonBackArrow);

        buttonBackArrow.setOnClickListener(v ->
                UiFx.finishWithTransition(this));

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        UiFx.finishWithTransition(
                                WorkoutHistoryActivity.this
                        );
                    }
                }
        );

        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext());

        databaseExecutor = Executors.newSingleThreadExecutor();

        textHistorySummary = findViewById(R.id.textHistorySummary);
        textWorkoutHistory = findViewById(R.id.textWorkoutHistory);

        findViewById(R.id.buttonHistoryBack)
                .setOnClickListener(v ->
                        UiFx.finishWithTransition(this));

        loadWorkoutHistory();

        // Bottom navigation
        View navHome = findViewById(R.id.navHome);
        View navProgress = findViewById(R.id.navProgress);
        View navTimer = findViewById(R.id.navTimer);
        View navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v ->
                openScreen(MainActivity.class));

        // Already on Progress screen
        navProgress.setOnClickListener(v -> {});

        navTimer.setOnClickListener(v ->
                openScreen(TimerActivity.class));

        navProfile.setOnClickListener(v ->
                openScreen(ProfileActivity.class));
    }

    private void openScreen(Class<?> activityClass) {
        Intent intent = new Intent(
                WorkoutHistoryActivity.this,
                activityClass
        );

        UiFx.startActivityWithTransition(this, intent);
    }


    private void loadWorkoutHistory() {
        databaseExecutor.execute(() -> {
            try {
                SharedPreferences session = getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

                String loggedInEmail = session.getString("email", null);

                List<Workout> workouts;

                if (loggedInEmail != null
                        && !loggedInEmail.trim().isEmpty()) {

                    User currentUser = database.userDao().getUserByEmail(
                            loggedInEmail.trim()
                    );

                    if (currentUser != null) {
                        workouts = database.workoutDao()
                                .getWorkoutsForUser(currentUser.getUserId());
                    } else {
                        workouts = new java.util.ArrayList<>();
                    }

                } else {
                    int guestUserId = LocalUser.getOrCreateId(database);

                    workouts = database.workoutDao()
                            .getWorkoutsForUser(guestUserId);
                }

                StringBuilder history = new StringBuilder();
                int completed = 0;
                int planned = 0;

                for (Workout workout : workouts) {

                    if (workout.isCompleted()) {
                        completed++;
                    } else {
                        planned++;
                    }

                    history.append(
                                    workout.isCompleted()
                                            ? "✓ COMPLETED\n"
                                            : "○ PLANNED\n"
                            )
                            .append(workout.getWorkoutName())
                            .append("\nDate: ")
                            .append(workout.getWorkoutDate());

                    String day = workout.getDayOfWeek();

                    if (day != null && !day.trim().isEmpty()) {
                        history.append("\nDay: ").append(day);
                    }

                    history.append("\n\n--------------------\n\n");
                }

                final int total = workouts.size();
                final int finalCompleted = completed;
                final int finalPlanned = planned;

                final String historyText = total == 0
                        ? "No completed or planned workouts yet. Start your training programme to begin your history."
                        : history.toString();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    textHistorySummary.setText(
                            "Total: " + total
                                    + " | Completed: " + finalCompleted
                                    + " | Planned: " + finalPlanned
                    );

                    textWorkoutHistory.setText(historyText);
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load workout history", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    textWorkoutHistory.setText(
                            "Unable to load workout history."
                    );
                });
            }
        });
    }


    private void markWorkoutCompleted(Workout workout) {
        if (workout.isCompleted()) {
            Toast.makeText(
                    this,
                    "This workout is already completed.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Complete Workout")
                .setMessage(
                        "Mark \"" + workout.getWorkoutName()
                                + "\" as completed?"
                )
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Complete", (dialog, which) -> {
                    databaseExecutor.execute(() -> {
                        try {
                            int updated = database.workoutDao()
                                    .updateWorkoutCompletion(
                                            workout.getWorkoutId(),
                                            true
                                    );

                            runOnUiThread(() -> {
                                if (updated > 0) {
                                    Toast.makeText(
                                            this,
                                            "Workout marked as completed!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    loadWorkoutHistory();
                                } else {
                                    Toast.makeText(
                                            this,
                                            "Workout could not be updated.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Unable to complete workout", e);

                            runOnUiThread(() ->
                                    Toast.makeText(
                                            this,
                                            "Error updating workout.",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                        }
                    });
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
