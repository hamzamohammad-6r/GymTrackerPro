package com.example.gymtrackerpro;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimerActivity extends AppCompatActivity {

    private TextView textStopwatch;
    private TextView textStopwatchStatus;
    private TextView textRestTimer;

    private Button buttonStopwatchStart;
    private Button buttonStopwatchReset;

    private Button buttonRestStart;
    private Button buttonRestReset;

    private Button buttonRest30;
    private Button buttonRest60;
    private Button buttonRest90;
    private Button buttonRest120;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Stopwatch variables
    private long stopwatchElapsed = 0;
    private long stopwatchStartTime = 0;
    private boolean stopwatchRunning = false;

    // Rest timer variables
    private long restRemaining = 60000;
    private long restEndTime = 0;
    private boolean restRunning = false;

    // Stopwatch updater
    private final Runnable stopwatchRunnable = new Runnable() {
        @Override
        public void run() {

            if (!stopwatchRunning) return;

            long elapsed = stopwatchElapsed
                    + (SystemClock.elapsedRealtime() - stopwatchStartTime);

            updateStopwatchDisplay(elapsed);

            handler.postDelayed(this, 30);
        }
    };

    // Rest timer updater
    private final Runnable restRunnable = new Runnable() {
        @Override
        public void run() {

            if (!restRunning) return;

            restRemaining = Math.max(
                    0,
                    restEndTime - SystemClock.elapsedRealtime()
            );

            updateRestDisplay();

            if (restRemaining <= 0) {

                restRunning = false;

                buttonRestStart.setText("START");
                return;
            }

            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply dashboard edge-to-edge styling
        UiFx.applyEdgeToEdge(this);

        // Midnight Blue status bar
        getWindow().setStatusBarColor(Color.parseColor("#0B1120"));
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_timer);

        initializeViews();

        setupStopwatch();

        setupRestTimer();

        setupBottomNavigation();

        updateStopwatchDisplay(0);

        updateRestDisplay();
    }

    private void initializeViews() {

        textStopwatch = findViewById(R.id.textStopwatch);
        textStopwatchStatus = findViewById(R.id.textStopwatchStatus);
        textRestTimer = findViewById(R.id.textRestTimer);

        buttonStopwatchStart = findViewById(R.id.buttonStopwatchStart);
        buttonStopwatchReset = findViewById(R.id.buttonStopwatchReset);

        buttonRestStart = findViewById(R.id.buttonRestStart);
        buttonRestReset = findViewById(R.id.buttonRestReset);

        buttonRest30 = findViewById(R.id.buttonRest30);
        buttonRest60 = findViewById(R.id.buttonRest60);
        buttonRest90 = findViewById(R.id.buttonRest90);
        buttonRest120 = findViewById(R.id.buttonRest120);
    }

    // STOPWATCH

    private void setupStopwatch() {

        buttonStopwatchStart.setOnClickListener(v -> {

            if (!stopwatchRunning) {

                stopwatchStartTime = SystemClock.elapsedRealtime();

                stopwatchRunning = true;

                buttonStopwatchStart.setText("PAUSE");

                textStopwatchStatus.setText("Stopwatch running");

                handler.post(stopwatchRunnable);

            } else {

                stopwatchElapsed +=
                        SystemClock.elapsedRealtime() - stopwatchStartTime;

                stopwatchRunning = false;

                handler.removeCallbacks(stopwatchRunnable);

                buttonStopwatchStart.setText("RESUME");

                textStopwatchStatus.setText("Stopwatch paused");

                updateStopwatchDisplay(stopwatchElapsed);
            }
        });

        buttonStopwatchReset.setOnClickListener(v -> {

            stopwatchRunning = false;

            stopwatchElapsed = 0;

            handler.removeCallbacks(stopwatchRunnable);

            buttonStopwatchStart.setText("START");

            textStopwatchStatus.setText("Ready when you are");

            updateStopwatchDisplay(0);
        });
    }

    private void updateStopwatchDisplay(long elapsed) {

        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;

        long centiseconds = (elapsed % 1000) / 10;

        textStopwatch.setText(String.format(
                Locale.getDefault(),
                "%02d:%02d.%02d",
                minutes,
                seconds,
                centiseconds
        ));
    }

    // REST TIMER

    private void setupRestTimer() {

        buttonRest30.setOnClickListener(v -> selectRestTime(30));

        buttonRest60.setOnClickListener(v -> selectRestTime(60));

        buttonRest90.setOnClickListener(v -> selectRestTime(90));

        buttonRest120.setOnClickListener(v -> selectRestTime(120));

        buttonRestStart.setOnClickListener(v -> {

            if (!restRunning) {

                if (restRemaining <= 0) return;

                restEndTime = SystemClock.elapsedRealtime() + restRemaining;

                restRunning = true;

                buttonRestStart.setText("PAUSE");

                handler.post(restRunnable);

            } else {

                restRemaining = Math.max(
                        0,
                        restEndTime - SystemClock.elapsedRealtime()
                );

                restRunning = false;

                handler.removeCallbacks(restRunnable);

                buttonRestStart.setText("RESUME");

                updateRestDisplay();
            }
        });

        buttonRestReset.setOnClickListener(v -> {

            restRunning = false;

            handler.removeCallbacks(restRunnable);

            restRemaining = 60000;

            buttonRestStart.setText("START");

            updateRestDisplay();
        });
    }

    private void selectRestTime(int seconds) {

        restRunning = false;

        handler.removeCallbacks(restRunnable);

        restRemaining = seconds * 1000L;

        buttonRestStart.setText("START");

        updateRestDisplay();
    }

    private void updateRestDisplay() {

        long totalSeconds = (restRemaining + 999) / 1000;

        long minutes = totalSeconds / 60;

        long seconds = totalSeconds % 60;

        textRestTimer.setText(String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        ));
    }

    // BOTTOM NAVIGATION

    private void setupBottomNavigation() {

        findViewById(R.id.navHome).setOnClickListener(v ->
                openScreen(MainActivity.class));

        findViewById(R.id.navProgress).setOnClickListener(v ->
                openScreen(WorkoutHistoryActivity.class));

        findViewById(R.id.navTimer).setOnClickListener(v -> {
            // Already on Timer screen
        });

        findViewById(R.id.navProfile).setOnClickListener(v ->
                openScreen(ProfileActivity.class));
    }

    private void openScreen(Class<?> activityClass) {

        Intent intent = new Intent(TimerActivity.this, activityClass);

        UiFx.startActivityWithTransition(this, intent);
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
