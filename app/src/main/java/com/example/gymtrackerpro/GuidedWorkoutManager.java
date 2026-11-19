package com.example.gymtrackerpro;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GuidedWorkoutManager {

    private GuidedWorkoutManager() {
    }

    public static final class SessionResult {
        private final int workoutId;
        private final boolean alreadyExists;
        private final boolean completed;

        public SessionResult(
                int workoutId,
                boolean alreadyExists,
                boolean completed
        ) {
            this.workoutId = workoutId;
            this.alreadyExists = alreadyExists;
            this.completed = completed;
        }

        public int getWorkoutId() {
            return workoutId;
        }

        public boolean alreadyExists() {
            return alreadyExists;
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    /**
     * Creates or retrieves one prescribed workout session.
     * Must be called off the main thread.
     */
    public static SessionResult createOrGetSession(
            GymTrackerDatabase database,
            int userId,
            ProgramCatalog.Program program,
            ProgramCatalog.Day day,
            Calendar sessionDate
    ) {

        if (database == null || program == null
                || day == null || sessionDate == null) {
            throw new IllegalArgumentException(
                    "Workout session information is incomplete."
            );
        }

        if (!program.days.contains(day)) {
            throw new IllegalArgumentException(
                    "This training day does not belong to the selected programme."
            );
        }

        Calendar date = (Calendar) sessionDate.clone();

        String weekday = new SimpleDateFormat(
                "EEEE",
                Locale.US
        ).format(date.getTime());

        if (!weekday.equalsIgnoreCase(day.weekday)) {
            throw new IllegalArgumentException(
                    "The selected date does not match the prescribed training day."
            );
        }

        String workoutDate = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(date.getTime());

        String workoutName = program.title + " - " + day.title;

        // Reuse existing library exercises and add only missing ones.
        ProgramImporter.seedExerciseLibrary(database);

        // Avoid creating duplicate sessions for the same user and date.
        List<Workout> existingWorkouts =
                database.workoutDao().getWorkoutsForUser(userId);

        for (Workout existing : existingWorkouts) {

            if (workoutName.equals(existing.getWorkoutName())
                    && workoutDate.equals(existing.getWorkoutDate())) {

                return new SessionResult(
                        existing.getWorkoutId(),
                        true,
                        existing.isCompleted()
                );
            }
        }

        final SessionResult[] result = new SessionResult[1];

        database.runInTransaction(() -> {

            // Recheck inside the transaction before inserting.
            List<Workout> workouts =
                    database.workoutDao().getWorkoutsForUser(userId);

            for (Workout existing : workouts) {

                if (workoutName.equals(existing.getWorkoutName())
                        && workoutDate.equals(existing.getWorkoutDate())) {

                    result[0] = new SessionResult(
                            existing.getWorkoutId(),
                            true,
                            existing.isCompleted()
                    );

                    return;
                }
            }

            Map<String, Integer> exerciseIds = new HashMap<>();

            List<Exercise> exercises =
                    database.exerciseDao().getAllExercises();

            for (Exercise exercise : exercises) {

                if (exercise.getExerciseName() != null) {

                    String key = exercise.getExerciseName()
                            .trim()
                            .toLowerCase(Locale.ROOT);

                    exerciseIds.put(key, exercise.getExerciseId());
                }
            }

            Workout workout = new Workout(
                    userId,
                    workoutName,
                    workoutDate
            );

            workout.setDayOfWeek(day.weekday);
            workout.setCompleted(false);

            int workoutId = (int) database.workoutDao()
                    .insertWorkout(workout);

            for (ProgramCatalog.Entry entry : day.exercises) {

                String key = entry.exerciseName
                        .trim()
                        .toLowerCase(Locale.ROOT);

                Integer exerciseId = exerciseIds.get(key);

                if (exerciseId == null) {

                    exerciseId = (int) database.exerciseDao()
                            .insertExercise(
                                    new Exercise(
                                            entry.exerciseName,
                                            entry.muscleGroup
                                    )
                            );

                    exerciseIds.put(key, exerciseId);
                }

                database.workoutExerciseDao().insertWorkoutExercise(
                        new WorkoutExercise(
                                workoutId,
                                exerciseId,
                                entry.sets,
                                entry.reps,
                                0.0,
                                entry.note
                        )
                );
            }

            result[0] = new SessionResult(
                    workoutId,
                    false,
                    false
            );
        });

        if (result[0] == null) {
            throw new IllegalStateException(
                    "The workout session could not be created."
            );
        }

        return result[0];
    }
}
