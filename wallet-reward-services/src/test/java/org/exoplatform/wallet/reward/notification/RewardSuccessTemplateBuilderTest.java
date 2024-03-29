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
package org.exoplatform.wallet.reward.notification;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.exoplatform.wallet.utils.RewardUtils.getRewardSettings;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.ChannelManager;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.BaseWalletRewardTest;
import org.exoplatform.wallet.reward.service.WalletRewardSettingsService;

public class RewardSuccessTemplateBuilderTest extends BaseWalletRewardTest {

  /**
   * Check that provider returns templates correctly
   */
  @Test
  public void testBuildMessage() {
    InitParams params = getParams();
    RewardSuccessNotificationPlugin plugin = new RewardSuccessNotificationPlugin(params);

    RewardSuccessTemplateProvider templateProvider =
                                                   new RewardSuccessTemplateProvider(container,
                                                                                     getService(WalletRewardSettingsService.class),
                                                                                     params);
    templateProvider.setMailTemplatePath("jar:/template/RewardSuccessReward.gtmpl");

    NotificationContext ctx = NotificationContextImpl.cloneInstance();

    RewardReport rewardReport = new RewardReport();
    RewardPeriod rewardPeriod = RewardPeriod.getCurrentPeriod(getRewardSettings());
    rewardReport.setPeriod(rewardPeriod);

    Set<WalletReward> rewards = new HashSet<>();
    for (int i = 0; i < 30; i++) {
      TransactionDetail transaction = new TransactionDetail();
      transaction.setHash("hash");
      transaction.setContractAmount(2);
      transaction.setPending(true);
      rewards.add(new WalletReward(null, null, transaction, null, null));
    }
    rewardReport.setRewards(rewards);
    ctx.append(REWARD_REPORT_NOTIFICATION_PARAM, rewardReport);
    NotificationInfo notification = plugin.buildNotification(ctx);
    ctx.setNotificationInfo(notification);
    notification.to(notification.getSendToUserIds().get(0));

    CommonsUtils.getService(PluginSettingService.class).registerPluginConfig(getPluginConfig());
    CommonsUtils.getService(PluginContainer.class).addPlugin(plugin);
    CommonsUtils.getService(ChannelManager.class).registerOverrideTemplateProvider(templateProvider);

    MessageInfo message = templateProvider.getBuilder().buildMessage(ctx);
    assertNotNull(message);
    assertNotNull(message.getPluginId());
    assertEquals("Reward success!", message.getBody().replaceAll("(?s)<!--.*?-->", "").trim());
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
