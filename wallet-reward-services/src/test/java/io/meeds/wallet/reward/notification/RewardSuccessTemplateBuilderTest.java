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
package io.meeds.wallet.reward.notification;

import java.util.HashSet;
import java.util.Set;

import io.meeds.wallet.model.*;

import io.meeds.wallet.reward.service.RewardSettingsService;
import io.meeds.wallet.utils.WalletUtils;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.ChannelManager;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static io.meeds.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-organization-configuration.xml"), })
public class RewardSuccessTemplateBuilderTest extends BaseExoTestCase {

  @Test
  public void testBuildMessage() {
    try (MockedStatic<WalletUtils> walletUtilsMockedStatic = Mockito.mockStatic(WalletUtils.class)) {
      ContractDetail mockContractDetail = new ContractDetail();
      mockContractDetail.setDecimals(12);
      walletUtilsMockedStatic.when(WalletUtils::getContractDetail).thenReturn(mockContractDetail);
      Set<String> rewardAdministrators = new HashSet<>();
      rewardAdministrators.add("root");
      walletUtilsMockedStatic.when(WalletUtils::getRewardAdministrators).thenReturn(rewardAdministrators);
      RewardSettingsService rewardSettingsService = mock(RewardSettingsService.class);
      when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
      InitParams params = getParams();
      RewardSuccessNotificationPlugin plugin = new RewardSuccessNotificationPlugin(params);

      RewardSuccessTemplateProvider templateProvider = new RewardSuccessTemplateProvider(rewardSettingsService, params);
      templateProvider.setMailTemplatePath("jar:/template/RewardSuccessReward.gtmpl");

      NotificationContext ctx = NotificationContextImpl.cloneInstance();

      RewardReport rewardReport = new RewardReport();
      RewardPeriod rewardPeriod = RewardPeriod.getCurrentPeriod(new RewardSettings());
      rewardReport.setPeriod(rewardPeriod);

      Set<WalletReward> rewards = new HashSet<>();
      for (int i = 0; i < 30; i++) {
        TransactionDetail transaction = new TransactionDetail();
        transaction.setHash("hash");
        transaction.setContractAmount(2);
        transaction.setPending(true);
        rewards.add(new WalletReward(null, transaction, 0, 0, 0, null, 1));
      }
      rewardReport.setRewards(rewards);
      ctx.append(REWARD_REPORT_NOTIFICATION_PARAM, rewardReport);
      NotificationInfo notification = plugin.buildNotification(ctx);
      ctx.setNotificationInfo(notification);
      notification.to(notification.getSendToUserIds().getFirst());

      CommonsUtils.getService(PluginSettingService.class).registerPluginConfig(getPluginConfig());
      CommonsUtils.getService(PluginContainer.class).addPlugin(plugin);
      CommonsUtils.getService(ChannelManager.class).registerOverrideTemplateProvider(templateProvider);

      MessageInfo message = templateProvider.getBuilder().buildMessage(ctx);
      assertNotNull(message);
      assertNotNull(message.getPluginId());
      assertEquals("Reward success!", message.getBody().replaceAll("(?s)<!--.*?-->", "").trim());
    }

  }

  private InitParams getParams() {
    InitParams initParams = new InitParams();
    ObjectParameter objectParam = new ObjectParameter();
    objectParam.setName("plugin-config");
    PluginConfig pluginConfig = getPluginConfig();
    objectParam.setObject(pluginConfig);
    initParams.addParam(objectParam);

    ValueParam valueParam = new ValueParam();
    valueParam.setName("channel-id");
    valueParam.setValue("MAIL_CHANNEL");
    initParams.addParam(valueParam);
    return initParams;
  }

  private PluginConfig getPluginConfig() {
    PluginConfig pluginConfig = new PluginConfig();
    pluginConfig.setPluginId("RewardSuccessNotificationPlugin");
    pluginConfig.setResourceBundleKey("UINotification.label.RewardSuccessNotificationPlugin");
    pluginConfig.setOrder("1");
    pluginConfig.setGroupId("wallet");
    pluginConfig.setBundlePath("locale.notification.WalletNotification");
    return pluginConfig;
  }

}
