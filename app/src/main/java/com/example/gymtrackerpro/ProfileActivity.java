package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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

    private static final String PREFS_NAME = "GymTrackerProfilePrefs";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";

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
                                                KEY_PROFILE_IMAGE_URI,
                                                uri.toString()
                                        )
                                        .apply();

                                imageProfilePicture.setImageURI(uri);

                                Toast.makeText(
                                        this,
                                        "Profile picture updated",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } catch (Exception e) {
                                Toast.makeText(
                                        this,
                                        "Could not save this picture. Please try another.",
                                        Toast.LENGTH_LONG
                                ).show();
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
                KEY_PROFILE_IMAGE_URI,
                null
        );

        if (savedUri != null) {
            try {
                imageProfilePicture.setImageURI(Uri.parse(savedUri));
            } catch (Exception e) {
                imageProfilePicture.setImageResource(
                        android.R.drawable.ic_menu_myplaces
                );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        TextView buttonBackArrow = findViewById(R.id.buttonBackArrow);

        buttonBackArrow.setOnClickListener(v -> finish());
        UiFx.applyEdgeToEdge(this);

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

        buttonChangeProfilePicture.setOnClickListener(v ->
                photoPicker.launch(new String[]{"image/*"})
        );

        loadSavedProfilePicture();

        Button buttonSaveProfile =
                findViewById(R.id.buttonSaveProfile);

        buttonSaveProfile.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        executor.execute(() -> {
            try {
                List<User> users = database.userDao().getAllUsers();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (users != null && !users.isEmpty()) {
                        currentUser = users.get(0);
                        textProfileDisplayName.setText(currentUser.getName());

                        editName.setText(currentUser.getName());
                        editEmail.setText(currentUser.getEmail());
                        editAge.setText(
                                String.valueOf(currentUser.getAge()));
                        editHeight.setText(
                                String.valueOf(currentUser.getHeight()));
                        editWeight.setText(
                                String.valueOf(currentUser.getWeight()));
                        editFitnessGoal.setText(
                                currentUser.getFitnessGoal());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this,
                                "Could not load profile",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String ageText = editAge.getText().toString().trim();
        String heightText = editHeight.getText().toString().trim();
        String weightText = editWeight.getText().toString().trim();
        String fitnessGoal =
                editFitnessGoal.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()
                || ageText.isEmpty() || heightText.isEmpty()
                || weightText.isEmpty() || fitnessGoal.isEmpty()) {

            Toast.makeText(this,
                    "Please complete all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            editEmail.setError("Enter a valid email address");
            return;
        }

        final int age;
        final double height;
        final double weight;

        try {
            age = Integer.parseInt(ageText);
            height = Double.parseDouble(heightText);
            weight = Double.parseDouble(weightText);

            if (age <= 0 || age > 120
                    || height <= 0 || weight <= 0) {
                Toast.makeText(this,
                        "Enter valid age, height and weight",
                        Toast.LENGTH_LONG).show();
                return;
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Check your age, height and weight",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                // Recheck the database before saving.
                List<User> users = database.userDao().getAllUsers();

                if (users != null && !users.isEmpty()) {
                    currentUser = users.get(0);
                }

                if (currentUser == null) {
                    currentUser = new User(
                            name, email, age,
                            height, weight, fitnessGoal
                    );

                    database.userDao().insertUser(currentUser);

                } else {
                    currentUser.setName(name);
                    currentUser.setEmail(email);
                    currentUser.setAge(age);
                    currentUser.setHeight(height);
                    currentUser.setWeight(weight);
                    currentUser.setFitnessGoal(fitnessGoal);

                    database.userDao().updateUser(currentUser);
                }

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this,
                                "Profile saved successfully",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this,
                                "Could not save profile",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
