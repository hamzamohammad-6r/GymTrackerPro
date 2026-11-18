package com.example.gymtrackerpro;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.ScrollView;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String[] MOTIVATION = {
            "Small steps every day add up to big results.",
            "You don't have to be extreme, just consistent.",
            "The hardest rep is walking through the door.",
            "Progress, not perfection.",
            "Future you will thank you for today's workout.",
            "Strong is built one session at a time.",
            "Show up. Do the work. Repeat."
    };

    /** Everything the dashboard needs, gathered off the main thread. */
    private static class Dashboard {
        int total;
        int completed;
        int planned;
        int rate;
        int streak;
        String todayPlanned;
        String todayDone;
    }

    private static class Tile {
        final String title;
        final String subtitle;
        final int icon;
        final int photo;
        final String color;
        final Class<? extends Activity> target;

        Tile(String title, String subtitle, int icon, int photo,
             String color, Class<? extends Activity> target) {
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
            this.photo = photo;
            this.color = color;
            this.target = target;
        }
    }

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private TextView textTotalWorkouts;
    private TextView textCompletedWorkouts;
    private TextView textPlannedWorkouts;
    private TextView textCompletionRate;
    private TextView textStreak;
    private TextView textTodayLabel;
    private TextView textTodayTitle;
    private TextView textTodaySub;
    private TextView textProgressCaption;
    private Button buttonTodayAction;
    private View cardToday;
    private LinearProgressIndicator progressCompletion;

    private ScrollView scrollHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiFx.applyEdgeToEdge(this);

        // Match Android status bar to GymTrackerPro theme
        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));

        getWindow().getDecorView().setSystemUiVisibility(0);

        database = GymTrackerDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();

        initializeViews();
        setGreeting();
        buildProgramsCarousel();
        buildTiles();

        // Fill the exercise library on first launch.
        databaseExecutor.execute(() ->
                ExerciseSeeder.seedIfNeeded(
                        getApplicationContext(), database));

        // The dashboard itself loads in onResume(), which runs right after
        // onCreate() and again whenever you return to this screen.

        UiFx.staggerIn(
                findViewById(R.id.headerRow),
                cardToday,
                findViewById(R.id.cardStats),
                findViewById(R.id.sectionPrograms),
                findViewById(R.id.containerTiles));
    }

    private void initializeViews() {
        textTotalWorkouts = findViewById(R.id.textTotalWorkouts);
        textCompletedWorkouts = findViewById(R.id.textCompletedWorkouts);
        textPlannedWorkouts = findViewById(R.id.textPlannedWorkouts);
        textCompletionRate = findViewById(R.id.textCompletionRate);
        textStreak = findViewById(R.id.textStreak);
        textTodayLabel = findViewById(R.id.textTodayLabel);
        textTodayTitle = findViewById(R.id.textTodayTitle);
        textTodaySub = findViewById(R.id.textTodaySub);
        textProgressCaption = findViewById(R.id.textProgressCaption);
        buttonTodayAction = findViewById(R.id.buttonTodayAction);
        cardToday = findViewById(R.id.cardToday);
        progressCompletion = findViewById(R.id.progressCompletion);

        UiFx.applyPressEffect(cardToday);

        findViewById(R.id.textSeeAllPrograms).setOnClickListener(v ->
                openActivity(ProgramsActivity.class));

        findViewById(R.id.textSeeAllPrograms).setOnClickListener(v ->
                openActivity(ProgramsActivity.class));

        findViewById(R.id.buttonOpenRegister).setOnClickListener(v ->
                openActivity(RegisterActivity.class));

        findViewById(R.id.buttonOpenLogin).setOnClickListener(v ->
                openActivity(LoginActivity.class));
    }

    private void setGreeting() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour < 12) {
            greeting = "GOOD MORNING";
        } else if (hour < 18) {
            greeting = "GOOD AFTERNOON";
        } else {
            greeting = "GOOD EVENING";
        }

        ((TextView) findViewById(R.id.textGreeting)).setText(greeting);

        int index = now.get(Calendar.DAY_OF_YEAR) % MOTIVATION.length;
        ((TextView) findViewById(R.id.textMotivation))
                .setText(MOTIVATION[index]);
    }

    private void buildProgramsCarousel() {
        LinearLayout container = findViewById(R.id.containerPrograms);

        for (ProgramCatalog.Program program : ProgramCatalog.all()) {
            View card = ProgramCardBinder.create(this, container, program);

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) card.getLayoutParams();

            params.width = UiFx.dp(card, 280);
            params.setMarginEnd(UiFx.dp(card, 12));

            container.addView(card);
        }
    }

    private void buildTiles() {
        LinearLayout container = findViewById(R.id.containerTiles);

        Tile[] tiles = {
                new Tile("Programs", "Free plans to follow",
                        R.drawable.ic_tile_programs, R.drawable.photo_programs,
                        "#3B82F6", ProgramsActivity.class),

                new Tile("My Workouts", "Plan your training days",
                        R.drawable.ic_tile_workouts, R.drawable.photo_workouts,
                        "#3B82F6", WorkoutsActivity.class),

                new Tile("Exercises", "Your exercise library",
                        R.drawable.ic_tile_exercises, R.drawable.photo_exercises,
                        "#3B82F6", ExercisesActivity.class),

                new Tile("Workout Plan", "Sets, reps and weights",
                        R.drawable.ic_tile_plan, R.drawable.photo_plan,
                        "#3B82F6", WorkoutExercisesActivity.class),

                new Tile("History", "Look back at your work",
                        R.drawable.ic_tile_history, R.drawable.photo_history,
                        "#3B82F6", WorkoutHistoryActivity.class),

                new Tile("Music", "A playlist per workout",
                        R.drawable.ic_tile_music, R.drawable.photo_music,
                        "#3B82F6", WorkoutMusicActivity.class),

                new Tile("Goals", "Set targets and hit them",
                        R.drawable.ic_tile_goals, R.drawable.photo_goals,
                        "#3B82F6", GoalsActivity.class),

                new Tile("Profile", "Your personal details",
                        R.drawable.ic_tile_profile, R.drawable.photo_profile,
                        "#3B82F6", ProfileActivity.class)
        };

        for (int i = 0; i < tiles.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = UiFx.dp(container, 12);
            row.setLayoutParams(rowParams);

            for (int j = 0; j < 2; j++) {
                if (i + j < tiles.length) {
                    View tile = createTile(row, tiles[i + j]);

                    LinearLayout.LayoutParams params =
                            (LinearLayout.LayoutParams) tile.getLayoutParams();

                    if (j == 0) {
                        params.setMarginEnd(UiFx.dp(tile, 6));
                    } else {
                        params.setMarginStart(UiFx.dp(tile, 6));
                    }

                    row.addView(tile);
                }
            }

            container.addView(row);
        }
    }

    private View createTile(LinearLayout parent, final Tile tile) {
        View view = getLayoutInflater()
                .inflate(R.layout.item_home_tile, parent, false);

        // Apply the new premium electric-blue tile background.
        view.setBackgroundResource(R.drawable.bg_home_tile_gold);

        int color = Color.parseColor(tile.color);

        ImageView photo = view.findViewById(R.id.imageTilePhoto);
        photo.setImageResource(tile.photo);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);


        ImageView icon = view.findViewById(R.id.imageTileIcon);
        icon.setImageResource(tile.icon);

        icon.setImageTintList(
                ColorStateList.valueOf(Color.parseColor("#60A5FA"))
        );

        icon.setBackgroundResource(R.drawable.bg_tile_icon);
        icon.setElevation(8f);


        TextView tileTitle =
                view.findViewById(R.id.textTileTitle);

        TextView tileSubtitle =
                view.findViewById(R.id.textTileSubtitle);

        tileTitle.setText(tile.title);
        tileSubtitle.setText(tile.subtitle);

// Custom details for the Profile tile only.
        if (tile.target == ProfileActivity.class) {

            SharedPreferences preferences = getSharedPreferences(
                    "GymTrackerProfilePrefs",
                    MODE_PRIVATE
            );

            String savedPhoto = preferences.getString(
                    "profile_image_uri",
                    null
            );

            if (savedPhoto != null) {
                try {
                    photo.setImageURI(Uri.parse(savedPhoto));
                } catch (Exception e) {
                    photo.setImageResource(tile.photo);
                }
            }

            databaseExecutor.execute(() -> {
                try {
                    List<User> users = database.userDao().getAllUsers();

                    if (users != null && !users.isEmpty()) {
                        User user = users.get(0);

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            tileTitle.setText(user.getName());
                            tileSubtitle.setText(
                                    user.getFitnessGoal() == null
                                            || user.getFitnessGoal().trim().isEmpty()
                                            ? "Your personal fitness profile"
                                            : user.getFitnessGoal()
                            );
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not load profile tile", e);
                }
            });
        }

        view.setOnClickListener(v -> openActivity(tile.target));
        UiFx.applyPressEffect(view);

        return view;
    }

    private void openActivity(Class<? extends Activity> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    // ------------------------------------------------------------------
    // Dashboard data
    // ------------------------------------------------------------------

    private void loadDashboard() {
        databaseExecutor.execute(() -> {
            final Dashboard data = new Dashboard();

            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                if (workouts != null) {
                    String today = formatDate(Calendar.getInstance());
                    Set<String> completedDates = new HashSet<>();

                    data.total = workouts.size();

                    for (Workout workout : workouts) {
                        if (workout == null) {
                            continue;
                        }

                        if (workout.isCompleted()) {
                            data.completed++;

                            if (workout.getWorkoutDate() != null) {
                                completedDates.add(workout.getWorkoutDate());
                            }
                        }

                        if (today.equals(workout.getWorkoutDate())) {
                            if (workout.isCompleted()) {
                                if (data.todayDone == null) {
                                    data.todayDone = workout.getWorkoutName();
                                }
                            } else if (data.todayPlanned == null) {
                                data.todayPlanned = workout.getWorkoutName();
                            }
                        }
                    }

                    data.streak = computeStreak(completedDates);
                }

                data.planned = data.total - data.completed;

                if (data.total > 0) {
                    data.rate = Math.round(
                            (data.completed * 100.0f) / data.total);
                }

            } catch (Exception e) {
                Log.e(TAG, "Unable to load dashboard", e);
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                renderDashboard(data);
            });
        });
    }

    /** Consecutive days with a completed workout, ending today or yesterday. */
    private int computeStreak(Set<String> completedDates) {
        Calendar day = Calendar.getInstance();

        if (!completedDates.contains(formatDate(day))) {
            // Today is still open, so the streak may end yesterday.
            day.add(Calendar.DAY_OF_MONTH, -1);
        }

        int streak = 0;

        while (completedDates.contains(formatDate(day))) {
            streak++;
            day.add(Calendar.DAY_OF_MONTH, -1);
        }

        return streak;
    }

    private String formatDate(Calendar calendar) {
        return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private void renderDashboard(Dashboard data) {
        animateNumber(textTotalWorkouts, data.total, "");
        animateNumber(textCompletedWorkouts, data.completed, "");
        animateNumber(textPlannedWorkouts, data.planned, "");
        animateNumber(textCompletionRate, data.rate, "%");

        progressCompletion.setProgressCompat(data.rate, true);

        if (data.total == 0) {
            textProgressCaption.setText(
                    "Add your first workout to start tracking progress.");
        } else if (data.rate >= 100) {
            textProgressCaption.setText(
                    "Every planned workout is done. Amazing!");
        } else {
            textProgressCaption.setText(
                    data.completed + " of " + data.total
                            + " workouts completed. Keep going!");
        }

        textStreak.setText(String.valueOf(data.streak));

        renderToday(data);
    }

    private void renderToday(Dashboard data) {
        final Class<? extends Activity> target;

        if (data.todayPlanned != null) {
            textTodayLabel.setText("TODAY'S WORKOUT");
            textTodayTitle.setText(data.todayPlanned);
            textTodaySub.setText(
                    "Planned for today. Open your workouts when you're ready.");
            buttonTodayAction.setText("Open workouts");
            target = WorkoutsActivity.class;

        } else if (data.todayDone != null) {
            textTodayLabel.setText("TODAY'S WORKOUT");
            textTodayTitle.setText("Nice work! \uD83C\uDF89");
            textTodaySub.setText(
                    data.todayDone + " is complete. Rest up and refuel.");
            buttonTodayAction.setText("View history");
            target = WorkoutHistoryActivity.class;

        } else if (data.total == 0) {
            textTodayLabel.setText("GET STARTED");
            textTodayTitle.setText("Build your first week");
            textTodaySub.setText(
                    "Pick a free program and we'll fill in the exercises, "
                            + "sets and reps for you.");
            buttonTodayAction.setText("Browse programs");
            target = ProgramsActivity.class;

        } else {
            textTodayLabel.setText("REST DAY?");
            textTodayTitle.setText("Nothing planned today");
            textTodaySub.setText(
                    "Take it easy, or add a session to your week.");
            buttonTodayAction.setText("Plan a workout");
            target = WorkoutsActivity.class;
        }

        View.OnClickListener listener = v -> openActivity(target);
        cardToday.setOnClickListener(listener);
        buttonTodayAction.setOnClickListener(listener);
    }

    private void animateNumber(final TextView view, int to,
                               final String suffix) {
        ValueAnimator animator = ValueAnimator.ofInt(0, to);
        animator.setDuration(650);
        animator.addUpdateListener(a ->
                view.setText(a.getAnimatedValue() + suffix));
        animator.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateAuthenticationButtons();

        // Refresh after returning from another screen.
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            loadDashboard();
        }
    }

    private void updateAuthenticationButtons() {
        SharedPreferences preferences = getSharedPreferences(
                "GymTrackerSession",
                MODE_PRIVATE
        );

        String accessToken = preferences.getString("accessToken", null);

        boolean isLoggedIn = accessToken != null && !accessToken.trim().isEmpty();

        Button loginButton = findViewById(R.id.buttonOpenLogin);
        Button registerButton = findViewById(R.id.buttonOpenRegister);

        loginButton.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        registerButton.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
    }
}
