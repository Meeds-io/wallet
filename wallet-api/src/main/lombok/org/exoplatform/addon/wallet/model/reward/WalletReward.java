package org.exoplatform.addon.wallet.model.reward;

import static org.exoplatform.addon.wallet.utils.RewardUtils.*;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletReward implements Serializable {
  private static final long       serialVersionUID = -4328398843364453949L;

  private Wallet                  wallet;

  private List<RewardTeam>        teams;

  private TransactionDetail       transaction;

  private Set<WalletPluginReward> rewards;

  public long getIdentityId() {
    return wallet == null ? 0 : wallet.getTechnicalId();
  }

  public boolean isEnabled() {
    if (wallet == null) {
      throw new IllegalStateException("wallet isn\'t loaded");
    }
    if (StringUtils.isNotBlank(wallet.getAddress()) && wallet.getIsApproved() == null) {
      throw new IllegalStateException("wallet blockchain state isn\'t loaded");
    }
    return wallet.isEnabled() && !wallet.isDeletedUser() && !wallet.isDisabledUser()
        && StringUtils.isNotBlank(wallet.getAddress()) && wallet.getIsApproved();
  }

  public String getPoolName() {
    if (teams == null) {
      return null;
    }
    return StringUtils.join(teams, ",");
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
      return rewards.stream().mapToDouble(reward -> reward.getAmount()).sum();
    }
  }

  public double getPoolTokensToSend() {
    if (rewards == null || rewards.isEmpty()) {
      return 0;
    } else {
      return rewards.stream().filter(WalletPluginReward::isPoolsUsed).mapToDouble(reward -> reward.getAmount()).sum();
    }
  }
}
