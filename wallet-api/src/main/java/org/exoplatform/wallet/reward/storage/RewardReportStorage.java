package org.exoplatform.wallet.reward.storage;

import java.util.List;

import org.exoplatform.wallet.model.reward.*;

public interface RewardReportStorage {

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

  /**
   * Retrieve the list of periods switch rewards payment status
   * 
   * @param rewardStatus
   * @return list of {@link RewardPeriod}
   */
  List<RewardPeriod> findRewardPeriodsByStatus(RewardStatus rewardStatus);

  /**
   * @param identityId
   * @param limit limit of items to return
   * @return a {@link List} of {@link WalletReward} for current person
   */
  List<WalletReward> listRewards(long identityId, int limit);

}
