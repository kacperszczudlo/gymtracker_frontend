package api.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class ExerciseProgressDto {
    @SerializedName("date")
    private String date; // Backend (java.sql.Date) będzie serializowany do String "yyyy-MM-dd"

    @SerializedName("maxWeight")
    private BigDecimal maxWeight;

    // Gettery i Settery
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getMaxWeight() { return maxWeight; }
    public void setMaxWeight(BigDecimal maxWeight) { this.maxWeight = maxWeight; }
}