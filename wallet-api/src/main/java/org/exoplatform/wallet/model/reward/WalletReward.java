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
package org.exoplatform.wallet.model.reward;

import static org.exoplatform.wallet.utils.RewardUtils.*;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletReward implements Serializable {
  private static final long       serialVersionUID = -4328398843364453949L;

  private Wallet                  wallet;

  @Exclude
  private List<RewardTeam>        teams;

  @Exclude
  private TransactionDetail       transaction;

  @Exclude
  private Set<WalletPluginReward> rewards;

  @Exclude
  private RewardPeriod            period;

  public long getIdentityId() {
    return wallet == null ? 0 : wallet.getTechnicalId();
  }

  public boolean isEnabled() {
    if (wallet == null) {
      throw new IllegalStateException("wallet isn\'t loaded");
    }
    return wallet.isEnabled() && !wallet.isDeletedUser() && !wallet.isDisabledUser()
        && StringUtils.isNotBlank(wallet.getAddress());
  }

  public String getPoolName() {
    if (teams == null) {
      return null;
    }
    Set<String> teamNames = teams.stream().map(RewardTeam::getName).collect(Collectors.toSet());
    return StringUtils.join(teamNames, ",");
  }

  public RewardTeam getTeam() {
    if (teams == null || teams.isEmpty()) {
      return null;
    }
    return teams.get(0);
  }

  public String getStatus() {
    if (transaction == null) {
      return null;
    }
    if (transaction.isPending()) {
      return TRANSACTION_STATUS_PENDING;
    } else if (transaction.isSucceeded()) {
      return TRANSACTION_STATUS_SUCCESS;
    } else {
      return TRANSACTION_STATUS_FAILED;
    }
  }

  public double getTokensSent() {
    if (transaction == null) {
      return 0;
    } else {
      return transaction.getContractAmount();
    }
  }

  public double getTokensToSend() {
    if (rewards == null || rewards.isEmpty()) {
      return 0;
    } else {
      return rewards.stream().mapToDouble(WalletPluginReward::getAmount).sum();
    }
  }

  public double getPoolTokensToSend() {
    if (rewards == null || rewards.isEmpty()) {
      return 0;
    } else {
      return rewards.stream().filter(WalletPluginReward::isPoolsUsed).mapToDouble(WalletPluginReward::getAmount).sum();
    }
  }
}
