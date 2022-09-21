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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.exoplatform.wallet.model.reward.RewardBudgetType;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardPluginSettings;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.BaseWalletRewardTest;
import org.exoplatform.wallet.reward.api.RewardPlugin;

public class WalletRewardSettingsServiceTest extends BaseWalletRewardTest {

  /**
   * Check that service is instantiated
   */
  @Test
  public void testServiceInstantiated() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    assertNotNull(rewardSettingsService);
  }

  @Test
  public void tetGetRewardPlugins() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    assertNotNull("List of rewards plugins shouldn't be null", rewardPlugins);
    assertEquals("Unexpected reward plugins count", 2, rewardPlugins.size());
  }

  @Test
  public void tetGetRewardPlugin() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

    String pluginId = "kudos";
    RewardPlugin kudosRewardPlugin = rewardSettingsService.getRewardPlugin(pluginId);
    assertNotNull("Rewards plugin 'kudos' shouldn't be null", kudosRewardPlugin);
    assertEquals("Unexpected reward plugin name", pluginId, kudosRewardPlugin.getName());
    assertEquals("Unexpected reward plugin id", pluginId, kudosRewardPlugin.getPluginId());
    Map<Long, Double> earnedPoints = kudosRewardPlugin.getEarnedPoints(Collections.emptySet(), 0, Long.MAX_VALUE);
    assertEquals("Unexpected reward plugin earned points",
                 new HashMap<>(),
                 earnedPoints);
  }

  @Test
  public void testRegisterPlugin() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

    rewardSettingsService.registerPlugin(CUSTOM_REWARD_PLUGIN);
    try {
      RewardPlugin registeredRewardPlugin = rewardSettingsService.getRewardPlugin(CUSTOM_PLUGIN_ID);

      assertNotNull("Rewards plugin 'kudos' shouldn't be null", registeredRewardPlugin);
      assertEquals("Returned rewards plugin isn't the same as the registered one", CUSTOM_REWARD_PLUGIN, registeredRewardPlugin);
      assertEquals("Unexpected reward plugin name", CUSTOM_PLUGIN_NAME, CUSTOM_REWARD_PLUGIN.getName());
      assertEquals("Unexpected reward plugin id", CUSTOM_PLUGIN_ID, CUSTOM_REWARD_PLUGIN.getPluginId());
      assertTrue("Reward plugin should be enabled by default", CUSTOM_REWARD_PLUGIN.isEnabled());

      Set<Long> identityIds = new HashSet<>(Arrays.asList(1l, 2l, 3l, 4l, 5l));
      Map<Long, Double> expectedRewards = getEarnedPoints(identityIds);
      Map<Long, Double> earnedPoints = CUSTOM_REWARD_PLUGIN.getEarnedPoints(identityIds, 0, Long.MAX_VALUE);
      assertEquals("Unexpected reward plugin earned points",
                   expectedRewards,
                   earnedPoints);
    } finally {
      rewardSettingsService.unregisterPlugin(CUSTOM_PLUGIN_ID);
    }
  }

  @Test
  public void testGetSettings() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    assertNotNull(defaultSettings);
    assertEquals("Wrong default reward period type", RewardPeriodType.DEFAULT, defaultSettings.getPeriodType());

    Set<RewardPluginSettings> pluginSettings = defaultSettings.getPluginSettings();
    assertNotNull("List of rewards plugin settings shouldn't be null", pluginSettings);
    assertEquals("Unexpected reward plugin settings count", 2, pluginSettings.size());

    RewardPluginSettings rewardPluginSettings = pluginSettings.iterator().next();
    assertNotNull(rewardPluginSettings);
    assertEquals(0, rewardPluginSettings.getAmount(), 0);
    assertEquals(RewardBudgetType.DEFAULT, rewardPluginSettings.getBudgetType());
    assertEquals(0, rewardPluginSettings.getThreshold(), 0);
    assertFalse(rewardPluginSettings.isEnabled());
  }

  @Test
  public void testSaveSettings() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    RewardSettings newSettings = cloneSettings(defaultSettings);

    Set<RewardPluginSettings> newPluginSettings = newSettings.getPluginSettings();
    long amount = 1l;
    boolean enabled = true;
    boolean usePools = true;
    long threshold = 2l;
    RewardBudgetType budgetType = RewardBudgetType.FIXED_PER_POINT;
    RewardPeriodType periodType = RewardPeriodType.SEMESTER;

    newSettings.setPeriodType(periodType);
    for (RewardPluginSettings pluginSetting : newPluginSettings) {
      pluginSetting.setAmount(amount);
      pluginSetting.setBudgetType(budgetType);
      pluginSetting.setEnabled(enabled);
      pluginSetting.setThreshold(threshold);
      pluginSetting.setUsePools(usePools);
    }
    rewardSettingsService.saveSettings(newSettings);
    try {
      newSettings = rewardSettingsService.getSettings();

      assertNotNull(newSettings);
      assertEquals(periodType, newSettings.getPeriodType());
      newPluginSettings = newSettings.getPluginSettings();
      for (RewardPluginSettings pluginSetting : newPluginSettings) {
        assertEquals(amount, pluginSetting.getAmount(), 0);
        assertEquals(threshold, pluginSetting.getThreshold(), 0);
        assertFalse("Enablement should be retrieved always from registered plugin and not from storage",
                    pluginSetting.isEnabled());
        assertEquals(budgetType, pluginSetting.getBudgetType());
        assertFalse("Pools can't be used when budget type equals to 'FIXED_PER_POINT'", pluginSetting.isUsePools());
      }

      budgetType = RewardBudgetType.FIXED;
      for (RewardPluginSettings pluginSetting : newPluginSettings) {
        pluginSetting.setBudgetType(budgetType);
        pluginSetting.setUsePools(usePools);
      }
      rewardSettingsService.saveSettings(newSettings);

      newSettings = rewardSettingsService.getSettings();

      newPluginSettings = newSettings.getPluginSettings();
      for (RewardPluginSettings pluginSetting : newPluginSettings) {
        assertEquals(budgetType, pluginSetting.getBudgetType());
        assertTrue("Pools should be saved using new value 'true' when budget type is set to 'FIXED'", pluginSetting.isUsePools());
      }
    } finally {
      rewardSettingsService.saveSettings(defaultSettings);
    }
  }

}
