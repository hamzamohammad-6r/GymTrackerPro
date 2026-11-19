package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutsActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutsActivity";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private TextView textSelectedProgramTitle;
    private TextView textProgramDescription;
    private TextView textProgramDuration;
    private TextView textProgramDays;
    private TextView textProgramLevel;
    private TextView textProgramProgress;
    private TextView textWorkoutList;

    private Button buttonChangeProgram;
    private Button buttonStartNextWorkout;
    private Button buttonBackHome;

    private ProgramCatalog.Program selectedProgram;
    private UserProgram userProgram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_workouts);
        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        databaseExecutor = Executors.newSingleThreadExecutor();

        bindViews();
        setupButtons();

        loadSelectedProgram();
    }


    private void bindViews() {

        textSelectedProgramTitle =
                findViewById(R.id.textSelectedProgramTitle);

        textProgramDescription =
                findViewById(R.id.textProgramDescription);

        textProgramDuration =
                findViewById(R.id.textProgramDuration);

        textProgramDays =
                findViewById(R.id.textProgramDays);

        textProgramLevel =
                findViewById(R.id.textProgramLevel);

        textProgramProgress =
                findViewById(R.id.textProgramProgress);

        textWorkoutList =
                findViewById(R.id.textWorkoutList);

        buttonChangeProgram =
                findViewById(R.id.buttonChangeProgram);

        buttonStartNextWorkout =
                findViewById(R.id.buttonStartNextWorkout);

        buttonBackHome =
                findViewById(R.id.buttonBackHome);
    }


    private void setupButtons() {

        TextView buttonBackArrow =
                findViewById(R.id.buttonBackArrow);

        buttonBackArrow.setOnClickListener(v -> finish());

        buttonBackHome.setOnClickListener(v -> finish());

        buttonChangeProgram.setOnClickListener(v -> {
            Toast.makeText(this, "CHANGE PROGRAMME BUTTON CLICKED",
                    Toast.LENGTH_SHORT).show();

            startActivity(new Intent(
                    WorkoutsActivity.this,
                    ProgramsActivity.class
            ));
        });

        buttonStartNextWorkout.setOnClickListener(v -> {
            Toast.makeText(this, "START WORKOUT BUTTON CLICKED",
                    Toast.LENGTH_SHORT).show();

            showNextWorkoutPreview();
        });

        UiFx.applyPressEffect(buttonChangeProgram);
        UiFx.applyPressEffect(buttonStartNextWorkout);
        UiFx.applyPressEffect(buttonBackHome);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseExecutor != null
                && !databaseExecutor.isShutdown()) {
            loadSelectedProgram();
        }
    }

    private void loadSelectedProgram() {

        if (databaseExecutor == null
                || databaseExecutor.isShutdown()) {
            return;
        }

        buttonStartNextWorkout.setEnabled(false);

        databaseExecutor.execute(() -> {

            ProgramCatalog.Program program = null;
            UserProgram savedProgram = null;
            String errorMessage = null;

            try {

                int userId = getCurrentUserId();

                savedProgram = UserProgramManager.getActiveProgram(
                        database,
                        userId
                );

                if (savedProgram != null) {
                    program = ProgramCatalog.byId(
                            savedProgram.getProgramId()
                    );
                }

            } catch (Exception e) {

                Log.e(TAG, "Unable to load selected programme", e);

                errorMessage = e.getMessage();
            }

            final ProgramCatalog.Program loadedProgram = program;
            final UserProgram loadedUserProgram = savedProgram;
            final String error = errorMessage;

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                selectedProgram = loadedProgram;
                userProgram = loadedUserProgram;

                buttonStartNextWorkout.setEnabled(
                        selectedProgram != null
                );

                if (error != null) {

                    showNoProgram(
                            "Could not load your programme. "
                                    + error
                    );

                    return;
                }

                if (selectedProgram == null) {

                    showNoProgram(
                            "You haven't selected a programme yet. "
                                    + "Choose one of our free programmes "
                                    + "to begin."
                    );

                    return;
                }

                displaySelectedProgram();
            });
        });
    }

    private int getCurrentUserId() {

        SharedPreferences session = getSharedPreferences(
                "GymTrackerSession",
                MODE_PRIVATE
        );

        String loggedInEmail = session.getString("email", null);

        return UserProgramManager.getCurrentUserId(
                database,
                loggedInEmail
        );
    }

    private void showNoProgram(String message) {

        textSelectedProgramTitle.setText(
                "No programme selected"
        );

        textProgramDescription.setText(message);

        textProgramDuration.setText("--");
        textProgramDays.setText("--");
        textProgramLevel.setText("--");

        textProgramProgress.setText(
                "Your programme progress will appear here "
                        + "after you select a programme."
        );

        textWorkoutList.setText(
                "No training schedule available.\n\n"
                        + "Choose a free programme to view "
                        + "your prescribed training days."
        );

        buttonStartNextWorkout.setEnabled(false);
    }

    private void displaySelectedProgram() {

        textSelectedProgramTitle.setText(
                selectedProgram.title
        );

        textProgramDescription.setText(
                selectedProgram.description
        );

        textProgramDuration.setText(
                selectedProgram.weeks + " weeks"
        );

        textProgramDays.setText(
                selectedProgram.daysPerWeek() + " days"
        );

        textProgramLevel.setText(
                selectedProgram.level
        );

        textProgramProgress.setText(
                "Programme started: "
                        + userProgram.getStartDate()
                        + "\n\nFollow your prescribed training days "
                        + "and complete each session to progress."
        );

        displayWeeklySchedule();

        buttonStartNextWorkout.setEnabled(true);
    }

    private void displayWeeklySchedule() {

        StringBuilder display = new StringBuilder();

        for (int i = 0; i < selectedProgram.days.size(); i++) {

            ProgramCatalog.Day day =
                    selectedProgram.days.get(i);

            display.append("DAY ")
                    .append(i + 1)
                    .append("  ·  ")
                    .append(day.weekday)
                    .append("\n");

            display.append(day.title)
                    .append("\n\n");

            for (ProgramCatalog.Entry exercise : day.exercises) {

                display.append("• ")
                        .append(exercise.exerciseName)
                        .append("\n");

                display.append("   ")
                        .append(exercise.prescription())
                        .append("\n");

                if (exercise.note != null
                        && !exercise.note.trim().isEmpty()) {

                    display.append("   ")
                            .append(exercise.note)
                            .append("\n");
                }

                display.append("\n");
            }

            display.append("------------------------------")
                    .append("\n\n");
        }

        textWorkoutList.setText(display.toString());
    }


    private void showNextWorkoutPreview() {

        if (selectedProgram == null
                || selectedProgram.days.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select a programme first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final ProgramCatalog.Program programToStart = selectedProgram;
        final ProgramCatalog.Day nextDay = getNextTrainingDay();

        if (nextDay == null) {

            Toast.makeText(
                    this,
                    "No training day was found.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        buttonStartNextWorkout.setEnabled(false);
        buttonStartNextWorkout.setText("Preparing Workout...");

        databaseExecutor.execute(() -> {

            try {

                SharedPreferences preferences = getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

                String email = preferences.getString("email", "");

                int userId = UserProgramManager.getCurrentUserId(
                        database,
                        email
                );

                java.util.Calendar sessionDate =
                        java.util.Calendar.getInstance();

                boolean matchingDateFound = false;

                for (int offset = 0; offset < 7; offset++) {

                    java.util.Calendar candidate =
                            (java.util.Calendar) sessionDate.clone();

                    candidate.add(
                            java.util.Calendar.DAY_OF_MONTH,
                            offset
                    );

                    String candidateWeekday =
                            new java.text.SimpleDateFormat(
                                    "EEEE",
                                    java.util.Locale.US
                            ).format(candidate.getTime());

                    if (candidateWeekday.equalsIgnoreCase(
                            nextDay.weekday
                    )) {

                        sessionDate = candidate;
                        matchingDateFound = true;
                        break;
                    }
                }

                if (!matchingDateFound) {
                    throw new IllegalStateException(
                            "Could not calculate the training date."
                    );
                }

                GuidedWorkoutManager.SessionResult result =
                        GuidedWorkoutManager.createOrGetSession(
                                database,
                                userId,
                                programToStart,
                                nextDay,
                                sessionDate
                        );

                runOnUiThread(() -> {

                    buttonStartNextWorkout.setEnabled(true);
                    buttonStartNextWorkout.setText("Start Workout");

                    if (result.isCompleted()) {

                        Toast.makeText(
                                WorkoutsActivity.this,
                                "This session has already been completed.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Intent intent = new Intent(
                            WorkoutsActivity.this,
                            ActiveWorkoutActivity.class
                    );

                    intent.putExtra(
                            ActiveWorkoutActivity.EXTRA_WORKOUT_ID,
                            result.getWorkoutId()
                    );

                    startActivity(intent);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    buttonStartNextWorkout.setEnabled(true);
                    buttonStartNextWorkout.setText("Start Workout");

                    Toast.makeText(
                            WorkoutsActivity.this,
                            "Could not prepare your workout: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }


    private ProgramCatalog.Day getNextTrainingDay() {

        Calendar calendar = Calendar.getInstance();

        int today = calendar.get(Calendar.DAY_OF_WEEK);

        // Convert Calendar weekday to Monday = 0, Sunday = 6.
        int todayIndex = (today + 5) % 7;

        ProgramCatalog.Day nextDay = null;
        int smallestOffset = 8;

        for (ProgramCatalog.Day day : selectedProgram.days) {

            int trainingDayIndex = getWeekdayIndex(day.weekday);

            if (trainingDayIndex == -1) {
                continue;
            }

            int offset =
                    (trainingDayIndex - todayIndex + 7) % 7;

            if (offset < smallestOffset) {

                smallestOffset = offset;
                nextDay = day;
            }
        }

        return nextDay;
    }

    private int getWeekdayIndex(String weekday) {

        if (weekday == null) {
            return -1;
        }

        String day = weekday.trim().toLowerCase(Locale.ROOT);

        switch (day) {
            case "monday":
                return 0;

            case "tuesday":
                return 1;

            case "wednesday":
                return 2;

            case "thursday":
                return 3;

            case "friday":
                return 4;

            case "saturday":
                return 5;

            case "sunday":
                return 6;

            default:
                return -1;
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
