package api.model;

import com.google.gson.annotations.SerializedName; // Upewnij się, że używasz tej adnotacji, jeśli nazwy pól w JSON są inne
import java.math.BigDecimal; // Możesz używać Double, jak w Twoim obecnym kodzie

public class UpdateUserProfileRequest {
    // Nowe pola dla aktualizacji danych User
    @SerializedName("username") // Opcjonalne, jeśli nazwy w JSON mają być inne
    private String username;
    @SerializedName("surname")
    private String surname;
    @SerializedName("email")
    private String email;
    @SerializedName("newPassword")
    private String newPassword; // Dla nowego hasła

    // Istniejące pola dla profilu (używam Double jak w Twoim oryginalnym kodzie Android)
    @SerializedName("gender")
    private String gender;
    @SerializedName("height")
    private Integer height;
    @SerializedName("weight")
    private Double weight;
    @SerializedName("waistCircumference")
    private Double waistCircumference;
    @SerializedName("armCircumference")
    private Double armCircumference;
    @SerializedName("hipCircumference")
    private Double hipCircumference;

    // Konstruktor dla aktualizacji tylko danych User (username, surname, email, newPassword)
    public UpdateUserProfileRequest(String username, String surname, String email, String newPassword) {
        this.username = username;
        this.surname = surname;
        this.email = email;
        this.newPassword = newPassword;
        // Pozostałe pola (profilowe) będą null i backend ich nie zaktualizuje, jeśli tak jest obsłużony
    }

    // Konstruktor dla aktualizacji tylko danych Profile (jak w UpdateMeasurementsActivity)
    public UpdateUserProfileRequest(String gender, Integer height, Double weight,
                                    Double waistCircumference, Double armCircumference, Double hipCircumference) {
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.waistCircumference = waistCircumference;
        this.armCircumference = armCircumference;
        this.hipCircumference = hipCircumference;
        // Pozostałe pola (user) będą null
    }

    // Konstruktor pełny, jeśli chcesz aktualizować wszystko naraz (opcjonalny)
    public UpdateUserProfileRequest(String username, String surname, String email, String newPassword,
                                    String gender, Integer height, Double weight,
                                    Double waistCircumference, Double armCircumference, Double hipCircumference) {
        this.username = username;
        this.surname = surname;
        this.email = email;
        this.newPassword = newPassword;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.waistCircumference = waistCircumference;
        this.armCircumference = armCircumference;
        this.hipCircumference = hipCircumference;
    }


    // Gettery i Settery dla wszystkich pól
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Double getWaistCircumference() { return waistCircumference; }
    public void setWaistCircumference(Double waistCircumference) { this.waistCircumference = waistCircumference; }
    public Double getArmCircumference() { return armCircumference; }
    public void setArmCircumference(Double armCircumference) { this.armCircumference = armCircumference; }
    public Double getHipCircumference() { return hipCircumference; }
    public void setHipCircumference(Double hipCircumference) { this.hipCircumference = hipCircumference; }
}