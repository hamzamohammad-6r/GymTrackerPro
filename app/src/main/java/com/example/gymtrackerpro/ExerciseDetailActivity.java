package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ExerciseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXERCISE_NAME = "exercise_name";
    public static final String EXTRA_MUSCLE_GROUP = "muscle_group";

    private TextView textExerciseTitle;
    private TextView textExerciseCategory;
    private TextView textExerciseInstructions;
    private ImageView imageExerciseIllustration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        textExerciseTitle = findViewById(R.id.textExerciseTitle);
        textExerciseCategory = findViewById(R.id.textExerciseCategory);
        textExerciseInstructions =
                findViewById(R.id.textExerciseInstructions);
        imageExerciseIllustration =
                findViewById(R.id.imageExerciseIllustration);

        Button buttonBackExercises =
                findViewById(R.id.buttonBackExercises);

        String exerciseName =
                getIntent().getStringExtra(EXTRA_EXERCISE_NAME);

        String muscleGroup =
                getIntent().getStringExtra(EXTRA_MUSCLE_GROUP);

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
        loadExerciseIllustration(exerciseName);
    }

    private void loadExerciseIllustration(String exerciseName) {

        String name = exerciseName.toLowerCase().trim();
        int imageResource = R.drawable.exercise_placeholder;

        if (name.contains("barbell back squat")) {
            imageResource = R.drawable.barbell_back_squat;

        } else if (name.contains("bench press")) {
            imageResource = R.drawable.bench_press;

        } else if (name.contains("bicep curl")
                || name.contains("biceps curl")) {
            imageResource = R.drawable.bicep_curl;

        } else if (name.contains("romanian deadlift")
                || name.equals("rdl")
                || name.contains("rdl")) {
            imageResource = R.drawable.barbell_rdl;

        } else if (name.contains("deadlift")) {
            imageResource = R.drawable.deadlift;

        } else if (name.contains("lat pulldown")
                || name.contains("lat pull-down")) {
            imageResource = R.drawable.lat_pulldown;

        } else if (name.contains("push up")
                || name.contains("push-up")
                || name.contains("pushup")) {
            imageResource = R.drawable.push_up;

        } else if (name.contains("shoulder dumbbell press")
                || name.contains("dumbbell shoulder press")) {
            imageResource = R.drawable.shoulder_dumbbell_press;

        } else if (name.equals("squat")
                || name.contains("squat")) {
            imageResource = R.drawable.squat;
        }

        imageExerciseIllustration.setImageResource(imageResource);
        imageExerciseIllustration.setContentDescription(
                exerciseName + " illustration"
        );
    }

    private void setExerciseInstructions(String exerciseName) {

        String name = exerciseName.toLowerCase();

        if (name.contains("squat")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Position your feet approximately shoulder-width apart.\n\n" +
                            "2. Brace your core and keep your back controlled.\n\n" +
                            "3. Bend your hips and knees to lower yourself with control.\n\n" +
                            "4. Push through your feet to return to standing.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Use a manageable weight and keep your knees aligned with your feet."
            );

        } else if (name.contains("push up")
                || name.contains("push-up")
                || name.contains("pushup")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Place your hands slightly wider than shoulder-width.\n\n" +
                            "2. Keep your body straight from head to heels.\n\n" +
                            "3. Lower your chest towards the floor with control.\n\n" +
                            "4. Push back up to the starting position.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Avoid letting your hips sag during the movement."
            );

        } else if (name.contains("curl")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Stand upright with the weights at your sides.\n\n" +
                            "2. Keep your elbows close to your body.\n\n" +
                            "3. Curl the weights upwards without swinging.\n\n" +
                            "4. Lower them slowly and with control.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Choose a weight that lets you maintain controlled movement."
            );

        } else if (name.contains("deadlift")
                || name.contains("rdl")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Stand with your feet about hip-width apart.\n\n" +
                            "2. Brace your core and keep your back in a controlled position.\n\n" +
                            "3. Hinge at your hips while keeping the weight close to your legs.\n\n" +
                            "4. Extend your hips to return to standing.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Avoid rounding your back or using a weight you cannot control."
            );

        } else if (name.contains("bench press")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Lie securely on the bench with your feet planted.\n\n" +
                            "2. Grip the bar evenly and lower it with control.\n\n" +
                            "3. Keep your wrists stable as you press upwards.\n\n" +
                            "4. Return the bar safely to the rack.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Use a spotter when lifting heavy weights."
            );

        } else if (name.contains("lat pulldown")
                || name.contains("lat pull-down")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Sit securely and position your thighs under the pads.\n\n" +
                            "2. Grip the bar wider than shoulder-width if comfortable.\n\n" +
                            "3. Pull the bar towards your upper chest without swinging.\n\n" +
                            "4. Slowly return the bar to the starting position.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Avoid pulling the bar behind your neck."
            );

        } else if (name.contains("shoulder")
                || name.contains("dumbbell press")) {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Hold the dumbbells at shoulder height.\n\n" +
                            "2. Brace your core and keep your torso stable.\n\n" +
                            "3. Press the dumbbells upwards with control.\n\n" +
                            "4. Lower them slowly back to shoulder height.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Use a comfortable range of motion without arching your back excessively."
            );

        } else {
            textExerciseInstructions.setText(
                    "HOW TO PERFORM\n\n" +
                            "1. Set up the equipment safely.\n\n" +
                            "2. Start with a manageable weight.\n\n" +
                            "3. Maintain controlled movement and good posture.\n\n" +
                            "4. Return slowly to the starting position.\n\n" +
                            "SAFETY TIP\n\n" +
                            "Stop if you feel sharp pain and check your technique."
            );
        }
    }
}
