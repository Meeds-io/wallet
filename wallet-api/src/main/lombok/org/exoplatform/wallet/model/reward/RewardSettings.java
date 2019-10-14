package org.exoplatform.wallet.model.reward;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSettings implements Serializable, Cloneable {
  private static final long         serialVersionUID = -8650247964730374760L;

  private RewardPeriodType          periodType       = RewardPeriodType.DEFAULT;

  private Set<RewardPluginSettings> pluginSettings;

  @Override
  public RewardSettings clone() { // NOSONAR
    try {
      return (RewardSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      @SuppressWarnings("unchecked")
      Set<RewardPluginSettings> clonedPluginSettings =
                                                     pluginSettings == null ? null
                                                                            : (Set<RewardPluginSettings>) new HashSet<>(pluginSettings).clone();
      return new RewardSettings(periodType, clonedPluginSettings);
    }
  }
}
