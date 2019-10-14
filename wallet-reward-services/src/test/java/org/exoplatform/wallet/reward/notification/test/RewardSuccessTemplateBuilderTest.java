package org.exoplatform.wallet.reward.notification.test;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.exoplatform.wallet.utils.RewardUtils.getRewardSettings;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.exoplatform.container.xml.*;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.notification.RewardSuccessNotificationPlugin;
import org.exoplatform.wallet.reward.notification.RewardSuccessTemplateProvider;
import org.exoplatform.wallet.reward.test.BaseWalletRewardTest;

public class RewardSuccessTemplateBuilderTest extends BaseWalletRewardTest {

  /**
   * Check that provider returns templates correctly
   */
  @Test
  public void testBuildMessage() {
    InitParams params = getParams();
    RewardSuccessNotificationPlugin plugin = new RewardSuccessNotificationPlugin(params);

    RewardSuccessTemplateProvider templateProvider = new RewardSuccessTemplateProvider(container, params);
    templateProvider.setWebTemplatePath("jar:/template/RewardSuccessReward.gtmpl");

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
    assertEquals("Reward success!", message.getBody());
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
    valueParam.setValue("WEB_CHANNEL");
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
