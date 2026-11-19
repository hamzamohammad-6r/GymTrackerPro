package com.example.gymtrackerpro;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gymtrackerpro.network.ApiClient;
import com.example.gymtrackerpro.network.AuthApi;
import com.example.gymtrackerpro.network.RegisterRequest;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etAge;
    private EditText etHeight;
    private EditText etWeight;
    private EditText etFitnessGoal;

    private Button btnCreateAccount;

    private GymTrackerDatabase database;
    private AuthApi authApi;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        database = GymTrackerDatabase.getDatabase(
                getApplicationContext()
        );

        authApi = ApiClient.getClient().create(AuthApi.class);

        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etAge = findViewById(R.id.etRegisterAge);
        etHeight = findViewById(R.id.etRegisterHeight);
        etWeight = findViewById(R.id.etRegisterWeight);
        etFitnessGoal = findViewById(R.id.etRegisterFitnessGoal);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnCreateAccount.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim()
                .toLowerCase();
        String password = etPassword.getText().toString();

        String ageText = etAge.getText().toString().trim();
        String heightText = etHeight.getText().toString().trim();
        String weightText = etWeight.getText().toString().trim();
        String fitnessGoal = etFitnessGoal.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()
                || password.isEmpty() || ageText.isEmpty()
                || heightText.isEmpty() || weightText.isEmpty()
                || fitnessGoal.isEmpty()) {

            Toast.makeText(this,
                    "Please complete all fields.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError(
                    "Password must be at least 8 characters"
            );
            etPassword.requestFocus();
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
        btnCreateAccount.setText("CREATING ACCOUNT...");

        executor.execute(() -> {

            try {
                User existingUser =
                        database.userDao().getUserByEmail(email);

                if (existingUser != null) {
                    runOnUiThread(() -> {
                        btnCreateAccount.setEnabled(true);
                        btnCreateAccount.setText("CREATE ACCOUNT");

                        etEmail.setError(
                                "This email already exists locally"
                        );
                        etEmail.requestFocus();
                    });
                    return;
                }

                runOnUiThread(() -> registerWithBackend(
                        name, email, password, age, height,
                        weight, fitnessGoal
                ));

            } catch (Exception e) {

                Log.e("RegisterActivity",
                        "Local account check failed", e);

                runOnUiThread(() -> {
                    btnCreateAccount.setEnabled(true);
                    btnCreateAccount.setText("CREATE ACCOUNT");

                    Toast.makeText(this,
                            "Could not check local account.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void registerWithBackend(
            String name,
            String email,
            String password,
            int age,
            double height,
            double weight,
            String fitnessGoal) {

        RegisterRequest request =
                new RegisterRequest(email, password);

        authApi.register(request).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response) {

                        if (response.isSuccessful()) {

                            saveLocalProfile(
                                    name, email, age,
                                    height, weight, fitnessGoal
                            );

                        } else {

                            btnCreateAccount.setEnabled(true);
                            btnCreateAccount.setText("CREATE ACCOUNT");

                            if (response.code() == 400
                                    || response.code() == 409) {

                                etEmail.setError(
                                        "Registration details rejected. "
                                                + "Check your email."
                                );
                            }

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Backend registration failed. HTTP "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            // Diagnostic logging
                            try {
                                String errorBody =
                                        response.errorBody() != null
                                                ? response.errorBody().string()
                                                : "No error body returned";

                                Log.e("RegisterActivity",
                                        "Registration HTTP error: "
                                                + response.code());

                                Log.e("RegisterActivity",
                                        "Registration error details: "
                                                + errorBody);

                            } catch (Exception e) {
                                Log.e("RegisterActivity",
                                        "Could not read registration error",
                                        e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t) {

                        btnCreateAccount.setEnabled(true);
                        btnCreateAccount.setText("CREATE ACCOUNT");

                        Toast.makeText(
                                RegisterActivity.this,
                                "Cannot connect to server. "
                                        + "Check your Wi-Fi and backend.",
                                Toast.LENGTH_LONG
                        ).show();

                        Log.e("RegisterActivity",
                                "Backend connection failed", t);
                    }
                }
        );
    }

    private void saveLocalProfile(
            String name,
            String email,
            int age,
            double height,
            double weight,
            String fitnessGoal) {

        executor.execute(() -> {

            try {
                User user = new User(
                        name, email, age, height, weight, fitnessGoal
                );

                long userId = database.userDao().insertUser(user);

                runOnUiThread(() -> {

                    btnCreateAccount.setEnabled(true);
                    btnCreateAccount.setText("CREATE ACCOUNT");

                    if (userId > 0) {
                        showSuccessDialog(name);
                    } else {
                        Toast.makeText(this,
                                "Server account created, but local "
                                        + "profile saving failed.",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {

                Log.e("RegisterActivity",
                        "Local profile saving failed", e);

                runOnUiThread(() -> {
                    btnCreateAccount.setEnabled(true);
                    btnCreateAccount.setText("CREATE ACCOUNT");

                    Toast.makeText(this,
                            "Server account created, but local "
                                    + "profile saving failed.",
                            Toast.LENGTH_LONG).show();
                });
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
