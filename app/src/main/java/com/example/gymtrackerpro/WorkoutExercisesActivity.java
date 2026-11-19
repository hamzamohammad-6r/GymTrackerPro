package com.example.gymtrackerpro;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutExercisesActivity extends AppCompatActivity {

    private static final String TAG = "GymTrackerPro";

    private GymTrackerDatabase database;

    private Spinner spinnerWorkouts;
    private Spinner spinnerExercises;

    private EditText editSets;
    private EditText editRepetitions;
    private EditText editWeight;
    private EditText editExerciseNotes;

    private TextView textAssignedExercises;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    private List<Workout> workouts = new ArrayList<>();
    private List<Exercise> exercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiFx.applyEdgeToEdge(this);

        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(R.layout.activity_workout_exercises);
        TextView buttonBackArrow = findViewById(R.id.buttonBackArrow);

        buttonBackArrow.setOnClickListener(v -> finish());
        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        spinnerWorkouts = findViewById(R.id.spinnerWorkouts);
        spinnerExercises = findViewById(R.id.spinnerExercises);

        editSets = findViewById(R.id.editSets);
        editRepetitions = findViewById(R.id.editRepetitions);
        editWeight = findViewById(R.id.editWeight);
        editExerciseNotes = findViewById(R.id.editExerciseNotes);

        textAssignedExercises =
                findViewById(R.id.textAssignedExercises);

        Button buttonAddExercise =
                findViewById(R.id.buttonAddExerciseToWorkout);

        Button buttonBackHome =
                findViewById(R.id.buttonBackHome);

        buttonAddExercise.setOnClickListener(
                v -> saveExerciseToWorkout()
        );

        buttonBackHome.setOnClickListener(v -> finish());

        loadDropdowns();
    }

    private void loadDropdowns() {
        databaseExecutor.execute(() -> {
            try {
                List<Workout> loadedWorkouts =
                        database.workoutDao().getAllWorkouts();

                List<Exercise> loadedExercises =
                        database.exerciseDao().getAllExercises();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    workouts = loadedWorkouts;
                    exercises = loadedExercises;

                    List<String> workoutNames = new ArrayList<>();

                    for (Workout workout : workouts) {
                        workoutNames.add(
                                workout.getWorkoutName()
                                        + " (" + workout.getWorkoutDate() + ")"
                        );
                    }

                    if (workoutNames.isEmpty()) {
                        workoutNames.add("No workouts available");
                    }

                    ArrayAdapter<String> workoutAdapter =
                            new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    workoutNames
                            );

                    workoutAdapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerWorkouts.setAdapter(workoutAdapter);

                    List<String> exerciseNames = new ArrayList<>();

                    for (Exercise exercise : exercises) {
                        exerciseNames.add(
                                exercise.getExerciseName()
                                        + " (" + exercise.getMuscleGroup() + ")"
                        );
                    }

                    if (exerciseNames.isEmpty()) {
                        exerciseNames.add("No exercises available");
                    }

                    ArrayAdapter<String> exerciseAdapter =
                            new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    exerciseNames
                            );

                    exerciseAdapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerExercises.setAdapter(exerciseAdapter);

                    spinnerWorkouts.setOnItemSelectedListener(
                            new android.widget.AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        android.widget.AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id
                                ) {
                                    loadAssignedExercises();
                                }

                                @Override
                                public void onNothingSelected(
                                        android.widget.AdapterView<?> parent
                                ) {
                                }
                            }
                    );

                    loadAssignedExercises();
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load dropdowns", e);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(
                                this,
                                "Could not load workouts or exercises. "
                                        + "Add a workout and an exercise first.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    private void saveExerciseToWorkout() {
        if (workouts.isEmpty()) {
            Toast.makeText(
                    this,
                    "Create a workout first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (exercises.isEmpty()) {
            Toast.makeText(
                    this,
                    "Create an exercise first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String setsText = editSets.getText().toString().trim();
        String repetitionsText =
                editRepetitions.getText().toString().trim();
        String weightText = editWeight.getText().toString().trim();
        String notes = editExerciseNotes.getText().toString().trim();

        if (setsText.isEmpty()) {
            editSets.setError("Enter the number of sets");
            editSets.requestFocus();
            return;
        }

        if (repetitionsText.isEmpty()) {
            editRepetitions.setError("Enter the repetitions");
            editRepetitions.requestFocus();
            return;
        }

        if (weightText.isEmpty()) {
            weightText = "0";
        }

        final int sets;
        final int repetitions;
        final double weight;

        try {
            sets = Integer.parseInt(setsText);
            repetitions = Integer.parseInt(repetitionsText);
            weight = Double.parseDouble(weightText);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Enter valid numbers.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (sets <= 0 || repetitions <= 0 || weight < 0
                || !Double.isFinite(weight)) {
            Toast.makeText(
                    this,
                    "Sets and reps must be positive. Weight cannot be negative.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Workout selectedWorkout =
                workouts.get(spinnerWorkouts.getSelectedItemPosition());

        Exercise selectedExercise =
                exercises.get(spinnerExercises.getSelectedItemPosition());

        WorkoutExercise assignment = new WorkoutExercise(
                selectedWorkout.getWorkoutId(),
                selectedExercise.getExerciseId(),
                sets,
                repetitions,
                weight,
                notes
        );

        databaseExecutor.execute(() -> {
            try {
                database.workoutExerciseDao()
                        .insertWorkoutExercise(assignment);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    editSets.setText("");
                    editRepetitions.setText("");
                    editWeight.setText("");
                    editExerciseNotes.setText("");

                    Toast.makeText(
                            this,
                            "Exercise added to workout!",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadAssignedExercises();
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to assign exercise", e);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(
                                this,
                                "Could not add exercise. Check Logcat.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    private void loadAssignedExercises() {
        if (workouts.isEmpty()
                || spinnerWorkouts.getSelectedItemPosition() < 0) {
            textAssignedExercises.setText(
                    "Create a workout and an exercise first."
            );
            return;
        }

        Workout selectedWorkout =
                workouts.get(spinnerWorkouts.getSelectedItemPosition());

        databaseExecutor.execute(() -> {
            try {
                List<WorkoutExercise> assignments =
                        database.workoutExerciseDao()
                                .getExercisesForWorkout(
                                        selectedWorkout.getWorkoutId()
                                );

                StringBuilder result = new StringBuilder();

                if (assignments.isEmpty()) {
                    result.append(
                            "No exercises assigned to this workout yet."
                    );
                } else {
                    for (WorkoutExercise assignment : assignments) {
                        Exercise exercise = null;

                        for (Exercise item : exercises) {
                            if (item.getExerciseId()
                                    == assignment.getExerciseId()) {
                                exercise = item;
                                break;
                            }
                        }

                        if (exercise != null) {
                            result.append(exercise.getExerciseName())
                                    .append("\nSets: ")
                                    .append(assignment.getSets())
                                    .append(" | Reps: ")
                                    .append(assignment.getRepetitions())
                                    .append(" | Weight: ")
                                    .append(String.format(
                                            Locale.US,
                                            "%.2f kg",
                                            assignment.getWeight()
                                    ));

                            if (assignment.getNotes() != null
                                    && !assignment.getNotes().trim().isEmpty()) {
                                result.append("\nNotes: ")
                                        .append(assignment.getNotes());
                            }

                            result.append("\n\n");
                        }
                    }
                }

                String displayText = result.toString();

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        textAssignedExercises.setText(displayText);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load assigned exercises", e);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        textAssignedExercises.setText(
                                "Could not load assigned exercises."
                        );
                    }
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
