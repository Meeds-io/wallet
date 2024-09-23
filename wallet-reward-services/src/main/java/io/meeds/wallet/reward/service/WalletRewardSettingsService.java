/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.reward.service;

import static io.meeds.wallet.wallet.utils.RewardUtils.*;
import static io.meeds.wallet.wallet.utils.WalletUtils.fromJsonString;
import static io.meeds.wallet.wallet.utils.WalletUtils.toJsonString;

import java.util.*;

import io.meeds.wallet.wallet.model.reward.RewardSettings;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

/**
 * A storage service to save/load reward transactions
 */
@Service
@Primary
public class WalletRewardSettingsService implements RewardSettingsService {

  @Autowired
  private SettingService settingService;

  private RewardSettings configuredRewardSettings;

  @Override
  public RewardSettings getSettings() { // NOSONAR
    // Retrieve cached reward settings
    if (this.configuredRewardSettings != null) {
      return this.configuredRewardSettings.clone();
    }

    SettingValue<?> settingsValue = settingService.get(REWARD_CONTEXT, REWARD_SCOPE, REWARD_SETTINGS_KEY_NAME);

    String settingsValueString = settingsValue == null || settingsValue.getValue() == null ? null
                                                                                           : settingsValue.getValue().toString();

    RewardSettings rewardSettings;
    if (settingsValueString == null) {
      rewardSettings = new RewardSettings();
      rewardSettings.setStoredSetting(false);
    } else {
      rewardSettings = fromJsonString(settingsValueString, RewardSettings.class);
      Objects.requireNonNull(rewardSettings).setStoredSetting(true);
    }

    // Cache reward settings
    this.configuredRewardSettings = rewardSettings;
    return this.configuredRewardSettings;
  }

  @Override
  public void saveSettings(RewardSettings rewardSettingsToStore) {
    if (rewardSettingsToStore == null) {
      throw new IllegalArgumentException("Empty settings to save");
    }

    String settingsString = toJsonString(rewardSettingsToStore);
    settingService.set(REWARD_CONTEXT, REWARD_SCOPE, REWARD_SETTINGS_KEY_NAME, SettingValue.create(settingsString));

    // Purge cached settings
    this.configuredRewardSettings = null;
  }

  @Override
  public void deleteSettings() {
    settingService.remove(REWARD_CONTEXT, REWARD_SCOPE, REWARD_SETTINGS_KEY_NAME);

    // Purge cached settings
    this.configuredRewardSettings = null;
  }
}
