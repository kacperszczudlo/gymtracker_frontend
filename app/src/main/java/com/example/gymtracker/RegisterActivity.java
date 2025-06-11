package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import api.model.ApiClient;
import api.model.ApiService;
import api.model.RegisterRequest;
import api.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, surnameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        usernameEditText = findViewById(R.id.registerUsernameEditText);
        surnameEditText = findViewById(R.id.registerUserSurnameEditText);
        emailEditText = findViewById(R.id.registerUserEmailEditText);
        passwordEditText = findViewById(R.id.registerPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText);
        Button registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String surname = surnameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();

            if (username.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Nieprawidłowy format adresu email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Hasła nie są zgodne", Toast.LENGTH_SHORT).show();
                return;
            }

            // KLUCZOWA ZMIANA: Przekazuj null dla pól, które będą uzupełniane później w ProfileActivity
            RegisterRequest request = new RegisterRequest(
                    username,
                    surname,
                    email,
                    password,
                    null, // gender
                    null, // height
                    null, // weight
                    null, // waistCircumference
                    null, // armCircumference
                    null  // hipCircumference
            );

            apiService.register(request).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putInt("user_id", user.getId())
                                .putString("email", user.getEmail())
                                .putString("username", user.getUsername())
                                .putString("surname", user.getSurname())
                                .apply();

                        Toast.makeText(RegisterActivity.this, "Zarejestrowano pomyślnie! Uzupełnij swój profil.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, ProfileActivity.class));
                        finish();
                    } else {
                        // Logowanie błędu po stronie serwera
                        String errorMessage = "Błąd rejestracji";
                        if (response.errorBody() != null) {
                            try {
                                errorMessage += ": " + response.code() + " - " + response.errorBody().string();
                                Log.e("RegisterActivity", "Error Body: " + errorMessage);
                            } catch (Exception e) {
                                Log.e("RegisterActivity", "Error parsing error body", e);
                                errorMessage += ": " + response.code();
                            }
                        } else {
                            errorMessage += ": " + response.code();
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.e("RegisterActivity", "Błąd połączenia", t);
                    Toast.makeText(RegisterActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        });
    }
}