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
package org.exoplatform.wallet.reward.service;

import java.util.Collection;

import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.api.RewardPlugin;

/**
 * A storage service to save/load reward transactions
 */
public interface RewardSettingsService {

  /**
   * @return the reward settings
   */
  public RewardSettings getSettings();

  /**
   * Save reward settings
   * 
   * @param rewardSettingsToStore reward settings object
   */
  public void saveSettings(RewardSettings rewardSettingsToStore);

  /**
   * @return configured reward settings plugins
   */
  public Collection<RewardPlugin> getRewardPlugins();

  /**
   * @param pluginId reward plugin id
   * @return configured reward plugin identified by an id
   */
  public RewardPlugin getRewardPlugin(String pluginId);

  /**
   * Registers a reward plugin
   * 
   * @param rewardPlugin reward component plugin
   */
  public void registerPlugin(RewardPlugin rewardPlugin);

  /**
   * Removes a previously registered reward plugin
   * 
   * @param pluginId
   */
  void unregisterPlugin(String pluginId);

}
