
package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editAge = findViewById(R.id.editAge);
        editHeight = findViewById(R.id.editHeight);
        editWeight = findViewById(R.id.editWeight);
        editFitnessGoal = findViewById(R.id.editFitnessGoal);

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
