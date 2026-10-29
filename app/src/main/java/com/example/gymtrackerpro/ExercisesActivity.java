package com.example.gymtrackerpro;

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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExercisesActivity extends AppCompatActivity {

    private static final String TAG = "GymTrackerPro";

    private GymTrackerDatabase database;

    private EditText editExerciseName;
    private Spinner spinnerMuscleGroup;
    private TextView textExerciseList;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    private final String[] muscleGroups = {
            "Select muscle group",
            "Chest",
            "Back",
            "Shoulders",
            "Biceps",
            "Triceps",
            "Legs",
            "Abs",
            "Full Body",
            "Cardio",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercises);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        editExerciseName = findViewById(R.id.editExerciseName);
        spinnerMuscleGroup = findViewById(R.id.spinnerMuscleGroup);
        textExerciseList = findViewById(R.id.textExerciseList);

        Button buttonSaveExercise =
                findViewById(R.id.buttonSaveExercise);

        Button buttonBackHome =
                findViewById(R.id.buttonBackHome);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        muscleGroups
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMuscleGroup.setAdapter(adapter);

        buttonSaveExercise.setOnClickListener(
                v -> saveExercise()
        );

        buttonBackHome.setOnClickListener(
                v -> finish()
        );

        loadExercises();
    }

    private void saveExercise() {
        String exerciseName =
                editExerciseName.getText()
                        .toString()
                        .trim();

        String muscleGroup =
                spinnerMuscleGroup.getSelectedItem().toString();

        if (exerciseName.isEmpty()) {
            editExerciseName.setError(
                    "Enter an exercise name"
            );
            editExerciseName.requestFocus();
            return;
        }

        if (spinnerMuscleGroup.getSelectedItemPosition() == 0) {
            Toast.makeText(
                    this,
                    "Please select a muscle group",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                Exercise exercise = new Exercise(
                        exerciseName,
                        muscleGroup
                );

                database.exerciseDao().insertExercise(exercise);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    editExerciseName.setText("");
                    spinnerMuscleGroup.setSelection(0);

                    Toast.makeText(
                            ExercisesActivity.this,
                            "Exercise saved successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadExercises();
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to save exercise", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            ExercisesActivity.this,
                            "Could not save exercise. Check Logcat.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void loadExercises() {
        databaseExecutor.execute(() -> {
            try {
                List<Exercise> exercises =
                        database.exerciseDao().getAllExercises();

                StringBuilder displayText =
                        new StringBuilder();

                if (exercises.isEmpty()) {
                    displayText.append(
                            "No exercises saved yet. "
                                    + "Add your first exercise above."
                    );
                } else {
                    for (Exercise exercise : exercises) {
                        displayText
                                .append(exercise.getExerciseName())
                                .append("\nMuscle group: ")
                                .append(exercise.getMuscleGroup())
                                .append("\n\n");
                    }
                }

                String result = displayText.toString();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textExerciseList.setText(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load exercises", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textExerciseList.setText(
                            "Could not load exercises. "
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