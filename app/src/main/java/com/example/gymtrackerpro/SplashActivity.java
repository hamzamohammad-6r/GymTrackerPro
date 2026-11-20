package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2200;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable navigationRunnable = () -> {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        "GymTrackerSession",
                        MODE_PRIVATE
                );

        String accessToken =
                preferences.getString("accessToken", null);

        String email =
                preferences.getString("email", null);

        boolean isLoggedIn =
                accessToken != null
                        && !accessToken.trim().isEmpty()
                        && email != null
                        && !email.trim().isEmpty();

        Intent intent;

        if (isLoggedIn) {

            intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );

        } else {

            intent = new Intent(
                    SplashActivity.this,
                    LoginActivity.class
            );
        }

        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                Color.parseColor("#0B1120")
        );

        getWindow().getDecorView()
                .setSystemUiVisibility(0);

        setContentView(R.layout.activity_splash);

        handler.postDelayed(
                navigationRunnable,
                SPLASH_DURATION
        );
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigationRunnable);
        super.onDestroy();
    }
}
