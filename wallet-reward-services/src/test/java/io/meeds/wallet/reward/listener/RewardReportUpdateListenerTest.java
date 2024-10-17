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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.meeds.wallet.model.*;
import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wallet.reward.service.RewardSettingsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;

import static io.meeds.wallet.reward.service.WalletRewardSettingsService.REWARD_SETTINGS_UPDATED;
import static io.meeds.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { RewardReportUpdateListener.class, })
class RewardReportUpdateListenerTest {

  @MockBean
  private RewardReportService                rewardReportService;

  @MockBean
  private RewardSettingsService              rewardSettingsService;

  @MockBean
  private ListenerService                    listenerService;

  @MockBean
  private Event<Object, Map<String, String>> event;

  @Autowired
  private RewardReportUpdateListener         rewardReportUpdateListener;

  @Test
  void onEvent() {
    RewardPeriod rewardPeriod = mock(RewardPeriod.class);
    when(rewardPeriod.getStartDateInSeconds()).thenReturn(1609459200L);
    when(rewardPeriod.getEndDateInSeconds()).thenReturn(1612137600L);
    when(rewardPeriod.getTimeZone()).thenReturn("UTC");
    when(rewardPeriod.getRewardPeriodType()).thenReturn(RewardPeriodType.WEEK);
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());

    when(event.getEventName()).thenReturn(REWARD_SETTINGS_UPDATED);
    when(rewardReportService.getRewardPeriodsNotSent()).thenReturn(List.of(rewardPeriod));
    RewardReport rewardReport = mock(RewardReport.class);

    when(rewardReport.getParticipationsCount()).thenReturn(0);
    when(rewardReportService.computeRewards(rewardPeriod.getPeriodMedianDate())).thenReturn(rewardReport);

    rewardReportUpdateListener.onEvent(event);
    verify(rewardReportService, times(0)).saveRewardReport(rewardReport);



    when(rewardReport.getParticipationsCount()).thenReturn(5);
    rewardReportUpdateListener.onEvent(event);
    verify(rewardReportService, times(1)).saveRewardReport(rewardReport);
  }
}
