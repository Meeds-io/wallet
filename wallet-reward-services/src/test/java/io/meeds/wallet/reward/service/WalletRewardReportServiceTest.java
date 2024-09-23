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
package io.meeds.wallet.reward.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import io.meeds.gamification.service.RealizationService;
import io.meeds.wallet.wallet.model.WalletState;
import org.junit.jupiter.api.Test;

import io.meeds.wallet.wallet.model.Wallet;
import io.meeds.wallet.wallet.model.reward.RewardReport;
import io.meeds.wallet.wallet.model.reward.RewardSettings;
import io.meeds.wallet.reward.storage.WalletRewardReportStorage;
import io.meeds.wallet.wallet.service.WalletAccountService;
import io.meeds.wallet.wallet.service.WalletTokenAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { WalletRewardReportService.class })
public class WalletRewardReportServiceTest { // NOSONAR

  @MockBean
  private WalletAccountService      walletAccountService;

  @MockBean
  private WalletTokenAdminService   walletTokenAdminService;

  @MockBean
  private RewardSettingsService rewardSettingsService;

  @MockBean
  private WalletRewardReportStorage rewardReportStorage;

  @MockBean
  private RealizationService        realizationService;

  @Autowired
  private RewardReportService rewardReportService;

  @Test
  public void testComputeRewards() {
    LocalDate date = YearMonth.of(2019, 3).atEndOfMonth();
    RewardSettings rewardSettings = new RewardSettings();
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);
    RewardReport rewardReport = rewardReportService.computeRewards(date);
    assertNotNull(rewardReport);
    assertNotNull(rewardReport.getRewards());
    assertEquals(0, rewardReport.getRewards().size());

    when(realizationService.getParticipantsBetweenDates(any(Date.class), any(Date.class))).thenReturn(List.of(1L, 4L, 5L));
    Set<Wallet> participantsWallet = new HashSet<>();
    participantsWallet.add(newWallet(1L));
    participantsWallet.add(newWallet(4L));
    participantsWallet.add(newWallet(5L));
    when(walletAccountService.listWalletsByIdentityIds(List.of(1L, 4L, 5L))).thenReturn(participantsWallet);

    rewardReport = rewardReportService.computeRewards(date);
    assertNotNull(rewardReport);
    // Even if settings are null, the returned rewards shouldn't be empty
    assertEquals(participantsWallet.size(), rewardReport.getRewards().size());
  }

  protected Wallet newWallet(long identityId) {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(identityId);
    wallet.setAddress("walletAddress" + identityId);
    wallet.setPassPhrase("passphrase");
    wallet.setEnabled(true);
    wallet.setIsInitialized(true);
    wallet.setEtherBalance(0d);
    wallet.setTokenBalance(0d);
    wallet.setInitializationState(WalletState.INITIALIZED.name());
    return wallet;
  }
}
