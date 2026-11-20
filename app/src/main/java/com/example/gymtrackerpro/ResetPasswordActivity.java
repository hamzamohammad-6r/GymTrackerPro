package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gymtrackerpro.network.ApiClient;
import com.example.gymtrackerpro.network.AuthApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etResetToken, etNewPassword;
    private Button btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etResetToken = findViewById(R.id.etResetToken);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {

        String token = etResetToken.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString();

        if (token.isEmpty()) {
            etResetToken.setError("Enter your reset token");
            return;
        }

        if (newPassword.length() < 8) {
            etNewPassword.setError(
                    "Password must be at least 8 characters"
            );
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("token", token);
        request.put("newPassword", newPassword);

        btnResetPassword.setEnabled(false);

        AuthApi authApi = ApiClient.getClient(this).create(AuthApi.class);

        authApi.resetPassword(request).enqueue(
                new Callback<Map<String, String>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, String>> call,
                            Response<Map<String, String>> response
                    ) {

                        if (isFinishing() || isDestroyed()) return;

                        btnResetPassword.setEnabled(true);

                        if (response.isSuccessful()) {

                            CustomNotification.show(
                                    ResetPasswordActivity.this,
                                    "Password Reset Successful",
                                    "Your password has been changed. You can now log in."
                            );

                            finish();

                        } else {

                            CustomNotification.show(
                                    ResetPasswordActivity.this,
                                    "Reset Failed",
                                    "Your reset token is invalid or has expired."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, String>> call,
                            Throwable t
                    ) {

                        if (isFinishing() || isDestroyed()) return;

                        btnResetPassword.setEnabled(true);

                        String errorMessage = t.getMessage();

                        if (errorMessage == null || errorMessage.trim().isEmpty()) {
                            errorMessage = "Please check your internet connection and try again.";
                        } else {
                            errorMessage = "Connection failed: " + errorMessage;
                        }

                        CustomNotification.show(
                                ResetPasswordActivity.this,
                                "Connection Error",
                                errorMessage
                        );
                    }
                }
        );
    }
}
