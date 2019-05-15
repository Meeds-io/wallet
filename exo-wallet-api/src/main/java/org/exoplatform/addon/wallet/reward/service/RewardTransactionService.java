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

import org.exoplatform.addon.wallet.reward.model.RewardTransaction;

/**
 * A storage service to save/load reward transactions
 */
public interface RewardTransactionService {

  /**
   * @param networkId
   * @param periodType
   * @param startDateInSeconds
   * @return
   */
  public List<RewardTransaction> getRewardTransactions(Long networkId,
                                                       String periodType,
                                                       long startDateInSeconds);

  /**
   * Save reward transaction
   * 
   * @param rewardTransaction
   */
  public void saveRewardTransaction(RewardTransaction rewardTransaction);
}
