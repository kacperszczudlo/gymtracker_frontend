package com.example.gymtracker;

// import android.content.ContentValues; // Już niepotrzebne
import android.content.Intent;
import android.content.SharedPreferences;
// import android.database.Cursor; // Już niepotrzebne
// import android.database.sqlite.SQLiteDatabase; // Już niepotrzebne
import android.os.Bundle;
import android.util.Log; // Dodaj, jeśli potrzebne
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import api.model.ApiClient; // DODAJ
import api.model.ApiService; // DODAJ
import api.model.UpdateUserProfileRequest; // DODAJ
import retrofit2.Call; // DODAJ
import retrofit2.Callback; // DODAJ
import retrofit2.Response; // DODAJ

public class UpdateUserDataActivity extends AppCompatActivity {
    private int userId;
    private EditText usernameEditText, surnameEditText, emailEditText, passwordEditText; // passwordEditText to dla nowego hasła

    private ApiService apiService; // DODAJEMY

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_user_data);

        apiService = ApiClient.getClient(this).create(ApiService.class); // DODAJEMY

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Błąd użytkownika. Spróbuj ponownie się zalogować.", Toast.LENGTH_SHORT).show();
            // ... (przekierowanie do logowania)
            finish();
            return;
        }

        usernameEditText = findViewById(R.id.usernameEditText);
        surnameEditText = findViewById(R.id.surnameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText); // To pole dla nowego hasła
        Button saveButton = findViewById(R.id.saveButton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton homeButton = findViewById(R.id.homeButton);

        // Null checks dla przycisków (jeśli potrzebne, zakładam, że są w layout)
        if (menuButton == null || profileButton == null || homeButton == null || saveButton == null) {
            Toast.makeText(this, "Błąd: Nie znaleziono przycisków interfejsu", Toast.LENGTH_SHORT).show();
            // rozważ finish();
            return;
        }

        loadUserDataFromPrefs(); // Zmieniona nazwa

        saveButton.setOnClickListener(v -> saveUserDataViaApi()); // Zmieniona nazwa

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDataActivity.this, AccountSettingsActivity.class);
            startActivity(intent);
        });
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDataActivity.this, UserProfileActivity.class);
            startActivity(intent);
            finish(); // Zamknij bieżącą, aby UserProfileActivity odświeżyło dane
        });
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDataActivity.this, TrainingMainActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserDataFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        usernameEditText.setText(prefs.getString("username", ""));
        surnameEditText.setText(prefs.getString("surname", ""));
        emailEditText.setText(prefs.getString("email", ""));
        // Pole hasła zostawiamy puste, użytkownik wprowadza nowe, jeśli chce zmienić
    }

    private void saveUserDataViaApi() {
        String username = usernameEditText.getText().toString().trim();
        String surname = surnameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String newPassword = passwordEditText.getText().toString(); // Nie .trim() dla hasła

        if (username.isEmpty() || surname.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nazwa użytkownika, nazwisko i email są wymagane", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Nieprawidłowy format email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Jeśli pole hasła jest puste, wysyłamy null, aby backend wiedział, że nie zmieniamy hasła.
        // Jeśli nie jest puste, wysyłamy jego zawartość.
        String passwordToSend = newPassword.isEmpty() ? null : newPassword;

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(username, surname, email, passwordToSend);
        // Pola profilowe (gender, height, weight etc.) będą null w tym żądaniu,
        // więc backend (jeśli jest dobrze napisany) nie powinien ich modyfikować.

        Log.d("UpdateUserData", "Wysyłanie żądania aktualizacji dla userId: " + userId + " z username: " + username + ", email: " + email + ", hasło ustawione: " + (passwordToSend != null));

        apiService.updateUserProfile(userId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UpdateUserDataActivity.this, "Dane zaktualizowane", Toast.LENGTH_SHORT).show();

                    // Zaktualizuj dane w SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", username);
                    editor.putString("surname", surname);
                    editor.putString("email", email);
                    // NIE ZAPISUJEMY HASŁA W SHARED PREFERENCES
                    editor.apply();

                    Intent intent = new Intent(UpdateUserDataActivity.this, UserProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Aby odświeżyć UserProfileActivity
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Błąd podczas aktualizacji";
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyStr = response.errorBody().string();
                            // Tutaj możesz spróbować sparsować JSON z błędem, jeśli backend go zwraca
                            // np. jeśli backend zwraca {"message": "Email zajęty"}
                            Log.e("UpdateUserData", "Błąd serwera: " + response.code() + " Body: " + errorBodyStr);
                            if (errorBodyStr.toLowerCase().contains("email") && errorBodyStr.toLowerCase().contains("zajęty")) {
                                errorMessage = "Podany adres e-mail jest już używany.";
                            } else {
                                errorMessage = "Błąd serwera: " + response.code();
                            }
                        } catch (Exception e) {
                            Log.e("UpdateUserData", "Błąd odczytu errorBody", e);
                            errorMessage = "Błąd serwera: " + response.code();
                        }
                    } else {
                        errorMessage = "Błąd serwera: " + response.code();
                    }
                    Toast.makeText(UpdateUserDataActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(UpdateUserDataActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("UpdateUserData", "Błąd połączenia", t);
            }
        });
    }
}