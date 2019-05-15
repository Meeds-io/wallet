package org.exoplatform.addon.wallet.reward.model;

import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardPeriod {

  private long startDateInSeconds;

  private long endDateInSeconds;

  public JSONObject toJSONObject() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("startDateInSeconds", startDateInSeconds);
      jsonObject.put("endDateInSeconds", endDateInSeconds);
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return toJSONObject().toString();
  }

  public static RewardPeriod getCurrentPeriod(RewardSettings rewardSettings) {
    return getPeriodOfTime(rewardSettings, LocalDateTime.now());
  }

  public static RewardPeriod getPeriodOfTime(RewardSettings rewardSettings, LocalDateTime localDateTime) {
    RewardPeriodType rewardPeriodType = null;
    if (rewardSettings == null || rewardSettings.getPeriodType() == null) {
      rewardPeriodType = RewardPeriodType.DEFAULT;
    } else {
      rewardPeriodType = rewardSettings.getPeriodType();
    }
    return rewardPeriodType.getPeriodOfTime(localDateTime);
  }

}
