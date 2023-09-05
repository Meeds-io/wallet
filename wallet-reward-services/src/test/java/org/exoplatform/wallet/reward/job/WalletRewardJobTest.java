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
package org.exoplatform.wallet.reward.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.RewardBudgetType;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardPluginSettings;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.BaseWalletRewardTest;
import org.exoplatform.wallet.reward.api.RewardPlugin;
import org.exoplatform.wallet.reward.service.WalletRewardReportService;
import org.exoplatform.wallet.reward.service.WalletRewardSettingsService;
import org.exoplatform.wallet.reward.service.WalletRewardTeamService;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTokenAdminService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.storage.WalletStorage;
import org.exoplatform.wallet.utils.WalletUtils;

public class WalletRewardJobTest extends BaseWalletRewardTest {

  @Test
  public void testGetRewardPlugins() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    assertEquals(2, rewardPlugins.size());

    RewardPeriod period = RewardPeriodType.WEEK.getPeriodOfTime(LocalDate.now(), ZoneId.systemDefault());
    for (RewardPlugin rewardPlugin : rewardPlugins) {
      try {
        rewardPlugin.getEarnedPoints(Collections.singleton(1l), period.getStartDateInSeconds(), period.getEndDateInSeconds());
        // Submodules aren't loaded yet
      } catch (Exception e) {
        // Expected
      }
    }
  }

  @Test
  public void testSendRewards() throws Exception {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);

    WalletRewardReportService walletRewardService = getService(WalletRewardReportService.class);
    WalletTokenAdminService tokenAdminService = Mockito.mock(WalletTokenAdminService.class);
    resetTokenAdminService(walletTransactionService, tokenAdminService, true, false);

    int contractDecimals = WalletUtils.getContractDetail().getDecimals();
    LocalDate date = YearMonth.of(2019, 07).atEndOfMonth();

    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    rewardSettingsService.registerPlugin(CUSTOM_REWARD_PLUGIN);
    try {
      // Build new settings
      RewardSettings newSettings = cloneSettings(rewardSettingsService.getSettings());
      Set<RewardPluginSettings> newPluginSettings = newSettings.getPluginSettings();

      newSettings.setPeriodType(RewardPeriodType.MONTH);
      RewardPluginSettings customPluginSetting = newPluginSettings.stream()
                                                                  .filter(plugin -> CUSTOM_PLUGIN_ID.equals(plugin.getPluginId()))
                                                                  .findFirst()
                                                                  .orElse(null);
      double sumOfTokensToSend = 5490d;
      customPluginSetting.setUsePools(true); // NOSONAR
      customPluginSetting.setBudgetType(RewardBudgetType.FIXED);
      customPluginSetting.setAmount(sumOfTokensToSend);
      customPluginSetting.setThreshold(0);
      customPluginSetting.setEnabled(true);
      rewardSettingsService.saveSettings(newSettings);

      WalletStorage walletStorage = getService(WalletStorage.class);
      int enabledWalletsCount = 60;
      for (int i = 0; i < enabledWalletsCount; i++) {
        Wallet wallet = newWallet(i + 1l);
        wallet = walletStorage.saveWallet(wallet, true);
        updateWalletBlockchainState(wallet);
        walletStorage.saveWalletBlockchainState(wallet, WalletUtils.getContractAddress());
        entitiesToClean.add(wallet);
      }

      // Build teams
      List<RewardTeam> teams = new ArrayList<>();
      RewardTeam rewardTeam1 = createTeamWithMembers(1, 10, RewardBudgetType.COMPUTED, false);
      teams.add(rewardTeam1);
      RewardTeam rewardTeam2 = createTeamWithMembers(11, 20, RewardBudgetType.FIXED, false);
      teams.add(rewardTeam2);
      RewardTeam rewardTeam3 = createTeamWithMembers(21, 30, RewardBudgetType.FIXED_PER_MEMBER, false);
      teams.add(rewardTeam3);
      RewardTeam rewardTeam4 = createTeamWithMembers(31, 40, RewardBudgetType.FIXED_PER_MEMBER, false);
      teams.add(rewardTeam4);
      RewardTeam rewardTeam5 = createTeamWithMembers(41, 50, RewardBudgetType.COMPUTED, false);
      teams.add(rewardTeam5);
      RewardTeam rewardTeam6 = createTeamWithMembers(51, 60, RewardBudgetType.COMPUTED, false);
      teams.add(rewardTeam6);

      List<RewardPeriod> initialRewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(initialRewardPeriodsInProgress);

      // Admin having enough funds
      Mockito.when(tokenAdminService.getTokenBalanceOf("adminAddress"))
             .thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1).pow(contractDecimals));
      walletRewardService.sendRewards(date, "root");
      Mockito.verify(tokenAdminService, Mockito.times(60)).reward(Mockito.any(), Mockito.any());

      List<RewardPeriod> rewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(initialRewardPeriodsInProgress.size() + 1l, rewardPeriodsInProgress.size());

      RewardStatusVerifierJob rewardReportNotificationJob = new RewardStatusVerifierJob();

      // Executing job shouldn't mark reward transactions as sent until
      // transactions are marked as success and not pending
      rewardReportNotificationJob.execute(null);
      rewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(initialRewardPeriodsInProgress.size() + 1l, rewardPeriodsInProgress.size());

      RewardReport rewardReport = walletRewardService.computeRewards(date);
      Set<WalletReward> rewards = rewardReport.getRewards();
      for (WalletReward walletReward : rewards) {
        String hash = walletReward.getTransaction().getHash();
        TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(hash);
        transactionDetail.setPending(false);
        transactionDetail.setSucceeded(true);
        walletTransactionService.saveTransactionDetail(transactionDetail, true);
      }

      WalletAccountService walletAccountServiceMock = mock(WalletAccountService.class);
      when(walletAccountServiceMock.isAdminAccountEnabled()).thenReturn(true);

      rewardReportNotificationJob.execute(null);

      rewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(1, rewardPeriodsInProgress.size());

      rewardReportNotificationJob.walletAccountService = walletAccountServiceMock;
      Wallet adminWallet = new Wallet();
      when(walletAccountServiceMock.getAdminWallet()).thenReturn(adminWallet);
      adminWallet.setEtherBalance(2d);
      adminWallet.setTokenBalance(3d);
      adminWallet.setEnabled(true);

      rewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(1, rewardPeriodsInProgress.size());

      walletRewardService.setRewardSendingInProgress(true);
      rewardReportNotificationJob.execute(null);

      rewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(1, rewardPeriodsInProgress.size());

      walletRewardService.setRewardSendingInProgress(false);
      rewardReportNotificationJob.execute(null);

      rewardPeriodsInProgress = walletRewardService.getRewardPeriodsInProgress();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(0, rewardPeriodsInProgress.size());
    } finally {
      rewardSettingsService.unregisterPlugin(CUSTOM_PLUGIN_ID);
      rewardSettingsService.saveSettings(defaultSettings);
    }
  }

  private void resetTokenAdminService(WalletTransactionService walletTransactionService,
                                      WalletTokenAdminService tokenAdminService,
                                      boolean pendingTransactions,
                                      boolean successTransactions) throws Exception { // NOSONAR
    if (container.getComponentInstanceOfType(WalletTokenAdminService.class) != null) {
      container.unregisterComponent(WalletTokenAdminService.class);
    }
    container.registerComponentInstance(WalletTokenAdminService.class, tokenAdminService);
    Mockito.reset(tokenAdminService);
    Mockito.when(tokenAdminService.getAdminWalletAddress()).thenReturn("adminAddress");
    Mockito.when(tokenAdminService.reward(Mockito.any(), Mockito.any())).thenAnswer(new Answer<TransactionDetail>() {
      @Override
      public TransactionDetail answer(InvocationOnMock invocation) throws Throwable {
        TransactionDetail transactionDetail = invocation.getArgument(0, TransactionDetail.class);
        transactionDetail.setHash(generateTransactionHash());
        transactionDetail.setPending(pendingTransactions);
        transactionDetail.setSucceeded(successTransactions);
        transactionDetail.setContractMethodName("reward");
        RequestLifeCycle.begin(container);
        try {
          walletTransactionService.saveTransactionDetail(transactionDetail, false);
        } finally {
          RequestLifeCycle.end();
        }
        entitiesToClean.add(transactionDetail);
        return transactionDetail;
      }
    });
  }

  private RewardTeam createTeamWithMembers(int startInclusive,
                                           int endInclusive,
                                           RewardBudgetType budgetType,
                                           boolean disabled) {
    WalletRewardTeamService rewardTeamService = getService(WalletRewardTeamService.class);
    long[] memberIds = LongStream.rangeClosed(startInclusive, endInclusive).toArray();
    RewardTeam rewardTeam = newRewardTeam(memberIds);
    rewardTeam.setName("Name" + startInclusive);
    rewardTeam.setDisabled(disabled);
    rewardTeam.setRewardType(budgetType);
    double totalBudget = (double) LongStream.rangeClosed(startInclusive, endInclusive).sum() * 3d;
    if (budgetType == RewardBudgetType.COMPUTED || budgetType == RewardBudgetType.FIXED) {
      rewardTeam.setBudget(totalBudget);
    } else {
      rewardTeam.setBudget(totalBudget / memberIds.length);
    }
    rewardTeam = rewardTeamService.saveTeam(rewardTeam);
    entitiesToClean.add(rewardTeam);
    return rewardTeam;
  }

}
