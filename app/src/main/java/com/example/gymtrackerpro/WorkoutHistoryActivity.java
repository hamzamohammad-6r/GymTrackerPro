package com.example.gymtrackerpro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.activity_workout_history);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext());

        databaseExecutor = Executors.newSingleThreadExecutor();

        textHistorySummary = findViewById(R.id.textHistorySummary);
        textWorkoutHistory = findViewById(R.id.textWorkoutHistory);

        findViewById(R.id.buttonHistoryBack)
                .setOnClickListener(v -> finish());

        loadWorkoutHistory();
    }

    private void loadWorkoutHistory() {
        databaseExecutor.execute(() -> {
            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

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
                                            : "○ PLANNED\n")
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
                        ? "No workouts found yet. Create a workout to start your history."
                        : history.toString();

                runOnUiThread(() -> {
                    textHistorySummary.setText(
                            "Total: " + total
                                    + " | Completed: " + finalCompleted
                                    + " | Planned: " + finalPlanned);

                    textWorkoutHistory.setText(historyText);
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load workout history", e);

                runOnUiThread(() ->
                        textWorkoutHistory.setText(
                                "Unable to load workout history."));
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
