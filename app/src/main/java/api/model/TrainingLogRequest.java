package api.model;

import java.util.List;

public class TrainingLogRequest {

    private Integer userId;
    private String date;
    private String dayName;
    private List<LogExerciseRequest> exercises;

    public TrainingLogRequest(Integer userId, String date, String dayName, List<LogExerciseRequest> exercises) {
        this.userId = userId;
        this.date = date;
        this.dayName = dayName;
        this.exercises = exercises;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public List<LogExerciseRequest> getExercises() { return exercises; }
    public void setExercises(List<LogExerciseRequest> exercises) { this.exercises = exercises; }
}
