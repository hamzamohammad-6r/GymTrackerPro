package com.example.gymtrackerpro;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExercisesActivity extends AppCompatActivity {

    private static final String TAG = "GymTrackerPro";

    private GymTrackerDatabase database;

    private EditText editExerciseName;
    private EditText editSearchExercise;
    private Spinner spinnerMuscleGroup;
    private Button buttonToggleAdd;
    private View cardAddExercise;
    private LinearLayout containerExercises;
    private TextView textExerciseEmpty;
    private TextView textExerciseCount;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    private final List<Exercise> allExercises = new ArrayList<>();

    /** null means "All". */
    private String selectedGroup = null;

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
        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        editExerciseName = findViewById(R.id.editExerciseName);
        editSearchExercise = findViewById(R.id.editSearchExercise);
        spinnerMuscleGroup = findViewById(R.id.spinnerMuscleGroup);
        buttonToggleAdd = findViewById(R.id.buttonToggleAdd);
        cardAddExercise = findViewById(R.id.cardAddExercise);
        containerExercises = findViewById(R.id.containerExercises);
        textExerciseEmpty = findViewById(R.id.textExerciseEmpty);
        textExerciseCount = findViewById(R.id.textExerciseCount);

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

        buttonToggleAdd.setOnClickListener(v -> toggleAddForm());

        buttonSaveExercise.setOnClickListener(
                v -> saveExercise()
        );

        buttonBackHome.setOnClickListener(
                v -> finish()
        );

        setupFilterChips();
        setupSearch();

        loadExercises();
    }

    private void toggleAddForm() {
        boolean show = cardAddExercise.getVisibility() != View.VISIBLE;

        cardAddExercise.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonToggleAdd.setText(show ? "Close" : "+  Add custom exercise");

        if (show) {
            editExerciseName.requestFocus();
        }
    }

    private void setupFilterChips() {
        final ChipGroup chipGroup = findViewById(R.id.chipGroupMuscle);

        // First chip is "All", then one chip per muscle group
        // (index 0 of muscleGroups is just the spinner placeholder).
        addChip(chipGroup, "All", null, true);

        for (int i = 1; i < muscleGroups.length; i++) {
            addChip(chipGroup, muscleGroups[i], muscleGroups[i], false);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }

            View checked = group.findViewById(checkedIds.get(0));
            Object tag = checked == null ? null : checked.getTag();

            selectedGroup = tag instanceof String ? (String) tag : null;
            renderExercises();
        });
    }

    private void addChip(ChipGroup group, String label,
                         String tag, boolean checked) {

        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.item_chip, group, false);

        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(tag);
        chip.setChecked(checked);

        group.addView(chip);
    }

    private void setupSearch() {
        editSearchExercise.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                renderExercises();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
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

        for (Exercise existing : allExercises) {
            if (existing.getExerciseName() != null
                    && existing.getExerciseName()
                    .equalsIgnoreCase(exerciseName)) {

                editExerciseName.setError(
                        "This exercise is already in your library"
                );
                editExerciseName.requestFocus();
                return;
            }
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

                    cardAddExercise.setVisibility(View.GONE);
                    buttonToggleAdd.setText("+  Add custom exercise");

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
                final List<Exercise> exercises =
                        database.exerciseDao().getAllExercises();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    allExercises.clear();

                    if (exercises != null) {
                        allExercises.addAll(exercises);
                    }

                    renderExercises();
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load exercises", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    containerExercises.removeAllViews();
                    textExerciseEmpty.setVisibility(View.VISIBLE);
                    textExerciseEmpty.setText(
                            "Could not load exercises. Check Logcat."
                    );
                });
            }
        });
    }

    /** Applies the search text and muscle filter, then draws the cards. */
    private void renderExercises() {
        String query = editSearchExercise.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        List<Exercise> visible = new ArrayList<>();

        for (Exercise exercise : allExercises) {
            String name = exercise.getExerciseName() == null
                    ? "" : exercise.getExerciseName();

            boolean matchesSearch = query.isEmpty()
                    || name.toLowerCase(Locale.ROOT).contains(query);

            boolean matchesGroup = selectedGroup == null
                    || selectedGroup.equals(exercise.getMuscleGroup());

            if (matchesSearch && matchesGroup) {
                visible.add(exercise);
            }
        }

        containerExercises.removeAllViews();

        for (Exercise exercise : visible) {
            containerExercises.addView(createExerciseCard(exercise));
        }

        // Count + empty states
        if (visible.size() == allExercises.size()) {
            textExerciseCount.setText(allExercises.size() + " exercises");
        } else {
            textExerciseCount.setText(
                    visible.size() + " of " + allExercises.size()
                            + " exercises");
        }

        if (visible.isEmpty()) {
            textExerciseEmpty.setVisibility(View.VISIBLE);
            textExerciseEmpty.setText(allExercises.isEmpty()
                    ? "No exercises yet.\nTap \"Add custom exercise\" to add your first one."
                    : "No exercises match your search.");
        } else {
            textExerciseEmpty.setVisibility(View.GONE);
        }
    }

    private View createExerciseCard(final Exercise exercise) {
        View card = getLayoutInflater().inflate(
                R.layout.item_exercise, containerExercises, false);

        MuscleIcons.bind(
                (ImageView) card.findViewById(R.id.imageMuscle),
                exercise.getMuscleGroup());

        ((TextView) card.findViewById(R.id.textExName))
                .setText(exercise.getExerciseName());

        TextView group = card.findViewById(R.id.textExGroup);
        group.setText(exercise.getMuscleGroup());
        group.setTextColor(MuscleIcons.colorFor(exercise.getMuscleGroup()));

        card.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(
                            ExercisesActivity.this,
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


        card.setOnLongClickListener(v -> {
            confirmDelete(exercise);
            return true;
        });

        return card;
    }

    private void confirmDelete(final Exercise exercise) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete exercise?")
                .setMessage("\"" + exercise.getExerciseName()
                        + "\" will also be removed from any workouts "
                        + "that use it.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete",
                        (dialog, which) -> deleteExercise(exercise))
                .show();
    }

    private void deleteExercise(final Exercise exercise) {
        databaseExecutor.execute(() -> {
            boolean deleted = false;

            try {
                deleted = database.exerciseDao()
                        .deleteExercise(exercise) > 0;
            } catch (Exception e) {
                Log.e(TAG, "Unable to delete exercise", e);
            }

            final boolean success = deleted;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                Toast.makeText(
                        this,
                        success ? "Exercise deleted."
                                : "Could not delete exercise.",
                        Toast.LENGTH_SHORT
                ).show();

                if (success) {
                    loadExercises();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}
