package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
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
                getIntent().getStringExtra(EXTRA_PROGRAM_ID)
        );

        if (program == null) {
            Toast.makeText(
                    this,
                    "Program not found.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

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
                    R.layout.item_program_day,
                    containerDays,
                    false
            );

            ((TextView) dayView.findViewById(R.id.textDayTitle))
                    .setText("Day " + (i + 1) + " · " + day.title);

            ((TextView) dayView.findViewById(R.id.textDayWeekday))
                    .setText(day.weekday);

            LinearLayout exerciseContainer =
                    dayView.findViewById(R.id.containerDayExercises);

            for (ProgramCatalog.Entry entry : day.exercises) {

                View row = inflater.inflate(
                        R.layout.item_program_exercise,
                        exerciseContainer,
                        false
                );

                MuscleIcons.bind(
                        (ImageView) row.findViewById(R.id.imageMuscle),
                        entry.muscleGroup
                );

                ((TextView) row.findViewById(R.id.textExName))
                        .setText(entry.exerciseName);

                ((TextView) row.findViewById(R.id.textExNote))
                        .setText(
                                entry.muscleGroup
                                        + (entry.note == null || entry.note.isEmpty()
                                        ? ""
                                        : " · " + entry.note)
                        );

                ((TextView) row.findViewById(R.id.textExSets))
                        .setText(entry.prescription());

                exerciseContainer.addView(row);
            }

            containerDays.addView(dayView);
        }
    }

    /**
     * Opens the confirmation dialog before saving a programme.
     */
    private void checkThenConfirm() {

        if (buttonStart == null) {
            return;
        }

        showConfirmDialog();
    }

    /**
     * Confirms that the user wants to start the selected programme.
     */
    private void showConfirmDialog() {

        StringBuilder days = new StringBuilder();

        for (int i = 0; i < program.days.size(); i++) {

            if (i > 0) {
                days.append(
                        i == program.days.size() - 1
                                ? " and "
                                : ", "
                );
            }

            days.append(program.days.get(i).weekday);
        }

        String message =
                "You are about to start " + program.title + ".\n\n"
                        + "Training days: " + days + "\n"
                        + "Duration: " + program.weeks + " weeks\n"
                        + "Sessions per week: " + program.daysPerWeek()
                        + "\n\n"
                        + "Your exercises, sets and repetitions are "
                        + "prescribed and cannot be changed.\n\n"
                        + "If you already have an active programme, "
                        + "this will replace it. Your previous workout "
                        + "history will be preserved.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Start " + program.title + "?")
                .setMessage(message)
                .setNegativeButton("Not now", null)
                .setPositiveButton(
                        "Start Programme",
                        (dialog, which) -> startProgram()
                )
                .show();
    }

    /**
     * Saves the selected programme for the currently logged-in user.
     * Existing workout history is preserved.
     */
    private void startProgram() {

        if (databaseExecutor == null || databaseExecutor.isShutdown()) {
            return;
        }

        buttonStart.setEnabled(false);

        databaseExecutor.execute(() -> {

            String errorMessage = null;
            boolean success = false;

            try {

                SharedPreferences session = getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

                String loggedInEmail = session.getString(
                        "email",
                        null
                );

                int userId = UserProgramManager.getCurrentUserId(
                        database,
                        loggedInEmail
                );

                UserProgramManager.selectProgram(
                        database,
                        userId,
                        program.id
                );

                success = true;

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Unable to save selected programme",
                        e
                );

                errorMessage = e.getMessage();
            }

            final boolean saved = success;
            final String error = errorMessage;

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                buttonStart.setEnabled(true);

                if (!saved) {

                    Toast.makeText(
                            this,
                            error != null
                                    ? error
                                    : "Could not save your programme.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                Toast.makeText(
                        this,
                        "Your programme has been saved!",
                        Toast.LENGTH_SHORT
                ).show();

                startActivity(
                        new Intent(
                                ProgramDetailActivity.this,
                                WorkoutsActivity.class
                        )
                );

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
