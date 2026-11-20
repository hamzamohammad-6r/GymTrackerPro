package com.example.gymtrackerpro;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gymtrackerpro.network.ApiClient;
import com.example.gymtrackerpro.network.AuthApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView textGoToRegister, textContinueOffline;

    private AuthApi authApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textGoToRegister = findViewById(R.id.textGoToRegister);
        textContinueOffline = findViewById(R.id.textContinueOffline);

        // Remove the offline login option.
        if (textContinueOffline != null) {
            textContinueOffline.setVisibility(View.GONE);
        }

        authApi = ApiClient.getClient(this).create(AuthApi.class);

        btnLogin.setOnClickListener(v -> login());

        findViewById(R.id.textForgotPassword).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    ForgotPasswordActivity.class
            );
            startActivity(intent);
        });

        textGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            ));
        });
    }

    private void login() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {

            CustomNotification.show(
                    this,
                    "Missing Information",
                    "Please enter your email address and password."
            );

            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        authApi.login(request).enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, Object>> call,
                            Response<Map<String, Object>> response
                    ) {

                        if (isFinishing() || isDestroyed()) return;

                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");

                        if (response.isSuccessful()
                                && response.body() != null) {

                            Map<String, Object> result = response.body();

                            Object tokenObject = result.get("accessToken");
                            Object accountIdObject = result.get("accountId");

                            if (tokenObject == null
                                    || tokenObject.toString().trim().isEmpty()) {

                                CustomNotification.show(
                                        LoginActivity.this,
                                        "Login Error",
                                        "The login response did not contain a valid access token."
                                );

                                return;
                            }

                            String accessToken = tokenObject.toString();

                            String accountId = accountIdObject == null
                                    ? ""
                                    : accountIdObject.toString();

                            try {

                                GymTrackerSession.saveSession(
                                        LoginActivity.this,
                                        accessToken,
                                        email,
                                        accountId
                                );

                                openDashboard();

                            } catch (Exception e) {

                                Log.e(TAG, "Secure session storage failed", e);

                                CustomNotification.show(
                                        LoginActivity.this,
                                        "Secure Storage Error",
                                        "Could not securely save your login. Please try again."
                                );
                            }

                        } else {

                            Log.e(
                                    TAG,
                                    "Login HTTP error. Status: " + response.code()
                            );

                            CustomNotification.show(
                                    LoginActivity.this,
                                    "Login Failed",
                                    "Check your email address and password, then try again."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, Object>> call,
                            Throwable t
                    ) {

                        Log.e(TAG, "Login network request failed", t);

                        if (isFinishing() || isDestroyed()) return;

                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");

                        CustomNotification.show(
                                LoginActivity.this,
                                "Connection Error",
                                "Could not connect to the server. Check your Wi-Fi connection."
                        );
                    }
                }
        );
    }

    private void openDashboard() {

        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        startActivity(intent);
        finish();
    }
}
