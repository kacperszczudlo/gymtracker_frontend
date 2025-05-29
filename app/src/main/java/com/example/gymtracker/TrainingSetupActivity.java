package com.example.gymtracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

import api.model.ApiClient;
import api.model.ApiService;
import api.model.ExerciseRequest;
import api.model.TrainingLogRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrainingSetupActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private ArrayList<Exercise> exerciseList;
    private String dayName;
    private int userId;
    private boolean isLogEdit;
    private String logDate;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_setup);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        isLogEdit = "EDIT_LOG_ENTRIES".equals(getIntent().getStringExtra("MODE"));
        logDate = getIntent().getStringExtra("DATE");

        recyclerView = findViewById(R.id.exerciseRecyclerView);
        Button addExerciseButton = findViewById(R.id.addExerciseButton);
        Button nextButton = findViewById(R.id.nextButton);
        TextView trainingTitle = findViewById(R.id.trainingTitleTextView);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        dayName = getIntent().getStringExtra("DAY_NAME");
        trainingTitle.setText("Trening - " + dayName);

        exerciseList = new ArrayList<>();
        if (getIntent().hasExtra("EXERCISE_LIST")) {
            exerciseList = getIntent().getParcelableArrayListExtra("EXERCISE_LIST");
        } else if (isLogEdit) {
            loadExercisesFromBackend();
        }

        adapter = new ExerciseAdapter(
                exerciseList,
                this::removeExercise,
                true,
                (dayId, exerciseName, seriesPosition) -> {
                    Exercise exercise = findExerciseByName(exerciseName);
                    if (exercise != null && seriesPosition >= 0 && seriesPosition < exercise.getSeriesList().size()) {
                        exercise.getSeriesList().remove(seriesPosition);
                        adapter.notifyDataSetChanged();
                    }
                },
                -1,
                isLogEdit
        );




        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addExerciseButton.setOnClickListener(v -> showExerciseDialog());

        nextButton.setOnClickListener(v -> saveTrainingLog());
    }

    private Exercise findExerciseByName(String name) {
        for (Exercise e : exerciseList) {
            if (e.getName().equals(name)) return e;
        }
        return null;
    }


    private void loadExercisesFromBackend() {
        apiService.getTrainingLog(userId, logDate, dayName).enqueue(new Callback<api.model.TrainingLog>() {
            @Override
            public void onResponse(Call<api.model.TrainingLog> call, Response<api.model.TrainingLog> response) {
                if (response.isSuccessful() && response.body() != null) {
                    exerciseList.clear();
                    // Map from response.body() to exerciseList
                    for (api.model.LogExercise logExercise : response.body().getExercises()) {
                        Exercise exercise = new Exercise(logExercise.getExerciseName());
                        for (api.model.LogSeries logSeries : logExercise.getSeriesList()) {
                            int reps = logSeries.getReps();
                            float weight = logSeries.getWeight() != null ? logSeries.getWeight().floatValue() : 0f;
                            exercise.addSeries(new Series(reps, weight));
                        }
                        exerciseList.add(exercise);
                    }

                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(TrainingSetupActivity.this, "Błąd wczytywania ćwiczeń", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<api.model.TrainingLog> call, Throwable t) {
                Toast.makeText(TrainingSetupActivity.this, "Błąd połączenia", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTrainingLog() {
        if (exerciseList.isEmpty()) {
            // Usuń log, jeśli nie ma ćwiczeń
            apiService.getTrainingLog(userId, logDate, dayName).enqueue(new Callback<api.model.TrainingLog>() {
                @Override
                public void onResponse(Call<api.model.TrainingLog> call, Response<api.model.TrainingLog> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        int logId = response.body().getId();
                        apiService.deleteTrainingLog(logId).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                Toast.makeText(TrainingSetupActivity.this, "Pusty trening usunięty!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(TrainingSetupActivity.this, "Błąd połączenia", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Brak logu – po prostu zamknij aktywność
                        setResult(RESULT_OK);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<api.model.TrainingLog> call, Throwable t) {
                    Toast.makeText(TrainingSetupActivity.this, "Błąd połączenia", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Normalne zapisywanie, jeśli są ćwiczenia
        ArrayList<api.model.LogExerciseRequest> exerciseRequests = new ArrayList<>();
        for (Exercise exercise : exerciseList) {
            ArrayList<api.model.LogSeriesRequest> seriesRequests = new ArrayList<>();
            for (Series s : exercise.getSeriesList()) {
                api.model.LogSeriesRequest seriesRequest = new api.model.LogSeriesRequest(
                        s.getReps(),
                        (double) s.getWeight()
                );
                seriesRequests.add(seriesRequest);
            }

            api.model.LogExerciseRequest exReq = new api.model.LogExerciseRequest(exercise.getName(), seriesRequests);
            exerciseRequests.add(exReq);
        }

        TrainingLogRequest request = new TrainingLogRequest(userId, logDate, dayName, exerciseRequests);

        apiService.saveTrainingLog(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TrainingSetupActivity.this, "Trening zapisany pomyślnie!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(TrainingSetupActivity.this, "Błąd zapisu treningu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(TrainingSetupActivity.this, "Błąd połączenia", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showExerciseDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        dialog.setContentView(R.layout.dialog_exercise_list);

        RecyclerView dialogRecyclerView = dialog.findViewById(R.id.dialogExerciseRecyclerView);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);

        apiService.getExercises().enqueue(new Callback<java.util.List<api.model.ExerciseDto>>() {
            @Override
            public void onResponse(Call<java.util.List<api.model.ExerciseDto>> call, Response<java.util.List<api.model.ExerciseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<String> exerciseNames = new ArrayList<>();
                    for (api.model.ExerciseDto dto : response.body()) {
                        exerciseNames.add(dto.getName());
                    }
                    ExerciseDialogAdapter dialogAdapter = new ExerciseDialogAdapter(new ArrayList<>(exerciseNames), exerciseName -> {
                        exerciseList.add(new Exercise(exerciseName));
                        adapter.notifyDataSetChanged();
                        dialog.dismiss();
                    });

                    if (dialogRecyclerView != null) {
                        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(TrainingSetupActivity.this));
                        dialogRecyclerView.setAdapter(dialogAdapter);
                    }
                } else {
                    Toast.makeText(TrainingSetupActivity.this, "Błąd wczytywania ćwiczeń", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<api.model.ExerciseDto>> call, Throwable t) {
                Toast.makeText(TrainingSetupActivity.this, "Błąd połączenia", Toast.LENGTH_SHORT).show();
            }
        });

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dialog.dismiss());
        }

        android.view.View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        dialog.show();
    }

    private void removeExercise(int position) {
        exerciseList.remove(position);
        adapter.notifyItemRemoved(position);
    }
}
