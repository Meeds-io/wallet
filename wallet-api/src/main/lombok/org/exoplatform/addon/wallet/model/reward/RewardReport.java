/*
   * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.addon.wallet.model.reward;

import static org.exoplatform.addon.wallet.utils.RewardUtils.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
public class RewardReport {

  private RewardPeriod      period  = null;

  private Set<WalletReward> rewards = new HashSet<>();

  public long countPending() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTransaction() != null
                      && StringUtils.equals(rewardItem.getTransaction().getStatus(),
                                            TRANSACTION_STATUS_PENDING))
                  .count();
  }

  public long countSuccess() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTransaction() != null
                      && StringUtils.equals(rewardItem.getTransaction().getStatus(),
                                            TRANSACTION_STATUS_SUCCESS))
                  .count();
  }

  public long countFailed() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTransaction() != null
                      && StringUtils.equals(rewardItem.getTransaction().getStatus(), TRANSACTION_STATUS_FAILED))
                  .count();
  }

  public long countTransactions() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTransaction() != null)
                  .count();
  }

  public Set<WalletReward> getValidRewards() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTokensToSend() > 0)
                  .collect(Collectors.toSet());
  }

  public double totalAmount() {
    return rewards.stream()
                  .mapToDouble(rewardItem -> rewardItem.getTokensSent())
                  .sum();
  }

  public long countValidRewards() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTokensToSend() > 0)
                  .count();
  }

  public boolean hasSuccessTransactions() {
    return countSuccess() > 0;
  }

  public boolean hasPendingTransactions() {
    return countPending() > 0;
  }

  public boolean hasErrorTransactions() {
    return countFailed() > 0;
  }

  public boolean isCompletelyProceeded() {
    // Can be greater if in the mean time of transaction confirmation, a member
    // has been invalidated / disabled / deleted
    return countTransactions() > 0 && countSuccess() >= countValidRewards();
  }
}
