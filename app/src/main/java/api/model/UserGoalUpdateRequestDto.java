package api.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class UserGoalUpdateRequestDto {
    @SerializedName("targetWeight")
    private BigDecimal targetWeight;
    @SerializedName("targetTrainingDays")
    private Integer targetTrainingDays;

    public UserGoalUpdateRequestDto(BigDecimal targetWeight, Integer targetTrainingDays) {
        this.targetWeight = targetWeight;
        this.targetTrainingDays = targetTrainingDays;
    }
    // Gettery i Settery
    public BigDecimal getTargetWeight() { return targetWeight; }
    public void setTargetWeight(BigDecimal targetWeight) { this.targetWeight = targetWeight; }
    public Integer getTargetTrainingDays() { return targetTrainingDays; }
    public void setTargetTrainingDays(Integer targetTrainingDays) { this.targetTrainingDays = targetTrainingDays; }
}