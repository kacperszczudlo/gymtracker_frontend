package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import api.model.ApiClient;
import api.model.ApiService;
import api.model.BodyStatHistoryDto;
import api.model.UserGoalDto;
import api.model.UserGoalUpdateRequestDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingGoalsActivity extends AppCompatActivity {
    private static final String TAG = "TrainingGoalsActivity";
    private int userId;
    private EditText targetWeightEditText, targetTrainingDaysEditText;
    private TextView weightProgressTextView, trainingDaysProgressTextView;
    private ProgressBar weightProgressBar, trainingDaysProgressBar;
    private Button saveGoalsButton; // Dodajemy referencję
    private View loadingOverlay; // Opcjonalnie, dla wskaźnika ładowania

    private ApiService apiService;

    // Zmienne do przechowywania danych z API
    private BigDecimal currentActualWeight = null; // Najnowsza waga z body_stat_history
    private BigDecimal goalTargetWeight = null;  // Cel wagowy z user_goals
    private BigDecimal goalStartWeight = null;   // Waga początkowa (zapisana przy ustawianiu celu) z user_goals
    private Integer currentActualTrainingDays = 0; // Zliczone z training_log
    private Integer goalTargetTrainingDays = 3;  // Cel dni z user_goals (domyślnie 3)

    private static final int DEFAULT_TARGET_TRAINING_DAYS = 3;
    private static final int REQUEST_CODE_UPDATE_MEASUREMENTS = 101; // Kod dla powrotu z UpdateMeasurementsActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_goals);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Błąd użytkownika. Zaloguj się ponownie.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        targetWeightEditText = findViewById(R.id.targetWeightEditText);
        targetTrainingDaysEditText = findViewById(R.id.targetTrainingDaysEditText);
        weightProgressTextView = findViewById(R.id.weightProgressTextView);
        trainingDaysProgressTextView = findViewById(R.id.trainingDaysProgressTextView);
        weightProgressBar = findViewById(R.id.weightProgressBar);
        trainingDaysProgressBar = findViewById(R.id.trainingDaysProgressBar);
        saveGoalsButton = findViewById(R.id.saveGoalsButton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton profileButton = findViewById(R.id.profileButton);
        loadingOverlay = findViewById(R.id.loadingOverlay); // Upewnij się, że masz to w XML lub usuń

        saveGoalsButton.setOnClickListener(v -> handleSaveGoals());

        menuButton.setOnClickListener(v -> {
            Intent intent = new Intent(TrainingGoalsActivity.this, AccountSettingsActivity.class);
            // Jeśli z AccountSettings można przejść do UpdateMeasurements, które zmienia wagę:
            startActivityForResult(intent, REQUEST_CODE_UPDATE_MEASUREMENTS);
        });
        homeButton.setOnClickListener(v -> startActivity(new Intent(this, TrainingMainActivity.class)));
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));

        // Początkowe załadowanie danych
        loadAllDataFromApi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_UPDATE_MEASUREMENTS && resultCode == RESULT_OK) {
            // Waga mogła się zmienić, więc odświeżamy wszystkie dane
            Log.d(TAG, "Returned from an activity, refreshing goals data.");
            loadAllDataFromApi();
        }
    }


    private void setLoadingState(boolean isLoading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        saveGoalsButton.setEnabled(!isLoading);
        targetWeightEditText.setEnabled(!isLoading);
        targetTrainingDaysEditText.setEnabled(!isLoading);
        if (isLoading) {
            weightProgressTextView.setText("Ładowanie...");
            trainingDaysProgressTextView.setText("Ładowanie...");
        }
    }

    private void loadAllDataFromApi() {
        setLoadingState(true);

        // Krok 1: Pobierz cele użytkownika (targetWeight, startWeight, targetTrainingDays)
        apiService.getUserGoals(userId).enqueue(new Callback<UserGoalDto>() {
            @Override
            public void onResponse(Call<UserGoalDto> call, Response<UserGoalDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserGoalDto goals = response.body();
                    goalTargetWeight = goals.getTargetWeight();
                    goalStartWeight = goals.getStartWeight(); // Waga zapisana przy ustawianiu celu
                    goalTargetTrainingDays = goals.getTargetTrainingDays() != null ? goals.getTargetTrainingDays() : DEFAULT_TARGET_TRAINING_DAYS;

                    if (goalTargetWeight != null) {
                        targetWeightEditText.setText(String.format(Locale.US, "%.1f", goalTargetWeight.doubleValue()));
                    } else {
                        targetWeightEditText.setText("");
                    }
                    targetTrainingDaysEditText.setText(String.valueOf(goalTargetTrainingDays));
                    Log.d(TAG, "Cele pobrane: TW=" + goalTargetWeight + ", SW=" + goalStartWeight + ", TTD=" + goalTargetTrainingDays);
                } else {
                    Log.e(TAG, "Błąd pobierania celów: " + response.code() + " - " + response.message());
                    // Ustaw domyślne wartości w UI, jeśli brak celów
                    goalTargetTrainingDays = DEFAULT_TARGET_TRAINING_DAYS;
                    targetTrainingDaysEditText.setText(String.valueOf(goalTargetTrainingDays));
                    targetWeightEditText.setText("");
                }
                fetchCurrentActualWeight(); // Następnie pobierz aktualną wagę
            }

            @Override
            public void onFailure(Call<UserGoalDto> call, Throwable t) {
                Log.e(TAG, "Failure pobierania celów: " + t.getMessage(), t);
                goalTargetTrainingDays = DEFAULT_TARGET_TRAINING_DAYS; // Fallback
                targetTrainingDaysEditText.setText(String.valueOf(goalTargetTrainingDays));
                targetWeightEditText.setText("");
                fetchCurrentActualWeight(); // Mimo wszystko spróbuj pobrać resztę
            }
        });
    }

    private void fetchCurrentActualWeight() {
        // Krok 2: Pobierz najnowszą wagę z body_stat_history
        apiService.getBodyStatHistory(userId).enqueue(new Callback<List<BodyStatHistoryDto>>() {
            @Override
            public void onResponse(Call<List<BodyStatHistoryDto>> call, Response<List<BodyStatHistoryDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Lista jest posortowana ASC przez backend, więc ostatni element jest najnowszy
                    currentActualWeight = response.body().get(response.body().size() - 1).getWeight();
                    Log.d(TAG, "Pobrana aktualna waga: " + currentActualWeight);
                } else {
                    currentActualWeight = null; // Brak historii wagi
                    Log.w(TAG, "Brak historii wagi lub błąd: " + response.code() + " - " + response.message());
                }
                updateWeightProgressUI(); // Zaktualizuj UI wagi (może być potrzebne, nawet jeśli reszta się ładuje)
                fetchCurrentActualTrainingDays(); // Następnie pobierz dni treningowe
            }

            @Override
            public void onFailure(Call<List<BodyStatHistoryDto>> call, Throwable t) {
                currentActualWeight = null;
                Log.e(TAG, "Failure pobierania historii wagi: " + t.getMessage(), t);
                updateWeightProgressUI();
                fetchCurrentActualTrainingDays();
            }
        });
    }

    private void fetchCurrentActualTrainingDays() {
        // Krok 3: Pobierz liczbę aktualnych aktywnych dni treningowych
        apiService.getActiveTrainingDaysInCurrentWeek(userId).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentActualTrainingDays = response.body();
                    Log.d(TAG, "Pobrane aktualne dni treningowe: " + currentActualTrainingDays);
                } else {
                    currentActualTrainingDays = 0; // Domyślnie 0, jeśli błąd
                    Log.e(TAG, "Błąd pobierania aktualnych dni treningowych: " + response.code() + " - " + response.message());
                }
                updateTrainingDaysProgressUI(); // Zaktualizuj UI dni treningowych
                setLoadingState(false); // Wszystkie dane załadowane
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                currentActualTrainingDays = 0;
                Log.e(TAG, "Failure pobierania aktualnych dni treningowych: " + t.getMessage(), t);
                updateTrainingDaysProgressUI();
                setLoadingState(false);
            }
        });
    }

    private void handleSaveGoals() {
        String targetWeightStr = targetWeightEditText.getText().toString().trim();
        String targetTrainingDaysStr = targetTrainingDaysEditText.getText().toString().trim();

        BigDecimal newTargetWeight = null;
        if (!targetWeightStr.isEmpty()) {
            try {
                newTargetWeight = new BigDecimal(targetWeightStr).setScale(1, RoundingMode.HALF_UP); // Ustawiamy skalę
                if (newTargetWeight.compareTo(BigDecimal.ZERO) <= 0) {
                    Toast.makeText(this, "Waga docelowa musi być większa od 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Nieprawidłowy format wagi docelowej", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer newTargetTrainingDays = null; // Wyślij null, jeśli pole jest puste
        if (!targetTrainingDaysStr.isEmpty()) {
            try {
                newTargetTrainingDays = Integer.parseInt(targetTrainingDaysStr);
                if (newTargetTrainingDays <= 0 || newTargetTrainingDays > 7) {
                    Toast.makeText(this, "Liczba dni treningowych musi być między 1 a 7", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Nieprawidłowy format liczby dni", Toast.LENGTH_SHORT).show();
                return;
            }
        }


        setLoadingState(true);
        UserGoalUpdateRequestDto request = new UserGoalUpdateRequestDto(newTargetWeight, newTargetTrainingDays);
        apiService.saveOrUpdateUserGoals(userId, request).enqueue(new Callback<UserGoalDto>() {
            @Override
            public void onResponse(Call<UserGoalDto> call, Response<UserGoalDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(TrainingGoalsActivity.this, "Cele zapisane", Toast.LENGTH_SHORT).show();
                    UserGoalDto updatedGoals = response.body();
                    // Zaktualizuj lokalne zmienne i UI
                    goalTargetWeight = updatedGoals.getTargetWeight();
                    goalStartWeight = updatedGoals.getStartWeight(); // Backend powinien zwrócić poprawny startWeight
                    goalTargetTrainingDays = updatedGoals.getTargetTrainingDays() != null ? updatedGoals.getTargetTrainingDays() : DEFAULT_TARGET_TRAINING_DAYS;

                    if (goalTargetWeight != null) {
                        targetWeightEditText.setText(String.format(Locale.US, "%.1f", goalTargetWeight.doubleValue()));
                    } else {
                        targetWeightEditText.setText("");
                    }
                    targetTrainingDaysEditText.setText(String.valueOf(goalTargetTrainingDays));

                    Log.d(TAG, "Cele zapisane i UI odświeżone: TW=" + goalTargetWeight + ", SW=" + goalStartWeight + ", TTD=" + goalTargetTrainingDays);
                    // Po zapisie celów, progres bary powinny się odświeżyć na podstawie nowych celów i aktualnych danych (które nie musiały się zmienić)
                    updateWeightProgressUI();
                    updateTrainingDaysProgressUI();
                } else {
                    Toast.makeText(TrainingGoalsActivity.this, "Błąd podczas zapisywania celów: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Błąd API przy zapisie celów: " + response.code() + " - " + response.message());
                }
                setLoadingState(false);
            }

            @Override
            public void onFailure(Call<UserGoalDto> call, Throwable t) {
                Toast.makeText(TrainingGoalsActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failure zapisu celów: " + t.getMessage(), t);
                setLoadingState(false);
            }
        });
    }

    private void updateWeightProgressUI() {
        if (currentActualWeight == null || goalTargetWeight == null || goalStartWeight == null) {
            weightProgressTextView.setText("Progres: -");
            weightProgressBar.setProgress(0);
            Log.d(TAG, "Nie można obliczyć progresu wagi: current=" + currentActualWeight + ", target=" + goalTargetWeight + ", start=" + goalStartWeight);
            return;
        }

        if (goalStartWeight.compareTo(goalTargetWeight) == 0) { // Waga początkowa = docelowa
            int progress = (currentActualWeight.compareTo(goalTargetWeight) >= 0) ? 100 : 0; // Jeśli osiągnięto lub przekroczono, to 100%
            if (goalTargetWeight.compareTo(goalStartWeight) > 0 && currentActualWeight.compareTo(goalStartWeight) < 0) progress = 0; // Cel to przytyć, a schudł
            if (goalTargetWeight.compareTo(goalStartWeight) < 0 && currentActualWeight.compareTo(goalStartWeight) > 0) progress = 0; // Cel to schudnąć, a przytył

            weightProgressBar.setProgress(progress);
            weightProgressTextView.setText(String.format(Locale.US, "Progres: %d%% (%.1fkg / %.1fkg)",
                    progress, currentActualWeight.doubleValue(), goalTargetWeight.doubleValue()));
            return;
        }

        BigDecimal progressFraction;
        // Cel: przybranie na wadze (target > start)
        if (goalTargetWeight.compareTo(goalStartWeight) > 0) {
            BigDecimal totalDiff = goalTargetWeight.subtract(goalStartWeight);
            BigDecimal currentDiff = currentActualWeight.subtract(goalStartWeight);
            if (totalDiff.compareTo(BigDecimal.ZERO) == 0) { // Unikaj dzielenia przez zero, jeśli start = target
                progressFraction = currentActualWeight.compareTo(goalTargetWeight) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            } else {
                progressFraction = currentDiff.divide(totalDiff, 4, RoundingMode.HALF_UP);
            }
        }
        // Cel: schudnięcie (target < start)
        else {
            BigDecimal totalDiff = goalStartWeight.subtract(goalTargetWeight);
            BigDecimal currentDiff = goalStartWeight.subtract(currentActualWeight);
            if (totalDiff.compareTo(BigDecimal.ZERO) == 0) {
                progressFraction = currentActualWeight.compareTo(goalTargetWeight) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            } else {
                progressFraction = currentDiff.divide(totalDiff, 4, RoundingMode.HALF_UP);
            }
        }

        int progressPercentage = progressFraction.multiply(new BigDecimal(100)).intValue();
        progressPercentage = Math.max(0, Math.min(100, progressPercentage)); // Ogranicz do 0-100

        weightProgressBar.setProgress(progressPercentage);
        weightProgressTextView.setText(String.format(Locale.US, "Progres: %d%% (%.1fkg / %.1fkg)",
                progressPercentage, currentActualWeight.doubleValue(), goalTargetWeight.doubleValue()));
        Log.d(TAG, "UI wagi zaktualizowane: " + progressPercentage + "%");
    }

    private void updateTrainingDaysProgressUI() {
        if (goalTargetTrainingDays == null || goalTargetTrainingDays == 0) {
            trainingDaysProgressTextView.setText("Progres: " + currentActualTrainingDays + "/-");
            trainingDaysProgressBar.setProgress(0);
            Log.d(TAG, "Nie można obliczyć progresu dni: targetDays=" + goalTargetTrainingDays);
            return;
        }

        int progressPercentage = (int) (((double) currentActualTrainingDays / goalTargetTrainingDays) * 100);
        progressPercentage = Math.max(0, Math.min(100, progressPercentage)); // Ogranicz do 0-100

        trainingDaysProgressBar.setProgress(progressPercentage);
        trainingDaysProgressTextView.setText(String.format(Locale.US, "Progres: %d/%d dni",
                currentActualTrainingDays, goalTargetTrainingDays));
        Log.d(TAG, "UI dni treningowych zaktualizowane: " + progressPercentage + "%");
    }
}