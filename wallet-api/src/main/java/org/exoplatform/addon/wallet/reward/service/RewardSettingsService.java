/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.reward.service;

import java.util.Collection;
import java.util.Set;

import org.exoplatform.addon.wallet.model.reward.RewardPeriod;
import org.exoplatform.addon.wallet.model.reward.RewardSettings;
import org.exoplatform.addon.wallet.reward.api.RewardPlugin;

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
   * @return days number before reward end date to create reminder
   */
  int getReminderDateInDays();

  /**
   * @return {@link Set} of {@link RewardPeriod} having rewards in progress
   */
  public Set<RewardPeriod> getRewardPeriodsInProgress();

  /**
   * Save {@link Set} of {@link RewardPeriod} having rewards in progress
   * 
   * @param rewardPeriods
   */
  void saveRewardPeriodInProgress(Set<RewardPeriod> rewardPeriods);

  /**
   * Add {@link RewardPeriod} to the already stored {@link Set} of
   * {@link RewardPeriod}
   * 
   * @param rewardPeriod {@link RewardPeriod} to add
   */
  void addRewardPeriodInProgress(RewardPeriod rewardPeriod);

}
