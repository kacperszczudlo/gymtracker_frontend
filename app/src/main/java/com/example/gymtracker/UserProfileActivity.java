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
import api.model.ExerciseExtremesDto; // DODAJ TEN IMPORT
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
            Intent intent = new Intent(this, UpdateUserDataActivity.class);
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
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            fetchDataAndDisplayProgress(); // Odśwież dane wagowe i pomiarów
            fetchBenchPressExtremes();    // Odśwież dane wyciskania
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String updatedUsername = prefs.getString("username", "Brak danych");
            binding.usernameTextView.setText(updatedUsername);
        } else if (requestCode == REQUEST_CODE_SETTINGS) {
            fetchDataAndDisplayProgress();
            fetchBenchPressExtremes();
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
        // binding.progressBenchPressTextView.setText("Ładowanie..."); // To będzie obsłużone przez fetchBenchPressExtremes

        initialStatsApi = null;
        historyStatsApi = null;

        apiService.getInitialBodyStat(userId).enqueue(new Callback<BodyStatHistoryDto>() {
            @Override
            public void onResponse(Call<BodyStatHistoryDto> call, Response<BodyStatHistoryDto> responseInitial) {
                if (responseInitial.isSuccessful() && responseInitial.body() != null) {
                    initialStatsApi = responseInitial.body();
                } else {
                    initialStatsApi = null;
                }
                fetchHistoryData();
            }
            @Override
            public void onFailure(Call<BodyStatHistoryDto> call, Throwable t) {
                initialStatsApi = null;
                fetchHistoryData();
            }
        });
    }

    private void fetchHistoryData() {
        apiService.getBodyStatHistory(userId).enqueue(new Callback<List<BodyStatHistoryDto>>() {
            @Override
            public void onResponse(Call<List<BodyStatHistoryDto>> call, Response<List<BodyStatHistoryDto>> responseHistory) {
                if (responseHistory.isSuccessful() && responseHistory.body() != null) {
                    historyStatsApi = responseHistory.body();
                } else {
                    historyStatsApi = null;
                }
                updateUiWithApiData();
            }
            @Override
            public void onFailure(Call<List<BodyStatHistoryDto>> call, Throwable t) {
                historyStatsApi = null;
                updateUiWithApiData();
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
                    } else if (latestBench != null) { // Tylko najnowszy wynik
                        updateProgressTextView(binding.progressBenchPressTextView, latestBench, BigDecimal.ZERO, "kg");
                    } else if (initialBench != null) { // Tylko początkowy wynik
                        updateProgressTextView(binding.progressBenchPressTextView, initialBench, BigDecimal.ZERO, "kg");
                        // Można dodać dopisek "(początkowy)", ale updateProgressTextView tego nie obsługuje
                        // binding.progressBenchPressTextView.setText(String.format(Locale.US, "%.1f kg (początkowy)", initialBench.doubleValue()));
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

        if (initialStatsApi != null && initialStatsApi.getWeight() != null) {
            initialWeightVal = initialStatsApi.getWeight();
        }
        if (historyStatsApi != null && !historyStatsApi.isEmpty()) {
            BodyStatHistoryDto latestEntry = historyStatsApi.get(historyStatsApi.size() - 1);
            if (latestEntry.getWeight() != null) {
                latestWeightVal = latestEntry.getWeight();
            }
            if (initialWeightVal == null && !historyStatsApi.isEmpty() && historyStatsApi.get(0).getWeight() != null) {
                initialWeightVal = historyStatsApi.get(0).getWeight();
            }
        }
        if (latestWeightVal != null && initialWeightVal != null) {
            BigDecimal weightProgress = latestWeightVal.subtract(initialWeightVal);
            updateProgressTextView(binding.progressWeightTextView, latestWeightVal, weightProgress, "kg");
        } else if (latestWeightVal != null) {
            updateProgressTextView(binding.progressWeightTextView, latestWeightVal, BigDecimal.ZERO, "kg");
        } else if (initialWeightVal != null) {
            updateProgressTextView(binding.progressWeightTextView, initialWeightVal, BigDecimal.ZERO, "kg");
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
            if (initialArmCircVal == null && !historyStatsApi.isEmpty() && historyStatsApi.get(0).getArmCircumference() != null) {
                initialArmCircVal = historyStatsApi.get(0).getArmCircumference();
            }
        }
        if (latestArmCircVal != null && initialArmCircVal != null) {
            BigDecimal armProgress = latestArmCircVal.subtract(initialArmCircVal);
            updateProgressTextView(binding.progressArmCircTextView, latestArmCircVal, armProgress, "cm");
        } else if (latestArmCircVal != null) {
            updateProgressTextView(binding.progressArmCircTextView, latestArmCircVal, BigDecimal.ZERO, "cm");
        } else if (initialArmCircVal != null) {
            updateProgressTextView(binding.progressArmCircTextView, initialArmCircVal, BigDecimal.ZERO, "cm");
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
        String progressTextFormat = "";

        if (progressValue != null && progressValue.compareTo(BigDecimal.ZERO) != 0) {
            progressTextFormat = String.format(Locale.US, " (%s%.1f %s)",
                    progressValue.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "-",
                    progressValue.abs().doubleValue(), unit);
        }

        SpannableString spannable = new SpannableString(currentValueText + progressTextFormat);
        if (!progressTextFormat.isEmpty()) {
            int color = getResources().getColor(progressValue.compareTo(BigDecimal.ZERO) >= 0 ? R.color.green : R.color.red, getTheme());
            spannable.setSpan(new ForegroundColorSpan(color), currentValueText.length(), spannable.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannable);
    }
}