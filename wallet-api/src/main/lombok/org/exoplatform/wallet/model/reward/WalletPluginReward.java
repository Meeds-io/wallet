package org.exoplatform.wallet.model.reward;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.ToString;

@Data
@ToString
public class WalletPluginReward implements Serializable {
  private static final long serialVersionUID = 1622627645862974585L;

  private String            pluginId;

  private long              identityId;

  @Exclude
  private boolean           poolsUsed;

  @Exclude
  private double            points;

  @Exclude
  private double            amount;

}
