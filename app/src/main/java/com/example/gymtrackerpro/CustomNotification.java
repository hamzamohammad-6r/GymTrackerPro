package com.example.gymtrackerpro;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class CustomNotification {

    private CustomNotification() {
        // Prevent creating instances.
    }

    public static void show(
            Activity activity,
            String title,
            String message
    ) {
        if (activity == null
                || activity.isFinishing()
                || activity.isDestroyed()) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {

            if (activity.isFinishing()
                    || activity.isDestroyed()) {
                return;
            }

            LayoutInflater inflater = activity.getLayoutInflater();

            View notificationView = inflater.inflate(
                    R.layout.layout_custom_notification,
                    null
            );

            TextView notificationTitle =
                    notificationView.findViewById(
                            R.id.textNotificationTitle
                    );

            TextView notificationMessage =
                    notificationView.findViewById(
                            R.id.textNotificationMessage
                    );

            notificationTitle.setText(title);
            notificationMessage.setText(message);

            FrameLayout root =
                    activity.findViewById(android.R.id.content);

            if (root == null) {
                return;
            }

            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.gravity = Gravity.TOP;

            float density = activity.getResources()
                    .getDisplayMetrics().density;

            int horizontalMargin = (int) (16 * density);
            int topMargin = (int) (40 * density);

            params.setMargins(
                    horizontalMargin,
                    topMargin,
                    horizontalMargin,
                    0
            );

            root.addView(notificationView, params);

            notificationView.setElevation(12 * density);
            notificationView.setAlpha(0f);
            notificationView.setTranslationY(-30 * density);

            notificationView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                if (notificationView.getParent() != null) {

                    notificationView.animate()
                            .alpha(0f)
                            .translationY(-20 * density)
                            .setDuration(250)
                            .withEndAction(() -> {
                                if (notificationView.getParent() == root) {
                                    root.removeView(notificationView);
                                }
                            })
                            .start();
                }

            }, 3500);
        });
    }
}
