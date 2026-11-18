package com.example.gymtrackerpro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
            etNewPassword.setError("Password must be at least 8 characters");
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("token", token);
        request.put("newPassword", newPassword);

        btnResetPassword.setEnabled(false);

        AuthApi authApi = ApiClient.getClient().create(AuthApi.class);

        authApi.resetPassword(request).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call,
                                   Response<Map<String, String>> response) {
                btnResetPassword.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this,
                            "Password reset successfully! You can now log in.",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this,
                            "Invalid or expired reset token.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                btnResetPassword.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this,
                        "Connection failed: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
