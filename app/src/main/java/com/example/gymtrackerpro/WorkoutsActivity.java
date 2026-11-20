package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;

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
    private LinearLayout textWorkoutList;

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
            startActivity(new Intent(
                    WorkoutsActivity.this,
                    ProgramsActivity.class
            ));
        });

        buttonStartNextWorkout.setOnClickListener(
                v -> showNextWorkoutPreview()
        );

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

        textSelectedProgramTitle.setText("No programme selected");

        textProgramDescription.setText(message);

        textProgramDuration.setText("--");
        textProgramDays.setText("--");
        textProgramLevel.setText("--");

        textProgramProgress.setText(
                "Your programme progress will appear here after you select a programme."
        );

        textWorkoutList.removeAllViews();

        TextView emptyMessage = createScheduleText(
                "No training schedule available.\n\nChoose a free programme to view your prescribed training days.",
                14,
                Color.parseColor("#94A3B8"),
                false
        );

        textWorkoutList.addView(emptyMessage);

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

        textWorkoutList.removeAllViews();

        for (int i = 0; i < selectedProgram.days.size(); i++) {

            ProgramCatalog.Day day = selectedProgram.days.get(i);

            String weekday = day.weekday == null
                    ? "TRAINING DAY"
                    : day.weekday.toUpperCase(Locale.ROOT);

            int dayColor = getDayColor(weekday);

            // MAIN DAY CARD
            LinearLayout dayCard = new LinearLayout(this);

            dayCard.setOrientation(LinearLayout.VERTICAL);
            dayCard.setPadding(
                    dp(16), dp(16), dp(16), dp(16)
            );

            dayCard.setBackground(
                    getDrawable(R.drawable.bg_glow_card)
            );

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            cardParams.bottomMargin = dp(16);

            dayCard.setLayoutParams(cardParams);

            // DAY NUMBER AND WEEKDAY HEADER
            LinearLayout header = new LinearLayout(this);

            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView dayNumber = createScheduleText(
                    "DAY " + (i + 1),
                    12,
                    Color.parseColor("#94A3B8"),
                    true
            );

            header.addView(dayNumber);

            View spacer = new View(this);

            header.addView(spacer, new LinearLayout.LayoutParams(
                    0,
                    1,
                    1
            ));

            TextView weekdayLabel = createScheduleText(
                    weekday,
                    12,
                    dayColor,
                    true
            );

            weekdayLabel.setGravity(Gravity.CENTER);
            weekdayLabel.setPadding(
                    dp(12), dp(7), dp(12), dp(7)
            );

            weekdayLabel.setBackground(createRoundedBackground(
                    Color.argb(
                            35,
                            Color.red(dayColor),
                            Color.green(dayColor),
                            Color.blue(dayColor)
                    ),
                    dayColor
            ));

            header.addView(weekdayLabel);

            dayCard.addView(header);

            // WORKOUT TITLE
            TextView workoutTitle = createScheduleText(
                    day.title,
                    19,
                    Color.parseColor("#F8FAFC"),
                    true
            );

            LinearLayout.LayoutParams titleParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            titleParams.topMargin = dp(14);

            dayCard.addView(workoutTitle, titleParams);

            // DIVIDER
            View divider = new View(this);

            divider.setBackgroundColor(
                    Color.parseColor("#334155")
            );

            LinearLayout.LayoutParams dividerParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(1)
                    );

            dividerParams.topMargin = dp(15);
            dividerParams.bottomMargin = dp(14);

            dayCard.addView(divider, dividerParams);

            // EXERCISES LABEL
            TextView exercisesLabel = createScheduleText(
                    "EXERCISES",
                    11,
                    Color.parseColor("#60A5FA"),
                    true
            );

            LinearLayout.LayoutParams exerciseLabelParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            exerciseLabelParams.bottomMargin = dp(12);

            dayCard.addView(exercisesLabel, exerciseLabelParams);

            // EXERCISE ROWS
            for (ProgramCatalog.Entry exercise : day.exercises) {

                LinearLayout exerciseCard = new LinearLayout(this);

                exerciseCard.setOrientation(LinearLayout.VERTICAL);
                exerciseCard.setPadding(
                        dp(12), dp(12), dp(12), dp(12)
                );

                exerciseCard.setBackground(createRoundedBackground(
                        Color.parseColor("#1E293B"),
                        Color.TRANSPARENT
                ));

                LinearLayout.LayoutParams exerciseParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                exerciseParams.bottomMargin = dp(9);

                exerciseCard.setLayoutParams(exerciseParams);

                LinearLayout exerciseHeader = new LinearLayout(this);

                exerciseHeader.setOrientation(LinearLayout.HORIZONTAL);
                exerciseHeader.setGravity(Gravity.CENTER_VERTICAL);

                TextView exerciseName = createScheduleText(
                        exercise.exerciseName,
                        14,
                        Color.parseColor("#F8FAFC"),
                        true
                );

                LinearLayout.LayoutParams nameParams =
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                        );

                exerciseHeader.addView(exerciseName, nameParams);

                TextView prescription = createScheduleText(
                        exercise.prescription(),
                        13,
                        Color.parseColor("#60A5FA"),
                        true
                );

                exerciseHeader.addView(prescription);

                exerciseCard.addView(exerciseHeader);

                if (exercise.note != null
                        && !exercise.note.trim().isEmpty()) {

                    TextView note = createScheduleText(
                            exercise.note,
                            12,
                            Color.parseColor("#94A3B8"),
                            false
                    );

                    LinearLayout.LayoutParams noteParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );

                    noteParams.topMargin = dp(7);

                    exerciseCard.addView(note, noteParams);
                }

                dayCard.addView(exerciseCard);
            }

            textWorkoutList.addView(dayCard);
        }
    }

    private int getDayColor(String weekday) {

        switch (weekday) {

            case "MONDAY":
                return Color.parseColor("#60A5FA");

            case "TUESDAY":
                return Color.parseColor("#22D3EE");

            case "WEDNESDAY":
                return Color.parseColor("#A78BFA");

            case "THURSDAY":
                return Color.parseColor("#2DD4BF");

            case "FRIDAY":
                return Color.parseColor("#FB923C");

            case "SATURDAY":
                return Color.parseColor("#4ADE80");

            case "SUNDAY":
                return Color.parseColor("#F87171");

            default:
                return Color.parseColor("#60A5FA");
        }
    }

    private TextView createScheduleText(
            String text,
            float size,
            int color,
            boolean bold
    ) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable createRoundedBackground(
            int backgroundColor,
            int strokeColor
    ) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(dp(14));

        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }

        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void showNextWorkoutPreview() {

        if (selectedProgram == null
                || selectedProgram.days.isEmpty()) {

            CustomNotification.show(
                    this,
                    "Programme Required",
                    "Please select a programme first."
            );

            return;
        }

        final ProgramCatalog.Program programToStart = selectedProgram;
        final ProgramCatalog.Day nextDay = getNextTrainingDay();

        if (nextDay == null) {

            CustomNotification.show(
                    this,
                    "Training Day Not Found",
                    "No training day was found in your selected programme."
            );

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

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    buttonStartNextWorkout.setEnabled(true);
                    buttonStartNextWorkout.setText("Start Workout");

                    if (result.isCompleted()) {

                        CustomNotification.show(
                                WorkoutsActivity.this,
                                "Workout Already Completed",
                                "This session has already been completed."
                        );

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

                Log.e(TAG, "Could not prepare workout", e);

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    buttonStartNextWorkout.setEnabled(true);
                    buttonStartNextWorkout.setText("Start Workout");

                    CustomNotification.show(
                            WorkoutsActivity.this,
                            "Workout Preparation Failed",
                            "Could not prepare your workout: "
                                    + (e.getMessage() == null
                                    ? "Please try again."
                                    : e.getMessage())
                    );
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
