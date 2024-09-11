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
package org.exoplatform.wallet.reward.notification;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_FAIL_COUNT;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_PENDING_COUNT;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_SUCCESS_COUNT;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_TRANSACTION_COUNT;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_VALID_MEMBERS_COUNT;
import static org.exoplatform.wallet.utils.WalletUtils.AMOUNT;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.model.settings.GlobalSettings;
import org.exoplatform.wallet.reward.BaseRewardTest;
import org.exoplatform.wallet.service.WalletService;
import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(BaseRewardTest.class)
public class RewardSuccessNotificationPluginTest extends BaseRewardTest {

  @Mock
  private InitParams    initParams;

  @Mock
  private WalletService walletService;

  @Test
  public void testMakeMessage() {
    GlobalSettings globalSettings = new GlobalSettings();
    when(walletService.getSettings()).thenReturn(globalSettings);
    RewardSuccessNotificationPlugin plugin = new RewardSuccessNotificationPlugin(initParams);
    plugin.walletService = walletService;
    NotificationContext ctx = NotificationContextImpl.cloneInstance();

    RewardReport rewardReport = new RewardReport();
    RewardSettings rewardSettings = new RewardSettings();
    RewardPeriod rewardPeriod = RewardPeriod.getCurrentPeriod(rewardSettings);
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
    assertTrue(plugin.isValid(ctx));

    NotificationInfo notification = plugin.buildNotification(ctx);
    assertNotNull(notification);
    assertEquals(String.valueOf(rewardReport.getFailedTransactionCount()),
                 notification.getValueOwnerParameter(REWARD_FAIL_COUNT));
    assertEquals(String.valueOf(rewardReport.getSuccessTransactionCount()),
                 notification.getValueOwnerParameter(REWARD_SUCCESS_COUNT));
    assertEquals(String.valueOf(rewardReport.getPendingTransactionCount()),
                 notification.getValueOwnerParameter(REWARD_PENDING_COUNT));
    assertEquals(String.valueOf(rewardReport.getTransactionsCount()),
                 notification.getValueOwnerParameter(REWARD_TRANSACTION_COUNT));
    assertEquals(String.valueOf(rewardReport.getValidRewardCount()),
                 notification.getValueOwnerParameter(REWARD_VALID_MEMBERS_COUNT));
    assertEquals(String.valueOf(rewardReport.getTokensSent()), notification.getValueOwnerParameter(AMOUNT));
  }
}
