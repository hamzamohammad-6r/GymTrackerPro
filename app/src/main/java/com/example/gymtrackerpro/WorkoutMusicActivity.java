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
import android.widget.Toast;

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
            List<Workout> result =
                    database.workoutDao().getAllWorkouts();

            runOnUiThread(() -> {
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
            Toast.makeText(
                    this,
                    "Please select a workout first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Workout selectedWorkout = workouts.get(workoutPosition);
        String songTitle = getFileName(uri);

        if (songTitle == null || songTitle.trim().isEmpty()) {
            songTitle = "Selected audio";
        }

        String finalSongTitle = songTitle;

        new Thread(() -> {
            WorkoutMusic music = new WorkoutMusic(
                    selectedWorkout.getWorkoutId(),
                    finalSongTitle,
                    "Unknown artist",
                    uri.toString()
            );

            database.workoutMusicDao().insertMusic(music);

            runOnUiThread(() -> {
                textSelectedSong.setText(
                        "Selected song: " + finalSongTitle
                );

                Toast.makeText(
                        WorkoutMusicActivity.this,
                        "Song assigned to "
                                + selectedWorkout.getWorkoutName(),
                        Toast.LENGTH_SHORT
                ).show();

                loadAssignedSongs();
            });
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
            List<WorkoutMusic> result =
                    database.workoutMusicDao().getMusicForWorkout(
                            selectedWorkout.getWorkoutId()
                    );

            runOnUiThread(() -> {
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
        }).start();
    }

    // ---------------------------------------------------------
    // PLAY SELECTED SONG
    // ---------------------------------------------------------

    private void playSelectedSong() {
        int songPosition =
                spinnerSongs.getSelectedItemPosition();

        if (songPosition < 0 || songPosition >= assignedMusic.size()) {
            Toast.makeText(
                    this,
                    "Please select a song first.",
                    Toast.LENGTH_SHORT
            ).show();
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

                if (mediaPlayer == player) {
                    player.release();
                    mediaPlayer = null;
                }

                seekBarPlayback.setProgress(1000);
                seekBarPlayback.setEnabled(false);

                textPlaybackTime.setText(
                        formatTime(player.getDuration()) + " / "
                                + formatTime(player.getDuration())
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

                Toast.makeText(
                        WorkoutMusicActivity.this,
                        "Unable to play this audio file. Try selecting it again.",
                        Toast.LENGTH_LONG
                ).show();

                return true;
            });

            textPlaybackStatus.setText("Loading song...");
            mediaPlayer.prepareAsync();

        } catch (IOException | IllegalArgumentException
                 | SecurityException e) {
            stopMusic();

            textPlaybackStatus.setText("Playback status: Error");

            Toast.makeText(
                    this,
                    "Could not open this audio file. Please select it again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ---------------------------------------------------------
    // PAUSE AND RESUME
    // ---------------------------------------------------------

    private void togglePauseResume() {
        if (mediaPlayer == null) {
            Toast.makeText(
                    this,
                    "Play a song first.",
                    Toast.LENGTH_SHORT
            ).show();
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
            Toast.makeText(
                    this,
                    "Unable to pause or resume this song.",
                    Toast.LENGTH_SHORT
            ).show();
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
            Toast.makeText(
                    this,
                    "Please select a song to delete.",
                    Toast.LENGTH_SHORT
            ).show();
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
                        database.workoutMusicDao().deleteMusic(music);

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    WorkoutMusicActivity.this,
                                    "Song deleted.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadAssignedSongs();
                        });
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
