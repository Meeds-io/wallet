package org.exoplatform.wallet.model.reward;

import java.io.Serializable;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardPluginSettings implements Serializable, Cloneable {
  private static final long serialVersionUID = -843790790474775405L;

  private String            pluginId;

  @Exclude
  private boolean           enabled;

  @Exclude
  private double            threshold;

  @Exclude
  private boolean           usePools;

  @Exclude
  private RewardBudgetType  budgetType       = RewardBudgetType.DEFAULT;

  @Exclude
  private double            amount;

  @Override
  public RewardPluginSettings clone() { // NOSONAR
    try {
      return (RewardPluginSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      return new RewardPluginSettings(pluginId, enabled, threshold, usePools, budgetType, amount);
    }
  }

}
