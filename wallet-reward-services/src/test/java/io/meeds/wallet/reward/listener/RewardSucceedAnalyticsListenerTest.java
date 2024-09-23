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
package io.meeds.wallet.reward.listener;

import java.util.HashSet;
import java.util.Set;

import io.meeds.wallet.wallet.model.reward.RewardPeriod;
import io.meeds.wallet.wallet.model.reward.RewardPeriodType;
import io.meeds.wallet.wallet.model.reward.WalletReward;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import io.meeds.wallet.wallet.model.reward.RewardReport;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { RewardSucceedAnalyticsListener.class, })
class RewardSucceedAnalyticsListenerTest {

  @MockBean
  private ListenerService                    listenerService;

  @MockBean
  private Event<RewardReport, Object>  event;

  @Autowired
  private RewardSucceedAnalyticsListener               rewardSucceedAnalyticsListener;


  @Test
  void createEvent() {
    RewardReport rewardReport = mock(RewardReport.class);
    RewardPeriod rewardPeriod = mock(RewardPeriod.class);
    when(rewardReport.getPeriod()).thenReturn(rewardPeriod);
    when(rewardPeriod.getStartDateInSeconds()).thenReturn(1609459200L);
    when(rewardPeriod.getEndDateInSeconds()).thenReturn(1612137600L);
    when(rewardPeriod.getTimeZone()).thenReturn("UTC");
    when(rewardPeriod.getRewardPeriodType()).thenReturn(RewardPeriodType.MONTH);
    when(rewardReport.getSuccessTransactionCount()).thenReturn(10L);
    when(rewardReport.getTokensSent()).thenReturn(500.0);
    when(rewardReport.getTokensToSend()).thenReturn(1000.0);
    when(rewardReport.getValidRewardCount()).thenReturn(5L);

    Set<WalletReward> rewards = new HashSet<>();
    rewards.add(new WalletReward());
    when(rewardReport.getRewards()).thenReturn(rewards);

    when(event.getSource()).thenReturn(rewardReport);

    try (MockedStatic<AnalyticsUtils> mockedAnalyticsUtils = Mockito.mockStatic(AnalyticsUtils.class)) {
      rewardSucceedAnalyticsListener.onEvent(event);
      ArgumentCaptor<StatisticData> captor = ArgumentCaptor.forClass(StatisticData.class);
      mockedAnalyticsUtils.verify(() -> AnalyticsUtils.addStatisticData(captor.capture()), times(1));

      StatisticData actualStatisticData = captor.getValue();

      // Verify the content of the StatisticData
      assertEquals("wallet", actualStatisticData.getModule());
      assertEquals("reward", actualStatisticData.getSubModule());
      assertEquals("sendPeriodRewards", actualStatisticData.getOperation());
      assertEquals("UTC", actualStatisticData.getParameters().get("rewardPeriodTimeZone"));
      assertEquals("month", actualStatisticData.getParameters().get("rewardPeriodType"));
      assertEquals("10", actualStatisticData.getParameters().get("rewardTransactionsCount"));
      assertEquals("500.0", actualStatisticData.getParameters().get("rewardTokensSent"));
      assertEquals("1000.0", actualStatisticData.getParameters().get("rewardTokensToSend"));
      assertEquals("5", actualStatisticData.getParameters().get("rewardRecipientWalletCount"));
      assertEquals("1", actualStatisticData.getParameters().get("rewardParticipantWalletCount"));
    }
  }
}
