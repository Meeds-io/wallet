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

import java.io.Serializable;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSettings implements Serializable, Cloneable {
  private static final long         serialVersionUID = -8650247964730374760L;

  private RewardPeriodType          periodType       = RewardPeriodType.DEFAULT;

  private String                    timeZone         = ZoneId.systemDefault().getId();

  private Set<RewardPluginSettings> pluginSettings;

  public ZoneId zoneId() {
    return ZoneId.of(timeZone);
  }

  @Override
  public RewardSettings clone() { // NOSONAR
    try {
      return (RewardSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      @SuppressWarnings("unchecked")
      Set<RewardPluginSettings> clonedPluginSettings =
                                                     pluginSettings == null ? null
                                                                            : (Set<RewardPluginSettings>) new HashSet<>(pluginSettings).clone();
      return new RewardSettings(periodType, timeZone, clonedPluginSettings);
    }
  }
}
