package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gymtrackerpro.network.ApiClient;
import com.example.gymtrackerpro.network.AuthApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etForgotEmail;
    private Button btnSendResetRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etForgotEmail = findViewById(R.id.etForgotEmail);
        btnSendResetRequest = findViewById(R.id.btnSendResetRequest);

        btnSendResetRequest.setOnClickListener(v -> requestPasswordReset());
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

        AuthApi authApi = ApiClient.getClient().create(AuthApi.class);

        authApi.forgotPassword(request).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call,
                                   Response<Map<String, String>> response) {
                btnSendResetRequest.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Reset request successful! Check your backend console for the token.",
                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(
                            ForgotPasswordActivity.this,
                            ResetPasswordActivity.class
                    );
                    startActivity(intent);

                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Could not process request. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                btnSendResetRequest.setEnabled(true);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Connection failed: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
