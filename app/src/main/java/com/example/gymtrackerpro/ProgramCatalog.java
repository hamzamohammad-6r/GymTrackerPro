package com.example.gymtrackerpro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Built-in, free training programs.
 *
 * To add or edit a program, change the data in buildAll() below.
 * Muscle group names must match the ones used by the Exercise Library:
 * Chest, Back, Shoulders, Biceps, Triceps, Legs, Abs, Full Body, Cardio, Other.
 * Weekday names must be English (Monday ... Sunday).
 */
public final class ProgramCatalog {

    private ProgramCatalog() {
    }

    // ------------------------------------------------------------------
    // Data classes
    // ------------------------------------------------------------------

    public static final class Entry {
        public final String exerciseName;
        public final String muscleGroup;
        public final int sets;
        public final int reps;
        public final String note;

        Entry(String exerciseName, String muscleGroup,
              int sets, int reps, String note) {
            this.exerciseName = exerciseName;
            this.muscleGroup = muscleGroup;
            this.sets = sets;
            this.reps = reps;
            this.note = note;
        }

        /** "3 x 10" for normal exercises, "3 x 30s" for timed holds. */
        public String prescription() {
            if (note != null && note.startsWith("Hold")) {
                return sets + " \u00d7 " + reps + "s";
            }
            return sets + " \u00d7 " + reps;
        }
    }

    public static final class Day {
        public final String title;
        public final String weekday;
        public final List<Entry> exercises;

        Day(String title, String weekday, List<Entry> exercises) {
            this.title = title;
            this.weekday = weekday;
            this.exercises = Collections.unmodifiableList(exercises);
        }
    }

    public static final class Program {
        public final String id;
        public final String title;
        public final String shortName;
        public final String tagline;
        public final String description;
        public final String level;
        public final String goal;
        public final String equipment;
        public final int weeks;
        public final int coverRes;
        public final int decoRes;
        public final List<Day> days;

        Program(String id, String title, String shortName, String tagline,
                String description, String level, String goal,
                String equipment, int weeks, int coverRes, int decoRes,
                List<Day> days) {
            this.id = id;
            this.title = title;
            this.shortName = shortName;
            this.tagline = tagline;
            this.description = description;
            this.level = level;
            this.goal = goal;
            this.equipment = equipment;
            this.weeks = weeks;
            this.coverRes = coverRes;
            this.decoRes = decoRes;
            this.days = Collections.unmodifiableList(days);
        }

        public int daysPerWeek() {
            return days.size();
        }

        public int totalExercises() {
            int total = 0;
            for (Day day : days) {
                total += day.exercises.size();
            }
            return total;
        }

        /** e.g. "3 days/week \u00b7 4 weeks" */
        public String scheduleSummary() {
            return daysPerWeek() + " days/week \u00b7 " + weeks + " weeks";
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    private static List<Program> cache;

    public static synchronized List<Program> all() {
        if (cache == null) {
            cache = Collections.unmodifiableList(buildAll());
        }
        return cache;
    }

    public static Program byId(String id) {
        for (Program program : all()) {
            if (program.id.equals(id)) {
                return program;
            }
        }
        return null;
    }

    /** Every distinct exercise used by any program. */
    public static List<Entry> allDistinctExercises() {
        List<Entry> result = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (Program program : all()) {
            for (Day day : program.days) {
                for (Entry entry : day.exercises) {
                    String key = entry.exerciseName.toLowerCase();
                    if (!seen.contains(key)) {
                        seen.add(key);
                        result.add(entry);
                    }
                }
            }
        }

        return result;
    }

    // ------------------------------------------------------------------
    // Program definitions
    // ------------------------------------------------------------------

    private static Entry ex(String name, String group,
                            int sets, int reps, String note) {
        return new Entry(name, group, sets, reps, note);
    }

    private static List<Entry> list(Entry... entries) {
        return new ArrayList<>(Arrays.asList(entries));
    }

    private static List<Program> buildAll() {
        List<Program> programs = new ArrayList<>();

        // 1 ---------------------------------------------------------------
        programs.add(new Program(
                "beginner_full_body",
                "Beginner Full Body",
                "Beginner Full Body",
                "Your first 4 weeks in the gym",
                "A gentle, balanced plan that trains your whole body three "
                        + "times a week with simple movements. Start light, "
                        + "focus on clean form, and add a little weight or "
                        + "an extra rep when a set feels easy.",
                "Beginner", "General fitness", "Minimal equipment", 4,
                R.drawable.cover_green, R.drawable.ic_deco_dumbbell,
                Arrays.asList(
                        new Day("Full Body A", "Monday", list(
                                ex("Bodyweight Squat", "Legs", 3, 12, "Rest 60s"),
                                ex("Push-Up", "Chest", 3, 8, "Knees down if needed"),
                                ex("Dumbbell Row", "Back", 3, 10, "Rest 60s"),
                                ex("Glute Bridge", "Legs", 3, 12, "Squeeze at the top"),
                                ex("Plank", "Abs", 3, 30, "Hold 30 seconds"))),
                        new Day("Full Body B", "Wednesday", list(
                                ex("Goblet Squat", "Legs", 3, 10, "Rest 75s"),
                                ex("Dumbbell Bench Press", "Chest", 3, 10, "Rest 75s"),
                                ex("Lat Pulldown", "Back", 3, 10, "Pull to upper chest"),
                                ex("Dumbbell Shoulder Press", "Shoulders", 3, 10, "Rest 60s"),
                                ex("Dead Bug", "Abs", 3, 10, "Slow and controlled"))),
                        new Day("Full Body C", "Friday", list(
                                ex("Walking Lunge", "Legs", 3, 10, "Each leg"),
                                ex("Push-Up", "Chest", 3, 10, "Rest 60s"),
                                ex("Seated Cable Row", "Back", 3, 10, "Rest 60s"),
                                ex("Lateral Raise", "Shoulders", 3, 12, "Light weight"),
                                ex("Bicycle Crunch", "Abs", 3, 16, "Total reps")))
                )));

        // 2 ---------------------------------------------------------------
        programs.add(new Program(
                "push_pull_legs",
                "Push / Pull / Legs",
                "PPL",
                "Classic 3-day muscle builder",
                "Each session targets a group of muscles that work "
                        + "together: pushing, pulling, then legs. A proven "
                        + "structure for building size and strength once "
                        + "you know the basic lifts.",
                "Intermediate", "Build muscle", "Full gym", 6,
                R.drawable.cover_violet, R.drawable.ic_deco_dumbbell,
                Arrays.asList(
                        new Day("Push", "Monday", list(
                                ex("Barbell Bench Press", "Chest", 4, 8, "Rest 2 min"),
                                ex("Overhead Press", "Shoulders", 3, 10, "Rest 90s"),
                                ex("Incline Dumbbell Press", "Chest", 3, 10, "Rest 90s"),
                                ex("Lateral Raise", "Shoulders", 3, 15, "Rest 45s"),
                                ex("Triceps Pushdown", "Triceps", 3, 12, "Rest 45s"))),
                        new Day("Pull", "Wednesday", list(
                                ex("Deadlift", "Back", 3, 5, "Rest 2-3 min"),
                                ex("Pull-Up", "Back", 3, 8, "Use assistance if needed"),
                                ex("Barbell Row", "Back", 3, 10, "Rest 90s"),
                                ex("Face Pull", "Shoulders", 3, 15, "Rest 45s"),
                                ex("Dumbbell Curl", "Biceps", 3, 12, "Rest 45s"))),
                        new Day("Legs", "Friday", list(
                                ex("Barbell Back Squat", "Legs", 4, 8, "Rest 2 min"),
                                ex("Romanian Deadlift", "Legs", 3, 10, "Rest 90s"),
                                ex("Leg Press", "Legs", 3, 12, "Rest 90s"),
                                ex("Leg Curl", "Legs", 3, 12, "Rest 60s"),
                                ex("Standing Calf Raise", "Legs", 4, 15, "Rest 45s")))
                )));

        // 3 ---------------------------------------------------------------
        programs.add(new Program(
                "upper_lower",
                "Upper / Lower Split",
                "Upper-Lower",
                "4 days, balanced strength",
                "Alternate upper-body and lower-body days across four "
                        + "sessions. Every muscle gets trained twice a week, "
                        + "with a rest day in between to recover.",
                "Intermediate", "Strength & size", "Full gym", 6,
                R.drawable.cover_blue, R.drawable.ic_deco_dumbbell,
                Arrays.asList(
                        new Day("Upper A", "Monday", list(
                                ex("Barbell Bench Press", "Chest", 4, 6, "Rest 2 min"),
                                ex("Barbell Row", "Back", 4, 8, "Rest 90s"),
                                ex("Overhead Press", "Shoulders", 3, 8, "Rest 90s"),
                                ex("Dumbbell Curl", "Biceps", 3, 12, "Rest 45s"),
                                ex("Triceps Pushdown", "Triceps", 3, 12, "Rest 45s"))),
                        new Day("Lower A", "Tuesday", list(
                                ex("Barbell Back Squat", "Legs", 4, 6, "Rest 2 min"),
                                ex("Romanian Deadlift", "Legs", 3, 8, "Rest 90s"),
                                ex("Walking Lunge", "Legs", 3, 10, "Each leg"),
                                ex("Standing Calf Raise", "Legs", 4, 12, "Rest 45s"),
                                ex("Plank", "Abs", 3, 45, "Hold 45 seconds"))),
                        new Day("Upper B", "Thursday", list(
                                ex("Incline Dumbbell Press", "Chest", 4, 10, "Rest 90s"),
                                ex("Lat Pulldown", "Back", 4, 10, "Rest 90s"),
                                ex("Lateral Raise", "Shoulders", 3, 15, "Rest 45s"),
                                ex("Seated Cable Row", "Back", 3, 12, "Rest 60s"),
                                ex("Hammer Curl", "Biceps", 3, 12, "Rest 45s"))),
                        new Day("Lower B", "Friday", list(
                                ex("Deadlift", "Back", 3, 5, "Rest 2-3 min"),
                                ex("Leg Press", "Legs", 3, 12, "Rest 90s"),
                                ex("Leg Curl", "Legs", 3, 12, "Rest 60s"),
                                ex("Glute Bridge", "Legs", 3, 15, "Squeeze at the top"),
                                ex("Hanging Knee Raise", "Abs", 3, 12, "Rest 45s")))
                )));

        // 4 ---------------------------------------------------------------
        programs.add(new Program(
                "home_bodyweight",
                "Home Bodyweight",
                "Home Bodyweight",
                "No equipment. No excuses.",
                "Train anywhere with nothing but your own bodyweight. "
                        + "Three simple sessions build strength, stamina "
                        + "and mobility at home.",
                "Beginner", "Strength at home", "No equipment", 4,
                R.drawable.cover_orange, R.drawable.ic_deco_bolt,
                Arrays.asList(
                        new Day("Upper Focus", "Monday", list(
                                ex("Push-Up", "Chest", 4, 10, "Rest 60s"),
                                ex("Pike Push-Up", "Shoulders", 3, 8, "Rest 60s"),
                                ex("Bench Dip", "Triceps", 3, 12, "Use a chair or sofa"),
                                ex("Superman Hold", "Back", 3, 12, "Squeeze your back"),
                                ex("Plank", "Abs", 3, 40, "Hold 40 seconds"))),
                        new Day("Lower Focus", "Wednesday", list(
                                ex("Bodyweight Squat", "Legs", 4, 15, "Rest 45s"),
                                ex("Reverse Lunge", "Legs", 3, 12, "Each leg"),
                                ex("Glute Bridge", "Legs", 3, 15, "Rest 45s"),
                                ex("Wall Sit", "Legs", 3, 30, "Hold 30 seconds"),
                                ex("Calf Raise", "Legs", 3, 20, "Slow and controlled"))),
                        new Day("Full Body Burn", "Friday", list(
                                ex("Burpee", "Full Body", 3, 8, "Rest 60s"),
                                ex("Mountain Climber", "Cardio", 3, 30, "Total reps"),
                                ex("Push-Up", "Chest", 3, 10, "Rest 45s"),
                                ex("Bodyweight Squat", "Legs", 3, 20, "Rest 45s"),
                                ex("Bicycle Crunch", "Abs", 3, 20, "Total reps")))
                )));

        // 5 ---------------------------------------------------------------
        programs.add(new Program(
                "fat_burn_circuit",
                "Fat Burn Circuit",
                "Fat Burn",
                "Sweat hard, finish fast",
                "Short, high-energy circuits that mix strength moves with "
                        + "cardio to keep your heart rate up. Do each "
                        + "exercise back to back, rest 60-90 seconds, then "
                        + "repeat.",
                "All levels", "Lose fat, build stamina", "Minimal equipment", 4,
                R.drawable.cover_red, R.drawable.ic_deco_bolt,
                Arrays.asList(
                        new Day("Circuit 1", "Tuesday", list(
                                ex("Jumping Jack", "Cardio", 3, 40, "Total reps"),
                                ex("Kettlebell Swing", "Full Body", 3, 15, "Hips drive the swing"),
                                ex("Squat Jump", "Legs", 3, 12, "Land softly"),
                                ex("Push-Up", "Chest", 3, 12, "Rest 60s"),
                                ex("High Knees", "Cardio", 3, 40, "Total reps"))),
                        new Day("Circuit 2", "Thursday", list(
                                ex("Burpee", "Full Body", 3, 10, "Rest 60s"),
                                ex("Dumbbell Thruster", "Full Body", 3, 12, "Rest 60s"),
                                ex("Mountain Climber", "Cardio", 3, 40, "Total reps"),
                                ex("Dumbbell Row", "Back", 3, 12, "Each arm"),
                                ex("Russian Twist", "Abs", 3, 20, "Total reps"))),
                        new Day("Circuit 3", "Saturday", list(
                                ex("Kettlebell Swing", "Full Body", 4, 15, "Rest 60s"),
                                ex("Reverse Lunge", "Legs", 3, 12, "Each leg"),
                                ex("Jumping Jack", "Cardio", 3, 50, "Total reps"),
                                ex("Plank", "Abs", 3, 40, "Hold 40 seconds"),
                                ex("Burpee", "Full Body", 3, 8, "Finish strong")))
                )));

        // 6 ---------------------------------------------------------------
        programs.add(new Program(
                "core_builder",
                "Core & Abs Builder",
                "Core Builder",
                "Short sessions, strong middle",
                "Three quick sessions that train your abs, obliques and "
                        + "lower back. Each takes about 15 minutes and fits "
                        + "easily around any other training.",
                "All levels", "Core strength", "No equipment", 4,
                R.drawable.cover_teal, R.drawable.ic_deco_bolt,
                Arrays.asList(
                        new Day("Core Basics", "Monday", list(
                                ex("Plank", "Abs", 3, 30, "Hold 30 seconds"),
                                ex("Crunch", "Abs", 3, 15, "Rest 30s"),
                                ex("Dead Bug", "Abs", 3, 10, "Slow and controlled"),
                                ex("Superman Hold", "Back", 3, 12, "Rest 30s"))),
                        new Day("Obliques & Twist", "Wednesday", list(
                                ex("Russian Twist", "Abs", 3, 20, "Total reps"),
                                ex("Side Plank", "Abs", 3, 25, "Hold 25 seconds each side"),
                                ex("Bicycle Crunch", "Abs", 3, 20, "Total reps"),
                                ex("Mountain Climber", "Cardio", 3, 30, "Total reps"))),
                        new Day("Strong Core", "Friday", list(
                                ex("Leg Raise", "Abs", 3, 12, "Lower back stays down"),
                                ex("Plank", "Abs", 3, 45, "Hold 45 seconds"),
                                ex("Hanging Knee Raise", "Abs", 3, 10, "Rest 45s"),
                                ex("Flutter Kick", "Abs", 3, 30, "Total reps")))
                )));

        return programs;
    }
}
