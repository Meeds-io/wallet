/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.reward.service;

import java.util.List;

import org.exoplatform.addon.wallet.model.reward.RewardTransaction;

/**
 * A storage service to save/load reward transactions
 */
public interface RewardTransactionService {

  /**
   * Get the list of transactions sent on blockchain of type 'reward' in a
   * period of time
   * 
   * @param periodType period type: week, month...
   * @param startDateInSeconds start timestamp of the period used to reward
   *          wallets
   * @return list of sent Reward transactions
   */
  public List<RewardTransaction> getRewardTransactions(String periodType,
                                                       long startDateInSeconds);

  /**
   * Save reward transaction in eXo internal datasource
   * 
   * @param rewardTransaction to save in DB
   */
  public void saveRewardTransaction(RewardTransaction rewardTransaction);

}
