package api.model;

public class RegisterRequest {
    private String username;
    private String surname;
    private String email;
    private String password;
    private String gender;
    private Integer height;

    private Double weight;
    private Double waistCircumference;
    private Double armCircumference;
    private Double hipCircumference;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String surname, String email, String password,
                           String gender, Integer height,
                           Double weight, Double waistCircumference,
                           Double armCircumference, Double hipCircumference) {
        this.username = username;
        this.surname = surname;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.waistCircumference = waistCircumference;
        this.armCircumference = armCircumference;
        this.hipCircumference = hipCircumference;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getWaistCircumference() {
        return waistCircumference;
    }

    public void setWaistCircumference(Double waistCircumference) {
        this.waistCircumference = waistCircumference;
    }

    public Double getArmCircumference() {
        return armCircumference;
    }

    public void setArmCircumference(Double armCircumference) {
        this.armCircumference = armCircumference;
    }

    public Double getHipCircumference() {
        return hipCircumference;
    }

    public void setHipCircumference(Double hipCircumference) {
        this.hipCircumference = hipCircumference;
    }
}
