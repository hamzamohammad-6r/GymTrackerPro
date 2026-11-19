package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.SeekBar;

import java.util.ArrayList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActiveWorkoutActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_ID = "workoutId";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private int workoutId;
    private int currentUserId;

    private Workout currentWorkout;

    private TextView textTitle;
    private TextView textDate;
    private TextView textProgress;
    private LinearLayout containerExercises;

    private Button buttonCompleteWorkout;
    private Button buttonWorkoutTimer;
    private Button buttonWorkoutMusic;

    private int totalExercises = 0;

    // Active workout music player
    private TextView textActiveMusicTitle;
    private TextView textActiveMusicStatus;

    private TextView textActiveMusicElapsed;
    private TextView textActiveMusicDuration;

    private SeekBar seekActiveMusic;

    private android.os.Handler musicProgressHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private boolean isUserSeekingMusic = false;

    private Button buttonActiveMusicPlayPause;
    private Button buttonActiveMusicNext;
    private Button buttonActiveMusicStop;

    private MediaPlayer activeWorkoutPlayer;

    private List<WorkoutMusic> assignedWorkoutMusic = new ArrayList<>();

    private int currentMusicIndex = 0;
    private boolean isMusicPrepared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiFx.applyEdgeToEdge(this);
        getWindow().setStatusBarColor(
                android.graphics.Color.parseColor("#0B1120")
        );
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_active_workout);

        database = GymTrackerDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();

        workoutId = getIntent().getIntExtra(EXTRA_WORKOUT_ID, -1);

        textTitle = findViewById(R.id.textActiveWorkoutTitle);
        textDate = findViewById(R.id.textActiveWorkoutDate);
        textProgress = findViewById(R.id.textActiveWorkoutProgress);

        containerExercises = findViewById(R.id.containerActiveExercises);

        buttonCompleteWorkout = findViewById(R.id.buttonCompleteWorkout);
        buttonWorkoutTimer = findViewById(R.id.buttonOpenWorkoutTimer);
        buttonWorkoutMusic = findViewById(R.id.buttonWorkoutMusic);

        textActiveMusicTitle = findViewById(R.id.textActiveMusicTitle);
        textActiveMusicStatus = findViewById(R.id.textActiveMusicStatus);

        textActiveMusicElapsed = findViewById(R.id.textActiveMusicElapsed);
        textActiveMusicDuration = findViewById(R.id.textActiveMusicDuration);

        seekActiveMusic = findViewById(R.id.seekActiveMusic);

        seekActiveMusic.setEnabled(false);

        buttonActiveMusicPlayPause = findViewById(R.id.buttonActiveMusicPlayPause);
        buttonActiveMusicNext = findViewById(R.id.buttonActiveMusicNext);
        buttonActiveMusicStop = findViewById(R.id.buttonActiveMusicStop);

        buttonActiveMusicPlayPause.setEnabled(false);
        buttonActiveMusicNext.setEnabled(false);
        buttonActiveMusicStop.setEnabled(false);

        buttonActiveMusicPlayPause.setOnClickListener(v -> toggleWorkoutMusic());

        buttonActiveMusicNext.setOnClickListener(v -> playNextWorkoutSong());

        buttonActiveMusicStop.setOnClickListener(v -> stopWorkoutMusic());

        seekActiveMusic.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser && activeWorkoutPlayer != null
                                && isMusicPrepared) {

                            textActiveMusicElapsed.setText(
                                    formatMusicTime(progress)
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        isUserSeekingMusic = true;
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                        isUserSeekingMusic = false;

                        if (activeWorkoutPlayer != null && isMusicPrepared) {
                            activeWorkoutPlayer.seekTo(
                                    seekBar.getProgress()
                            );
                        }
                    }
                }
        );

        findViewById(R.id.buttonBackActiveWorkout)
                .setOnClickListener(v -> finish());

        buttonCompleteWorkout.setOnClickListener(
                v -> confirmWorkoutCompletion()
        );

        buttonWorkoutTimer.setOnClickListener(v -> {
            startActivity(new Intent(
                    ActiveWorkoutActivity.this,
                    TimerActivity.class
            ));
        });

        if (workoutId == -1) {
            showErrorAndClose("Workout session could not be found.");
            return;
        }

        loadWorkout();
    }


    private void loadWorkout() {

        databaseExecutor.execute(() -> {

            try {

                SharedPreferences preferences = getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

                String email = preferences.getString("email", "");

                if (email != null && !email.trim().isEmpty()) {

                    User user = database.userDao().getUserByEmail(
                            email.trim()
                    );

                    if (user == null) {
                        runOnUiThread(() ->
                                showErrorAndClose(
                                        "Your local profile could not be found."
                                )
                        );
                        return;
                    }

                    currentUserId = user.getUserId();

                } else {

                    currentUserId = LocalUser.getOrCreateId(database);
                }

                Workout workout = database.workoutDao()
                        .getWorkoutById(workoutId);

                if (workout == null
                        || workout.getUserId() != currentUserId) {

                    runOnUiThread(() ->
                            showErrorAndClose(
                                    "You do not have access to this workout."
                            )
                    );
                    return;
                }

                currentWorkout = workout;

                // Load music assigned specifically to this workout.
                List<WorkoutMusic> musicList =
                        database.workoutMusicDao()
                                .getMusicForWorkout(workoutId);

                List<WorkoutExercise> workoutExercises =
                        database.workoutExerciseDao()
                                .getExercisesForWorkout(workoutId);

                // Retrieve exercise details on the background thread.
                List<Exercise> exercises = new java.util.ArrayList<>();

                for (WorkoutExercise workoutExercise : workoutExercises) {

                    Exercise exercise = database.exerciseDao()
                            .getExerciseById(
                                    workoutExercise.getExerciseId()
                            );

                    exercises.add(exercise);
                }

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (currentWorkout.isCompleted()) {
                        showErrorAndClose(
                                "This workout has already been completed."
                        );
                        return;
                    }

                    displayWorkout(
                            workout,
                            workoutExercises,
                            exercises
                    );

                    assignedWorkoutMusic.clear();
                    assignedWorkoutMusic.addAll(musicList);

                    initializeWorkoutMusic();

                });

            } catch (Exception e) {

                Log.e("ActiveWorkout", "Workout loading error", e);

                runOnUiThread(() ->
                        showErrorAndClose(
                                "Unable to load this workout. Please try again."
                        )
                );
            }
        });
    }



    private void displayWorkout(
            Workout workout,
            List<WorkoutExercise> workoutExercises,
            List<Exercise> exercises
    ) {

        textTitle.setText(workout.getWorkoutName());

        String date = workout.getWorkoutDate();

        textDate.setText(
                date + "  •  " + workout.getDayOfWeek()
        );

        containerExercises.removeAllViews();

        totalExercises = workoutExercises.size();

        textProgress.setText(
                totalExercises + " prescribed exercises"
        );

        if (workoutExercises.isEmpty()) {

            TextView empty = createText(
                    "No exercises have been added to this session.",
                    14,
                    "#94A3B8"
            );

            containerExercises.addView(empty);

            buttonCompleteWorkout.setEnabled(false);
            return;
        }

        for (int i = 0; i < workoutExercises.size(); i++) {

            WorkoutExercise workoutExercise = workoutExercises.get(i);
            Exercise exercise = exercises.get(i);

            if (exercise == null) {
                continue;
            }

            addExerciseCard(exercise, workoutExercise);
        }
    }


    private void initializeWorkoutMusic() {

        if (assignedWorkoutMusic.isEmpty()) {
            textActiveMusicTitle.setText("No song assigned");
            textActiveMusicStatus.setText("Assign music to this workout to get started.");

            buttonActiveMusicPlayPause.setEnabled(false);
            buttonActiveMusicNext.setEnabled(false);
            buttonActiveMusicStop.setEnabled(false);
            return;
        }

        currentMusicIndex = 0;

        buttonActiveMusicPlayPause.setEnabled(true);
        buttonActiveMusicNext.setEnabled(
                assignedWorkoutMusic.size() > 1
        );
        buttonActiveMusicStop.setEnabled(true);

        playWorkoutSong(currentMusicIndex);
    }

    private String formatMusicTime(int milliseconds) {

        int totalSeconds = milliseconds / 1000;

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds
        );
    }


    private void playWorkoutSong(int index) {

        if (assignedWorkoutMusic.isEmpty()) {
            return;
        }

        if (index < 0 || index >= assignedWorkoutMusic.size()) {
            return;
        }

        currentMusicIndex = index;

        stopWorkoutPlayerOnly();

        WorkoutMusic music = assignedWorkoutMusic.get(currentMusicIndex);

        textActiveMusicTitle.setText(music.getSongTitle());
        textActiveMusicStatus.setText("Loading music...");

        buttonActiveMusicPlayPause.setText("Loading...");

        try {

            activeWorkoutPlayer = new MediaPlayer();

            activeWorkoutPlayer.setDataSource(
                    this,
                    Uri.parse(music.getSongUri())
            );

            activeWorkoutPlayer.setOnPreparedListener(player -> {
                isMusicPrepared = true;

                int duration = player.getDuration();

                seekActiveMusic.setMax(duration);
                seekActiveMusic.setProgress(0);
                seekActiveMusic.setEnabled(true);

                textActiveMusicElapsed.setText("0:00");
                textActiveMusicDuration.setText(formatMusicTime(duration));

                player.start();

                updateMusicProgress();

                if (isFinishing() || isDestroyed()) {
                    player.release();
                    return;
                }

                isMusicPrepared = true;

                player.start();

                textActiveMusicStatus.setText("Now playing");
                buttonActiveMusicPlayPause.setText("Pause Music");
            });

            activeWorkoutPlayer.setOnCompletionListener(player -> {

                if (!assignedWorkoutMusic.isEmpty()) {

                    int nextIndex =
                            (currentMusicIndex + 1)
                                    % assignedWorkoutMusic.size();

                    playWorkoutSong(nextIndex);
                }
            });

            activeWorkoutPlayer.setOnErrorListener((player, what, extra) -> {

                textActiveMusicStatus.setText("Unable to play this song.");
                buttonActiveMusicPlayPause.setText("Play Music");

                stopWorkoutPlayerOnly();

                return true;
            });

            activeWorkoutPlayer.prepareAsync();

        } catch (Exception e) {

            Log.e("ActiveWorkout", "Music playback error", e);

            textActiveMusicStatus.setText("Could not load this song.");
            buttonActiveMusicPlayPause.setText("Play Music");

            stopWorkoutPlayerOnly();
        }
    }


    private void toggleWorkoutMusic() {

        try {

            if (activeWorkoutPlayer == null) {

                playWorkoutSong(currentMusicIndex);
                return;
            }

            if (!isMusicPrepared) {
                return;
            }

            if (activeWorkoutPlayer.isPlaying()) {

                activeWorkoutPlayer.pause();

                textActiveMusicStatus.setText("Music paused");
                buttonActiveMusicPlayPause.setText("Resume Music");

            } else {

                activeWorkoutPlayer.start();

                textActiveMusicStatus.setText("Now playing");
                buttonActiveMusicPlayPause.setText("Pause Music");
            }

        } catch (Exception e) {

            Log.e("ActiveWorkout", "Music control error", e);
        }
    }

    private final Runnable musicProgressRunnable = new Runnable() {
        @Override
        public void run() {

            if (activeWorkoutPlayer != null && isMusicPrepared) {

                try {
                    int currentPosition =
                            activeWorkoutPlayer.getCurrentPosition();

                    if (!isUserSeekingMusic) {
                        seekActiveMusic.setProgress(currentPosition);

                        textActiveMusicElapsed.setText(
                                formatMusicTime(currentPosition)
                        );
                    }

                } catch (Exception e) {
                    Log.e("ActiveWorkout", "Progress update error", e);
                }
            }

            musicProgressHandler.postDelayed(this, 500);
        }
    };


    private void updateMusicProgress() {

        musicProgressHandler.removeCallbacks(musicProgressRunnable);

        musicProgressHandler.post(musicProgressRunnable);
    }


    private void playNextWorkoutSong() {

        if (assignedWorkoutMusic.isEmpty()) {
            return;
        }

        int nextIndex =
                (currentMusicIndex + 1)
                        % assignedWorkoutMusic.size();

        playWorkoutSong(nextIndex);
    }


    private void stopWorkoutMusic() {

        stopWorkoutPlayerOnly();

        textActiveMusicStatus.setText("Music stopped");
        buttonActiveMusicPlayPause.setText("Play Music");
    }


    private void stopWorkoutPlayerOnly() {

        musicProgressHandler.removeCallbacks(musicProgressRunnable);

        seekActiveMusic.setProgress(0);
        textActiveMusicElapsed.setText("0:00");

        isMusicPrepared = false;

        if (activeWorkoutPlayer != null) {

            try {
                activeWorkoutPlayer.stop();
            } catch (Exception ignored) {
            }

            activeWorkoutPlayer.release();
            activeWorkoutPlayer = null;
        }
    }



    private void addExerciseCard(
            Exercise exercise,
            WorkoutExercise workoutExercise
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = UiFx.dp(card, 12);

        card.setLayoutParams(cardParams);
        card.setBackgroundColor(
                android.graphics.Color.parseColor("#151F33")
        );

        TextView name = createText(
                exercise.getExerciseName(),
                18,
                "#F8FAFC"
        );

        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView muscle = createText(
                exercise.getMuscleGroup(),
                13,
                "#60A5FA"
        );

        muscle.setPadding(0, 6, 0, 8);

        String prescription =
                workoutExercise.getSets()
                        + " sets × "
                        + workoutExercise.getRepetitions()
                        + " reps";

        TextView details = createText(
                prescription,
                15,
                "#F8FAFC"
        );

        TextView notes = createText(
                workoutExercise.getNotes() == null
                        || workoutExercise.getNotes().trim().isEmpty()
                        ? "Follow the prescribed technique."
                        : workoutExercise.getNotes(),
                13,
                "#94A3B8"
        );

        notes.setPadding(0, 8, 0, 8);

        Button detailsButton = new Button(this);
        detailsButton.setText("View Exercise Details");
        detailsButton.setTextColor(
                android.graphics.Color.WHITE
        );
        detailsButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#1E3A8A")
                )
        );

        detailsButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ActiveWorkoutActivity.this,
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

        card.addView(name);
        card.addView(muscle);
        card.addView(details);
        card.addView(notes);
        card.addView(detailsButton);

        containerExercises.addView(card);
    }

    private TextView createText(
            String value,
            int size,
            String color
    ) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(
                android.graphics.Color.parseColor(color)
        );

        return text;
    }

    private void confirmWorkoutCompletion() {

        new AlertDialog.Builder(this)
                .setTitle("Complete Workout")
                .setMessage(
                        "Have you finished all the prescribed exercises?"
                )
                .setNegativeButton("Not Yet", null)
                .setPositiveButton("Complete", (dialog, which) ->
                        completeWorkout()
                )
                .show();
    }

    private void completeWorkout() {

        buttonCompleteWorkout.setEnabled(false);

        databaseExecutor.execute(() -> {

            try {

                Workout latestWorkout = database.workoutDao()
                        .getWorkoutById(workoutId);

                if (latestWorkout == null
                        || latestWorkout.getUserId() != currentUserId) {

                    runOnUiThread(() ->
                            showErrorAndClose(
                                    "Workout session could not be verified."
                            )
                    );
                    return;
                }

                if (latestWorkout.isCompleted()) {

                    runOnUiThread(() ->
                            showErrorAndClose(
                                    "This workout has already been completed."
                            )
                    );
                    return;
                }

                int updated = database.workoutDao()
                        .updateWorkoutCompletion(workoutId, true);

                runOnUiThread(() -> {

                    if (updated > 0) {

                        Toast.makeText(
                                ActiveWorkoutActivity.this,
                                "Workout completed successfully!",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();

                    } else {

                        buttonCompleteWorkout.setEnabled(true);

                        Toast.makeText(
                                ActiveWorkoutActivity.this,
                                "Could not save workout completion.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    buttonCompleteWorkout.setEnabled(true);

                    Toast.makeText(
                            ActiveWorkoutActivity.this,
                            "An error occurred while saving your workout.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void showErrorAndClose(String message) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Workout Unavailable")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.e("ActiveWorkout", "Workout loading failed: " + message);
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {



        stopWorkoutPlayerOnly();

        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
