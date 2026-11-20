package com.example.gymtrackerpro;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.Locale;

public class GoalsActivity extends AppCompatActivity {

    private EditText editGoalType;
    private EditText editTargetValue;
    private EditText editCurrentValue;
    private Button buttonSelectDate;
    private String selectedTargetDate = "";
    private TextView textGoalsList;

    private GymTrackerDatabase database;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiFx.applyEdgeToEdge(this);

        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_goals);

        TextView buttonBackArrow = findViewById(R.id.buttonBackArrow);
        buttonBackArrow.setOnClickListener(v -> finish());

        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        editGoalType = findViewById(R.id.editGoalType);
        editTargetValue = findViewById(R.id.editTargetValue);
        editCurrentValue = findViewById(R.id.editCurrentValue);
        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        textGoalsList = findViewById(R.id.textGoalsList);

        buttonSelectDate.setOnClickListener(v -> showDatePicker());

        Button buttonSaveGoal = findViewById(R.id.buttonSaveGoal);
        Button buttonRefreshGoals = findViewById(R.id.buttonRefreshGoals);

        buttonSaveGoal.setOnClickListener(v -> saveGoal());
        buttonRefreshGoals.setOnClickListener(v -> loadGoals());

        loadGoals();
    }

    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                GoalsActivity.this,
                (view, year, month, dayOfMonth) -> {

                    selectedTargetDate = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    buttonSelectDate.setText(selectedTargetDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(
                System.currentTimeMillis() - 1000
        );

        datePickerDialog.show();
    }

    private void saveGoal() {

        String goalType = editGoalType.getText().toString().trim();
        String targetText = editTargetValue.getText().toString().trim();
        String currentText = editCurrentValue.getText().toString().trim();
        String targetDate = selectedTargetDate;

        if (goalType.isEmpty()
                || targetText.isEmpty()
                || currentText.isEmpty()
                || targetDate.isEmpty()) {

            CustomNotification.show(
                    this,
                    "Incomplete Information",
                    "Please complete all fields and select a target date."
            );

            return;
        }

        final double target;
        final double current;

        try {

            target = Double.parseDouble(targetText);
            current = Double.parseDouble(currentText);

            if (!Double.isFinite(target)
                    || !Double.isFinite(current)
                    || target <= 0
                    || current < 0) {

                CustomNotification.show(
                        this,
                        "Invalid Goal Values",
                        "Enter a positive target and a non-negative current value."
                );

                return;
            }

        } catch (NumberFormatException e) {

            CustomNotification.show(
                    this,
                    "Invalid Numbers",
                    "Please enter valid numbers for your goal."
            );

            return;
        }

        // Existing user ID retained from your original implementation.
        Goal goal = new Goal(
                1,
                goalType,
                target,
                current,
                targetDate
        );

        executor.execute(() -> {

            try {

                database.goalDao().insertGoal(goal);

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    CustomNotification.show(
                            this,
                            "Goal Saved!",
                            "Your fitness goal has been saved successfully."
                    );

                    editGoalType.setText("");
                    editTargetValue.setText("");
                    editCurrentValue.setText("");

                    selectedTargetDate = "";
                    buttonSelectDate.setText("Select Target Date");

                    loadGoals();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    CustomNotification.show(
                            this,
                            "Save Failed",
                            "Could not save your goal. Please try again."
                    );
                });
            }
        });
    }

    private void loadGoals() {

        executor.execute(() -> {

            try {

                List<Goal> goals = database.goalDao().getAllGoals();

                StringBuilder builder = new StringBuilder();

                if (goals.isEmpty()) {

                    builder.append("No fitness goals yet.");

                } else {

                    for (Goal goal : goals) {

                        builder.append("Goal: ")
                                .append(goal.goalType)
                                .append("\nTarget: ")
                                .append(goal.targetValue)
                                .append("\nCurrent: ")
                                .append(goal.currentValue)
                                .append("\nTarget date: ")
                                .append(goal.targetDate)
                                .append("\n\n");
                    }
                }

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textGoalsList.setText(builder.toString());
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textGoalsList.setText("Unable to load goals.");

                    CustomNotification.show(
                            this,
                            "Loading Failed",
                            "We couldn't load your fitness goals. Please try refreshing."
                    );
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
