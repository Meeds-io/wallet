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

import java.util.*;

import io.meeds.wallet.model.*;
import io.meeds.wallet.reward.service.RewardSettingsService;
import io.meeds.wallet.reward.service.WalletRewardReportService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import static io.meeds.wallet.reward.service.WalletRewardSettingsService.REWARD_SETTINGS_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { RewardReportUpdateListener.class, })
class RewardReportUpdateListenerTest {

  @MockBean
  private WalletRewardReportService          rewardReportService;

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
    when(event.getEventName()).thenReturn(REWARD_SETTINGS_UPDATED);

    RewardPeriod rewardPeriod1 = mock(RewardPeriod.class);
    RewardPeriod rewardPeriod2 = mock(RewardPeriod.class);
    when(rewardPeriod1.getId()).thenReturn(1L);
    when(rewardPeriod2.getId()).thenReturn(2L);
    List<RewardPeriod> rewardPeriods = Arrays.asList(rewardPeriod1, rewardPeriod2);
    when(rewardReportService.getRewardPeriodsNotSent()).thenReturn(rewardPeriods);

    Map<Long, Boolean> rewardSettingChangedMap = new HashMap<>();
    when(rewardReportService.getRewardSettingChanged()).thenReturn(rewardSettingChangedMap);

    rewardReportUpdateListener.onEvent(event);

    Map<Long, Boolean> expectedMap = new HashMap<>();
    expectedMap.put(1L, true);
    expectedMap.put(2L, true);

    verify(rewardReportService).getRewardPeriodsNotSent();
    verify(rewardReportService).getRewardSettingChanged();
    assertEquals(expectedMap, rewardSettingChangedMap);

    when(event.getEventName()).thenReturn("OTHER_EVENT");

    RewardSettings rewardSettings = mock(RewardSettings.class);
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);
    RewardPeriod rewardPeriod = mock(RewardPeriod.class);
    when(rewardPeriod.getId()).thenReturn(3L);
    try (MockedStatic<RewardPeriod> mockedRewardPeriod = mockStatic(RewardPeriod.class)) {
      mockedRewardPeriod.when(() -> RewardPeriod.getCurrentPeriod(rewardSettings)).thenReturn(rewardPeriod);

      rewardSettingChangedMap = new HashMap<>();
      when(rewardReportService.getRewardSettingChanged()).thenReturn(rewardSettingChangedMap);

      rewardReportUpdateListener.onEvent(event);

      rewardSettingChangedMap.put(3L, true);

      expectedMap = new HashMap<>();
      expectedMap.put(3L, true);

      verify(rewardSettingsService).getSettings();
      verify(rewardReportService, times(2)).getRewardSettingChanged();
      assertEquals(expectedMap, rewardSettingChangedMap);
    }
  }
}
