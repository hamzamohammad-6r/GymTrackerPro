package com.example.gymtrackerpro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProgramDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROGRAM_ID = "program_id";

    private static final String TAG = "ProgramDetail";

    private GymTrackerDatabase database;
    private ExecutorService databaseExecutor;

    private ProgramCatalog.Program program;
    private Button buttonStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_detail);
        UiFx.applyEdgeToEdge(this);

        program = ProgramCatalog.byId(
                getIntent().getStringExtra(EXTRA_PROGRAM_ID));

        if (program == null) {
            Toast.makeText(this, "Program not found.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = GymTrackerDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();

        bindHeader();
        bindDays();

        buttonStart = findViewById(R.id.buttonStartProgram);
        buttonStart.setOnClickListener(v -> checkThenConfirm());

        UiFx.applyPressEffect(buttonStart);

        findViewById(R.id.buttonBackHome)
                .setOnClickListener(v -> finish());
    }

    private void bindHeader() {
        findViewById(R.id.viewDetailCover)
                .setBackgroundResource(program.coverRes);

        ((ImageView) findViewById(R.id.imageDetailDeco))
                .setImageResource(program.decoRes);

        ((TextView) findViewById(R.id.textDetailTitle))
                .setText(program.title);

        ((TextView) findViewById(R.id.textDetailTagline))
                .setText(program.tagline);

        ((TextView) findViewById(R.id.textInfoDays))
                .setText(String.valueOf(program.daysPerWeek()));

        ((TextView) findViewById(R.id.textInfoWeeks))
                .setText(String.valueOf(program.weeks));

        ((TextView) findViewById(R.id.textInfoLevel))
                .setText(program.level);

        ((TextView) findViewById(R.id.textDetailDescription))
                .setText(program.description);

        ((TextView) findViewById(R.id.textDetailGoal))
                .setText("Goal: " + program.goal);

        ((TextView) findViewById(R.id.textDetailEquipment))
                .setText("Equipment: " + program.equipment);
    }

    private void bindDays() {
        LinearLayout containerDays = findViewById(R.id.containerDays);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < program.days.size(); i++) {
            ProgramCatalog.Day day = program.days.get(i);

            View dayView = inflater.inflate(
                    R.layout.item_program_day, containerDays, false);

            ((TextView) dayView.findViewById(R.id.textDayTitle))
                    .setText("Day " + (i + 1) + " \u00b7 " + day.title);

            ((TextView) dayView.findViewById(R.id.textDayWeekday))
                    .setText(day.weekday);

            LinearLayout exerciseContainer =
                    dayView.findViewById(R.id.containerDayExercises);

            for (ProgramCatalog.Entry entry : day.exercises) {
                View row = inflater.inflate(
                        R.layout.item_program_exercise,
                        exerciseContainer,
                        false);

                MuscleIcons.bind(
                        (ImageView) row.findViewById(R.id.imageMuscle),
                        entry.muscleGroup);

                ((TextView) row.findViewById(R.id.textExName))
                        .setText(entry.exerciseName);

                ((TextView) row.findViewById(R.id.textExNote))
                        .setText(entry.muscleGroup
                                + (entry.note == null || entry.note.isEmpty()
                                ? "" : " \u00b7 " + entry.note));

                ((TextView) row.findViewById(R.id.textExSets))
                        .setText(entry.prescription());

                exerciseContainer.addView(row);
            }

            containerDays.addView(dayView);
        }
    }

    /** Checks whether the program was already added, then asks to confirm. */
    private void checkThenConfirm() {
        buttonStart.setEnabled(false);

        databaseExecutor.execute(() -> {
            boolean alreadyAdded = false;

            try {
                List<Workout> workouts =
                        database.workoutDao().getAllWorkouts();

                String prefix = program.shortName + " - ";

                if (workouts != null) {
                    for (Workout workout : workouts) {
                        String name = workout.getWorkoutName();
                        if (name != null && name.startsWith(prefix)) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to check existing workouts", e);
            }

            final boolean duplicate = alreadyAdded;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                buttonStart.setEnabled(true);
                showConfirmDialog(duplicate);
            });
        });
    }

    private void showConfirmDialog(boolean duplicate) {
        StringBuilder days = new StringBuilder();

        for (int i = 0; i < program.days.size(); i++) {
            if (i > 0) {
                days.append(i == program.days.size() - 1 ? " and " : ", ");
            }
            days.append(program.days.get(i).weekday);
        }

        String message = "This adds " + program.daysPerWeek()
                + " workouts to My Workouts (" + days + "), "
                + "with every exercise, set and rep already filled in. "
                + "Repeat the week for " + program.weeks + " weeks.";

        if (duplicate) {
            message = "You already added this program before. "
                    + "Adding it again creates another week of workouts.\n\n"
                    + message;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Start " + program.title + "?")
                .setMessage(message)
                .setNegativeButton("Not now", null)
                .setPositiveButton("Add to my week",
                        (dialog, which) -> startProgram())
                .show();
    }

    private void startProgram() {
        buttonStart.setEnabled(false);

        databaseExecutor.execute(() -> {
            ProgramImporter.Result result = null;

            try {
                result = ProgramImporter.importProgram(
                        database, program, Calendar.getInstance());
            } catch (Exception e) {
                Log.e(TAG, "Unable to import program", e);
            }

            final ProgramImporter.Result finalResult = result;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                buttonStart.setEnabled(true);

                if (finalResult == null) {
                    Toast.makeText(this,
                            "Sorry, we couldn't add this program.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(this,
                        "Added " + finalResult.workoutsAdded
                                + " workouts. Let's go! \uD83D\uDCAA",
                        Toast.LENGTH_LONG).show();

                startActivity(new Intent(this, WorkoutsActivity.class));
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
