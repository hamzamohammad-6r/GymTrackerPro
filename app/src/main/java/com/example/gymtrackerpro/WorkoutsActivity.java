
package com.example.gymtrackerpro;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutsActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutsActivity";
    private static final String LOCAL_USER_EMAIL =
            "gymtrackerpro@local.test";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private EditText editWorkoutName;
    private EditText editWorkoutDate;

    private Spinner spinnerWorkoutDay;
    private Spinner spinnerSelectWorkout;

    private TextView textWorkoutList;

    private Button buttonSaveWorkout;
    private Button buttonBackHome;
    private Button buttonEditWorkout;
    private Button buttonDeleteWorkout;
    private Button buttonToggleCompletion;

    private final String[] daysOfWeek = {
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday"
    };

    private final List<Workout> selectableWorkouts =
            new ArrayList<>();

    private boolean editingWorkout = false;
    private int editingWorkoutId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workouts);
        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext());

        databaseExecutor = Executors.newSingleThreadExecutor();

        editWorkoutName = findViewById(R.id.editWorkoutName);
        editWorkoutDate = findViewById(R.id.editWorkoutDate);

        spinnerWorkoutDay = findViewById(R.id.spinnerWorkoutDay);
        spinnerSelectWorkout = findViewById(R.id.spinnerSelectWorkout);

        textWorkoutList = findViewById(R.id.textWorkoutList);

        buttonSaveWorkout = findViewById(R.id.buttonSaveWorkout);
        buttonBackHome = findViewById(R.id.buttonBackHome);
        buttonEditWorkout = findViewById(R.id.buttonEditWorkout);
        buttonDeleteWorkout = findViewById(R.id.buttonDeleteWorkout);
        buttonToggleCompletion =
                findViewById(R.id.buttonToggleCompletion);

        // Set up weekday spinner.
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                daysOfWeek);

        dayAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinnerWorkoutDay.setAdapter(dayAdapter);

        // Date picker.
        editWorkoutDate.setFocusable(false);
        editWorkoutDate.setClickable(true);
        editWorkoutDate.setOnClickListener(v -> showDatePicker());

        // Buttons.
        buttonSaveWorkout.setOnClickListener(v -> saveWorkout());

        buttonEditWorkout.setOnClickListener(
                v -> editSelectedWorkout());

        buttonDeleteWorkout.setOnClickListener(
                v -> confirmDeleteWorkout());

        buttonToggleCompletion.setOnClickListener(
                v -> toggleWorkoutCompletion());

        buttonBackHome.setOnClickListener(v -> finish());

        spinnerSelectWorkout.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {
                        updateCompletionButton();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                        buttonToggleCompletion.setText(
                                "Mark as Completed");
                    }
                });

        loadWorkouts();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String existingDate =
                editWorkoutDate.getText().toString().trim();

        if (existingDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = existingDate.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                day = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Could not parse date", e);
            }
        }

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay);

                    editWorkoutDate.setText(date);
                },
                year,
                month,
                day);

        picker.show();
    }

    private int getOrCreateUserId() {
        User user = database.userDao()
                .getUserByEmail(LOCAL_USER_EMAIL);

        if (user != null) {
            return user.getUserId();
        }

        User newUser = new User(
                "GymTrackerPro User",
                LOCAL_USER_EMAIL,
                0,
                0.0,
                0.0,
                "General fitness");

        return (int) database.userDao().insertUser(newUser);
    }

    private void saveWorkout() {
        String name = editWorkoutName.getText()
                .toString().trim();

        String date = editWorkoutDate.getText()
                .toString().trim();

        Object dayObject = spinnerWorkoutDay.getSelectedItem();

        if (name.isEmpty()) {
            editWorkoutName.setError("Enter a workout name");
            editWorkoutName.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            editWorkoutDate.setError("Select a workout date");
            editWorkoutDate.requestFocus();
            return;
        }

        if (dayObject == null) {
            Toast.makeText(this,
                    "Select a weekday.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String day = dayObject.toString();

        boolean updating = editingWorkout;
        int selectedId = editingWorkoutId;

        buttonSaveWorkout.setEnabled(false);

        databaseExecutor.execute(() -> {
            boolean success = false;
            String error = null;

            try {
                if (updating) {
                    Workout workout = database.workoutDao()
                            .getWorkoutById(selectedId);

                    if (workout != null) {
                        workout.setWorkoutName(name);
                        workout.setWorkoutDate(date);
                        workout.setDayOfWeek(day);

                        success = database.workoutDao()
                                .updateWorkout(workout) > 0;
                    } else {
                        error = "Workout not found.";
                    }
                } else {
                    int userId = getOrCreateUserId();

                    Workout workout = new Workout(
                            userId, name, date);

                    workout.setDayOfWeek(day);
                    workout.setCompleted(false);

                    success = database.workoutDao()
                            .insertWorkout(workout) > 0;
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to save workout", e);
                error = "Database error while saving workout.";
            }

            final boolean saved = success;
            final String errorMessage = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                buttonSaveWorkout.setEnabled(true);

                if (saved) {
                    editingWorkout = false;
                    editingWorkoutId = -1;

                    buttonSaveWorkout.setText("Save Workout");

                    editWorkoutName.setText("");
                    editWorkoutDate.setText("");
                    spinnerWorkoutDay.setSelection(0);

                    Toast.makeText(
                            this,
                            updating
                                    ? "Workout updated."
                                    : "Workout saved.",
                            Toast.LENGTH_SHORT).show();

                    loadWorkouts();
                } else {
                    Toast.makeText(
                            this,
                            errorMessage != null
                                    ? errorMessage
                                    : "Could not save workout.",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadWorkouts() {
        databaseExecutor.execute(() -> {
            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                StringBuilder display = new StringBuilder();

                if (workouts.isEmpty()) {
                    display.append("No workouts saved yet.\n")
                            .append("Add your first workout above.");
                } else {
                    for (String day : daysOfWeek) {
                        boolean headingAdded = false;

                        for (Workout workout : workouts) {
                            String workoutDay =
                                    workout.getDayOfWeek();

                            if (workoutDay != null
                                    && workoutDay.equalsIgnoreCase(day)) {

                                if (!headingAdded) {
                                    display.append(day)
                                            .append("\n")
                                            .append("--------------------")
                                            .append("\n");

                                    headingAdded = true;
                                }

                                appendWorkout(display, workout);
                            }
                        }
                    }

                    boolean unassignedHeadingAdded = false;

                    for (Workout workout : workouts) {
                        String day = workout.getDayOfWeek();

                        if (day == null || day.trim().isEmpty()) {
                            if (!unassignedHeadingAdded) {
                                display.append("No day assigned")
                                        .append("\n")
                                        .append("--------------------")
                                        .append("\n");

                                unassignedHeadingAdded = true;
                            }

                            appendWorkout(display, workout);
                        }
                    }
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textWorkoutList.setText(display.toString());

                    selectableWorkouts.clear();
                    selectableWorkouts.addAll(workouts);

                    List<String> labels = new ArrayList<>();

                    for (Workout workout : workouts) {
                        String day = workout.getDayOfWeek();

                        if (day == null || day.trim().isEmpty()) {
                            day = "No day assigned";
                        }

                        String status = workout.isCompleted()
                                ? "Completed"
                                : "Planned";

                        labels.add(
                                status + " - " + day + " - "
                                        + workout.getWorkoutName()
                                        + " (" + workout.getWorkoutDate()
                                        + ")");
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    WorkoutsActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    labels);

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);

                    spinnerSelectWorkout.setAdapter(adapter);

                    boolean editingStillExists = false;

                    for (Workout workout : workouts) {
                        if (workout.getWorkoutId()
                                == editingWorkoutId) {
                            editingStillExists = true;
                            break;
                        }
                    }

                    if (editingWorkout && !editingStillExists) {
                        editingWorkout = false;
                        editingWorkoutId = -1;
                        buttonSaveWorkout.setText("Save Workout");
                    }

                    updateCompletionButton();
                });

            } catch (Exception e) {
                Log.e(TAG, "Unable to load workouts", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textWorkoutList.setText(
                            "Could not load workouts. Check Logcat.");
                });
            }
        });
    }

    private void appendWorkout(
            StringBuilder display, Workout workout) {

        display.append(workout.isCompleted()
                        ? "✓ COMPLETED\n"
                        : "○ PLANNED\n")
                .append("• ")
                .append(workout.getWorkoutName())
                .append("\nDate: ")
                .append(workout.getWorkoutDate())
                .append("\n\n");
    }

    private Workout getSelectedWorkout() {
        int position =
                spinnerSelectWorkout.getSelectedItemPosition();

        if (position < 0
                || position >= selectableWorkouts.size()) {
            Toast.makeText(this,
                    "Please select a workout first.",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        return selectableWorkouts.get(position);
    }

    private void updateCompletionButton() {
        if (buttonToggleCompletion == null) {
            return;
        }

        Workout workout = getSelectedWorkoutWithoutToast();

        if (workout == null) {
            buttonToggleCompletion.setText("Mark as Completed");
            buttonToggleCompletion.setEnabled(false);
            return;
        }

        buttonToggleCompletion.setEnabled(true);

        buttonToggleCompletion.setText(
                workout.isCompleted()
                        ? "Mark as Planned"
                        : "Mark as Completed");
    }

    private Workout getSelectedWorkoutWithoutToast() {
        int position =
                spinnerSelectWorkout.getSelectedItemPosition();

        if (position < 0
                || position >= selectableWorkouts.size()) {
            return null;
        }

        return selectableWorkouts.get(position);
    }

    private void editSelectedWorkout() {
        Workout workout = getSelectedWorkout();

        if (workout == null) {
            return;
        }

        editingWorkout = true;
        editingWorkoutId = workout.getWorkoutId();

        editWorkoutName.setText(workout.getWorkoutName());
        editWorkoutDate.setText(workout.getWorkoutDate());

        String savedDay = workout.getDayOfWeek();
        int position = 0;

        if (savedDay != null) {
            for (int i = 0; i < daysOfWeek.length; i++) {
                if (daysOfWeek[i].equalsIgnoreCase(savedDay)) {
                    position = i;
                    break;
                }
            }
        }

        spinnerWorkoutDay.setSelection(position);
        buttonSaveWorkout.setText("Save Changes");

        Toast.makeText(this,
                "Edit the fields, then tap Save Changes.",
                Toast.LENGTH_LONG).show();
    }

    private void confirmDeleteWorkout() {
        Workout workout = getSelectedWorkout();

        if (workout == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Workout")
                .setMessage(
                        "Delete \"" + workout.getWorkoutName()
                                + "\"?\n\nThis cannot be undone. "
                                + "Assigned music may also be deleted.")
                .setNegativeButton("Cancel",
                        (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete",
                        (dialog, which) -> deleteWorkout(workout))
                .show();
    }

    private void deleteWorkout(Workout workout) {
        databaseExecutor.execute(() -> {
            boolean deleted = false;

            try {
                deleted = database.workoutDao()
                        .deleteWorkout(workout) > 0;
            } catch (Exception e) {
                Log.e(TAG, "Unable to delete workout", e);
            }

            final boolean success = deleted;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (success) {
                    if (editingWorkoutId
                            == workout.getWorkoutId()) {
                        editingWorkout = false;
                        editingWorkoutId = -1;

                        buttonSaveWorkout.setText("Save Workout");
                        editWorkoutName.setText("");
                        editWorkoutDate.setText("");
                        spinnerWorkoutDay.setSelection(0);
                    }

                    Toast.makeText(this,
                            "Workout deleted.",
                            Toast.LENGTH_SHORT).show();

                    loadWorkouts();
                } else {
                    Toast.makeText(this,
                            "Could not delete workout.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void toggleWorkoutCompletion() {
        Workout workout = getSelectedWorkout();

        if (workout == null) {
            return;
        }

        int workoutId = workout.getWorkoutId();
        boolean newStatus = !workout.isCompleted();

        buttonToggleCompletion.setEnabled(false);

        databaseExecutor.execute(() -> {
            boolean updated = false;

            try {
                Workout savedWorkout = database.workoutDao()
                        .getWorkoutById(workoutId);

                if (savedWorkout != null) {
                    savedWorkout.setCompleted(newStatus);

                    updated = database.workoutDao()
                            .updateWorkout(savedWorkout) > 0;
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to update completion status", e);
            }

            final boolean success = updated;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                buttonToggleCompletion.setEnabled(true);

                if (success) {
                    Toast.makeText(
                            this,
                            newStatus
                                    ? "Workout marked completed!"
                                    : "Workout marked as planned.",
                            Toast.LENGTH_SHORT).show();

                    loadWorkouts();
                } else {
                    Toast.makeText(
                            this,
                            "Could not update workout status.",
                            Toast.LENGTH_SHORT).show();

                    updateCompletionButton();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}