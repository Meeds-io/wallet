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
package io.meeds.wallet.reward.service;


import org.springframework.stereotype.Service;

import io.meeds.wallet.model.RewardSettings;

/**
 * A storage service to save/load reward transactions
 */
@Service("rewardSettingsService")
public interface RewardSettingsService {

  /**
   * @return the reward settings
   */
  RewardSettings getSettings();

  /**
   * Save reward settings
   * 
   * @param rewardSettingsToStore reward settings object
   */
  void saveSettings(RewardSettings rewardSettingsToStore);

  /**
   * Delete reward settings
   * */
  void deleteSettings();

}
