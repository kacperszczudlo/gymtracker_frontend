package api.model;

// ... inne importy
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import api.model.ExerciseProgressDto; // DODAJ TEN IMPORT

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ... (istniejące metody)

    @POST("auth/register")
    Call<User> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<UserProfileResponse> login(@Body LoginRequest request);

    @PUT("auth/users/{id}")
    Call<Void> updateUserProfile(@Path("id") int userId, @Body UpdateUserProfileRequest request);

    @GET("exercises")
    Call<List<ExerciseDto>> getExercises();

    @POST("exercises")
    Call<com.example.gymtracker.Exercise> addExercise(@Body ExerciseRequest request); // Zmieniono typ odpowiedzi na frontendowy Exercise

    @PUT("exercises/{id}")
    Call<com.example.gymtracker.Exercise> updateExercise(@Path("id") int id, @Body ExerciseRequest request); // Zmieniono typ odpowiedzi

    @DELETE("exercises/{id}")
    Call<Void> deleteExercise(@Path("id") int id);

    @POST("training-logs")
    Call<Void> saveTrainingLog(@Body TrainingLogRequest request);

    @GET("training-logs")
    Call<TrainingLog> getTrainingLog(@Query("userId") int userId, @Query("date") String date, @Query("dayName") String dayName);

    @DELETE("training-logs/{id}")
    Call<Void> deleteTrainingLog(@Path("id") int id);

    @DELETE("log-exercises/{id}")
    Call<Void> deleteLogExercise(@Path("id") int id);

    @GET("training-logs")
    Call<ResponseBody> getTrainingLogRaw(@Query("userId") int userId, @Query("date") String date);

    @GET("users/{userId}/body-stats")
    Call<List<BodyStatHistoryDto>> getBodyStatHistory(@Path("userId") int userId);

    @GET("users/{userId}/body-stats/initial")
    Call<BodyStatHistoryDto> getInitialBodyStat(@Path("userId") int userId);

    @GET("users/{userId}/achievements/max-weights")
    Call<Map<String, BigDecimal>> getMaxWeightsForAchievements(
            @Path("userId") int userId,
            @Query("exerciseNames") List<String> exerciseNames
    );

    // Nowa metoda do pobierania progresu ćwiczenia
    @GET("users/{userId}/achievements/exercise-progress/{exerciseName}")
    Call<List<ExerciseProgressDto>> getExerciseProgress(
            @Path("userId") int userId,
            @Path("exerciseName") String exerciseName
    );

    @GET("users/{userId}/goals")
    Call<UserGoalDto> getUserGoals(@Path("userId") int userId);

    @PUT("users/{userId}/goals")
    Call<UserGoalDto> saveOrUpdateUserGoals(@Path("userId") int userId, @Body UserGoalUpdateRequestDto request);

    @GET("users/{userId}/training-logs/active-days-current-week")
    Call<Integer> getActiveTrainingDaysInCurrentWeek(@Path("userId") int userId);

    // DODAJ NOWĄ METODĘ:
    @GET("users/{userId}/achievements/exercise-extremes/{exerciseName}")
    Call<ExerciseExtremesDto> getExerciseExtremes(
            @Path("userId") int userId,
            @Path("exerciseName") String exerciseName
    );
}