package api.model; // Upewnij się, że pakiet jest poprawny

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal; // Możesz użyć BigDecimal, jeśli chcesz precyzji

public class BodyStatHistoryDto {
    @SerializedName("id")
    private Integer id;

    @SerializedName("date")
    private String date; // Backend wysyła java.sql.Date, Gson powinien sparsować to do Stringa.
    // Można też użyć `java.util.Date` i skonfigurować Gson z formatem daty.

    @SerializedName("weight")
    private BigDecimal weight; // Lub Double

    @SerializedName("armCircumference")
    private BigDecimal armCircumference; // Lub Double

    @SerializedName("waistCircumference")
    private BigDecimal waistCircumference; // Lub Double

    @SerializedName("hipCircumference")
    private BigDecimal hipCircumference; // Lub Double

    // Gettery i Settery
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getArmCircumference() { return armCircumference; }
    public void setArmCircumference(BigDecimal armCircumference) { this.armCircumference = armCircumference; }
    public BigDecimal getWaistCircumference() { return waistCircumference; }
    public void setWaistCircumference(BigDecimal waistCircumference) { this.waistCircumference = waistCircumference; }
    public BigDecimal getHipCircumference() { return hipCircumference; }
    public void setHipCircumference(BigDecimal hipCircumference) { this.hipCircumference = hipCircumference; }
}