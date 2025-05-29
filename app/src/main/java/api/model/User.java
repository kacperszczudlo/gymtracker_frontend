package api.model;

public class User {
    private Integer id;
    private String username;
    private String surname;
    private String email;
    private String gender;
    private Integer height;

    // Gettery i settery
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}
