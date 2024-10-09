/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Lab contact@meedslab.com
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

import com.google.javascript.jscomp.jarjar.com.google.common.base.Objects;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;

import io.meeds.wallet.model.RewardBudgetType;
import io.meeds.wallet.model.RewardPeriodType;
import io.meeds.wallet.model.RewardSettings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static io.meeds.wallet.utils.WalletUtils.toJsonString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { WalletRewardSettingsService.class })
public class WalletRewardSettingsServiceTest {

  @MockBean
  private SettingService        settingService;

  @Autowired
  private RewardSettingsService rewardSettingsService;

  @Test
  public void testGetSettings() {

    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    verify(settingService, times(1)).get(any(), any(), any());
    assertNotNull(defaultSettings);
    assertEquals(RewardPeriodType.DEFAULT, defaultSettings.getPeriodType());

    assertEquals(0, defaultSettings.getAmount(), 0);
    assertEquals(RewardBudgetType.DEFAULT, defaultSettings.getBudgetType());
    assertEquals(0, defaultSettings.getThreshold(), 0);
  }

  @Test
  public void testSaveSettings() {
    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    RewardSettings newSettings = defaultSettings.clone();
    long amount = 1L;
    long threshold = 2L;
    RewardBudgetType budgetType = RewardBudgetType.FIXED_PER_POINT;
    RewardPeriodType periodType = RewardPeriodType.MONTH;

    newSettings.setPeriodType(periodType);
    newSettings.setAmount(amount);
    newSettings.setBudgetType(budgetType);
    newSettings.setThreshold(threshold);

    rewardSettingsService.saveSettings(newSettings);
    RewardSettings finalNewSettings = newSettings;
    verify(settingService, times(1)).set(any(),
                                         any(),
                                         any(),
                                         argThat(value -> Objects.equal(value.getValue(), toJsonString(finalNewSettings))));
    try {
      SettingValue value = SettingValue.create(toJsonString(finalNewSettings));
      when(settingService.get(any(), any(), any())).thenReturn(value);
      newSettings = rewardSettingsService.getSettings();

      assertNotNull(newSettings);
      assertEquals(periodType, newSettings.getPeriodType());
      assertEquals(amount, newSettings.getAmount(), 0);
      assertEquals(threshold, newSettings.getThreshold(), 0);
      assertEquals(budgetType, newSettings.getBudgetType());

      budgetType = RewardBudgetType.FIXED;
      newSettings.setBudgetType(budgetType);
      rewardSettingsService.saveSettings(newSettings);
      value = SettingValue.create(toJsonString(newSettings));
      when(settingService.get(any(), any(), any())).thenReturn(value);
      newSettings = rewardSettingsService.getSettings();
      assertEquals(budgetType, newSettings.getBudgetType());
    } finally {
      rewardSettingsService.saveSettings(defaultSettings);
    }
  }

  @Test
  public void testDeleteSettings() {
    rewardSettingsService.deleteSettings();
    verify(settingService, times(1)).remove(any(), any(), any());
  }
}
