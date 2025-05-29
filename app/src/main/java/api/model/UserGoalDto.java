package api.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class UserGoalDto {
    @SerializedName("targetWeight")
    private BigDecimal targetWeight;
    @SerializedName("startWeight")
    private BigDecimal startWeight;
    @SerializedName("targetTrainingDays")
    private Integer targetTrainingDays;

    // Gettery i Settery
    public BigDecimal getTargetWeight() { return targetWeight; }
    public void setTargetWeight(BigDecimal targetWeight) { this.targetWeight = targetWeight; }
    public BigDecimal getStartWeight() { return startWeight; }
    public void setStartWeight(BigDecimal startWeight) { this.startWeight = startWeight; }
    public Integer getTargetTrainingDays() { return targetTrainingDays; }
    public void setTargetTrainingDays(Integer targetTrainingDays) { this.targetTrainingDays = targetTrainingDays; }
}