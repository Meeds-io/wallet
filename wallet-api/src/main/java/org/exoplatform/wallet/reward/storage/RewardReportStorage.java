/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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

  /**
   * Replaces an old reward transaction hash to a new one, that had boosted the
   * first one
   * 
   * @param oldHash old Transaction hash
   * @param newHash new Transaction hash
   */
  void replaceRewardTransactions(String oldHash, String newHash);

}
