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
package io.meeds.wallet.wallet.model.reward;

import static io.meeds.wallet.wallet.utils.RewardUtils.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;

@Data
public class RewardReport {
  private RewardPeriod      period  = null;

  private Set<WalletReward> rewards = new HashSet<>();

  private int participationsCount;

  public long getPendingTransactionCount() {
    return rewards.stream()
                  .filter(rewardItem -> StringUtils.equals(rewardItem.getStatus(),
                                                           TRANSACTION_STATUS_PENDING))
                  .count();
  }

  public long getSuccessTransactionCount() {
    return rewards.stream()
                  .filter(rewardItem -> StringUtils.equals(rewardItem.getStatus(),
                                                           TRANSACTION_STATUS_SUCCESS))
                  .count();
  }

  public long getFailedTransactionCount() {
    return rewards.stream()
                  .filter(rewardItem -> StringUtils.equals(rewardItem.getStatus(),
                                                           TRANSACTION_STATUS_FAILED))
                  .count();
  }

  public long getTransactionsCount() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getTransaction() != null)
                  .count();
  }

  public Set<WalletReward> getValidRewards() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getAmount() > 0)
                  .collect(Collectors.toSet());
  }

  public double getRemainingTokensToSend() {
    return rewards.stream()
                  .mapToDouble(rewardItem -> rewardItem.getTokensSent() == 0 ? rewardItem.getAmount() : 0)
                  .sum();
  }

  public long getValidRewardCount() {
    return rewards.stream()
                  .filter(rewardItem -> rewardItem.getAmount() > 0)
                  .count();
  }

  public double getTokensToSend() {
    return rewards.stream()
                  .mapToDouble(WalletReward::getAmount)
                  .sum();
  }

  public double getTokensSent() {
    return rewards.stream()
                  .mapToDouble(WalletReward::getTokensSent)
                  .sum();
  }

  public boolean hasSuccessTransactions() {
    return getSuccessTransactionCount() > 0;
  }

  public boolean hasPendingTransactions() {
    return getPendingTransactionCount() > 0;
  }

  public boolean hasErrorTransactions() {
    return getFailedTransactionCount() > 0;
  }

  public boolean isCompletelyProceeded() {
    // Can be greater if in the mean time of transaction confirmation, a member
    // has been invalidated / disabled / deleted
    return getTransactionsCount() > 0 && getSuccessTransactionCount() >= getValidRewardCount();
  }
}
