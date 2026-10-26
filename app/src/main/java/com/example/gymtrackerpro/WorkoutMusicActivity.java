
package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class WorkoutMusicActivity extends AppCompatActivity {

    private Spinner spinnerWorkouts;
    private Button buttonChooseSong;
    private TextView textSelectedSong;
    private TextView textAssignedSongs;

    private GymTrackerDatabase database;

    private final List<Workout> workouts = new ArrayList<>();
    private final List<String> workoutLabels = new ArrayList<>();

    private String selectedSongTitle = "";
    private String selectedSongUri = "";

    private ActivityResultLauncher<String> musicPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_music);

        spinnerWorkouts = findViewById(R.id.spinnerWorkouts);
        buttonChooseSong = findViewById(R.id.buttonChooseSong);
        textSelectedSong = findViewById(R.id.textSelectedSong);
        textAssignedSongs = findViewById(R.id.textAssignedSongs);

        database = GymTrackerDatabase.getDatabase(this);

        musicPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedSongUri = uri.toString();
                        selectedSongTitle = selectedSongUri;

                        textSelectedSong.setText(
                                "Selected song:\n" + selectedSongTitle
                        );

                        saveSelectedSong();
                    }
                }
        );

        buttonChooseSong.setOnClickListener(v ->
                musicPickerLauncher.launch("audio/*")
        );

        loadWorkouts();
    }

    private void loadWorkouts() {
        new Thread(() -> {
            List<Workout> result = database.workoutDao().getAllWorkouts();

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

                if (workoutLabels.isEmpty()) {
                    workoutLabels.add("No workouts found");
                    buttonChooseSong.setEnabled(false);
                } else {
                    buttonChooseSong.setEnabled(true);
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

                if (!workouts.isEmpty()) {
                    loadAssignedSongs();
                }
            });
        }).start();
    }

    private void saveSelectedSong() {
        int selectedPosition = spinnerWorkouts.getSelectedItemPosition();

        if (workouts.isEmpty()
                || selectedPosition < 0
                || selectedPosition >= workouts.size()) {
            Toast.makeText(
                    this,
                    "Please create and select a workout first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Workout selectedWorkout = workouts.get(selectedPosition);

        WorkoutMusic music = new WorkoutMusic(
                selectedWorkout.getWorkoutId(),
                selectedSongTitle,
                "",
                selectedSongUri
        );

        new Thread(() -> {
            database.workoutMusicDao().insertMusic(music);

            runOnUiThread(() -> {
                Toast.makeText(
                        this,
                        "Song assigned to workout!",
                        Toast.LENGTH_SHORT
                ).show();

                loadAssignedSongs();
            });
        }).start();
    }

    private void loadAssignedSongs() {
        int selectedPosition = spinnerWorkouts.getSelectedItemPosition();

        if (selectedPosition < 0 || selectedPosition >= workouts.size()) {
            textAssignedSongs.setText("No songs assigned yet.");
            return;
        }

        int workoutId = workouts.get(selectedPosition).getWorkoutId();

        new Thread(() -> {
            List<WorkoutMusic> musicList =
                    database.workoutMusicDao().getMusicForWorkout(workoutId);

            runOnUiThread(() -> {
                if (musicList.isEmpty()) {
                    textAssignedSongs.setText("No songs assigned to this workout yet.");
                    return;
                }

                StringBuilder builder = new StringBuilder();

                for (WorkoutMusic music : musicList) {
                    builder.append("• ")
                            .append(music.getSongTitle())
                            .append("\n");
                }

                textAssignedSongs.setText(builder.toString());
            });
        }).start();
    }
}
