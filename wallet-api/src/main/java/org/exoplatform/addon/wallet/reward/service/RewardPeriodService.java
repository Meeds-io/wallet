package org.exoplatform.addon.wallet.reward.service;

import org.exoplatform.addon.wallet.model.reward.RewardPeriodType;
import org.exoplatform.addon.wallet.model.reward.RewardReport;

public interface RewardPeriodService {

  /**
   * Retrieve a {@link RewardReport} corresponding to a period of time
   * 
   * @param periodType period type of rewards payment periodicity
   * @param periodTimeInSeconds selected date period
   * @return {@link RewardReport} if there is a saved one, else null
   */
  RewardReport getRewardReport(RewardPeriodType periodType, long periodTimeInSeconds);

  /**
   * @param rewardReport save generated reward report
   */
  void saveRewardReport(RewardReport rewardReport);

}
