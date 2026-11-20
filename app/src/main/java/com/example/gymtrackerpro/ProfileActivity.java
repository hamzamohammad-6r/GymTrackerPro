package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editEmail;
    private EditText editAge;
    private EditText editHeight;
    private EditText editWeight;
    private EditText editFitnessGoal;

    private GymTrackerDatabase database;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private User currentUser;

    private ImageView imageProfilePicture;
    private TextView textProfileDisplayName;
    private Button buttonChangeProfilePicture;

    private static final String PREFS_NAME =
            "GymTrackerProfilePrefs";

    private static final String KEY_PROFILE_IMAGE_URI =
            "profile_image_uri";

    private final ActivityResultLauncher<String[]> photoPicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {

                        if (uri != null) {

                            try {

                                getContentResolver()
                                        .takePersistableUriPermission(
                                                uri,
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        );

                                getSharedPreferences(
                                        PREFS_NAME,
                                        MODE_PRIVATE
                                ).edit()
                                        .putString(
                                                getAccountProfileImageKey(),
                                                uri.toString()
                                        )
                                        .apply();

                                imageProfilePicture.setImageURI(uri);

                                CustomNotification.show(
                                        this,
                                        "Profile Picture Updated",
                                        "Your profile picture has been updated successfully."
                                );

                            } catch (Exception e) {

                                CustomNotification.show(
                                        this,
                                        "Picture Update Failed",
                                        "Could not save this picture. Please try another."
                                );
                            }
                        }
                    }
            );

    private void loadSavedProfilePicture() {

        SharedPreferences preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        String savedUri = preferences.getString(
                getAccountProfileImageKey(),
                null
        );

        if (savedUri != null) {

            try {

                imageProfilePicture.setImageURI(
                        Uri.parse(savedUri)
                );

            } catch (Exception e) {

                imageProfilePicture.setImageResource(
                        android.R.drawable.ic_menu_myplaces
                );
            }

        } else {

            imageProfilePicture.setImageResource(
                    android.R.drawable.ic_menu_myplaces
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        UiFx.applyEdgeToEdge(this);

        getWindow().setStatusBarColor(
                Color.parseColor("#0B1120")
        );

        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_profile);

        TextView buttonBackArrow =
                findViewById(R.id.buttonBackArrow);

        buttonBackArrow.setOnClickListener(
                v -> UiFx.finishWithTransition(this)
        );

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        UiFx.finishWithTransition(
                                ProfileActivity.this
                        );
                    }
                }
        );

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editAge = findViewById(R.id.editAge);
        editHeight = findViewById(R.id.editHeight);
        editWeight = findViewById(R.id.editWeight);
        editFitnessGoal = findViewById(R.id.editFitnessGoal);

        imageProfilePicture =
                findViewById(R.id.imageProfilePicture);

        textProfileDisplayName =
                findViewById(R.id.textProfileDisplayName);

        buttonChangeProfilePicture =
                findViewById(R.id.buttonChangeProfilePicture);

        buttonChangeProfilePicture.setOnClickListener(
                v -> photoPicker.launch(
                        new String[]{"image/*"}
                )
        );

        loadSavedProfilePicture();

        Button buttonSaveProfile =
                findViewById(R.id.buttonSaveProfile);

        buttonSaveProfile.setOnClickListener(
                v -> saveProfile()
        );

        Button buttonSignOut =
                findViewById(R.id.buttonSignOut);

        buttonSignOut.setOnClickListener(
                v -> signOut()
        );

        loadProfile();

        // Bottom navigation.

        View navHome = findViewById(R.id.navHome);
        View navProgress = findViewById(R.id.navProgress);
        View navTimer = findViewById(R.id.navTimer);
        View navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v ->
                openScreen(MainActivity.class)
        );

        navProgress.setOnClickListener(v ->
                openScreen(WorkoutHistoryActivity.class)
        );

        navTimer.setOnClickListener(v ->
                openScreen(TimerActivity.class)
        );

        // Already on Profile screen.
        navProfile.setOnClickListener(v -> {
            // No navigation needed.
        });
    }

    private void openScreen(Class<?> activityClass) {

        Intent intent = new Intent(
                ProfileActivity.this,
                activityClass
        );

        UiFx.startActivityWithTransition(this, intent);
    }

    private void loadProfile() {

        SharedPreferences session = getSharedPreferences(
                "GymTrackerSession",
                MODE_PRIVATE
        );

        String loggedInEmail = session.getString(
                "email",
                null
        );

        executor.execute(() -> {

            try {

                User user = null;

                if (loggedInEmail != null
                        && !loggedInEmail.trim().isEmpty()) {

                    user = database.userDao()
                            .getUserByEmail(loggedInEmail);
                }

                final User loadedUser = user;

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    currentUser = loadedUser;

                    if (currentUser != null) {

                        textProfileDisplayName.setText(
                                currentUser.getName()
                        );

                        editName.setText(
                                currentUser.getName()
                        );

                        editEmail.setText(loggedInEmail);

                        editAge.setText(
                                String.valueOf(currentUser.getAge())
                        );

                        editHeight.setText(
                                String.valueOf(currentUser.getHeight())
                        );

                        editWeight.setText(
                                String.valueOf(currentUser.getWeight())
                        );

                        editFitnessGoal.setText(
                                currentUser.getFitnessGoal()
                        );

                    } else {

                        // No local profile exists for this account yet.

                        textProfileDisplayName.setText(
                                "Your Profile"
                        );

                        editName.setText("");

                        editEmail.setText(
                                loggedInEmail != null
                                        ? loggedInEmail
                                        : ""
                        );

                        editAge.setText("");
                        editHeight.setText("");
                        editWeight.setText("");
                        editFitnessGoal.setText("");
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        CustomNotification.show(
                                ProfileActivity.this,
                                "Profile Loading Failed",
                                "Could not load your profile. Please try again."
                        );
                    }
                });
            }
        });
    }

    private void saveProfile() {

        String name = editName.getText()
                .toString()
                .trim();

        String enteredEmail = editEmail.getText()
                .toString()
                .trim();

        String ageText = editAge.getText()
                .toString()
                .trim();

        String heightText = editHeight.getText()
                .toString()
                .trim();

        String weightText = editWeight.getText()
                .toString()
                .trim();

        String fitnessGoal = editFitnessGoal.getText()
                .toString()
                .trim();

        SharedPreferences session = getSharedPreferences(
                "GymTrackerSession",
                MODE_PRIVATE
        );

        String loggedInEmail = session.getString(
                "email",
                null
        );

        // Use the logged-in account email as the account identifier.

        String accountEmail = loggedInEmail != null
                && !loggedInEmail.trim().isEmpty()
                ? loggedInEmail.trim()
                : enteredEmail;

        // Validate required fields.

        if (name.isEmpty() || accountEmail.isEmpty()
                || ageText.isEmpty() || heightText.isEmpty()
                || weightText.isEmpty() || fitnessGoal.isEmpty()) {

            CustomNotification.show(
                    this,
                    "Missing Information",
                    "Please complete all fields before saving your profile."
            );

            return;
        }

        // Validate email.

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(accountEmail)
                .matches()) {

            editEmail.setError(
                    "Enter a valid email address"
            );

            editEmail.requestFocus();

            return;
        }

        // A logged-in user cannot change the account identifier here.

        if (loggedInEmail != null
                && !loggedInEmail.trim()
                .equalsIgnoreCase(enteredEmail)) {

            editEmail.setError(
                    "Your account email cannot be changed here"
            );

            editEmail.setText(loggedInEmail);

            return;
        }

        final int age;
        final double height;
        final double weight;

        // Convert numerical information.

        try {

            age = Integer.parseInt(ageText);
            height = Double.parseDouble(heightText);
            weight = Double.parseDouble(weightText);

        } catch (NumberFormatException e) {

            CustomNotification.show(
                    this,
                    "Invalid Information",
                    "Check your age, height and weight."
            );

            return;
        }

        // Validate numerical ranges.

        if (age <= 0 || age > 120
                || !Double.isFinite(height)
                || !Double.isFinite(weight)
                || height <= 0
                || weight <= 0) {

            CustomNotification.show(
                    this,
                    "Invalid Information",
                    "Enter valid age, height and weight."
            );

            return;
        }

        // Save the profile in the background.

        executor.execute(() -> {

            try {

                // Find only this account's local profile.

                User userToSave = database.userDao()
                        .getUserByEmail(accountEmail);

                if (userToSave == null) {

                    userToSave = new User(
                            name,
                            accountEmail,
                            age,
                            height,
                            weight,
                            fitnessGoal
                    );

                    database.userDao().insertUser(userToSave);

                } else {

                    userToSave.setName(name);
                    userToSave.setEmail(accountEmail);
                    userToSave.setAge(age);
                    userToSave.setHeight(height);
                    userToSave.setWeight(weight);
                    userToSave.setFitnessGoal(fitnessGoal);

                    database.userDao().updateUser(userToSave);
                }

                User savedUser = userToSave;

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        currentUser = savedUser;

                        textProfileDisplayName.setText(
                                savedUser.getName()
                        );

                        CustomNotification.show(
                                ProfileActivity.this,
                                "Profile Saved",
                                "Your profile has been saved successfully."
                        );
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (!isFinishing() && !isDestroyed()) {

                        CustomNotification.show(
                                ProfileActivity.this,
                                "Profile Saving Failed",
                                "Could not save your profile. Please try again."
                        );
                    }
                });
            }
        });
    }

    private String getAccountProfileImageKey() {

        SharedPreferences session = getSharedPreferences(
                "GymTrackerSession",
                MODE_PRIVATE
        );

        String email = session.getString(
                "email",
                null
        );

        if (email == null || email.trim().isEmpty()) {

            return KEY_PROFILE_IMAGE_URI;
        }

        return KEY_PROFILE_IMAGE_URI + "_"
                + email.trim().toLowerCase(
                java.util.Locale.ROOT
        );
    }

    private void signOut() {

        new AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Sign Out?")
                .setMessage(
                        "Are you sure you want to sign out of GymTrackerPro?"
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Sign Out",
                        (dialog, which) -> {

                            // Clear only authentication information.
                            // Keep profile pictures and local user data.

                            SharedPreferences preferences =
                                    getSharedPreferences(
                                            "GymTrackerSession",
                                            MODE_PRIVATE
                                    );

                            preferences.edit()
                                    .remove("accessToken")
                                    .remove("email")
                                    .remove("accountId")
                                    .apply();

                            // Return directly to Login.

                            Intent intent = new Intent(
                                    ProfileActivity.this,
                                    LoginActivity.class
                            );

                            // Prevent returning to previous screens.

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();
                        }
                )
                .show();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executor.shutdown();
    }
}
