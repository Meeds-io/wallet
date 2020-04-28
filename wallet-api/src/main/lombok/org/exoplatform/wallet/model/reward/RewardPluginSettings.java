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
