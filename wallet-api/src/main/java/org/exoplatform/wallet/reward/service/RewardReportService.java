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
package org.exoplatform.wallet.reward.service;

import java.util.List;
import java.util.Set;

import org.exoplatform.wallet.model.reward.*;

/**
 * A storage service to save/load reward settings
 */
public interface RewardReportService {

  /**
   * Compute rewards swicth configurations for the list of identities passed in
   * parameters
   * 
   * @param periodDateInSeconds a timestamp in seconds inside the period time
   *          that will be retrieved
   * @return a {@link Set} of {@link WalletReward} with the details of sent
   *         tokens and tokens to send
   */
  RewardReport computeRewards(long periodDateInSeconds);

  /**
   * Send rewards transactions
   * 
   * @param periodDateInSeconds a timestamp in seconds inside the period time
   *          that will be retrieved
   * @param username current username sending rewards
   * @throws Exception if an error occurs while sending the rewards transactions
   *           on blockchain
   */
  void sendRewards(long periodDateInSeconds, String username) throws Exception; // NOSONAR

  /**
   * Retrieve a {@link RewardReport} corresponding to a period of time
   * 
   * @param periodTimeInSeconds selected date period
   * @return {@link RewardReport} if there is a saved one, else null
   */
  RewardReport getRewardReport(long periodTimeInSeconds);

  /**
   * @param rewardReport save generated reward report
   */
  void saveRewardReport(RewardReport rewardReport);

  /**
   * @return a {@link List} of {@link RewardPeriod} that are in progress
   */
  List<RewardPeriod> getRewardPeriodsInProgress();

  /*
   * Return list of reward reports not sent yet
   */
  List<RewardPeriod> getRewardPeriodsNotSent();

  /**
   * @param currentUser current user listing his rewards
   * @param limit size limit of items to return
   * @return a {@link List} of {@link WalletReward} of current user
   */
  List<WalletReward> listRewards(String currentUser, int limit);

  /**
   * Replaces an old reward transaction hash to a new one, that had boosted the
   * first one
   * 
   * @param oldHash old Transaction hash
   * @param newHash new Transaction hash
   */
  void replaceRewardTransactions(String oldHash, String newHash);

}
