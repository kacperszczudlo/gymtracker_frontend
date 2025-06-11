package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Dodaj dla logowania
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal; // DODAJ
import java.util.Arrays; // DODAJ
import java.util.List; // DODAJ
import java.util.Map; // DODAJ
import java.util.Locale; // DODAJ

import api.model.ApiClient; // DODAJ
import api.model.ApiService; // DODAJ
import retrofit2.Call; // DODAJ
import retrofit2.Callback; // DODAJ
import retrofit2.Response; // DODAJ

public class AchievementsActivity extends AppCompatActivity {
    private int userId;
    private ApiService apiService; // DODAJEMY

    private TextView benchPressTextView;
    private TextView squatsTextView;
    private TextView deadliftTextView;

    private final String EXERCISE_BENCH_PRESS = "Wyciskanie sztangi";
    private final String EXERCISE_SQUATS = "Przysiady";
    private final String EXERCISE_DEADLIFT = "Martwy ciag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        apiService = ApiClient.getClient(this).create(ApiService.class); // DODAJEMY

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Błąd użytkownika. Spróbuj ponownie się zalogować.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize TextViews
        benchPressTextView = findViewById(R.id.benchPressTextView);
        squatsTextView = findViewById(R.id.squatsTextView);
        deadliftTextView = findViewById(R.id.deadliftTextView);

        // Ustawiamy tekst "Ładowanie..." na początku
        benchPressTextView.setText("Ładowanie...");
        squatsTextView.setText("Ładowanie...");
        deadliftTextView.setText("Ładowanie...");

        fetchAchievementsData();

        // Set up navigation (bez zmian)
        ImageButton menuButton = findViewById(R.id.menuButton);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton profileButton = findViewById(R.id.profileButton);

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(AchievementsActivity.this, AccountSettingsActivity.class);
            startActivity(intent);
        });

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AchievementsActivity.this, TrainingMainActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(AchievementsActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });
    }

    private void fetchAchievementsData() {
        List<String> exercisesToFetch = Arrays.asList(EXERCISE_BENCH_PRESS, EXERCISE_SQUATS, EXERCISE_DEADLIFT);

        Log.d("AchievementsActivity", "Pobieranie max ciężarów dla userId: " + userId + " dla ćwiczeń: " + exercisesToFetch);

        apiService.getMaxWeightsForAchievements(userId, exercisesToFetch).enqueue(new Callback<Map<String, BigDecimal>>() {
            @Override
            public void onResponse(Call<Map<String, BigDecimal>> call, Response<Map<String, BigDecimal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, BigDecimal> maxWeights = response.body();
                    Log.d("AchievementsActivity", "Otrzymano max ciężary: " + maxWeights);

                    updateTextViewWithMaxWeight(benchPressTextView, maxWeights.get(EXERCISE_BENCH_PRESS));
                    updateTextViewWithMaxWeight(squatsTextView, maxWeights.get(EXERCISE_SQUATS));
                    updateTextViewWithMaxWeight(deadliftTextView, maxWeights.get(EXERCISE_DEADLIFT));

                } else {
                    Toast.makeText(AchievementsActivity.this, "Błąd pobierania osiągnięć: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("AchievementsActivity", "Błąd API: " + response.code() + " - " + response.message());
                    benchPressTextView.setText("Błąd danych");
                    squatsTextView.setText("Błąd danych");
                    deadliftTextView.setText("Błąd danych");
                }
            }

            @Override
            public void onFailure(Call<Map<String, BigDecimal>> call, Throwable t) {
                Toast.makeText(AchievementsActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("AchievementsActivity", "Błąd połączenia", t);
                benchPressTextView.setText("Błąd sieci");
                squatsTextView.setText("Błąd sieci");
                deadliftTextView.setText("Błąd sieci");
            }
        });
    }

    private void updateTextViewWithMaxWeight(TextView textView, BigDecimal maxWeight) {
        if (textView == null) return;

        if (maxWeight != null && maxWeight.compareTo(BigDecimal.ZERO) > 0) {
            textView.setText(String.format(Locale.US, "%.1f kg", maxWeight.doubleValue()));
        } else {
            textView.setText("Brak danych");
        }
    }
}