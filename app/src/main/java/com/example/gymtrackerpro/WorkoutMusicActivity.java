package com.example.gymtrackerpro;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutMusicActivity extends AppCompatActivity {

    private Spinner spinnerWorkouts;
    private Spinner spinnerSongs;

    private Button buttonChooseSong;
    private Button buttonPlaySong;
    private Button buttonPauseResumeSong;
    private Button buttonStopSong;
    private Button buttonDeleteSong;
    private Button buttonRefreshSongs;

    private TextView textSelectedSong;
    private TextView textAssignedSongs;
    private TextView textPlaybackStatus;
    private TextView textPlaybackTime;

    private SeekBar seekBarPlayback;

    private GymTrackerDatabase database;
    private MediaPlayer mediaPlayer;

    private final List<Workout> workouts = new ArrayList<>();
    private final List<String> workoutLabels = new ArrayList<>();

    private final List<WorkoutMusic> assignedMusic = new ArrayList<>();
    private final List<String> songLabels = new ArrayList<>();

    private ActivityResultLauncher<String[]> musicPickerLauncher;

    private final Handler playbackHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable playbackUpdater = new Runnable() {
        @Override
        public void run() {

            if (mediaPlayer == null) {
                return;
            }

            try {

                if (mediaPlayer.isPlaying()) {

                    int position = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();

                    if (duration > 0) {

                        seekBarPlayback.setProgress(
                                (int) ((position * 1000L) / duration)
                        );

                        textPlaybackTime.setText(
                                formatTime(position) + " / "
                                        + formatTime(duration)
                        );
                    }
                }

                playbackHandler.postDelayed(this, 500);

            } catch (IllegalStateException ignored) {
                // The player may have been released.
            }
        }
    };

    private String formatTime(int milliseconds) {

        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_workout_music);

        TextView buttonBackArrow = findViewById(R.id.buttonBackArrow);
        buttonBackArrow.setOnClickListener(v -> finish());

        UiFx.applyEdgeToEdge(this);

        // Connect XML views.
        spinnerWorkouts = findViewById(R.id.spinnerWorkouts);
        spinnerSongs = findViewById(R.id.spinnerSongs);

        buttonChooseSong = findViewById(R.id.buttonChooseSong);
        buttonPlaySong = findViewById(R.id.buttonPlaySong);

        buttonPauseResumeSong =
                findViewById(R.id.buttonPauseResumeSong);

        buttonStopSong = findViewById(R.id.buttonStopSong);
        buttonDeleteSong = findViewById(R.id.buttonDeleteSong);
        buttonRefreshSongs = findViewById(R.id.buttonRefreshSongs);

        textSelectedSong = findViewById(R.id.textSelectedSong);
        textAssignedSongs = findViewById(R.id.textAssignedSongs);
        textPlaybackStatus = findViewById(R.id.textPlaybackStatus);
        textPlaybackTime = findViewById(R.id.textPlaybackTime);

        seekBarPlayback = findViewById(R.id.seekBarPlayback);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        // Initial playback display.
        textPlaybackStatus.setText("Playback status: Stopped");
        textPlaybackTime.setText("00:00 / 00:00");

        seekBarPlayback.setProgress(0);
        seekBarPlayback.setEnabled(false);

        buttonPlaySong.setEnabled(false);
        buttonPauseResumeSong.setEnabled(false);
        buttonStopSong.setEnabled(false);
        buttonDeleteSong.setEnabled(false);

        // Open the device audio picker.
        musicPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        saveSelectedSong(uri);
                    }
                }
        );

        buttonChooseSong.setOnClickListener(v ->
                musicPickerLauncher.launch(new String[]{"audio/*"})
        );

        buttonPlaySong.setOnClickListener(v ->
                playSelectedSong()
        );

        buttonPauseResumeSong.setOnClickListener(v ->
                togglePauseResume()
        );

        buttonStopSong.setOnClickListener(v ->
                stopMusic()
        );

        buttonDeleteSong.setOnClickListener(v ->
                deleteSelectedSong()
        );

        buttonRefreshSongs.setOnClickListener(v ->
                loadAssignedSongs()
        );

        // Reload songs when the selected workout changes.
        spinnerWorkouts.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        stopMusic();
                        loadAssignedSongs();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                        // Nothing to do.
                    }
                }
        );

        // Allow seeking through a song.
        seekBarPlayback.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {

                        if (fromUser && mediaPlayer != null) {

                            try {

                                int duration = mediaPlayer.getDuration();

                                if (duration > 0) {

                                    int position = (int) (
                                            duration * (progress / 1000.0)
                                    );

                                    textPlaybackTime.setText(
                                            formatTime(position) + " / "
                                                    + formatTime(duration)
                                    );
                                }

                            } catch (IllegalStateException ignored) {
                                // Player may no longer be available.
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Nothing needed here.
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                        if (mediaPlayer != null) {

                            try {

                                int duration = mediaPlayer.getDuration();

                                if (duration > 0) {

                                    int position = (int) (
                                            duration
                                                    * (seekBar.getProgress()
                                                    / 1000.0)
                                    );

                                    mediaPlayer.seekTo(position);
                                }

                            } catch (IllegalStateException ignored) {
                                // Player may no longer be available.
                            }
                        }
                    }
                }
        );

        loadWorkouts();
    }

    // ---------------------------------------------------------
    // LOAD WORKOUTS
    // ---------------------------------------------------------

    private void loadWorkouts() {

        new Thread(() -> {

            try {

                List<Workout> result =
                        database.workoutDao().getAllWorkouts();

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    workouts.clear();
                    workoutLabels.clear();

                    workouts.addAll(result);

                    for (Workout workout : workouts) {

                        workoutLabels.add(
                                workout.getWorkoutName()
                                        + " - "
                                        + workout.getWorkoutDate()
                        );
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            WorkoutMusicActivity.this,
                            android.R.layout.simple_spinner_item,
                            workoutLabels
                    );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerWorkouts.setAdapter(adapter);

                    boolean hasWorkouts = !workouts.isEmpty();

                    buttonChooseSong.setEnabled(hasWorkouts);
                    buttonRefreshSongs.setEnabled(hasWorkouts);

                    if (!hasWorkouts) {

                        assignedMusic.clear();
                        songLabels.clear();

                        spinnerSongs.setAdapter(new ArrayAdapter<>(
                                WorkoutMusicActivity.this,
                                android.R.layout.simple_spinner_item,
                                songLabels
                        ));

                        textAssignedSongs.setText(
                                "No workouts found. Create a workout first."
                        );

                        buttonPlaySong.setEnabled(false);
                        buttonPauseResumeSong.setEnabled(false);
                        buttonStopSong.setEnabled(false);
                        buttonDeleteSong.setEnabled(false);

                    } else {

                        loadAssignedSongs();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        CustomNotification.show(
                                this,
                                "Loading Failed",
                                "Could not load your workouts. Please try again."
                        );
                    }
                });
            }

        }).start();
    }

    // ---------------------------------------------------------
    // SELECT AND SAVE A SONG
    // ---------------------------------------------------------

    private void saveSelectedSong(Uri uri) {

        try {

            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

        } catch (SecurityException ignored) {
            // Continue if the provider does not support persistence.
        }

        int workoutPosition =
                spinnerWorkouts.getSelectedItemPosition();

        if (workoutPosition < 0 || workoutPosition >= workouts.size()) {

            CustomNotification.show(
                    this,
                    "Workout Required",
                    "Please select a workout before assigning music."
            );

            return;
        }

        Workout selectedWorkout = workouts.get(workoutPosition);

        String songTitle = getFileName(uri);

        if (songTitle == null || songTitle.trim().isEmpty()) {
            songTitle = "Selected audio";
        }

        String finalSongTitle = songTitle;

        new Thread(() -> {

            try {

                WorkoutMusic music = new WorkoutMusic(
                        selectedWorkout.getWorkoutId(),
                        finalSongTitle,
                        "Unknown artist",
                        uri.toString()
                );

                database.workoutMusicDao().insertMusic(music);

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    textSelectedSong.setText(
                            "Selected song: " + finalSongTitle
                    );

                    CustomNotification.show(
                            this,
                            "Song Assigned!",
                            "Your song has been assigned to "
                                    + selectedWorkout.getWorkoutName() + "."
                    );

                    loadAssignedSongs();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        CustomNotification.show(
                                this,
                                "Assignment Failed",
                                "Could not assign this song. Please try again."
                        );
                    }
                });
            }

        }).start();
    }

    private String getFileName(Uri uri) {

        String fileName = null;

        try (Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        )) {

            if (cursor != null && cursor.moveToFirst()) {

                int nameIndex = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                );

                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
            }

        } catch (Exception ignored) {
            // Use a fallback title if the filename is unavailable.
        }

        return fileName;
    }

    // ---------------------------------------------------------
    // LOAD SONGS FOR THE SELECTED WORKOUT
    // ---------------------------------------------------------

    private void loadAssignedSongs() {

        int workoutPosition =
                spinnerWorkouts.getSelectedItemPosition();

        if (workoutPosition < 0 || workoutPosition >= workouts.size()) {
            return;
        }

        Workout selectedWorkout = workouts.get(workoutPosition);

        new Thread(() -> {

            try {

                List<WorkoutMusic> result =
                        database.workoutMusicDao().getMusicForWorkout(
                                selectedWorkout.getWorkoutId()
                        );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    assignedMusic.clear();
                    songLabels.clear();

                    assignedMusic.addAll(result);

                    for (WorkoutMusic music : assignedMusic) {
                        songLabels.add(music.getSongTitle());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            WorkoutMusicActivity.this,
                            android.R.layout.simple_spinner_item,
                            songLabels
                    );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerSongs.setAdapter(adapter);

                    if (assignedMusic.isEmpty()) {

                        textAssignedSongs.setText(
                                "No songs assigned to "
                                        + selectedWorkout.getWorkoutName()
                                        + "."
                        );

                        textSelectedSong.setText("Selected song: None");

                        buttonPlaySong.setEnabled(false);
                        buttonPauseResumeSong.setEnabled(false);
                        buttonDeleteSong.setEnabled(false);

                    } else {

                        textAssignedSongs.setText(
                                "Songs assigned to "
                                        + selectedWorkout.getWorkoutName()
                                        + ": " + assignedMusic.size()
                        );

                        buttonPlaySong.setEnabled(true);
                        buttonDeleteSong.setEnabled(true);
                    }

                    buttonStopSong.setEnabled(mediaPlayer != null);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        CustomNotification.show(
                                this,
                                "Loading Failed",
                                "Could not load the assigned songs. Please refresh and try again."
                        );
                    }
                });
            }

        }).start();
    }

    // ---------------------------------------------------------
    // PLAY SELECTED SONG
    // ---------------------------------------------------------

    private void playSelectedSong() {

        int songPosition =
                spinnerSongs.getSelectedItemPosition();

        if (songPosition < 0 || songPosition >= assignedMusic.size()) {

            CustomNotification.show(
                    this,
                    "Song Required",
                    "Please select a song to continue."
            );

            return;
        }

        WorkoutMusic music = assignedMusic.get(songPosition);

        stopMusic();

        try {

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setDataSource(
                    this,
                    Uri.parse(music.getSongUri())
            );

            mediaPlayer.setOnPreparedListener(player -> {

                if (mediaPlayer != player) {
                    player.release();
                    return;
                }

                int duration = player.getDuration();

                seekBarPlayback.setEnabled(duration > 0);
                seekBarPlayback.setProgress(0);

                textPlaybackTime.setText(
                        formatTime(0) + " / " + formatTime(duration)
                );

                player.start();

                textPlaybackStatus.setText(
                        "Now Playing: " + music.getSongTitle()
                );

                buttonStopSong.setEnabled(true);
                buttonPauseResumeSong.setEnabled(true);
                buttonPauseResumeSong.setText("Pause Song");

                playbackHandler.removeCallbacks(playbackUpdater);
                playbackHandler.post(playbackUpdater);
            });

            mediaPlayer.setOnCompletionListener(player -> {

                playbackHandler.removeCallbacks(playbackUpdater);

                int duration = player.getDuration();

                if (mediaPlayer == player) {
                    player.release();
                    mediaPlayer = null;
                }

                seekBarPlayback.setProgress(1000);
                seekBarPlayback.setEnabled(false);

                textPlaybackTime.setText(
                        formatTime(duration) + " / "
                                + formatTime(duration)
                );

                textPlaybackStatus.setText(
                        "Playback status: Finished"
                );

                buttonStopSong.setEnabled(false);
                buttonPauseResumeSong.setEnabled(false);
                buttonPauseResumeSong.setText("Pause Song");
            });

            mediaPlayer.setOnErrorListener((player, what, extra) -> {

                stopMusic();

                textPlaybackStatus.setText(
                        "Playback status: Error"
                );

                CustomNotification.show(
                        WorkoutMusicActivity.this,
                        "Playback Error",
                        "Unable to play this audio file. Try selecting it again."
                );

                return true;
            });

            textPlaybackStatus.setText("Loading song...");

            mediaPlayer.prepareAsync();

        } catch (IOException | IllegalArgumentException
                 | SecurityException e) {

            stopMusic();

            textPlaybackStatus.setText("Playback status: Error");

            CustomNotification.show(
                    this,
                    "File Error",
                    "Could not open this audio file. Please select it again."
            );
        }
    }

    // ---------------------------------------------------------
    // PAUSE AND RESUME
    // ---------------------------------------------------------

    private void togglePauseResume() {

        if (mediaPlayer == null) {

            CustomNotification.show(
                    this,
                    "No Song Playing",
                    "Select and play a song first."
            );

            return;
        }

        try {

            if (mediaPlayer.isPlaying()) {

                mediaPlayer.pause();

                buttonPauseResumeSong.setText("Resume Song");

                textPlaybackStatus.setText(
                        "Playback status: Paused"
                );

                playbackHandler.removeCallbacks(playbackUpdater);

            } else {

                mediaPlayer.start();

                buttonPauseResumeSong.setText("Pause Song");

                textPlaybackStatus.setText(
                        "Playback status: Playing"
                );

                playbackHandler.removeCallbacks(playbackUpdater);
                playbackHandler.post(playbackUpdater);
            }

        } catch (IllegalStateException e) {

            CustomNotification.show(
                    this,
                    "Playback Error",
                    "Unable to pause or resume this song."
            );
        }
    }

    // ---------------------------------------------------------
    // STOP PLAYBACK
    // ---------------------------------------------------------

    private void stopMusic() {

        playbackHandler.removeCallbacks(playbackUpdater);

        if (mediaPlayer != null) {

            try {

                mediaPlayer.setOnPreparedListener(null);
                mediaPlayer.setOnCompletionListener(null);
                mediaPlayer.setOnErrorListener(null);

                mediaPlayer.release();

            } catch (Exception ignored) {
                // The player may already have been released.
            }

            mediaPlayer = null;
        }

        if (seekBarPlayback != null) {
            seekBarPlayback.setProgress(0);
            seekBarPlayback.setEnabled(false);
        }

        if (textPlaybackTime != null) {
            textPlaybackTime.setText("00:00 / 00:00");
        }

        if (textPlaybackStatus != null) {

            textPlaybackStatus.setText(
                    "Playback status: Stopped"
            );
        }

        if (buttonStopSong != null) {
            buttonStopSong.setEnabled(false);
        }

        if (buttonPauseResumeSong != null) {

            buttonPauseResumeSong.setEnabled(false);
            buttonPauseResumeSong.setText("Pause Song");
        }
    }

    // ---------------------------------------------------------
    // DELETE SELECTED SONG
    // ---------------------------------------------------------

    private void deleteSelectedSong() {

        int songPosition =
                spinnerSongs.getSelectedItemPosition();

        if (songPosition < 0 || songPosition >= assignedMusic.size()) {

            CustomNotification.show(
                    this,
                    "Song Required",
                    "Please select a song to delete."
            );

            return;
        }

        WorkoutMusic music = assignedMusic.get(songPosition);

        new AlertDialog.Builder(this)
                .setTitle("Delete song")
                .setMessage(
                        "Remove \"" + music.getSongTitle()
                                + "\" from this workout?"
                )
                .setNegativeButton("Cancel", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Delete", (dialog, which) -> {

                    stopMusic();

                    new Thread(() -> {

                        try {

                            database.workoutMusicDao().deleteMusic(music);

                            runOnUiThread(() -> {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                CustomNotification.show(
                                        this,
                                        "Song Deleted!",
                                        "The song was successfully removed from this workout."
                                );

                                loadAssignedSongs();
                            });

                        } catch (Exception e) {

                            runOnUiThread(() -> {

                                if (!isFinishing() && !isDestroyed()) {

                                    CustomNotification.show(
                                            this,
                                            "Delete Failed",
                                            "Could not delete this song. Please try again."
                                    );
                                }
                            });
                        }

                    }).start();
                })
                .show();
    }

    // ---------------------------------------------------------
    // CLEAN UP WHEN LEAVING THE SCREEN
    // ---------------------------------------------------------

    @Override
    protected void onStop() {
        stopMusic();
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        playbackHandler.removeCallbacks(playbackUpdater);

        stopMusic();

        super.onDestroy();
    }
}
