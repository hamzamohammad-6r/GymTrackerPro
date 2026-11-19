package com.example.gymtrackerpro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class UiFx {

    private UiFx() {
    }

    /**
     * Applies edge-to-edge system bar padding.
     */
    public static void applyEdgeToEdge(Activity activity) {

        final View content = activity.findViewById(android.R.id.content);

        if (content == null) {
            return;
        }

        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();
        final int bottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {

            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(content);
    }

    /**
     * Smoothly opens another activity.
     */
    @SuppressWarnings("deprecation")
    public static void startActivityWithTransition(
            Activity activity,
            Intent intent
    ) {

        activity.startActivity(intent);

        activity.overridePendingTransition(
                R.anim.screen_enter,
                R.anim.screen_exit
        );
    }

    /**
     * Smoothly closes the current activity.
     */
    @SuppressWarnings("deprecation")
    public static void finishWithTransition(Activity activity) {

        activity.finish();

        activity.overridePendingTransition(
                R.anim.screen_pop_enter,
                R.anim.screen_pop_exit
        );
    }

    /**
     * Press feedback for buttons and cards.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void applyPressEffect(View view) {

        view.setOnTouchListener((v, event) -> {

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:

                    v.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(90)
                            .start();

                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(130)
                            .start();

                    break;
            }

            return false;
        });
    }

    /**
     * Staggered entrance animation for screen content.
     */
    public static void staggerIn(View... views) {

        long delay = 0;

        for (View view : views) {

            if (view == null) {
                continue;
            }

            float offset = 24f * view.getResources()
                    .getDisplayMetrics().density;

            view.setAlpha(0f);
            view.setTranslationY(offset);

            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(380)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            delay += 70;
        }
    }

    /**
     * Converts dp to pixels.
     */
    public static int dp(View view, float value) {

        return Math.round(
                value * view.getResources().getDisplayMetrics().density
        );
    }
}
