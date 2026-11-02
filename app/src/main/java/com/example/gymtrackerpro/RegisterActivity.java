
package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etAge;
    private EditText etHeight;
    private EditText etWeight;
    private EditText etFitnessGoal;
    private Button btnCreateAccount;

    private GymTrackerDatabase database;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etAge = findViewById(R.id.etRegisterAge);
        etHeight = findViewById(R.id.etRegisterHeight);
        etWeight = findViewById(R.id.etRegisterWeight);
        etFitnessGoal = findViewById(R.id.etRegisterFitnessGoal);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnCreateAccount.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String ageText = etAge.getText().toString().trim();
        String heightText = etHeight.getText().toString().trim();
        String weightText = etWeight.getText().toString().trim();
        String fitnessGoal =
                etFitnessGoal.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()
                || ageText.isEmpty() || heightText.isEmpty()
                || weightText.isEmpty() || fitnessGoal.isEmpty()) {
            Toast.makeText(this,
                    "Please complete all fields.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        final int age;
        final double height;
        final double weight;

        try {
            age = Integer.parseInt(ageText);
            height = Double.parseDouble(heightText);
            weight = Double.parseDouble(weightText);
        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Enter valid numbers for age, height and weight.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (age < 1 || age > 120 || height <= 0 || weight <= 0) {
            Toast.makeText(this,
                    "Please check your age, height and weight.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);

        executor.execute(() -> {
            try {
                User existingUser =
                        database.userDao().getUserByEmail(email);

                if (existingUser != null) {
                    runOnUiThread(() -> {
                        btnCreateAccount.setEnabled(true);
                        etEmail.setError(
                                "An account with this email already exists"
                        );
                        etEmail.requestFocus();
                    });
                    return;
                }

                User user = new User(
                        name, email, age, height, weight, fitnessGoal
                );

                long userId = database.userDao().insertUser(user);

                runOnUiThread(() -> {
                    btnCreateAccount.setEnabled(true);

                    if (userId > 0) {
                        showSuccessDialog(name);
                    } else {
                        Toast.makeText(this,
                                "Account could not be created.",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnCreateAccount.setEnabled(true);
                    Toast.makeText(this,
                            "Registration failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                });

                android.util.Log.e(
                        "RegisterActivity",
                        "Account creation failed",
                        e
                );
            }
        });
    }

    private void showSuccessDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle("Account Created Successfully!")
                .setMessage(
                        "Welcome, " + name
                                + "!\n\nYour GymTrackerPro account "
                                + "has been created successfully."
                )
                .setCancelable(false)
                .setPositiveButton("Get Started", (dialog, which) -> {
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
