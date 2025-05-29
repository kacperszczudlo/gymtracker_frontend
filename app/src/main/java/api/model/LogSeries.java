package api.model;

public class LogSeries {

    private Integer id;
    private Integer reps;
    private Double weight;

    public LogSeries() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
