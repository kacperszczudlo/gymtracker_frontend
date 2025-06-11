package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gymtracker.databinding.ActivityUserProfileBinding;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import api.model.ApiClient;
import api.model.ApiService;
import api.model.BodyStatHistoryDto;
import api.model.ExerciseExtremesDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {
    private ActivityUserProfileBinding binding;
    private int userId;
    private static final int REQUEST_CODE_SETTINGS = 1;

    private ApiService apiService;
    private BodyStatHistoryDto initialStatsApi = null;
    private List<BodyStatHistoryDto> historyStatsApi = null;

    private final String EXERCISE_BENCH_PRESS_NAME = "Wyciskanie sztangi"; // Nazwa ćwiczenia

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient(this).create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        Log.d("UserProfileActivity", "User ID: " + userId);

        if (userId == -1) {
            Log.e("UserProfileActivity", "Invalid userId, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String username = prefs.getString("username", "Brak danych");
        binding.usernameTextView.setText(username);

        binding.fullProgressButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullProgressActivity.class);
            startActivity(intent);
        });

        binding.achievementsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AchievementsActivity.class);
            startActivity(intent);
        });

        binding.accountSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, UpdateUserDataActivity.class); // Zmienione na UpdateUserDataActivity jeśli to ustawienia konta
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        });

        binding.logoutButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountSettingsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        });

        binding.homeButton.setOnClickListener(v -> {
            startActivity(new Intent(this, TrainingMainActivity.class));
        });

        binding.profileButton.setOnClickListener(v -> {
            Toast.makeText(this, "Jesteś już na ekranie profilu", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTINGS) { // Odświeżaj niezależnie od resultCode, bo dane mogły się zmienić
            fetchDataAndDisplayProgress(); // Odśwież dane wagowe i pomiarów
            fetchBenchPressExtremes();    // Odśwież dane wyciskania
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String updatedUsername = prefs.getString("username", "Brak danych");
            binding.usernameTextView.setText(updatedUsername);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDataAndDisplayProgress(); // Pobiera dane wagowe, obwodu ramienia
        fetchBenchPressExtremes();     // Pobiera dane dla wyciskania
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUsername = prefs.getString("username", "Brak danych");
        binding.usernameTextView.setText(currentUsername);
    }

    private void fetchDataAndDisplayProgress() {
        binding.progressWeightTextView.setText("Ładowanie...");
        binding.progressArmCircTextView.setText("Ładowanie...");

        initialStatsApi = null; // Resetuj przed każdym pobraniem
        historyStatsApi = null; // Resetuj przed każdym pobraniem

        apiService.getInitialBodyStat(userId).enqueue(new Callback<BodyStatHistoryDto>() {
            @Override
            public void onResponse(Call<BodyStatHistoryDto> call, Response<BodyStatHistoryDto> responseInitial) {
                if (responseInitial.isSuccessful() && responseInitial.body() != null) {
                    initialStatsApi = responseInitial.body();
                    Log.d("UserProfileActivity", "Initial stats API: " + (initialStatsApi != null ? "Weight: " + initialStatsApi.getWeight() + ", Arm: " + initialStatsApi.getArmCircumference() : "null"));
                } else {
                    initialStatsApi = null;
                    Log.d("UserProfileActivity", "Initial stats API: no body or not successful. Code: " + responseInitial.code());
                }
                fetchHistoryData(); // Zawsze wywołuj pobieranie historii
            }
            @Override
            public void onFailure(Call<BodyStatHistoryDto> call, Throwable t) {
                initialStatsApi = null;
                Log.e("UserProfileActivity", "Initial stats API: failure", t);
                fetchHistoryData(); // Zawsze wywołuj pobieranie historii
            }
        });
    }

    private void fetchHistoryData() {
        apiService.getBodyStatHistory(userId).enqueue(new Callback<List<BodyStatHistoryDto>>() {
            @Override
            public void onResponse(Call<List<BodyStatHistoryDto>> call, Response<List<BodyStatHistoryDto>> responseHistory) {
                if (responseHistory.isSuccessful() && responseHistory.body() != null) {
                    historyStatsApi = responseHistory.body();
                    if (!historyStatsApi.isEmpty()) {
                        Log.d("UserProfileActivity", "History stats API: " + historyStatsApi.size() + " entries. Last weight: " + historyStatsApi.get(historyStatsApi.size()-1).getWeight());
                    } else {
                        Log.d("UserProfileActivity", "History stats API: empty list.");
                    }
                } else {
                    historyStatsApi = null;
                    Log.d("UserProfileActivity", "History stats API: no body or not successful. Code: " + responseHistory.code());
                }
                updateUiWithApiData(); // Zawsze aktualizuj UI
            }
            @Override
            public void onFailure(Call<List<BodyStatHistoryDto>> call, Throwable t) {
                historyStatsApi = null;
                Log.e("UserProfileActivity", "History stats API: failure", t);
                updateUiWithApiData(); // Zawsze aktualizuj UI
            }
        });
    }

    private void fetchBenchPressExtremes() {
        binding.progressBenchPressTextView.setText("Ładowanie...");
        apiService.getExerciseExtremes(userId, EXERCISE_BENCH_PRESS_NAME).enqueue(new Callback<ExerciseExtremesDto>() {
            @Override
            public void onResponse(Call<ExerciseExtremesDto> call, Response<ExerciseExtremesDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ExerciseExtremesDto extremes = response.body();
                    BigDecimal initialBench = extremes.getInitialWeight();
                    BigDecimal latestBench = extremes.getLatestWeight();
                    Log.d("UserProfileActivity", "Bench Press: Initial=" + initialBench + ", Latest=" + latestBench);

                    if (latestBench != null && initialBench != null) {
                        BigDecimal benchProgress = latestBench.subtract(initialBench);
                        updateProgressTextView(binding.progressBenchPressTextView, latestBench, benchProgress, "kg");
                    } else if (latestBench != null) { // initialBench JEST null
                        updateProgressTextView(binding.progressBenchPressTextView, latestBench, null, "kg");
                    } else if (initialBench != null) { // latestBench jest null
                        updateProgressTextView(binding.progressBenchPressTextView, initialBench, null, "kg");
                    } else {
                        binding.progressBenchPressTextView.setText("Brak danych");
                    }
                } else {
                    binding.progressBenchPressTextView.setText("Błąd danych");
                    Log.e("UserProfileActivity", "Błąd pobierania ekstremów dla wyciskania: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ExerciseExtremesDto> call, Throwable t) {
                binding.progressBenchPressTextView.setText("Błąd sieci");
                Log.e("UserProfileActivity", "Failure pobierania ekstremów dla wyciskania", t);
            }
        });
    }

    private void updateUiWithApiData() {
        // Logika dla Wagi
        BigDecimal initialWeightVal = null;
        BigDecimal latestWeightVal = null;

        // Użyj initialStatsApi jako głównego źródła danych początkowych
        if (initialStatsApi != null && initialStatsApi.getWeight() != null) {
            initialWeightVal = initialStatsApi.getWeight();
        }

        // Pobierz najnowsze dane z historii
        if (historyStatsApi != null && !historyStatsApi.isEmpty()) {
            BodyStatHistoryDto latestEntry = historyStatsApi.get(historyStatsApi.size() - 1);
            if (latestEntry.getWeight() != null) {
                latestWeightVal = latestEntry.getWeight();
            }
            // Jeśli initialStatsApi nie dało wagi początkowej (np. użytkownik zarejestrował się bez wagi,
            // a potem dodał pierwszy pomiar), użyj pierwszego wpisu z historii jako początkowego.
            if (initialWeightVal == null && historyStatsApi.get(0).getWeight() != null) {
                initialWeightVal = historyStatsApi.get(0).getWeight();
            }
        }
        Log.d("UserProfileActivity", "Weight values for UI: Initial=" + initialWeightVal + ", Latest=" + latestWeightVal);

        if (latestWeightVal != null && initialWeightVal != null) {
            BigDecimal weightProgress = latestWeightVal.subtract(initialWeightVal);
            updateProgressTextView(binding.progressWeightTextView, latestWeightVal, weightProgress, "kg");
        } else if (latestWeightVal != null) { // initialWeightVal JEST null
            updateProgressTextView(binding.progressWeightTextView, latestWeightVal, null, "kg");
        } else if (initialWeightVal != null) { // latestWeightVal jest null
            updateProgressTextView(binding.progressWeightTextView, initialWeightVal, null, "kg");
        } else {
            binding.progressWeightTextView.setText("Brak danych");
        }

        // Logika dla Obwodu Ramienia
        BigDecimal initialArmCircVal = null;
        BigDecimal latestArmCircVal = null;

        if (initialStatsApi != null && initialStatsApi.getArmCircumference() != null) {
            initialArmCircVal = initialStatsApi.getArmCircumference();
        }

        if (historyStatsApi != null && !historyStatsApi.isEmpty()) {
            BodyStatHistoryDto latestEntry = historyStatsApi.get(historyStatsApi.size() - 1);
            if (latestEntry.getArmCircumference() != null) {
                latestArmCircVal = latestEntry.getArmCircumference();
            }
            if (initialArmCircVal == null && historyStatsApi.get(0).getArmCircumference() != null) {
                initialArmCircVal = historyStatsApi.get(0).getArmCircumference();
            }
        }
        Log.d("UserProfileActivity", "ArmCirc values for UI: Initial=" + initialArmCircVal + ", Latest=" + latestArmCircVal);

        if (latestArmCircVal != null && initialArmCircVal != null) {
            BigDecimal armProgress = latestArmCircVal.subtract(initialArmCircVal);
            updateProgressTextView(binding.progressArmCircTextView, latestArmCircVal, armProgress, "cm");
        } else if (latestArmCircVal != null) { // initialArmCircVal JEST null
            updateProgressTextView(binding.progressArmCircTextView, latestArmCircVal, null, "cm");
        } else if (initialArmCircVal != null) { // latestArmCircVal jest null
            updateProgressTextView(binding.progressArmCircTextView, initialArmCircVal, null, "cm");
        } else {
            binding.progressArmCircTextView.setText("Brak danych");
        }
    }

    private void updateProgressTextView(android.widget.TextView textView, BigDecimal currentValue, BigDecimal progressValue, String unit) {
        if (currentValue == null) {
            textView.setText("Brak danych");
            return;
        }

        String currentValueText = String.format(Locale.US, "%.1f %s", currentValue.doubleValue(), unit);
        String progressTextFormat = ""; // Domyślnie pusty

        // Jeśli progressValue jest dostępne i różne od zera, formatuj tekst progresu
        // Jeśli progressValue jest null (bo initialValue było null), to progressTextFormat pozostanie pusty.
        if (progressValue != null && progressValue.compareTo(BigDecimal.ZERO) != 0) {
            progressTextFormat = String.format(Locale.US, " (%s%.1f %s)",
                    progressValue.compareTo(BigDecimal.ZERO) > 0 ? "+" : "-", // Znak + lub -
                    progressValue.abs().doubleValue(), // Wartość absolutna progresu
                    unit);
        }
        // Jeśli progressValue to BigDecimal.ZERO, progressTextFormat również pozostanie pusty, co jest poprawne.

        SpannableString spannable = new SpannableString(currentValueText + progressTextFormat);
        if (!progressTextFormat.isEmpty()) { // Koloruj tylko jeśli jest tekst progresu
            int color = getResources().getColor(progressValue.compareTo(BigDecimal.ZERO) >= 0 ? R.color.green : R.color.red, getTheme());
            spannable.setSpan(new ForegroundColorSpan(color), currentValueText.length(), spannable.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannable);
    }
}