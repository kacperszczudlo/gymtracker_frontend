package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button; // 🟢 Dodaj importy
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import api.model.ApiClient;
import api.model.ApiService;
import api.model.UpdateUserProfileRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateMeasurementsActivity extends AppCompatActivity {
    private static final String TAG = "UpdateMeasurements";
    private EditText heightEditText, armCircEditText, waistCircEditText, hipCircEditText, weightEditText;
    private int userId;
    private String loadedGender = "unspecified";
    private Integer loadedHeight = null;
    private ApiService apiService;
    private Button saveButton; // 🟢 Deklaracja przycisku
    private ImageButton menuButton, homeButton, profileButton; // 🟢 Deklaracja przycisków nawigacji

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_measurements);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        // 🟢🟢🟢 DODAJEMY INICJALIZACJĘ WIDOKÓW! 🟢🟢🟢
        heightEditText = findViewById(R.id.heightEditText);
        armCircEditText = findViewById(R.id.armCircEditText);
        waistCircEditText = findViewById(R.id.waistCircEditText);
        hipCircEditText = findViewById(R.id.hipCircEditText);
        weightEditText = findViewById(R.id.weightEditText);
        saveButton = findViewById(R.id.saveButton); // Inicjalizacja przycisku save
        menuButton = findViewById(R.id.menuButton);
        homeButton = findViewById(R.id.homeButton);
        profileButton = findViewById(R.id.profileButton);

        if (heightEditText == null || armCircEditText == null || waistCircEditText == null ||
                hipCircEditText == null || weightEditText == null || saveButton == null ||
                menuButton == null || homeButton == null || profileButton == null) {
            Log.e(TAG, "Jeden lub więcej widoków nie został znaleziony!");
            Toast.makeText(this, "Błąd inicjalizacji widoków!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 🟢🟢🟢 KONIEC INICJALIZACJI WIDOKÓW 🟢🟢🟢

        if (userId == -1) {
            Toast.makeText(this, "Błąd użytkownika. Spróbuj ponownie się zalogować.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadProfileDataFromPrefs();

        saveButton.setOnClickListener(v -> saveMeasurementsViaApi()); // Teraz `saveButton` jest zainicjalizowany

        // 🟢 DODAJEMY LISTENERY DLA NAWIGACJI 🟢
        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateMeasurementsActivity.this, AccountSettingsActivity.class);
            startActivity(intent);
            setResult(RESULT_OK);
            finish();
        });

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateMeasurementsActivity.this, TrainingMainActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateMeasurementsActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });
        // 🟢 KONIEC LISTENERÓW DLA NAWIGACJI 🟢
    }

    private void loadProfileDataFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        loadedGender = prefs.getString("gender", "unspecified");
        int heightFromPrefs = prefs.getInt("height", 0);
        loadedHeight = (heightFromPrefs != 0) ? heightFromPrefs : null;

        if (loadedHeight != null) {
            heightEditText.setText(String.format(Locale.US, "%d", loadedHeight));
        }
        // Możesz dodać ładowanie ostatnich pomiarów z SharedPreferences, jeśli chcesz
        weightEditText.setText(prefs.contains("weight") ? String.format(Locale.US, "%.1f", prefs.getFloat("weight", 0f)) : "");
        armCircEditText.setText(prefs.contains("arm") ? String.format(Locale.US, "%.1f", prefs.getFloat("arm", 0f)) : "");
        waistCircEditText.setText(prefs.contains("waist") ? String.format(Locale.US, "%.1f", prefs.getFloat("waist", 0f)) : "");
        hipCircEditText.setText(prefs.contains("hip") ? String.format(Locale.US, "%.1f", prefs.getFloat("hip", 0f)) : "");

        Log.d(TAG, "Loaded from Prefs: gender=" + loadedGender + ", height=" + loadedHeight);
    }


    private void saveMeasurementsViaApi() {
        String heightStr = heightEditText.getText().toString().trim();
        String armCircStr = armCircEditText.getText().toString().trim();
        String waistCircStr = waistCircEditText.getText().toString().trim();
        String hipCircStr = hipCircEditText.getText().toString().trim();
        String weightStr = weightEditText.getText().toString().trim();

        if (weightStr.isEmpty()) {
            Toast.makeText(this, "Waga jest wymagana", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer height = null;
        Double armCirc = null;
        Double waistCirc = null;
        Double hipCirc = null;
        Double weight;

        try {
            weight = Double.parseDouble(weightStr);
            if (!heightStr.isEmpty()) height = Integer.parseInt(heightStr); else height = loadedHeight; // Użyj starej jeśli nowa pusta
            if (!armCircStr.isEmpty()) armCirc = Double.parseDouble(armCircStr);
            if (!waistCircStr.isEmpty()) waistCirc = Double.parseDouble(waistCircStr);
            if (!hipCircStr.isEmpty()) hipCirc = Double.parseDouble(hipCircStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Nieprawidłowy format liczby", Toast.LENGTH_SHORT).show();
            return;
        }

        String genderToSend = loadedGender;
        if (genderToSend == null || genderToSend.isEmpty()){
            genderToSend = "unspecified";
        }

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                genderToSend,
                height,
                weight,
                waistCirc,
                armCirc,
                hipCirc
        );

        Log.d(TAG, "Wysyłanie aktualizacji profilu dla userId: " + userId + " z danymi: G:"+genderToSend + " H:"+height + " W:"+weight + " A:"+armCirc + " WC:"+waistCirc + " HC:"+hipCirc);

        apiService.updateUserProfile(userId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UpdateMeasurementsActivity.this, "Pomiary zapisane", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Pomiary zaktualizowane pomyślnie dla userId=" + userId);

                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    if (request.getGender() != null) editor.putString("gender", request.getGender());
                    if (request.getHeight() != null) editor.putInt("height", request.getHeight());
                    if (request.getWeight() != null) editor.putFloat("weight", request.getWeight().floatValue());
                    if (request.getWaistCircumference() != null) editor.putFloat("waist", request.getWaistCircumference().floatValue());
                    if (request.getArmCircumference() != null) editor.putFloat("arm", request.getArmCircumference().floatValue());
                    if (request.getHipCircumference() != null) editor.putFloat("hip", request.getHipCircumference().floatValue());
                    editor.apply();

                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(UpdateMeasurementsActivity.this, "Błąd serwera: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Błąd aktualizacji pomiarów, kod: " + response.code() + ", wiadomość: " + response.message());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Błąd ciała odpowiedzi: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Błąd odczytu errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(UpdateMeasurementsActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Błąd połączenia przy aktualizacji pomiarów", t);
            }
        });
    }
}