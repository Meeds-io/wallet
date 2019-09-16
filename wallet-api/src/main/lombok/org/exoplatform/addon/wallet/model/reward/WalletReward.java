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
  private RewardTransaction       transaction;

  @Exclude
  private long                    poolId;

  @Exclude
  private String                  poolName;

  private boolean                 enabled;

  public double getTokensSent() {
    if (transaction == null || transaction.getTokensSent() == 0) {
      return 0;
    } else {
      return transaction.getTokensSent();
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
