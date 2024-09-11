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
package org.exoplatform.wallet.reward.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import io.meeds.gamification.service.RealizationService;
import org.exoplatform.wallet.model.WalletState;
import org.junit.jupiter.api.Test;

import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.storage.WalletRewardReportStorage;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTokenAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { WalletRewardReportService.class })
public class WalletRewardReportServiceTest { // NOSONAR

  @MockBean
  private WalletAccountService      walletAccountService;

  @MockBean
  private WalletTokenAdminService   walletTokenAdminService;

  @MockBean
  private RewardSettingsService     rewardSettingsService;

  @MockBean
  private RewardTeamService         rewardTeamService;

  @MockBean
  private WalletRewardReportStorage rewardReportStorage;

  @MockBean
  private RealizationService        realizationService;

  @Autowired
  private RewardReportService       rewardReportService;

  @Test
  public void testComputeRewards() {
    LocalDate date = YearMonth.of(2019, 3).atEndOfMonth();
    RewardSettings rewardSettings = new RewardSettings();
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);
    RewardReport rewardReport = rewardReportService.computeRewards(date);
    assertNotNull(rewardReport);
    assertNotNull(rewardReport.getRewards());
    assertEquals(0, rewardReport.getRewards().size());

    int enabledWalletsCount = 59;
    Set<Wallet> wallets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    IntStream.range(0, enabledWalletsCount).parallel().forEach(i -> {
      Wallet wallet = newWallet(i + 1L);
      wallets.add(wallet);
    });
    when(walletAccountService.listWallets()).thenReturn(wallets);

    rewardReport = rewardReportService.computeRewards(date);
    assertNotNull(rewardReport);
    // Even if settings are null, the returned rewards shouldn't be empty
    assertEquals(enabledWalletsCount, rewardReport.getRewards().size());
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
