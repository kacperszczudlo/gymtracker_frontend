package api.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class ExerciseExtremesDto {
    @SerializedName("initialWeight")
    private BigDecimal initialWeight;

    @SerializedName("latestWeight")
    private BigDecimal latestWeight;

    // Gettery i Settery
    public BigDecimal getInitialWeight() { return initialWeight; }
    public void setInitialWeight(BigDecimal initialWeight) { this.initialWeight = initialWeight; }
    public BigDecimal getLatestWeight() { return latestWeight; }
    public void setLatestWeight(BigDecimal latestWeight) { this.latestWeight = latestWeight; }
}