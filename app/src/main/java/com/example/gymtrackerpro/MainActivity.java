package com.example.gymtrackerpro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String DEFAULT_USER_NAME = "GymTrackerPro User";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private TextView textSubtitle;
    private TextView textTotalWorkouts;
    private TextView textCompletedWorkouts;
    private TextView textPlannedWorkouts;
    private TextView textCompletionRate;
    private ProgressBar progressCompletion;

    // Next workout card
    private View cardNextWorkout;
    private TextView textNextLabel;
    private TextView textNextTitle;
    private TextView textNextMeta;
    private MaterialButton buttonCompleteNext;
    private int nextWorkoutId = -1;

    private LinearLayout containerTraining;
    private LinearLayout containerPersonal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = GymTrackerDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();

        initializeViews();
        applyWindowInsets();
        buildMenu();
        setupNextWorkoutCard();
    }

    private void initializeViews() {
        textSubtitle = findViewById(R.id.textSubtitle);
        textTotalWorkouts = findViewById(R.id.textTotalWorkouts);
        textCompletedWorkouts = findViewById(R.id.textCompletedWorkouts);
        textPlannedWorkouts = findViewById(R.id.textPlannedWorkouts);
        textCompletionRate = findViewById(R.id.textCompletionRate);
        progressCompletion = findViewById(R.id.progressCompletion);

        cardNextWorkout = findViewById(R.id.cardNextWorkout);
        textNextLabel = findViewById(R.id.textNextLabel);
        textNextTitle = findViewById(R.id.textNextTitle);
        textNextMeta = findViewById(R.id.textNextMeta);
        buttonCompleteNext = findViewById(R.id.buttonCompleteNext);

        containerTraining = findViewById(R.id.containerTraining);
        containerPersonal = findViewById(R.id.containerPersonal);
    }

    /** Keeps content clear of the status bar / navigation bar on every Android version. */
    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left + bars.left, top + bars.top,
                    right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    private void buildMenu() {

        // TRAINING
        addMenuItem(containerTraining, "My Workouts",
                "Create and manage your training days",
                R.drawable.ic_calendar, R.color.gt_blue, WorkoutsActivity.class);

        addMenuItem(containerTraining, "Exercises",
                "Build your exercise library",
                R.drawable.ic_dumbbell, R.color.gt_green, ExercisesActivity.class);

        addMenuItem(containerTraining, "Workout Exercises",
                "Assign exercises, sets and reps",
                R.drawable.ic_list, R.color.gt_purple, WorkoutExercisesActivity.class);

        View musicRow = addMenuItem(containerTraining, "Workout Music",
                "Match your music to each workout day",
                R.drawable.ic_music, R.color.gt_pink, WorkoutMusicActivity.class);
        setBottomMarginDp(musicRow, 28); // gap before History

        addMenuItem(containerTraining, "Workout History",
                "Review completed and planned sessions",
                R.drawable.ic_history, R.color.gt_sky, WorkoutHistoryActivity.class);

        // PERSONAL
        addMenuItem(containerPersonal, "My Goals",
                "Set targets and track achievements",
                R.drawable.ic_flag, R.color.gt_yellow, GoalsActivity.class);

        addMenuItem(containerPersonal, "My Profile",
                "Manage your personal information",
                R.drawable.ic_person, R.color.gt_sky, ProfileActivity.class);
    }

    private View addMenuItem(ViewGroup container,
                             String title,
                             String subtitle,
                             @DrawableRes int iconRes,
                             @ColorRes int accentRes,
                             Class<? extends AppCompatActivity> target) {

        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_menu_card, container, false);

        int accent = ContextCompat.getColor(this, accentRes);

        ImageView icon = row.findViewById(R.id.imageIcon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        // Soft tinted badge background derived from the accent colour (18% opacity).
        icon.getBackground().mutate().setTint(ColorUtils.setAlphaComponent(accent, 46));

        ((TextView) row.findViewById(R.id.textTitle)).setText(title);
        ((TextView) row.findViewById(R.id.textSubtitle)).setText(subtitle);

        row.setOnClickListener(v -> startActivity(new Intent(this, target)));

        container.addView(row);
        return row;
    }

    private void setBottomMarginDp(View view, int dp) {
        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.bottomMargin = Math.round(dp * getResources().getDisplayMetrics().density);
        view.setLayoutParams(lp);
    }

    // ------------------------------------------------------------------
    // Next workout card
    // ------------------------------------------------------------------

    private void setupNextWorkoutCard() {
        // Tapping the card always opens the workout planner.
        cardNextWorkout.setOnClickListener(v ->
                startActivity(new Intent(this, WorkoutsActivity.class)));

        buttonCompleteNext.setOnClickListener(v -> {
            if (nextWorkoutId < 0) {
                return;
            }
            final int id = nextWorkoutId;
            buttonCompleteNext.setEnabled(false);

            databaseExecutor.execute(() -> {
                try {
                    database.workoutDao().updateWorkoutCompletion(id, true);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to complete workout", e);
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(this, "Workout completed. Nice work!",
                            Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
            });
        });
    }

    private void bindNextWorkout(Workout workout, int exerciseCount, String today) {
        buttonCompleteNext.setEnabled(true);

        if (workout == null) {
            nextWorkoutId = -1;
            textNextLabel.setText("UP NEXT");
            textNextLabel.setTextColor(ContextCompat.getColor(this, R.color.gt_blue));
            textNextTitle.setText("No workout planned");
            textNextMeta.setText("Tap to plan your next session");
            buttonCompleteNext.setVisibility(View.GONE);
            return;
        }

        nextWorkoutId = workout.getWorkoutId();

        String date = workout.getWorkoutDate() == null ? "" : workout.getWorkoutDate();

        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = formatDbDate(tomorrowCal);

        String label;
        int labelColor;
        if (date.compareTo(today) < 0) {
            label = "OVERDUE";
            labelColor = R.color.gt_orange;
        } else if (date.equals(today)) {
            label = "TODAY";
            labelColor = R.color.gt_green;
        } else if (date.equals(tomorrow)) {
            label = "TOMORROW";
            labelColor = R.color.gt_blue;
        } else {
            label = "UP NEXT";
            labelColor = R.color.gt_blue;
        }

        String name = workout.getWorkoutName();
        textNextLabel.setText(label);
        textNextLabel.setTextColor(ContextCompat.getColor(this, labelColor));
        textNextTitle.setText(name == null || name.trim().isEmpty() ? "Workout" : name.trim());

        String exercises = exerciseCount == 0
                ? "No exercises assigned yet"
                : exerciseCount + (exerciseCount == 1 ? " exercise" : " exercises");
        textNextMeta.setText(prettyDate(date) + "  \u00B7  " + exercises);

        buttonCompleteNext.setVisibility(View.VISIBLE);
    }

    /** yyyy-MM-dd, the format WorkoutsActivity saves dates in. */
    private static String formatDbDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
    }

    /** "2026-11-02" -> "Mon, 2 Nov". Falls back to the raw text if it can't be parsed. */
    private static String prettyDate(String dbDate) {
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dbDate);
            if (parsed != null) {
                return new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(parsed);
            }
        } catch (ParseException ignored) {
            // fall through
        }
        return dbDate;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    private void loadDashboardData() {
        databaseExecutor.execute(() -> {

            int total = 0;
            int completed = 0;
            int rate = 0;
            String userName = null;
            Workout nextWorkout = null;
            int nextExerciseCount = 0;
            final String today = formatDbDate(Calendar.getInstance());

            try {
                List<Workout> workouts = database.workoutDao().getAllWorkouts();

                if (workouts != null) {
                    total = workouts.size();
                    for (Workout workout : workouts) {
                        if (workout != null && workout.isCompleted()) {
                            completed++;
                        }
                    }
                }

                if (total > 0) {
                    rate = Math.round((completed * 100.0f) / total);
                }

                nextWorkout = database.workoutDao().getNextPlannedWorkout(today);
                if (nextWorkout != null) {
                    nextExerciseCount = database.workoutExerciseDao()
                            .countExercisesForWorkout(nextWorkout.getWorkoutId());
                }

                List<User> users = database.userDao().getAllUsers();
                if (users != null && !users.isEmpty()) {
                    userName = users.get(0).getName();
                }

            } catch (Exception e) {
                Log.e(TAG, "Unable to load dashboard data", e);
            }

            final int fTotal = total;
            final int fCompleted = completed;
            final int fPlanned = total - completed;
            final int fRate = rate;
            final String fName = userName;
            final Workout fNext = nextWorkout;
            final int fNextCount = nextExerciseCount;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                textTotalWorkouts.setText(String.valueOf(fTotal));
                textCompletedWorkouts.setText(String.valueOf(fCompleted));
                textPlannedWorkouts.setText(String.valueOf(fPlanned));
                textCompletionRate.setText(fRate + "%");
                progressCompletion.setProgress(fRate);
                bindNextWorkout(fNext, fNextCount, today);

                if (fName != null
                        && !fName.trim().isEmpty()
                        && !DEFAULT_USER_NAME.equals(fName.trim())) {
                    textSubtitle.setText("Welcome back, " + fName.trim()
                            + ". Let's get stronger today.");
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from another screen.
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            loadDashboardData();
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