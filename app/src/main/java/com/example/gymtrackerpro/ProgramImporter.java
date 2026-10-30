package com.example.gymtrackerpro;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Copies a built-in program into the user's own data using the existing
 * Workout, Exercise and WorkoutExercise tables. No database changes needed.
 *
 * All methods here touch the database, so call them off the main thread.
 */
public final class ProgramImporter {

    private ProgramImporter() {
    }

    public static final class Result {
        public int workoutsAdded = 0;
        public int exercisesAdded = 0;
    }

    /**
     * Adds one week of the program, starting from startDate.
     * Each program day is scheduled on its weekday (first occurrence on or
     * after startDate). Everything is saved in one transaction, so a failure
     * never leaves a half-imported program behind.
     */
    public static Result importProgram(
            final GymTrackerDatabase database,
            final ProgramCatalog.Program program,
            final Calendar startDate) {

        final Result result = new Result();

        database.runInTransaction(() -> {
            int userId = LocalUser.getOrCreateId(database);

            Map<String, Integer> exerciseIds = loadExerciseIds(database);

            for (ProgramCatalog.Day day : program.days) {
                Workout workout = new Workout(
                        userId,
                        program.shortName + " - " + day.title,
                        dateFor(startDate, day.weekday));

                workout.setDayOfWeek(day.weekday);
                workout.setCompleted(false);

                int workoutId = (int) database.workoutDao()
                        .insertWorkout(workout);

                result.workoutsAdded++;

                for (ProgramCatalog.Entry entry : day.exercises) {
                    int exerciseId = ensureExercise(
                            database, exerciseIds, entry, result);

                    database.workoutExerciseDao().insertWorkoutExercise(
                            new WorkoutExercise(
                                    workoutId,
                                    exerciseId,
                                    entry.sets,
                                    entry.reps,
                                    0.0,
                                    entry.note));
                }
            }
        });

        return result;
    }

    /**
     * Makes sure every exercise used by the built-in programs exists in the
     * library. Existing exercises (matched by name) are never duplicated.
     * Returns how many new exercises were added.
     */
    public static int seedExerciseLibrary(GymTrackerDatabase database) {
        final Result result = new Result();

        database.runInTransaction(() -> {
            Map<String, Integer> exerciseIds = loadExerciseIds(database);

            for (ProgramCatalog.Entry entry
                    : ProgramCatalog.allDistinctExercises()) {
                ensureExercise(database, exerciseIds, entry, result);
            }
        });

        return result.exercisesAdded;
    }

    // ------------------------------------------------------------------

    private static Map<String, Integer> loadExerciseIds(
            GymTrackerDatabase database) {

        Map<String, Integer> ids = new HashMap<>();
        List<Exercise> existing = database.exerciseDao().getAllExercises();

        if (existing != null) {
            for (Exercise exercise : existing) {
                if (exercise.getExerciseName() != null) {
                    ids.put(exercise.getExerciseName()
                                    .trim().toLowerCase(Locale.ROOT),
                            exercise.getExerciseId());
                }
            }
        }

        return ids;
    }

    private static int ensureExercise(
            GymTrackerDatabase database,
            Map<String, Integer> exerciseIds,
            ProgramCatalog.Entry entry,
            Result result) {

        String key = entry.exerciseName.trim().toLowerCase(Locale.ROOT);
        Integer existingId = exerciseIds.get(key);

        if (existingId != null) {
            return existingId;
        }

        int newId = (int) database.exerciseDao().insertExercise(
                new Exercise(entry.exerciseName, entry.muscleGroup));

        exerciseIds.put(key, newId);
        result.exercisesAdded++;

        return newId;
    }

    private static int calendarDay(String weekday) {
        switch (weekday) {
            case "Monday":
                return Calendar.MONDAY;
            case "Tuesday":
                return Calendar.TUESDAY;
            case "Wednesday":
                return Calendar.WEDNESDAY;
            case "Thursday":
                return Calendar.THURSDAY;
            case "Friday":
                return Calendar.FRIDAY;
            case "Saturday":
                return Calendar.SATURDAY;
            default:
                return Calendar.SUNDAY;
        }
    }

    /** First date on or after start that falls on the given weekday. */
    private static String dateFor(Calendar start, String weekday) {
        Calendar date = (Calendar) start.clone();
        int target = calendarDay(weekday);

        while (date.get(Calendar.DAY_OF_WEEK) != target) {
            date.add(Calendar.DAY_OF_MONTH, 1);
        }

        return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH));
    }
}
