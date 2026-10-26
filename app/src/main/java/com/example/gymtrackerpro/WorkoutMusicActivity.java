package com.example.gymtrackerpro;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorkoutMusicActivity extends AppCompatActivity {

    private Spinner spinnerWorkouts;
    private Spinner spinnerSongs;
    private Button buttonChooseSong;
    private Button buttonPlaySong;
    private Button buttonStopSong;
    private Button buttonDeleteSong;
    private TextView textSelectedSong;
    private TextView textAssignedSongs;

    private GymTrackerDatabase database;
    private MediaPlayer mediaPlayer;

    private final List<Workout> workouts = new ArrayList<>();
    private final List<String> workoutLabels = new ArrayList<>();
    private final List<WorkoutMusic> assignedMusic = new ArrayList<>();
    private final List<String> songLabels = new ArrayList<>();

    private ActivityResultLauncher<String[]> musicPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_music);

        spinnerWorkouts = findViewById(R.id.spinnerWorkouts);
        buttonChooseSong = findViewById(R.id.buttonChooseSong);
        buttonDeleteSong = findViewById(R.id.buttonDeleteSong);
        buttonPlaySong = findViewById(R.id.buttonPlaySong);
        buttonStopSong = findViewById(R.id.buttonStopSong);
        textSelectedSong = findViewById(R.id.textSelectedSong);
        textAssignedSongs = findViewById(R.id.textAssignedSongs);
        spinnerSongs = findViewById(R.id.spinnerSongs);

        database = GymTrackerDatabase.getDatabase(this);

        musicPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        saveSelectedSong(uri);
                    }
                }
        );

        buttonChooseSong.setOnClickListener(v -> {
            if (workouts.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please create a workout first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            musicPickerLauncher.launch(new String[]{"audio/*"});
        });

        buttonDeleteSong.setOnClickListener(v -> deleteLatestSong());
        buttonPlaySong.setOnClickListener(v -> playSelectedSong());
        buttonStopSong.setOnClickListener(v -> stopMusic());

        spinnerWorkouts.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id
                    ) {
                        loadAssignedSongs();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                        textAssignedSongs.setText("No workout selected.");
                    }
                }
        );

        loadWorkouts();
    }


    private void deleteLatestSong() {
        int position = spinnerWorkouts.getSelectedItemPosition();

        if (position < 0 || position >= workouts.size()) {
            Toast.makeText(
                    this,
                    "Please select a workout first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (assignedMusic.isEmpty()) {
            Toast.makeText(
                    this,
                    "There are no songs to delete.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int songPosition = spinnerSongs.getSelectedItemPosition();

        if (songPosition < 0 || songPosition >= assignedMusic.size()) {
            Toast.makeText(this, "Please select a song.", Toast.LENGTH_SHORT).show();
            return;
        }

        WorkoutMusic music = assignedMusic.get(songPosition);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage(
                        "Remove \"" + music.getSongTitle()
                                + "\" from this workout?"
                )
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        database.workoutMusicDao().deleteMusic(music);

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    this,
                                    "Song removed from workout.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadAssignedSongs();
                        });
                    }).start();
                })
                .show();
    }


    private void loadWorkouts() {
        new Thread(() -> {
            List<Workout> result =
                    database.workoutDao().getAllWorkouts();

            runOnUiThread(() -> {
                workouts.clear();
                workouts.addAll(result);
                workoutLabels.clear();

                for (Workout workout : workouts) {
                    workoutLabels.add(
                            workout.getWorkoutName()
                                    + " (" + workout.getWorkoutDate() + ")"
                    );
                }

                if (workouts.isEmpty()) {
                    workoutLabels.add("No workouts found");
                    buttonChooseSong.setEnabled(false);
                    buttonPlaySong.setEnabled(false);
                } else {
                    buttonChooseSong.setEnabled(true);
                    buttonPlaySong.setEnabled(true);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        workoutLabels
                );

                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );

                spinnerWorkouts.setAdapter(adapter);

                if (workouts.isEmpty()) {
                    textAssignedSongs.setText(
                            "Create a workout before assigning music."
                    );
                } else {
                    loadAssignedSongs();
                }
            });
        }).start();
    }

    private void saveSelectedSong(Uri uri) {
        int position = spinnerWorkouts.getSelectedItemPosition();

        if (position < 0 || position >= workouts.size()) {
            Toast.makeText(
                    this,
                    "Please select a workout first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException e) {
            // Some providers do not support persistent permissions.
        }

        String songTitle = getFileName(uri);
        Workout selectedWorkout = workouts.get(position);

        WorkoutMusic music = new WorkoutMusic(
                selectedWorkout.getWorkoutId(),
                songTitle,
                "",
                uri.toString()
        );

        textSelectedSong.setText("Selected song: " + songTitle);

        new Thread(() -> {
            database.workoutMusicDao().insertMusic(music);

            runOnUiThread(() -> {
                Toast.makeText(
                        this,
                        "Song assigned to " + selectedWorkout.getWorkoutName(),
                        Toast.LENGTH_SHORT
                ).show();

                loadAssignedSongs();
            });
        }).start();
    }

    private String getFileName(Uri uri) {
        String fileName = null;

        try (Cursor cursor = getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                );

                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = uri.getLastPathSegment();
        }

        return fileName == null ? "Selected audio file" : fileName;
    }


    private void loadAssignedSongs() {
        int position = spinnerWorkouts.getSelectedItemPosition();

        if (position < 0 || position >= workouts.size()) {
            assignedMusic.clear();
            songLabels.clear();
            textAssignedSongs.setText("No workout selected.");
            spinnerSongs.setAdapter(null);
            return;
        }

        int workoutId = workouts.get(position).getWorkoutId();

        new Thread(() -> {
            List<WorkoutMusic> result =
                    database.workoutMusicDao()
                            .getMusicForWorkout(workoutId);

            runOnUiThread(() -> {
                assignedMusic.clear();
                assignedMusic.addAll(result);

                songLabels.clear();

                for (WorkoutMusic music : assignedMusic) {
                    songLabels.add(music.getSongTitle());
                }

                if (assignedMusic.isEmpty()) {
                    textAssignedSongs.setText(
                            "No songs assigned to this workout yet."
                    );
                    songLabels.add("No songs available");
                } else {
                    StringBuilder builder = new StringBuilder();

                    for (int i = 0; i < assignedMusic.size(); i++) {
                        builder.append(i + 1)
                                .append(". ")
                                .append(assignedMusic.get(i).getSongTitle())
                                .append("\n");
                    }

                    textAssignedSongs.setText(builder.toString());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        songLabels
                );

                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );

                spinnerSongs.setAdapter(adapter);

                boolean hasSongs = !assignedMusic.isEmpty();
                buttonPlaySong.setEnabled(hasSongs);
                buttonDeleteSong.setEnabled(hasSongs);
            });
        }).start();
    }



    private void playSelectedSong() {
        int position = spinnerWorkouts.getSelectedItemPosition();

        if (position < 0 || position >= workouts.size()) {
            Toast.makeText(this, "Select a workout first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (assignedMusic.isEmpty()) {
            Toast.makeText(this, "This workout has no assigned music.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Plays the newest song in the selected workout.
        int songPosition = spinnerSongs.getSelectedItemPosition();

        if (songPosition < 0 || songPosition >= assignedMusic.size()) {
            Toast.makeText(this, "Please select a song.", Toast.LENGTH_SHORT).show();
            return;
        }

        WorkoutMusic music = assignedMusic.get(songPosition);

        stopMusic();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(music.getSongUri()));

            mediaPlayer.setOnPreparedListener(player -> {
                player.start();
                Toast.makeText(
                        this,
                        "Playing: " + music.getSongTitle(),
                        Toast.LENGTH_SHORT
                ).show();
            });

            mediaPlayer.setOnCompletionListener(player -> stopMusic());

            mediaPlayer.setOnErrorListener((player, what, extra) -> {
                stopMusic();
                Toast.makeText(
                        this,
                        "Unable to play this audio file.",
                        Toast.LENGTH_LONG
                ).show();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (IOException | IllegalArgumentException e) {
            stopMusic();
            Toast.makeText(
                    this,
                    "Could not open this song. Please select it again.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        stopMusic();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopMusic();
        super.onDestroy();
    }
}
