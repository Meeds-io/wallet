package org.exoplatform.wallet.reward.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.api.RewardPlugin;
import org.exoplatform.wallet.reward.job.RewardStatusVerifierJob;
import org.exoplatform.wallet.reward.service.*;
import org.exoplatform.wallet.reward.storage.RewardReportStorage;
import org.exoplatform.wallet.reward.test.BaseWalletRewardTest;
import org.exoplatform.wallet.service.*;
import org.exoplatform.wallet.storage.WalletStorage;
import org.exoplatform.wallet.utils.RewardUtils;
import org.exoplatform.wallet.utils.WalletUtils;

public class WalletRewardJobTest extends BaseWalletRewardTest {

  @Test
  public void testGetRewardPlugins() {
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    assertEquals(2, rewardPlugins.size());

    RewardPeriod period = RewardPeriodType.WEEK.getPeriodOfTime(RewardUtils.timeFromSeconds(System.currentTimeMillis() / 1000));
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
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    RewardTeamService rewardTeamService = getService(RewardTeamService.class);
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    RewardReportStorage rewardReportStorage = getService(RewardReportStorage.class);

    WalletRewardReportService walletRewardService = new WalletRewardReportService(walletAccountService,
                                                                                  rewardSettingsService,
                                                                                  rewardTeamService,
                                                                                  rewardReportStorage);
    WalletTokenAdminService tokenAdminService = Mockito.mock(WalletTokenAdminService.class);
    resetTokenAdminService(walletTransactionService, tokenAdminService, true, false);

    int contractDecimals = WalletUtils.getContractDetail().getDecimals();
    long startDateInSeconds = RewardUtils.timeToSecondsAtDayStart(YearMonth.of(2019, 07)
                                                                           .atEndOfMonth()
                                                                           .atStartOfDay());

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
      Mockito.when(tokenAdminService.balanceOf(Mockito.eq("adminAddress")))
             .thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1).pow(contractDecimals));
      walletRewardService.sendRewards(startDateInSeconds, "root");
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

      RewardReport rewardReport = walletRewardService.computeRewards(startDateInSeconds);
      Set<WalletReward> rewards = rewardReport.getRewards();
      for (WalletReward walletReward : rewards) {
        String hash = walletReward.getTransaction().getHash();
        TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(hash);
        transactionDetail.setPending(false);
        transactionDetail.setSucceeded(true);
        walletTransactionService.saveTransactionDetail(transactionDetail, true);
      }

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
    Mockito.when(tokenAdminService.getAdminLevel(Mockito.eq("adminAddress"))).thenReturn(2);
    Mockito.when(tokenAdminService.reward(Mockito.any(), Mockito.any())).thenAnswer(new Answer<TransactionDetail>() {
      @Override
      public TransactionDetail answer(InvocationOnMock invocation) throws Throwable {
        TransactionDetail transactionDetail = invocation.getArgumentAt(0, TransactionDetail.class);
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
