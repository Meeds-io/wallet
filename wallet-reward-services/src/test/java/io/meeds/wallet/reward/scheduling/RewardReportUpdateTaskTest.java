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
package io.meeds.wallet.reward.scheduling;

import java.util.*;

import io.meeds.wallet.model.*;
import io.meeds.wallet.reward.scheduling.task.RewardReportUpdateTask;
import io.meeds.wallet.reward.service.RewardReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { RewardReportUpdateTask.class })
class RewardReportUpdateTaskTest {

    @MockBean
    private RewardReportService      rewardReportService;

    @Autowired
    private RewardReportUpdateTask rewardReportUpdateTask;


    @Test
    void executeWhenRewardSettingChanged() {

        RewardPeriod rewardPeriod = mock(RewardPeriod.class);
        Map<Long, Boolean> rewardSettingChanged = new HashMap<>();
        rewardSettingChanged.put(1L, true);
        rewardSettingChanged.put(2L, false);
        when(rewardReportService.getRewardSettingChanged()).thenReturn(rewardSettingChanged);

        // When
        when(rewardReportService.getRewardPeriodById(1L)).thenReturn(rewardPeriod);

        rewardReportUpdateTask.execute();

        // Then
        verify(rewardReportService, times(1)).getReport(rewardPeriod);
        verify(rewardReportService, times(1)).getRewardPeriodById(1L);
    }

    @Test
    void executeWhenRewardSettingNotChanged() {
        // When
        Map<Long, Boolean> rewardSettingChanged = new HashMap<>();
        rewardSettingChanged.put(1L, false);
        when(rewardReportService.getRewardSettingChanged()).thenReturn(rewardSettingChanged);

        rewardReportUpdateTask.execute();

        // Then
        verify(rewardReportService, never()).getReport(any());
    }
}
