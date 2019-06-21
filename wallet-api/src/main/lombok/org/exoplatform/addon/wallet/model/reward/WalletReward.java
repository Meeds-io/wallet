package org.exoplatform.addon.wallet.model.reward;

import java.io.Serializable;
import java.util.Set;

import org.exoplatform.addon.wallet.model.Wallet;

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
  private Set<WalletPluginReward> rewards;

  @Exclude
  private RewardTransaction       rewardTransaction;

  @Exclude
  private String                  poolName;

  private boolean                 enabled;

  public double getTokensSent() {
    if (rewardTransaction == null || rewardTransaction.getTokensSent() == 0) {
      return 0;
    } else {
      return rewardTransaction.getTokensSent();
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
