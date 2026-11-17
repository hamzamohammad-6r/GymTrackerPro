package com.example.gymtrackerpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gymtrackerpro.network.ApiClient;
import com.example.gymtrackerpro.network.AuthApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

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

        authApi = ApiClient.getClient().create(AuthApi.class);

        btnLogin.setOnClickListener(v -> login());

        textGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        textContinueOffline.setOnClickListener(v -> openDashboard());
    }

    private void login() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter your email and password.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        Map<String, String> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);

        authApi.login(request).enqueue(new Callback<Map<String, Object>>() {

            @Override
            public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response) {

                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                if (response.isSuccessful() && response.body() != null) {

                    Map<String, Object> result = response.body();

                    Object tokenObject = result.get("accessToken");
                    Object accountIdObject = result.get("accountId");

                    if (tokenObject == null) {
                        Toast.makeText(LoginActivity.this,
                                "Login response did not contain an access token.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String accessToken = tokenObject.toString();
                    String accountId = accountIdObject == null
                            ? ""
                            : accountIdObject.toString();

                    SharedPreferences preferences =
                            getSharedPreferences("GymTrackerSession", MODE_PRIVATE);

                    preferences.edit()
                            .putString("accessToken", accessToken)
                            .putString("email", email)
                            .putString("accountId", accountId)
                            .apply();

                    Toast.makeText(LoginActivity.this,
                            "Login successful!",
                            Toast.LENGTH_SHORT).show();

                    openDashboard();

                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login failed. Check your email and password.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(
                    Call<Map<String, Object>> call,
                    Throwable t) {

                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                Toast.makeText(LoginActivity.this,
                        "Could not connect to the server. Check your Wi-Fi connection.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openDashboard() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
