package com.example.gymtrackerpro;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/** Fills the exercise library once, on first launch. */
public final class ExerciseSeeder {

    private static final String TAG = "ExerciseSeeder";
    private static final String PREFS = "gymtracker_prefs";
    private static final String KEY_SEEDED = "starter_exercises_seeded_v1";

    private ExerciseSeeder() {
    }

    /** Call off the main thread. Safe to call every launch. */
    public static void seedIfNeeded(Context context,
                                    GymTrackerDatabase database) {

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_SEEDED, false)) {
            return;
        }

        try {
            int added = ProgramImporter.seedExerciseLibrary(database);
            prefs.edit().putBoolean(KEY_SEEDED, true).apply();
            Log.i(TAG, "Seeded " + added + " starter exercises");
        } catch (Exception e) {
            // Try again next launch.
            Log.e(TAG, "Unable to seed starter exercises", e);
        }
    }
}
