package api.model;

import java.util.List;

public class TrainingLog {

    private Integer id;
    private String date;
    private String dayName;
    private User user;
    private List<LogExercise> exercises;

    public TrainingLog() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<LogExercise> getExercises() { return exercises; }
    public void setExercises(List<LogExercise> exercises) { this.exercises = exercises; }
}
