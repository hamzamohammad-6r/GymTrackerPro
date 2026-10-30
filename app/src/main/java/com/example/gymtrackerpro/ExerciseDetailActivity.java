package com.example.gymtrackerpro;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class ExerciseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXERCISE_NAME = "exercise_name";
    public static final String EXTRA_MUSCLE_GROUP = "muscle_group";

    private TextView textExerciseTitle;
    private TextView textExerciseCategory;
    private TextView textExerciseInstructions;
    private TextView textVideoPlaceholder;
    private VideoView videoExerciseDemo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        textExerciseTitle = findViewById(R.id.textExerciseTitle);
        textExerciseCategory = findViewById(R.id.textExerciseCategory);
        textExerciseInstructions = findViewById(R.id.textExerciseInstructions);
        textVideoPlaceholder = findViewById(R.id.textVideoPlaceholder);
        videoExerciseDemo = findViewById(R.id.videoExerciseDemo);

        Button buttonBackExercises = findViewById(R.id.buttonBackExercises);

        String exerciseName = getIntent().getStringExtra(EXTRA_EXERCISE_NAME);
        String muscleGroup = getIntent().getStringExtra(EXTRA_MUSCLE_GROUP);

        if (exerciseName == null || exerciseName.trim().isEmpty()) {
            exerciseName = "Exercise Details";
        }

        if (muscleGroup == null || muscleGroup.trim().isEmpty()) {
            muscleGroup = "General";
        }

        textExerciseTitle.setText(exerciseName);
        textExerciseCategory.setText(muscleGroup.toUpperCase());

        buttonBackExercises.setOnClickListener(v -> finish());

        setExerciseInstructions(exerciseName);
        loadExerciseVideo(exerciseName);
    }


    private void loadExerciseVideo(String exerciseName) {
        String name = exerciseName.toLowerCase().trim();
        int videoResource = 0;

        if (name.contains("barbell back squat")) {
            videoResource = R.raw.barbell_back_squat;
        } else if (name.contains("bench press")) {
            videoResource = R.raw.bench_press;
        } else if (name.contains("bicep curl") || name.contains("biceps curl")) {
            videoResource = R.raw.bicep_curl;
        } else if (name.contains("deadlift") && !name.contains("romanian")) {
            videoResource = R.raw.deadlift;
        } else if (name.contains("romanian deadlift")
                || name.contains("rdl")) {
            videoResource = R.raw.rdl;
        } else if (name.contains("lat pulldown")
                || name.contains("lat pull-down")) {
            videoResource = R.raw.lat_pulldown;
        } else if (name.contains("push up") || name.contains("push-up")) {
            videoResource = R.raw.push_up;
        } else if (name.contains("shoulder dumbbell press")
                || name.contains("dumbbell shoulder press")) {
            videoResource = R.raw.shoulder_dumbbell_press;
        } else if (name.equals("squat") || name.contains("squat")) {
            videoResource = R.raw.squat;
        }

        if (videoResource == 0) {
            videoExerciseDemo.setVisibility(View.GONE);
            textVideoPlaceholder.setVisibility(View.VISIBLE);
            textVideoPlaceholder.setText(
                    "No offline video is available for this exercise yet."
            );
            return;
        }

        videoExerciseDemo.setVisibility(View.VISIBLE);
        textVideoPlaceholder.setVisibility(View.GONE);

        Uri videoUri = Uri.parse(
                "android.resource://" + getPackageName() + "/" + videoResource
        );

        videoExerciseDemo.setVideoURI(videoUri);

        videoExerciseDemo.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setLooping(false);
            videoExerciseDemo.start();
        });

        videoExerciseDemo.setOnErrorListener((mediaPlayer, what, extra) -> {
            textVideoPlaceholder.setVisibility(View.VISIBLE);
            textVideoPlaceholder.setText(
                    "Unable to play this video. Check the MP4 file format."
            );
            return true;
        });
    }



    private void setExerciseInstructions(String exerciseName) {
        String name = exerciseName.toLowerCase();

        if (name.contains("squat")) {
            textExerciseInstructions.setText(
                    "1. Position the bar securely across your upper back.\n\n" +
                            "2. Stand with your feet about shoulder-width apart.\n\n" +
                            "3. Bend your hips and knees while keeping your back controlled.\n\n" +
                            "4. Push through your feet to return to standing."
            );
        } else if (name.contains("push up") || name.contains("push-up")) {
            textExerciseInstructions.setText(
                    "1. Place your hands slightly wider than shoulder-width.\n\n" +
                            "2. Keep your body straight from head to heels.\n\n" +
                            "3. Lower your chest towards the floor with control.\n\n" +
                            "4. Push back up to the starting position."
            );
        } else if (name.contains("curl")) {
            textExerciseInstructions.setText(
                    "1. Stand upright and hold the weights at your sides.\n\n" +
                            "2. Keep your elbows close to your body.\n\n" +
                            "3. Curl the weights upwards without swinging.\n\n" +
                            "4. Lower them slowly and with control."
            );
        } else {
            textExerciseInstructions.setText(
                    "1. Set up the equipment safely.\n\n" +
                            "2. Start with a manageable weight and controlled movement.\n\n" +
                            "3. Keep correct posture throughout each repetition.\n\n" +
                            "4. Return to the starting position slowly."
            );
        }
    }
}
