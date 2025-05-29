package com.example.gymtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import api.model.ApiClient;
import api.model.ApiService;
import api.model.LogExercise;
import api.model.LogExerciseRequest;
import api.model.LogSeries;
import api.model.LogSeriesRequest;
import api.model.TrainingLog;
import api.model.TrainingLogRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingMainActivity extends AppCompatActivity {
    private RecyclerView exerciseRecyclerView;
    private ExerciseAdapter exerciseAdapter;
    private ArrayList<Exercise> exerciseList;

    private ApiService apiService;
    private int logId = -1;

    private RecyclerView weekDaysRecyclerView;
    private WeekDaysAdapter weekDaysAdapter;
    private String selectedDayName;
    private int currentDayOfWeekIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

    private ImageButton prevWeekButton;
    private TextView dateTextView;
    private ImageButton nextWeekButton;
    private Calendar currentDisplayCalendar;
    private SimpleDateFormat dateFormatForTextView;
    private SimpleDateFormat dateFormatForDb;
    private String currentSelectedDateString;

    private TextView timerTextView;
    private Button timerToggleButton;
    private CountDownTimer timer;
    private boolean isRunning = false;
    private long timeLeftInMillis = 60 * 1000;
    private final long startTimeInMillis = 60 * 1000;

    private static final int REQUEST_CODE_EDIT_EXERCISES = 2;
    private HashMap<String, Integer> dayNameToCalendarField;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_main);

        initializeDayNameMapping();

        apiService = ApiClient.getClient(this).create(ApiService.class);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        dateFormatForTextView = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        dateFormatForDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        currentDisplayCalendar = Calendar.getInstance();

        if (userId == -1) {
            Toast.makeText(this, "Błąd użytkownika. Spróbuj ponownie się zalogować.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView);
        exerciseList = new ArrayList<>();
        exerciseAdapter = new ExerciseAdapter(
                exerciseList,
                null,
                false,
                (dayId, exerciseName, seriesPosition) ->
                        Log.d("TrainingMain", "Usunięto serię: " + exerciseName + " (pozycja: " + seriesPosition + ")"),
                -1L,
                false
        );
        exerciseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        exerciseRecyclerView.setAdapter(exerciseAdapter);

        initRestTimer();
        initDateNavigation();

        weekDaysRecyclerView = findViewById(R.id.weekDaysRecyclerView);
        weekDaysAdapter = new WeekDaysAdapter(dayName -> {
            this.selectedDayName = dayName;
            Integer calendarDayConstant = dayNameToCalendarField.get(dayName);
            if (calendarDayConstant != null) {
                this.currentDayOfWeekIndex = calendarDayConstant;
                updateCalendarToSelectedDay();
                updateDateTextView();
                this.currentSelectedDateString = dateFormatForDb.format(currentDisplayCalendar.getTime());
                loadExercisesForDay();
                weekDaysAdapter.setSelectedUserDay(this.selectedDayName);
            }
        });
        weekDaysRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        weekDaysRecyclerView.setAdapter(weekDaysAdapter);

        setInitialDayAndView();

        Button editButton = findViewById(R.id.editButton);
        editButton.setOnClickListener(v -> {
            if (selectedDayName == null || selectedDayName.isEmpty()) {
                Toast.makeText(this, "Proszę najpierw wybrać dzień.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, TrainingSetupActivity.class);
            intent.putExtra("DAY_NAME", selectedDayName);
            intent.putParcelableArrayListExtra("EXERCISE_LIST", new ArrayList<>(exerciseList));
            intent.putExtra("MODE", "EDIT_LOG_ENTRIES");
            intent.putExtra("DATE", currentSelectedDateString);
            startActivityForResult(intent, REQUEST_CODE_EDIT_EXERCISES);
        });

        findViewById(R.id.menuButton).setOnClickListener(v -> startActivity(new Intent(this, AccountSettingsActivity.class)));
        findViewById(R.id.profileButton).setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));
        findViewById(R.id.homeButton).setOnClickListener(v -> Toast.makeText(this, "Jesteś już na stronie głównej", Toast.LENGTH_SHORT).show());

        findViewById(R.id.saveTrainingButton).setOnClickListener(v -> saveTrainingLogToBackend());
    }

    private void initializeDayNameMapping() {
        dayNameToCalendarField = new HashMap<>();
        String[] fullDayNames = WeekDaysAdapter.FULL_DAYS;
        int[] calendarFields = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
        for (int i = 0; i < fullDayNames.length; i++) {
            dayNameToCalendarField.put(fullDayNames[i], calendarFields[i]);
        }
    }

    private void setInitialDayAndView() {
        setStartOfWeek();

        int calendarDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int ourDayIndex = WeekDaysAdapter.getOurIndexFromCalendarField(calendarDayOfWeek);
        if (ourDayIndex < 0) ourDayIndex = 0;

        selectedDayName = weekDaysAdapter.getFullDayName(ourDayIndex);
        currentDayOfWeekIndex = dayNameToCalendarField.get(selectedDayName);
        updateCalendarToSelectedDay();
        currentSelectedDateString = dateFormatForDb.format(currentDisplayCalendar.getTime());
        updateDateTextView();

        int initialPosition = Integer.MAX_VALUE / 2 - (Integer.MAX_VALUE / 2 % 7) + ourDayIndex;
        weekDaysRecyclerView.scrollToPosition(initialPosition);
        weekDaysAdapter.setSelectedUserDay(selectedDayName);

        loadExercisesForDay();
    }

    private void updateDateTextView() {
        dateTextView = findViewById(R.id.dateTextView);
        if (dateTextView != null && currentDisplayCalendar != null) {
            dateTextView.setText(dateFormatForTextView.format(currentDisplayCalendar.getTime()));
        }
    }

    private void initDateNavigation() {
        prevWeekButton = findViewById(R.id.prevWeekButton);
        nextWeekButton = findViewById(R.id.nextWeekButton);

        updateDateTextView();

        prevWeekButton.setOnClickListener(v -> {
            currentDisplayCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            updateCalendarToSelectedDay();
            updateDateTextView();
            currentSelectedDateString = dateFormatForDb.format(currentDisplayCalendar.getTime());
            loadExercisesForDay();
        });

        nextWeekButton.setOnClickListener(v -> {
            currentDisplayCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            updateCalendarToSelectedDay();
            updateDateTextView();
            currentSelectedDateString = dateFormatForDb.format(currentDisplayCalendar.getTime());
            loadExercisesForDay();
        });
    }

    private void updateCalendarToSelectedDay() {
        if (currentDisplayCalendar != null && currentDayOfWeekIndex != -1) {
            currentDisplayCalendar.set(Calendar.DAY_OF_WEEK, currentDayOfWeekIndex);
            currentSelectedDateString = dateFormatForDb.format(currentDisplayCalendar.getTime());
            Log.d("DEBUG", "Nowa data: " + currentSelectedDateString + ", dzień: " + selectedDayName);
        }
    }

    private void loadExercisesForDay() {
        Log.d("TrainingMain", "Pobieranie ćwiczeń dla daty: " + currentSelectedDateString + ", dzień: " + selectedDayName);
        apiService.getTrainingLog(userId, currentSelectedDateString, selectedDayName).enqueue(new Callback<TrainingLog>() {
            @Override
            public void onResponse(Call<TrainingLog> call, Response<TrainingLog> response) {
                // 🟢 ZAWSZE czyścimy listę na początku!
                exerciseList.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (LogExercise logExercise : response.body().getExercises()) {
                        Exercise ex = new Exercise(logExercise.getExerciseName(), new ArrayList<>());
                        for (LogSeries s : logExercise.getSeriesList()) {
                            ex.addSeries(new Series(s.getReps(), s.getWeight().floatValue()));
                        }
                        exerciseList.add(ex);
                    }
                    Log.d("DEBUG", "Wczytano ćwiczenia z backendu");
                } else {
                    Log.d("DEBUG", "Brak logu dla tego dnia — pusty dzień");
                }
                exerciseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<TrainingLog> call, Throwable t) {
                // Toast.makeText(TrainingMainActivity.this, "Błąd: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // 🟢 W razie błędu też wyczyść listę
                exerciseList.clear();
                exerciseAdapter.notifyDataSetChanged();
            }
        });
    }



    private void saveTrainingLogToBackend() {
        if (selectedDayName == null || selectedDayName.isEmpty()) {
            Toast.makeText(this, "Wybierz dzień treningowy.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<LogExerciseRequest> logExercises = new ArrayList<>();
        for (Exercise ex : exerciseList) {
            List<LogSeriesRequest> seriesList = new ArrayList<>();
            for (Series s : ex.getSeriesList()) {
                seriesList.add(new LogSeriesRequest(s.getReps(), (double) s.getWeight()));
            }
            logExercises.add(new LogExerciseRequest(ex.getName(), seriesList));
        }

        TrainingLogRequest request = new TrainingLogRequest(userId, currentSelectedDateString, selectedDayName, logExercises);

        apiService.saveTrainingLog(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                String msg = response.isSuccessful() ? "Trening zapisany pomyślnie!" : "Błąd zapisu!";
                Toast.makeText(TrainingMainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TrainingMainActivity.this, "Błąd: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_EXERCISES && resultCode == RESULT_OK && selectedDayName != null) {
            loadExercisesForDay();
        }
    }

    private void initRestTimer() {
        timerTextView = findViewById(R.id.timerTextView);
        timerToggleButton = findViewById(R.id.timerToggleButton);
        updateTimerText();
        timerToggleButton.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
    }

    private void startTimer() {
        timer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timerToggleButton.setText("Start");
                if (!isFinishing()) {
                    timerToggleButton.setBackgroundTintList(ContextCompat.getColorStateList(TrainingMainActivity.this, R.color.green));
                }
                timeLeftInMillis = startTimeInMillis;
                updateTimerText();
                Toast.makeText(TrainingMainActivity.this, "Koniec przerwy!", Toast.LENGTH_SHORT).show();
            }
        }.start();
        isRunning = true;
        timerToggleButton.setText("Stop");
        timerToggleButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_light));
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        isRunning = false;
        timerToggleButton.setText("Start");
        timerToggleButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void setStartOfWeek() {
        currentDisplayCalendar.setFirstDayOfWeek(Calendar.MONDAY);
        currentDisplayCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    }
}
