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

import static org.exoplatform.addon.wallet.utils.RewardUtils.*;
import static org.exoplatform.addon.wallet.utils.WalletUtils.fromJsonString;
import static org.exoplatform.addon.wallet.utils.WalletUtils.toJsonString;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.reward.api.RewardPlugin;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.container.xml.InitParams;

/**
 * A storage service to save/load reward transactions
 */
public class WalletRewardSettingsService implements RewardSettingsService {

  private static final String       REMINDER_DAYS_PARAMETER = "reminder.days";

  private SettingService            settingService;

  private RewardSettings            rewardSettings;

  private Map<String, RewardPlugin> rewardPlugins           = new HashMap<>();

  private int                       reminderDateInDays      = 3;

  public WalletRewardSettingsService(SettingService settingService, InitParams params) {
    this.settingService = settingService;
    if (params != null && params.containsKey(REMINDER_DAYS_PARAMETER)) {
      this.reminderDateInDays = Integer.parseInt(params.getValueParam(REMINDER_DAYS_PARAMETER).getValue());
    }
  }

  @Override
  public RewardSettings getSettings() {
    // Retrieve cached reward settings
    if (this.rewardSettings != null) {
      return this.rewardSettings.clone();
    }

    SettingValue<?> settingsValue =
                                  settingService.get(REWARD_CONTEXT, REWARD_SCOPE, REWARD_SETTINGS_KEY_NAME);

    String settingsValueString = settingsValue == null || settingsValue.getValue() == null ? null
                                                                                           : settingsValue.getValue().toString();

    RewardSettings storedRewardSettings = fromJsonString(settingsValueString, RewardSettings.class);
    if (storedRewardSettings == null) {
      storedRewardSettings = new RewardSettings();
    }

    Set<RewardPluginSettings> pluginSettings = storedRewardSettings.getPluginSettings();
    if (pluginSettings == null) {
      pluginSettings = new HashSet<>();
      storedRewardSettings.setPluginSettings(pluginSettings);
    }

    // Add configured plugin settings if not already stored
    Set<String> configuredPluginIds = rewardPlugins.keySet();
    if (!configuredPluginIds.isEmpty()) {
      for (String configuredPluginId : configuredPluginIds) {
        if (pluginSettings.stream().noneMatch(plugin -> StringUtils.equals(plugin.getPluginId(), configuredPluginId))) {
          RewardPluginSettings emptyRewardSettings = new RewardPluginSettings();
          emptyRewardSettings.setPluginId(configuredPluginId);
          pluginSettings.add(emptyRewardSettings);
        }
      }
    }

    // Check enabled plugins
    for (RewardPluginSettings rewardPluginSettings : pluginSettings) {
      if (rewardPluginSettings != null) {
        String pluginId = rewardPluginSettings.getPluginId();
        RewardPlugin rewardPlugin = getRewardPlugin(pluginId);
        boolean enabled = false;
        if (rewardPlugin != null) {
          enabled = rewardPlugin.isEnabled();
        }
        rewardPluginSettings.setEnabled(enabled);
      }
    }

    // Cache reward settings
    this.rewardSettings = storedRewardSettings;
    return this.rewardSettings;
  }

  @Override
  public void saveSettings(RewardSettings rewardSettingsToStore) {
    if (rewardSettingsToStore == null) {
      throw new IllegalArgumentException("Empty settings to save");
    }

    // Check using pool only if not budget is of type 'points reward'
    Set<RewardPluginSettings> pluginSettings = rewardSettingsToStore.getPluginSettings();
    if (pluginSettings != null && !pluginSettings.isEmpty()) {
      for (RewardPluginSettings rewardPluginSettings : pluginSettings) {
        if (rewardPluginSettings.getBudgetType() == RewardBudgetType.FIXED_PER_POINT) {
          rewardPluginSettings.setUsePools(false);
        }
      }
    }
    String settingsString = toJsonString(rewardSettingsToStore);
    settingService.set(REWARD_CONTEXT, REWARD_SCOPE, REWARD_SETTINGS_KEY_NAME, SettingValue.create(settingsString));

    // Purge cached settings
    this.rewardSettings = null;
  }

  @Override
  public void registerPlugin(RewardPlugin rewardPlugin) {
    rewardPlugins.put(rewardPlugin.getPluginId(), rewardPlugin);

    // Purge cached settings
    this.rewardSettings = null;
  }

  public void unregisterPlugin(String pluginId) {
    rewardPlugins.remove(pluginId);

    // Purge cached settings
    this.rewardSettings = null;
  }

  @Override
  public Collection<RewardPlugin> getRewardPlugins() {
    return rewardPlugins.values();
  }

  @Override
  public RewardPlugin getRewardPlugin(String pluginId) {
    return rewardPlugins.get(pluginId);
  }

  @Override
  public int getReminderDateInDays() {
    return reminderDateInDays;
  }
}
