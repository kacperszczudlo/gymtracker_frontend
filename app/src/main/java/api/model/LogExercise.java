package api.model;

import java.util.List;

public class LogExercise {

    private Integer id;
    private String exerciseName;
    private List<LogSeries> seriesList;

    public LogExercise() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public List<LogSeries> getSeriesList() { return seriesList; }
    public void setSeriesList(List<LogSeries> seriesList) { this.seriesList = seriesList; }
}
