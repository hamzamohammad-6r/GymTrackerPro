package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutHistory";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private TextView textHistorySummary;
    private LinearLayout textWorkoutHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiFx.applyEdgeToEdge(this);

        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_workout_history);

        TextView buttonBackArrow = findViewById(R.id.buttonBackArrow);

        buttonBackArrow.setOnClickListener(v ->
                UiFx.finishWithTransition(this));

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        UiFx.finishWithTransition(
                                WorkoutHistoryActivity.this
                        );
                    }
                }
        );

        UiFx.applyEdgeToEdge(this);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        databaseExecutor = Executors.newSingleThreadExecutor();

        textHistorySummary = findViewById(R.id.textHistorySummary);
        textWorkoutHistory = findViewById(R.id.textWorkoutHistory);

        findViewById(R.id.buttonHistoryBack)
                .setOnClickListener(v ->
                        UiFx.finishWithTransition(this));

        loadWorkoutHistory();

        // Bottom navigation
        View navHome = findViewById(R.id.navHome);
        View navProgress = findViewById(R.id.navProgress);
        View navTimer = findViewById(R.id.navTimer);
        View navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v ->
                openScreen(MainActivity.class));

        // Already on Progress screen
        navProgress.setOnClickListener(v -> {});

        navTimer.setOnClickListener(v ->
                openScreen(TimerActivity.class));

        navProfile.setOnClickListener(v ->
                openScreen(ProfileActivity.class));
    }

    private void openScreen(Class<?> activityClass) {

        Intent intent = new Intent(
                WorkoutHistoryActivity.this,
                activityClass
        );

        UiFx.startActivityWithTransition(this, intent);
    }

    private void loadWorkoutHistory() {

        databaseExecutor.execute(() -> {

            try {

                SharedPreferences session = getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

                String loggedInEmail = session.getString("email", null);

                List<Workout> workouts;

                if (loggedInEmail != null
                        && !loggedInEmail.trim().isEmpty()) {

                    User currentUser = database.userDao().getUserByEmail(
                            loggedInEmail.trim()
                    );

                    if (currentUser != null) {

                        workouts = database.workoutDao()
                                .getWorkoutsForUser(currentUser.getUserId());

                    } else {

                        workouts = new java.util.ArrayList<>();
                    }

                } else {

                    int guestUserId = LocalUser.getOrCreateId(database);

                    workouts = database.workoutDao()
                            .getWorkoutsForUser(guestUserId);
                }

                final List<Workout> historyWorkouts =
                        workouts == null
                                ? new java.util.ArrayList<>()
                                : new java.util.ArrayList<>(workouts);

                int completed = 0;
                int planned = 0;

                for (Workout workout : historyWorkouts) {

                    if (workout.isCompleted()) {
                        completed++;
                    } else {
                        planned++;
                    }
                }

                final int total = historyWorkouts.size();
                final int finalCompleted = completed;
                final int finalPlanned = planned;

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) return;

                    textHistorySummary.setText(
                            "Total: " + total
                                    + " | Completed: " + finalCompleted
                                    + " | Planned: " + finalPlanned
                    );

                    textWorkoutHistory.removeAllViews();

                    if (historyWorkouts.isEmpty()) {

                        TextView empty = new TextView(this);

                        empty.setText(
                                "No completed or planned workouts yet.\n"
                                        + "Start your training programme to begin your history."
                        );

                        empty.setTextColor(Color.parseColor("#94A3B8"));
                        empty.setTextSize(14);
                        empty.setGravity(android.view.Gravity.CENTER);
                        empty.setPadding(dp(20), dp(30), dp(20), dp(30));

                        textWorkoutHistory.addView(empty);

                    } else {

                        for (Workout workout : historyWorkouts) {

                            textWorkoutHistory.addView(
                                    createSessionCard(workout)
                            );
                        }
                    }
                });

            } catch (Exception e) {

                Log.e(TAG, "Unable to load workout history", e);

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) return;

                    textWorkoutHistory.removeAllViews();

                    TextView error = new TextView(this);

                    error.setText("Unable to load workout history.");
                    error.setTextColor(Color.parseColor("#F87171"));
                    error.setTextSize(14);

                    textWorkoutHistory.addView(error);
                });
            }
        });
    }

    private View createSessionCard(Workout workout) {

        boolean completed = workout.isCompleted();

        int accentColor = Color.parseColor(
                completed ? "#22C55E" : "#F59E0B"
        );

        int glowColor = Color.parseColor(
                completed ? "#3322C55E" : "#33F59E0B"
        );

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable outer = new GradientDrawable();
        outer.setColor(glowColor);
        outer.setCornerRadius(dp(20));
        outer.setStroke(dp(2), accentColor);

        GradientDrawable inner = new GradientDrawable();
        inner.setColor(Color.parseColor("#151F33"));
        inner.setCornerRadius(dp(17));
        inner.setStroke(dp(1), accentColor);

        Drawable[] layers = {
                outer,
                new InsetDrawable(inner, dp(3))
        };

        card.setBackground(new LayerDrawable(layers));

        // Apply padding after setting the background
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);

        TextView status = new TextView(this);

        status.setText(completed ? "✓  COMPLETED" : "○  PLANNED");
        status.setTextColor(accentColor);
        status.setTextSize(12);
        status.setTypeface(null, android.graphics.Typeface.BOLD);

        card.addView(status);

        TextView name = new TextView(this);

        name.setText(workout.getWorkoutName());
        name.setTextColor(Color.parseColor("#F8FAFC"));
        name.setTextSize(17);
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        nameParams.topMargin = dp(9);
        card.addView(name, nameParams);

        TextView date = new TextView(this);

        date.setText("Date: " + workout.getWorkoutDate());
        date.setTextColor(Color.parseColor("#CBD5E1"));
        date.setTextSize(13);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        dateParams.topMargin = dp(7);
        card.addView(date, dateParams);

        String day = workout.getDayOfWeek();

        if (day != null && !day.trim().isEmpty()) {

            TextView dayText = new TextView(this);

            dayText.setText("Day: " + day);
            dayText.setTextColor(Color.parseColor("#94A3B8"));
            dayText.setTextSize(13);

            LinearLayout.LayoutParams dayParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            dayParams.topMargin = dp(4);
            card.addView(dayText, dayParams);
        }

        return card;
    }

    private int dp(float value) {

        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void markWorkoutCompleted(Workout workout) {

        if (workout.isCompleted()) {

            CustomNotification.show(
                    this,
                    "Already Completed",
                    "This workout has already been marked as completed."
            );

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Complete Workout")
                .setMessage(
                        "Mark \"" + workout.getWorkoutName()
                                + "\" as completed?"
                )
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Complete", (dialog, which) -> {

                    databaseExecutor.execute(() -> {

                        try {

                            int updated = database.workoutDao()
                                    .updateWorkoutCompletion(
                                            workout.getWorkoutId(),
                                            true
                                    );

                            runOnUiThread(() -> {

                                if (isFinishing() || isDestroyed()) return;

                                if (updated > 0) {

                                    CustomNotification.show(
                                            this,
                                            "Workout Completed!",
                                            "Your workout has been marked as completed."
                                    );

                                    loadWorkoutHistory();

                                } else {

                                    CustomNotification.show(
                                            this,
                                            "Update Failed",
                                            "Your workout could not be updated. Please try again."
                                    );
                                }
                            });

                        } catch (Exception e) {

                            Log.e(TAG, "Unable to complete workout", e);

                            runOnUiThread(() -> {

                                if (isFinishing() || isDestroyed()) return;

                                CustomNotification.show(
                                        this,
                                        "Error",
                                        "An error occurred while updating your workout."
                                );
                            });
                        }
                    });
                })
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
