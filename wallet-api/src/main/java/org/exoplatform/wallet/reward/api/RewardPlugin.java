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
package org.exoplatform.wallet.reward.api;

import java.util.Map;
import java.util.Set;

import org.exoplatform.container.component.BaseComponentPlugin;

/**
 * A reward plugin to manage rewarded points
 */
public abstract class RewardPlugin extends BaseComponentPlugin {

  /**
   * get reward plugin unique identifier
   * 
   * @return reward plugin ID
   */
  public String getPluginId() {
    return getName();
  }

  /**
   * Checks is the plugin is enabled
   * 
   * @return whether the plugin is enabled or not
   */
  public boolean isEnabled() {
    return true;
  }

  /**
   * Retrieves earned points for identities in a selected period of time
   * 
   * @param identityIds identity ids of wallets to consider in computation
   * @param startDateInSeconds start timestamp in seconds of reward period
   * @param endDateInSeconds end timestamp in seconds of reward period
   * @return a {@link Map} of identity ID with the sum of tokens to send as
   *         reward
   */
  public abstract Map<Long, Double> getEarnedPoints(Set<Long> identityIds, long startDateInSeconds, long endDateInSeconds);

}
