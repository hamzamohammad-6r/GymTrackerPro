package com.example.gymtrackerpro;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class UserProgramManager {

    private UserProgramManager() {
    }

    /**
     * Gets the local Room user associated with the current session.
     * If the user is offline, the existing local profile is used.
     * Must be called off the main thread.
     */
    public static int getCurrentUserId(GymTrackerDatabase database,
                                       String loggedInEmail) {

        if (loggedInEmail != null && !loggedInEmail.trim().isEmpty()) {

            User user = database.userDao().getUserByEmail(
                    loggedInEmail.trim()
            );

            if (user != null) {
                return user.getUserId();
            }

            throw new IllegalStateException(
                    "No local user profile found for this account."
            );
        }

        return LocalUser.getOrCreateId(database);
    }

    /**
     * Activates a programme for one user.
     * The previous programme is replaced, but workout history is preserved.
     * Must be called off the main thread.
     */
    public static void selectProgram(GymTrackerDatabase database,
                                     int userId,
                                     String programId) {

        if (programId == null || programId.trim().isEmpty()
                || ProgramCatalog.byId(programId) == null) {

            throw new IllegalArgumentException(
                    "Please select a valid free programme."
            );
        }

        String startDate = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(new Date());

        UserProgram current = database.userProgramDao()
                .getProgramForUser(userId);

        if (current == null) {

            UserProgram selected = new UserProgram(
                    userId,
                    programId,
                    startDate,
                    true
            );

            database.userProgramDao().insertOrReplace(selected);

        } else {

            current.setProgramId(programId);
            current.setStartDate(startDate);
            current.setActive(true);

            database.userProgramDao().updateProgram(current);
        }
    }

    /**
     * Retrieves the active programme for one user.
     * Must be called off the main thread.
     */
    public static UserProgram getActiveProgram(GymTrackerDatabase database,
                                               int userId) {

        UserProgram selected = database.userProgramDao()
                .getProgramForUser(userId);

        if (selected == null || !selected.isActive()) {
            return null;
        }

        return selected;
    }
}
