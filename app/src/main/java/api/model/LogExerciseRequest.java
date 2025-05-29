package api.model;

import java.util.List;

public class LogExerciseRequest {

    private String exerciseName;
    private List<LogSeriesRequest> series;

    public LogExerciseRequest(String exerciseName, List<LogSeriesRequest> series) {
        this.exerciseName = exerciseName;
        this.series = series;
    }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public List<LogSeriesRequest> getSeries() { return series; }
    public void setSeries(List<LogSeriesRequest> series) { this.series = series; }
}
