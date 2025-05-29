package api.model;

public class ExerciseDto {
    private Integer id;
    private String name;

    // Konstruktor bezparametrowy
    public ExerciseDto() {}

    // Konstruktor z nazwą
    public ExerciseDto(String name) {
        this.name = name;
    }

    // Gettery i settery
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
