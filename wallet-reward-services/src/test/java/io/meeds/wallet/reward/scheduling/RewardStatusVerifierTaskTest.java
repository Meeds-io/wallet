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
import io.meeds.wallet.reward.scheduling.task.RewardStatusVerifierTask;
import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wallet.service.WalletAccountService;
import org.exoplatform.services.listener.ListenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static io.meeds.wallet.utils.RewardUtils.REWARD_SUCCESS_EVENT_NAME;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { RewardStatusVerifierTask.class })
class RewardStatusVerifierTaskTest {

  @MockBean
  private RewardReportService      rewardReportService;

  @MockBean
  private WalletAccountService     walletAccountService;

  @MockBean
  private ListenerService          listenerService;

  @Autowired
  private RewardStatusVerifierTask rewardStatusVerifierTask;

  @Test
  void testSendRewards_NoAdminWallet() {
    // No admin wallet or admin wallet has zero balance
    when(walletAccountService.getAdminWallet()).thenReturn(null);
    when(walletAccountService.isAdminAccountEnabled()).thenReturn(true);

    rewardStatusVerifierTask.execute();

    // Verify no interactions with rewardReportService or listenerService
    verify(rewardReportService, never()).getRewardPeriodsInProgress();
    verify(listenerService, never()).broadcast(anyString(), any(), any());
    }

  @Test
  void testSendRewards_RewardPeriodsInProgress() {
    // When
    Wallet adminWallet = new Wallet();
    adminWallet.setEtherBalance(100.0);
    adminWallet.setTokenBalance(100.0);

    when(walletAccountService.getAdminWallet()).thenReturn(adminWallet);
    when(walletAccountService.isAdminAccountEnabled()).thenReturn(true);

    RewardPeriod rewardPeriod = new RewardPeriod();
    List<RewardPeriod> rewardPeriodsInProgress = List.of(rewardPeriod);
    when(rewardReportService.getRewardPeriodsInProgress()).thenReturn(rewardPeriodsInProgress);

    RewardReport rewardReport = mock(RewardReport.class);
    when(rewardReport.isCompletelyProceeded()).thenReturn(true);

    when(rewardReportService.computeRewards(any())).thenReturn(rewardReport);

    rewardStatusVerifierTask.execute();

    // Then
    verify(rewardReportService, times(1)).computeRewards(any());
    verify(listenerService, times(1)).broadcast(REWARD_SUCCESS_EVENT_NAME, rewardReport, null);
    verify(rewardReportService, times(1)).saveRewardReport(any(RewardReport.class));
  }
}
