package api.model;

public class ExerciseRequest {

    private String name;

    public ExerciseRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
