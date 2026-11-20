package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gymtrackerpro.network.ApiClient;
import com.example.gymtrackerpro.network.AuthApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText etForgotEmail;
    private Button btnSendResetRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_forgot_password);

        etForgotEmail = findViewById(R.id.etForgotEmail);
        btnSendResetRequest = findViewById(R.id.btnSendResetRequest);

        btnSendResetRequest.setOnClickListener(
                v -> requestPasswordReset()
        );
    }

    private void requestPasswordReset() {

        String email = etForgotEmail.getText().toString().trim();

        if (email.isEmpty()) {

            etForgotEmail.setError("Enter your email address");
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("email", email);

        btnSendResetRequest.setEnabled(false);

        AuthApi authApi = ApiClient.getClient(this)
                .create(AuthApi.class);

        authApi.forgotPassword(request).enqueue(
                new Callback<Map<String, String>>() {

                    @Override
                    public void onResponse(
                            Call<Map<String, String>> call,
                            Response<Map<String, String>> response
                    ) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnSendResetRequest.setEnabled(true);

                        if (response.isSuccessful()) {

                            CustomNotification.show(
                                    ForgotPasswordActivity.this,
                                    "Reset Request Successful",
                                    "Check your backend console for the reset token."
                            );

                            Intent intent = new Intent(
                                    ForgotPasswordActivity.this,
                                    ResetPasswordActivity.class
                            );

                            startActivity(intent);

                        } else {

                            CustomNotification.show(
                                    ForgotPasswordActivity.this,
                                    "Request Failed",
                                    "Could not process your request. Please try again."
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Map<String, String>> call,
                            Throwable t
                    ) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnSendResetRequest.setEnabled(true);

                        Log.e(
                                TAG,
                                "Password reset request failed",
                                t
                        );

                        String errorMessage = t.getMessage();

                        if (errorMessage == null
                                || errorMessage.trim().isEmpty()) {
                            errorMessage = "Please check your internet connection and try again.";
                        } else {
                            errorMessage = "Connection failed: " + errorMessage;
                        }

                        CustomNotification.show(
                                ForgotPasswordActivity.this,
                                "Connection Error",
                                errorMessage
                        );
                    }
                }
        );
    }
}
