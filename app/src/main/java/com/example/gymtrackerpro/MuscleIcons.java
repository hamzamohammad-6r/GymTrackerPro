package com.example.gymtrackerpro;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;

import androidx.core.graphics.ColorUtils;

/** Maps a muscle group name to its picture and accent colour. */
public final class MuscleIcons {

    private MuscleIcons() {
    }

    public static int iconFor(String group) {
        if (group == null) {
            return R.drawable.ic_muscle_other;
        }

        switch (group) {
            case "Chest":
                return R.drawable.ic_muscle_chest;
            case "Back":
                return R.drawable.ic_muscle_back;
            case "Shoulders":
                return R.drawable.ic_muscle_shoulder;
            case "Biceps":
                return R.drawable.ic_muscle_biceps;
            case "Triceps":
                return R.drawable.ic_muscle_triceps;
            case "Legs":
                return R.drawable.ic_muscle_legs;
            case "Abs":
                return R.drawable.ic_muscle_abs;
            case "Full Body":
                return R.drawable.ic_muscle_fullbody;
            case "Cardio":
                return R.drawable.ic_muscle_cardio;
            default:
                return R.drawable.ic_muscle_other;
        }
    }

    public static int colorFor(String group) {
        if (group == null) {
            return Color.parseColor("#94A3B8");
        }

        switch (group) {
            case "Chest":
                return Color.parseColor("#60A5FA");
            case "Back":
                return Color.parseColor("#34D399");
            case "Shoulders":
                return Color.parseColor("#FBBF24");
            case "Biceps":
                return Color.parseColor("#F472B6");
            case "Triceps":
                return Color.parseColor("#A78BFA");
            case "Legs":
                return Color.parseColor("#38BDF8");
            case "Abs":
                return Color.parseColor("#FB923C");
            case "Full Body":
                return Color.parseColor("#60A5FA");
            case "Cardio":
                return Color.parseColor("#F87171");
            default:
                return Color.parseColor("#94A3B8");
        }
    }

    /** Shows the muscle picture on a softly tinted rounded tile. */
    public static void bind(ImageView image, String group) {
        float radius = 14f * image.getResources()
                .getDisplayMetrics().density;

        GradientDrawable tile = new GradientDrawable();
        tile.setCornerRadius(radius);
        tile.setColor(ColorUtils.setAlphaComponent(colorFor(group), 0x26));

        image.setBackground(tile);
        image.setImageResource(iconFor(group));
    }
}
