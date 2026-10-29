package com.example.gymtrackerpro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private TextView textTotalWorkouts;
    private TextView textCompletedWorkouts;
    private TextView textPlannedWorkouts;
    private TextView textCompletionRate;

    private CardView cardWorkouts;
    private CardView cardExercises;
    private CardView cardWorkoutExercises;
    private CardView cardWorkoutMusic;
    private CardView cardGoals;
    private CardView cardProfile;
    private CardView cardWorkoutHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        databaseExecutor = Executors.newSingleThreadExecutor();

        initializeViews();
        setupNavigation();
        loadWorkoutStatistics();
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {

        textTotalWorkouts = findViewById(R.id.textTotalWorkouts);
        textCompletedWorkouts = findViewById(R.id.textCompletedWorkouts);
        textPlannedWorkouts = findViewById(R.id.textPlannedWorkouts);
        textCompletionRate = findViewById(R.id.textCompletionRate);

        cardWorkouts = findViewById(R.id.cardWorkouts);
        cardExercises = findViewById(R.id.cardExercises);
        cardWorkoutExercises = findViewById(R.id.cardWorkoutExercises);
        cardWorkoutMusic = findViewById(R.id.cardWorkoutMusic);
        cardGoals = findViewById(R.id.cardGoals);
        cardProfile = findViewById(R.id.cardProfile);

        // Optional: only available if this card exists in activity_main.xml.
        cardWorkoutHistory = findViewById(R.id.textWorkoutHistory);
    }

    private void setupNavigation() {

        // Workout planner and calendar.
        if (cardWorkouts != null) {
            cardWorkouts.setOnClickListener(v ->
                    openActivity(WorkoutsActivity.class));
        }

        // Exercise library.
        if (cardExercises != null) {
            cardExercises.setOnClickListener(v ->
                    openActivity(ExercisesActivity.class));
        }

        // Assign exercises, sets and reps to a workout.
        if (cardWorkoutExercises != null) {
            cardWorkoutExercises.setOnClickListener(v ->
                    openActivity(WorkoutExercisesActivity.class));
        }

        // Workout history.
        if (cardWorkoutHistory != null) {
            cardWorkoutHistory.setOnClickListener(v ->
                    openActivity(WorkoutHistoryActivity.class));
        }

        // Optional screens. These use their class names so the
        // project can still compile if a screen has not been created.
        if (cardGoals != null) {
            cardGoals.setOnClickListener(v ->
                    openOptionalActivity("GoalsActivity"));
        }

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v ->
                    openOptionalActivity("ProfileActivity"));
        }

        if (cardWorkoutMusic != null) {
            cardWorkoutMusic.setOnClickListener(v ->
                    openOptionalActivity("WorkoutMusicActivity"));
        }
    }

    private void openActivity(
            Class<? extends AppCompatActivity> activityClass) {

        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

    private void openOptionalActivity(String className) {
        try {
            Class<?> activityClass = Class.forName(
                    getPackageName() + "." + className
            );

            if (!AppCompatActivity.class.isAssignableFrom(activityClass)) {
                Toast.makeText(
                        this,
                        "This screen is not configured correctly.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            @SuppressWarnings("unchecked")
            Class<? extends AppCompatActivity> target =
                    (Class<? extends AppCompatActivity>) activityClass;

            openActivity(target);

        } catch (ClassNotFoundException e) {
            Toast.makeText(
                    this,
                    className.replace("Activity", "")
                            + " screen has not been created yet.",
                    Toast.LENGTH_SHORT
            ).show();

            Log.w(TAG, "Optional activity not found: " + className, e);
        }
    }

    private void loadWorkoutStatistics() {

        databaseExecutor.execute(() -> {

            int total = 0;
            int completed = 0;
            int planned = 0;
            int completionRate = 0;

            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                if (workouts != null) {
                    total = workouts.size();

                    for (Workout workout : workouts) {
                        if (workout != null && workout.isCompleted()) {
                            completed++;
                        }
                    }
                }

                planned = total - completed;

                if (total > 0) {
                    completionRate = Math.round(
                            (completed * 100.0f) / total
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Unable to load workout statistics", e);
            }

            final int finalTotal = total;
            final int finalCompleted = completed;
            final int finalPlanned = planned;
            final int finalRate = completionRate;

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (textTotalWorkouts != null) {
                    textTotalWorkouts.setText(
                            String.valueOf(finalTotal)
                    );
                }

                if (textCompletedWorkouts != null) {
                    textCompletedWorkouts.setText(
                            String.valueOf(finalCompleted)
                    );
                }

                if (textPlannedWorkouts != null) {
                    textPlannedWorkouts.setText(
                            String.valueOf(finalPlanned)
                    );
                }

                if (textCompletionRate != null) {
                    textCompletionRate.setText(
                            finalRate + "%"
                    );
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh statistics after returning from another screen.
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            loadWorkoutStatistics();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
