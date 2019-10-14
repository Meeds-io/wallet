package org.exoplatform.wallet.model.reward;

import static org.exoplatform.wallet.utils.RewardUtils.formatTime;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RewardPeriod implements Serializable {
  private static final long serialVersionUID = -4860665131754056537L;

  private RewardPeriodType  rewardPeriodType;

  private long              startDateInSeconds;

  private long              endDateInSeconds;

  public RewardPeriod(RewardPeriodType rewardPeriodType) {
    this.rewardPeriodType = rewardPeriodType;
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

  public String getStartDateFormatted(String lang) {
    return formatTime(startDateInSeconds, lang);
  }

  public String getEndDateFormatted(String lang) {
    return formatTime(endDateInSeconds, lang);
  }

}
