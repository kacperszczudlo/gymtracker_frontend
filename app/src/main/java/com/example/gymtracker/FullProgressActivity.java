package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.math.BigDecimal;
import java.text.ParseException; // DODAJ
import java.text.SimpleDateFormat; // DODAJ
import java.util.ArrayList;
import java.util.Collections; // DODAJ
import java.util.Comparator; // DODAJ
import java.util.List;
import java.util.Locale;
// import java.util.Date; // Już niepotrzebne, jeśli używamy SimpleDateFormat do formatowania Stringa z datą

import api.model.ApiClient;
import api.model.ApiService;
import api.model.BodyStatHistoryDto;
import api.model.ExerciseProgressDto; // DODAJ
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FullProgressActivity extends AppCompatActivity {

    private TextView armCircTextView, waistCircTextView, hipCircTextView;
    private LineChart weightChart;
    private LineChart benchChart, squatChart, deadliftChart;

    private int userId;
    private ApiService apiService;

    private BodyStatHistoryDto initialStatsApi = null;
    private List<BodyStatHistoryDto> historyStatsApi = null;

    // Nazwy ćwiczeń - upewnij się, że pasują do tych w bazie danych
    private final String EXERCISE_BENCH_PRESS = "Wyciskanie sztangi";
    private final String EXERCISE_SQUATS = "Przysiady";
    private final String EXERCISE_DEADLIFT = "Martwy ciag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_progress);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        Log.d("FullProgressActivity", "userId = " + userId);

        if (userId == -1) {
            Toast.makeText(this, "Błąd: użytkownik niezalogowany", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        armCircTextView = findViewById(R.id.armCircTextView);
        waistCircTextView = findViewById(R.id.waistCircTextView);
        hipCircTextView = findViewById(R.id.hipCircTextView);
        weightChart = findViewById(R.id.weightChart);

        benchChart = findViewById(R.id.benchChart);
        squatChart = findViewById(R.id.squatChart);
        deadliftChart = findViewById(R.id.deadliftChart);

        fetchApiDataAndUpdateUi(); // Pobiera dane wagowe i pomiarów
        // Pobieranie danych dla wykresów ćwiczeń
        fetchExerciseProgressData(benchChart, EXERCISE_BENCH_PRESS);
        fetchExerciseProgressData(squatChart, EXERCISE_SQUATS);
        fetchExerciseProgressData(deadliftChart, EXERCISE_DEADLIFT);

        findViewById(R.id.menuButton).setOnClickListener(v ->
                startActivity(new Intent(this, AccountSettingsActivity.class)));
        findViewById(R.id.homeButton).setOnClickListener(v ->
                startActivity(new Intent(this, TrainingMainActivity.class)));
        findViewById(R.id.profileButton).setOnClickListener(v ->
                startActivity(new Intent(this, UserProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchApiDataAndUpdateUi();
        fetchExerciseProgressData(benchChart, EXERCISE_BENCH_PRESS);
        fetchExerciseProgressData(squatChart, EXERCISE_SQUATS);
        fetchExerciseProgressData(deadliftChart, EXERCISE_DEADLIFT);
    }

    private void fetchApiDataAndUpdateUi() {
        initialStatsApi = null;
        historyStatsApi = null;

        armCircTextView.setText("Ładowanie...");
        waistCircTextView.setText("Ładowanie...");
        hipCircTextView.setText("Ładowanie...");
        weightChart.setNoDataText("Ładowanie danych wykresu wagi...");
        weightChart.invalidate();

        apiService.getInitialBodyStat(userId).enqueue(new Callback<BodyStatHistoryDto>() {
            @Override
            public void onResponse(Call<BodyStatHistoryDto> call, Response<BodyStatHistoryDto> responseInitial) {
                if (responseInitial.isSuccessful() && responseInitial.body() != null) {
                    initialStatsApi = responseInitial.body();
                } else {
                    initialStatsApi = null;
                }
                fetchHistoryStats();
            }

            @Override
            public void onFailure(Call<BodyStatHistoryDto> call, Throwable t) {
                initialStatsApi = null;
                fetchHistoryStats();
            }
        });
    }

    private void fetchHistoryStats() {
        apiService.getBodyStatHistory(userId).enqueue(new Callback<List<BodyStatHistoryDto>>() {
            @Override
            public void onResponse(Call<List<BodyStatHistoryDto>> call, Response<List<BodyStatHistoryDto>> responseHistory) {
                if (responseHistory.isSuccessful() && responseHistory.body() != null) {
                    historyStatsApi = responseHistory.body();
                } else {
                    historyStatsApi = null;
                }
                updateUiAfterApiFetch();
            }

            @Override
            public void onFailure(Call<List<BodyStatHistoryDto>> call, Throwable t) {
                historyStatsApi = null;
                updateUiAfterApiFetch();
            }
        });
    }

    private void updateUiAfterApiFetch() {
        showLatestMeasurementsWithApiData();
        drawWeightChartWithApiData();
    }

    private void fetchExerciseProgressData(final LineChart chart, final String exerciseName) {
        if (chart == null) return;
        chart.setNoDataText("Ładowanie danych dla " + exerciseName + "...");
        chart.setNoDataTextColor(getResources().getColor(android.R.color.white, getTheme()));
        chart.invalidate();

        apiService.getExerciseProgress(userId, exerciseName).enqueue(new Callback<List<ExerciseProgressDto>>() {
            @Override
            public void onResponse(Call<List<ExerciseProgressDto>> call, Response<List<ExerciseProgressDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseProgressDto> progressData = response.body();
                    Log.d("FullProgressActivity", "Pobrano dane progresu dla " + exerciseName + ": " + progressData.size() + " wpisów");
                    if (progressData.isEmpty()) {
                        setNoDataForExerciseChart(chart, exerciseName);
                    } else {
                        // Backend powinien zwracać posortowane po dacie ASC,
                        // ale na wszelki wypadek można posortować tutaj, jeśli daty są jako String 'yyyy-MM-dd'
                        Collections.sort(progressData, Comparator.comparing(ExerciseProgressDto::getDate));
                        setupExerciseChartWithApiData(chart, progressData, exerciseName);
                    }
                } else {
                    Log.e("FullProgressActivity", "Błąd pobierania progresu dla " + exerciseName + ": " + response.code() + " - " + response.message());
                    setNoDataForExerciseChart(chart, exerciseName);
                }
            }

            @Override
            public void onFailure(Call<List<ExerciseProgressDto>> call, Throwable t) {
                Log.e("FullProgressActivity", "Failure pobierania progresu dla " + exerciseName, t);
                setNoDataForExerciseChart(chart, exerciseName);
                // Sprawdź, czy aktywność nie jest w trakcie kończenia lub już zniszczona
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(FullProgressActivity.this, "Błąd sieci dla " + exerciseName, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupExerciseChartWithApiData(LineChart chart, List<ExerciseProgressDto> progressData, String exerciseName) {
        if (progressData == null || progressData.isEmpty()) {
            setNoDataForExerciseChart(chart, exerciseName);
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        final ArrayList<String> xLabels = new ArrayList<>(); // Do przechowywania dat jako etykiet

        for (int i = 0; i < progressData.size(); i++) {
            ExerciseProgressDto dto = progressData.get(i);
            if (dto.getMaxWeight() != null) {
                entries.add(new Entry(i, dto.getMaxWeight().floatValue()));
                xLabels.add(dto.getDate()); // Dodajemy string daty "yyyy-MM-dd"
            }
        }

        if (entries.isEmpty()) {
            setNoDataForExerciseChart(chart, exerciseName);
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, exerciseName + " (kg)");
        // Użyj predefiniowanego koloru lub zdefiniuj go w colors.xml
        int chartColor = R.color.chart_blue; // Przykładowy kolor
        if (exerciseName.equals(EXERCISE_BENCH_PRESS)) chartColor = R.color.chart_red; // Możesz zróżnicować kolory
        else if (exerciseName.equals(EXERCISE_SQUATS)) chartColor = R.color.chart_green_exercise;
        else if (exerciseName.equals(EXERCISE_DEADLIFT)) chartColor = R.color.chart_yellow;

        dataSet.setColor(getResources().getColor(chartColor, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(getResources().getColor(chartColor, getTheme()));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < xLabels.size()) {
                    try {
                        SimpleDateFormat backendFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM", Locale.US);
                        java.util.Date date = backendFormat.parse(xLabels.get(index));
                        return displayFormat.format(date);
                    } catch (ParseException e) {
                        // W razie błędu parsowania, spróbuj pokazać tylko część oryginalnego stringa
                        String originalDate = xLabels.get(index);
                        if (originalDate.length() >= 5) return originalDate.substring(5); // "MM-dd"
                        return originalDate;
                    }
                }
                return "";
            }
        });

        if (xLabels.size() > 6) { // Obracaj etykiety, jeśli jest ich więcej niż 6
            xAxis.setLabelRotationAngle(-45);
        } else {
            xAxis.setLabelRotationAngle(0);
        }
        // Ustaw maksymalną liczbę etykiet, aby uniknąć nakładania, ale wymuś ich wyświetlenie
        xAxis.setLabelCount(Math.min(xLabels.size(), 7), true);


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        leftAxis.setTextSize(14f);
        // leftAxis.setAxisMinimum(0f); // Opcjonalnie

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        chart.getLegend().setTextSize(12f);
        chart.animateX(1000);
        chart.invalidate();
        Log.d("FullProgressActivity", "Narysowano wykres dla " + exerciseName + " z " + entries.size() + " punktami.");
    }

    private void setNoDataForExerciseChart(LineChart chart, String exerciseName) {
        if (chart != null) {
            chart.setNoDataText("Brak danych dla " + exerciseName);
            chart.setNoDataTextColor(getResources().getColor(android.R.color.white, getTheme()));
            chart.clear(); // Wyczyść poprzednie dane, jeśli były
            chart.invalidate();
        }
    }

    private void showLatestMeasurementsWithApiData() {
        BodyStatHistoryDto firstStat = null;
        BodyStatHistoryDto lastStat = null;

        if (initialStatsApi != null) {
            firstStat = initialStatsApi;
        }

        if (historyStatsApi != null && !historyStatsApi.isEmpty()) {
            lastStat = historyStatsApi.get(historyStatsApi.size() - 1);
            if (firstStat == null && !historyStatsApi.isEmpty()) {
                firstStat = historyStatsApi.get(0);
            }
        }

        if (lastStat != null && lastStat.getArmCircumference() != null && firstStat != null && firstStat.getArmCircumference() != null) {
            BigDecimal armDiff = lastStat.getArmCircumference().subtract(firstStat.getArmCircumference());
            armCircTextView.setText(createSpannableWithProgress("Obwód ramienia: ", lastStat.getArmCircumference(), armDiff, "cm"));
        } else if (lastStat != null && lastStat.getArmCircumference() != null) {
            armCircTextView.setText(String.format(Locale.US, "Obwód ramienia: %.1f cm", lastStat.getArmCircumference().doubleValue()));
        } else {
            armCircTextView.setText("Obwód ramienia: Brak danych");
        }

        if (lastStat != null && lastStat.getWaistCircumference() != null && firstStat != null && firstStat.getWaistCircumference() != null) {
            BigDecimal waistDiff = lastStat.getWaistCircumference().subtract(firstStat.getWaistCircumference());
            waistCircTextView.setText(createSpannableWithProgress("Obwód talii: ", lastStat.getWaistCircumference(), waistDiff, "cm"));
        } else if (lastStat != null && lastStat.getWaistCircumference() != null) {
            waistCircTextView.setText(String.format(Locale.US, "Obwód talii: %.1f cm", lastStat.getWaistCircumference().doubleValue()));
        } else {
            waistCircTextView.setText("Obwód talii: Brak danych");
        }

        if (lastStat != null && lastStat.getHipCircumference() != null && firstStat != null && firstStat.getHipCircumference() != null) {
            BigDecimal hipDiff = lastStat.getHipCircumference().subtract(firstStat.getHipCircumference());
            hipCircTextView.setText(createSpannableWithProgress("Obwód bioder: ", lastStat.getHipCircumference(), hipDiff, "cm"));
        } else if (lastStat != null && lastStat.getHipCircumference() != null) {
            hipCircTextView.setText(String.format(Locale.US, "Obwód bioder: %.1f cm", lastStat.getHipCircumference().doubleValue()));
        } else {
            hipCircTextView.setText("Obwód bioder: Brak danych");
        }
    }

    private SpannableString createSpannableWithProgress(String label, BigDecimal value, BigDecimal diff, String unit) {
        String baseText = String.format(Locale.US, "%s%.1f %s", label, value.doubleValue(), unit);
        if (diff == null || diff.compareTo(BigDecimal.ZERO) == 0) {
            return new SpannableString(baseText);
        }

        String progress = String.format(Locale.US, " (%s%.1f %s)", diff.compareTo(BigDecimal.ZERO) > 0 ? "+" : "-", diff.abs().doubleValue(), unit);
        SpannableString spannable = new SpannableString(baseText + progress);

        int start = baseText.length();
        int end = baseText.length() + progress.length();

        int colorResId = diff.compareTo(BigDecimal.ZERO) >= 0 ? R.color.green : R.color.red;
        spannable.setSpan(
                new ForegroundColorSpan(getResources().getColor(colorResId, getTheme())),
                start, end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return spannable;
    }

    private void drawWeightChartWithApiData() {
        if (historyStatsApi == null || historyStatsApi.isEmpty()) {
            weightChart.setNoDataText("Brak danych do wykresu wagi");
            weightChart.setNoDataTextColor(getResources().getColor(android.R.color.white, getTheme()));
            weightChart.clear();
            weightChart.invalidate();
            Log.d("FullProgressActivity", "Brak danych do wykresu wagi.");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        final List<String> weightXLabels = new ArrayList<>();

        for (int i = 0; i < historyStatsApi.size(); i++) {
            BodyStatHistoryDto stat = historyStatsApi.get(i);
            if (stat.getWeight() != null) {
                entries.add(new Entry(i, stat.getWeight().floatValue()));
                weightXLabels.add(stat.getDate()); // Zakładając, że stat.getDate() to String "yyyy-MM-dd"
            }
        }

        if (entries.isEmpty()) {
            weightChart.setNoDataText("Brak danych o wadze w historii");
            weightChart.setNoDataTextColor(getResources().getColor(android.R.color.white, getTheme()));
            weightChart.clear();
            weightChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Waga (kg)");
        dataSet.setColor(getResources().getColor(R.color.green, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(getResources().getColor(R.color.green, getTheme()));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        weightChart.setData(lineData);

        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < weightXLabels.size()) {
                    try {
                        SimpleDateFormat backendFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM", Locale.US);
                        java.util.Date date = backendFormat.parse(weightXLabels.get(index));
                        return displayFormat.format(date);
                    } catch (ParseException e) {
                        String originalDate = weightXLabels.get(index);
                        if (originalDate.length() >= 5) return originalDate.substring(5);
                        return originalDate;
                    }
                }
                return "";
            }
        });
        if (weightXLabels.size() > 6) {
            xAxis.setLabelRotationAngle(-45);
        } else {
            xAxis.setLabelRotationAngle(0);
        }
        xAxis.setLabelCount(Math.min(weightXLabels.size(), 7), true);


        YAxis leftAxis = weightChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        leftAxis.setTextSize(14f);

        YAxis rightAxis = weightChart.getAxisRight();
        rightAxis.setEnabled(false);

        weightChart.getDescription().setEnabled(false);
        weightChart.getLegend().setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        weightChart.getLegend().setTextSize(12f);
        weightChart.animateX(1000);
        weightChart.invalidate();
        Log.d("FullProgressActivity", "Narysowano wykres wagi z " + entries.size() + " punktami.");
    }
}