package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActiveWorkoutActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_ID = "workoutId";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private int workoutId;
    private int currentUserId;

    private Workout currentWorkout;

    private TextView textTitle;
    private TextView textDate;
    private TextView textProgress;
    private LinearLayout containerExercises;

    private Button buttonCompleteWorkout;
    private Button buttonWorkoutTimer;
    private Button buttonWorkoutMusic;

    private int totalExercises = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiFx.applyEdgeToEdge(this);
        getWindow().setStatusBarColor(
                android.graphics.Color.parseColor("#0B1120")
        );
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_active_workout);

        database = GymTrackerDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();

        workoutId = getIntent().getIntExtra(EXTRA_WORKOUT_ID, -1);

        textTitle = findViewById(R.id.textActiveWorkoutTitle);
        textDate = findViewById(R.id.textActiveWorkoutDate);
        textProgress = findViewById(R.id.textActiveWorkoutProgress);

        containerExercises = findViewById(R.id.containerActiveExercises);

        buttonCompleteWorkout = findViewById(R.id.buttonCompleteWorkout);
        buttonWorkoutTimer = findViewById(R.id.buttonOpenWorkoutTimer);
        buttonWorkoutMusic = findViewById(R.id.buttonWorkoutMusic);

        findViewById(R.id.buttonBackActiveWorkout)
                .setOnClickListener(v -> finish());

        buttonCompleteWorkout.setOnClickListener(
                v -> confirmWorkoutCompletion()
        );

        buttonWorkoutTimer.setOnClickListener(v -> {
            startActivity(new Intent(
                    ActiveWorkoutActivity.this,
                    TimerActivity.class
            ));
        });

        if (workoutId == -1) {
            showErrorAndClose("Workout session could not be found.");
            return;
        }

        loadWorkout();
    }


    private void loadWorkout() {

        databaseExecutor.execute(() -> {

            try {

                SharedPreferences preferences = getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

                String email = preferences.getString("email", "");

                if (email != null && !email.trim().isEmpty()) {

                    User user = database.userDao().getUserByEmail(
                            email.trim()
                    );

                    if (user == null) {
                        runOnUiThread(() ->
                                showErrorAndClose(
                                        "Your local profile could not be found."
                                )
                        );
                        return;
                    }

                    currentUserId = user.getUserId();

                } else {

                    currentUserId = LocalUser.getOrCreateId(database);
                }

                Workout workout = database.workoutDao()
                        .getWorkoutById(workoutId);

                if (workout == null
                        || workout.getUserId() != currentUserId) {

                    runOnUiThread(() ->
                            showErrorAndClose(
                                    "You do not have access to this workout."
                            )
                    );
                    return;
                }

                currentWorkout = workout;

                List<WorkoutExercise> workoutExercises =
                        database.workoutExerciseDao()
                                .getExercisesForWorkout(workoutId);

                // Retrieve exercise details on the background thread.
                List<Exercise> exercises = new java.util.ArrayList<>();

                for (WorkoutExercise workoutExercise : workoutExercises) {

                    Exercise exercise = database.exerciseDao()
                            .getExerciseById(
                                    workoutExercise.getExerciseId()
                            );

                    exercises.add(exercise);
                }

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (currentWorkout.isCompleted()) {
                        showErrorAndClose(
                                "This workout has already been completed."
                        );
                        return;
                    }

                    displayWorkout(
                            workout,
                            workoutExercises,
                            exercises
                    );
                });

            } catch (Exception e) {

                Log.e("ActiveWorkout", "Workout loading error", e);

                runOnUiThread(() ->
                        showErrorAndClose(
                                "Unable to load this workout. Please try again."
                        )
                );
            }
        });
    }



    private void displayWorkout(
            Workout workout,
            List<WorkoutExercise> workoutExercises,
            List<Exercise> exercises
    ) {

        textTitle.setText(workout.getWorkoutName());

        String date = workout.getWorkoutDate();

        textDate.setText(
                date + "  •  " + workout.getDayOfWeek()
        );

        containerExercises.removeAllViews();

        totalExercises = workoutExercises.size();

        textProgress.setText(
                totalExercises + " prescribed exercises"
        );

        if (workoutExercises.isEmpty()) {

            TextView empty = createText(
                    "No exercises have been added to this session.",
                    14,
                    "#94A3B8"
            );

            containerExercises.addView(empty);

            buttonCompleteWorkout.setEnabled(false);
            return;
        }

        for (int i = 0; i < workoutExercises.size(); i++) {

            WorkoutExercise workoutExercise = workoutExercises.get(i);
            Exercise exercise = exercises.get(i);

            if (exercise == null) {
                continue;
            }

            addExerciseCard(exercise, workoutExercise);
        }
    }


    private void addExerciseCard(
            Exercise exercise,
            WorkoutExercise workoutExercise
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = UiFx.dp(card, 12);

        card.setLayoutParams(cardParams);
        card.setBackgroundColor(
                android.graphics.Color.parseColor("#151F33")
        );

        TextView name = createText(
                exercise.getExerciseName(),
                18,
                "#F8FAFC"
        );

        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView muscle = createText(
                exercise.getMuscleGroup(),
                13,
                "#60A5FA"
        );

        muscle.setPadding(0, 6, 0, 8);

        String prescription =
                workoutExercise.getSets()
                        + " sets × "
                        + workoutExercise.getRepetitions()
                        + " reps";

        TextView details = createText(
                prescription,
                15,
                "#F8FAFC"
        );

        TextView notes = createText(
                workoutExercise.getNotes() == null
                        || workoutExercise.getNotes().trim().isEmpty()
                        ? "Follow the prescribed technique."
                        : workoutExercise.getNotes(),
                13,
                "#94A3B8"
        );

        notes.setPadding(0, 8, 0, 8);

        Button detailsButton = new Button(this);
        detailsButton.setText("View Exercise Details");
        detailsButton.setTextColor(
                android.graphics.Color.WHITE
        );
        detailsButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#1E3A8A")
                )
        );

        detailsButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ActiveWorkoutActivity.this,
                    ExerciseDetailActivity.class
            );

            intent.putExtra(
                    ExerciseDetailActivity.EXTRA_EXERCISE_NAME,
                    exercise.getExerciseName()
            );

            intent.putExtra(
                    ExerciseDetailActivity.EXTRA_MUSCLE_GROUP,
                    exercise.getMuscleGroup()
            );

            startActivity(intent);
        });

        card.addView(name);
        card.addView(muscle);
        card.addView(details);
        card.addView(notes);
        card.addView(detailsButton);

        containerExercises.addView(card);
    }

    private TextView createText(
            String value,
            int size,
            String color
    ) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(
                android.graphics.Color.parseColor(color)
        );

        return text;
    }

    private void confirmWorkoutCompletion() {

        new AlertDialog.Builder(this)
                .setTitle("Complete Workout")
                .setMessage(
                        "Have you finished all the prescribed exercises?"
                )
                .setNegativeButton("Not Yet", null)
                .setPositiveButton("Complete", (dialog, which) ->
                        completeWorkout()
                )
                .show();
    }

    private void completeWorkout() {

        buttonCompleteWorkout.setEnabled(false);

        databaseExecutor.execute(() -> {

            try {

                Workout latestWorkout = database.workoutDao()
                        .getWorkoutById(workoutId);

                if (latestWorkout == null
                        || latestWorkout.getUserId() != currentUserId) {

                    runOnUiThread(() ->
                            showErrorAndClose(
                                    "Workout session could not be verified."
                            )
                    );
                    return;
                }

                if (latestWorkout.isCompleted()) {

                    runOnUiThread(() ->
                            showErrorAndClose(
                                    "This workout has already been completed."
                            )
                    );
                    return;
                }

                int updated = database.workoutDao()
                        .updateWorkoutCompletion(workoutId, true);

                runOnUiThread(() -> {

                    if (updated > 0) {

                        Toast.makeText(
                                ActiveWorkoutActivity.this,
                                "Workout completed successfully!",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();

                    } else {

                        buttonCompleteWorkout.setEnabled(true);

                        Toast.makeText(
                                ActiveWorkoutActivity.this,
                                "Could not save workout completion.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    buttonCompleteWorkout.setEnabled(true);

                    Toast.makeText(
                            ActiveWorkoutActivity.this,
                            "An error occurred while saving your workout.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void showErrorAndClose(String message) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Workout Unavailable")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.e("ActiveWorkout", "Workout loading failed: " + message);
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
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
