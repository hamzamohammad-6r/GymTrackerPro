package com.example.gymtrackerpro;

/** The app has a single local profile; this finds or creates it. */
public final class LocalUser {

    public static final String EMAIL = "gymtrackerpro@local.test";

    private LocalUser() {
    }

    /** Must be called off the main thread. */
    public static int getOrCreateId(GymTrackerDatabase database) {
        User user = database.userDao().getUserByEmail(EMAIL);

        if (user != null) {
            return user.getUserId();
        }

        User created = new User(
                "GymTrackerPro User",
                EMAIL,
                0,
                0.0,
                0.0,
                "General fitness");

        return (int) database.userDao().insertUser(created);
    }
}
