package org.exoplatform.addon.wallet.reward.test.service;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.reward.service.*;
import org.exoplatform.addon.wallet.reward.storage.RewardReportStorage;
import org.exoplatform.addon.wallet.reward.test.BaseWalletRewardTest;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.addon.wallet.utils.RewardUtils;
import org.exoplatform.addon.wallet.utils.WalletUtils;
import org.exoplatform.container.component.RequestLifeCycle;

public class WalletRewardReportServiceTest extends BaseWalletRewardTest {

  /**
   * Check that service is instantiated
   */
  @Test
  public void testServiceInstantiated() {
    WalletRewardReportService walletRewardService = getService(WalletRewardReportService.class);
    assertNotNull(walletRewardService);
  }

  @Test
  public void testComputeRewards() {
    WalletRewardReportService walletRewardService = getService(WalletRewardReportService.class);
    long startDateInSeconds = RewardUtils.timeToSecondsAtDayStart(YearMonth.of(2019, 03)
                                                                           .atEndOfMonth()
                                                                           .atStartOfDay());
    RewardReport rewardReport = walletRewardService.computeRewards(startDateInSeconds);
    assertNotNull(rewardReport);
    assertNotNull(rewardReport.getRewards());
    assertEquals(0, rewardReport.getRewards().size());

    WalletAccountService accountService = getService(WalletAccountService.class);
    int enabledWalletsCount = 60;
    for (int i = 0; i < enabledWalletsCount; i++) {
      Wallet wallet = newWallet(i + 1l);
      wallet = accountService.saveWallet(wallet, true);
      updateWalletBlockchainState(wallet);
      accountService.saveWalletBlockchainState(wallet, WalletUtils.getContractAddress());
      entitiesToClean.add(wallet);
    }

    rewardReport = walletRewardService.computeRewards(startDateInSeconds);
    assertNotNull(rewardReport);
    // Even if settings are null, the returned rewards shouldn't be empty
    assertEquals(enabledWalletsCount, rewardReport.getRewards().size());

    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    rewardSettingsService.registerPlugin(CUSTOM_REWARD_PLUGIN);
    try {
      // Build new settings
      RewardSettings newSettings = cloneSettings(rewardSettingsService.getSettings());

      Set<RewardPluginSettings> newPluginSettings = newSettings.getPluginSettings();
      long amount = 3l;

      newSettings.setPeriodType(RewardPeriodType.MONTH);

      RewardPluginSettings customPluginSetting = newPluginSettings.stream()
                                                                  .filter(plugin -> CUSTOM_PLUGIN_ID.equals(plugin.getPluginId()))
                                                                  .findFirst()
                                                                  .orElse(null);

      assertNotNull(customPluginSetting);

      customPluginSetting.setAmount(amount); // NOSONAR
      customPluginSetting.setBudgetType(RewardBudgetType.FIXED_PER_POINT);
      customPluginSetting.setThreshold(0);
      customPluginSetting.setEnabled(true);
      customPluginSetting.setUsePools(false);
      rewardSettingsService.saveSettings(newSettings);

      // Check computed amount for plugin per wallet when no teams and with
      // fixed
      // budget per point
      double sumOfTokensToSend = checkComputedRewards(walletRewardService,
                                                      startDateInSeconds,
                                                      enabledWalletsCount,
                                                      amount);

      customPluginSetting.setBudgetType(RewardBudgetType.FIXED);
      customPluginSetting.setAmount(sumOfTokensToSend);
      rewardSettingsService.saveSettings(newSettings);

      // Check computed amount for plugin per wallet when no teams and with
      // fixed total budget for reward plugin
      checkComputedRewards(walletRewardService,
                           startDateInSeconds,
                           enabledWalletsCount,
                           amount);

      customPluginSetting.setBudgetType(RewardBudgetType.FIXED_PER_MEMBER);
      customPluginSetting.setAmount(sumOfTokensToSend / enabledWalletsCount);
      rewardSettingsService.saveSettings(newSettings);

      // Check computed amount for plugin per wallet when no teams and with
      // fixed budget per member
      checkComputedRewards(walletRewardService,
                           startDateInSeconds,
                           enabledWalletsCount,
                           amount);

      customPluginSetting.setThreshold(31);
      customPluginSetting.setAmount(amount); // NOSONAR
      customPluginSetting.setBudgetType(RewardBudgetType.FIXED_PER_POINT);
      rewardSettingsService.saveSettings(newSettings);

      // Check computed amount for plugin per wallet when no teams and with
      // fixed budget per point and with Threshold
      checkComputedRewards(walletRewardService,
                           startDateInSeconds,
                           30,
                           amount);

      customPluginSetting.setThreshold(0);
      rewardSettingsService.saveSettings(newSettings);

      RewardTeam rewardTeam1 = createTeamWithMembers(1, 10, RewardBudgetType.COMPUTED, true);
      RewardTeam rewardTeam2 = createTeamWithMembers(11, 20, RewardBudgetType.FIXED, true);
      RewardTeam rewardTeam3 = createTeamWithMembers(21, 30, RewardBudgetType.FIXED_PER_MEMBER, true);
      RewardTeam rewardTeam4 = createTeamWithMembers(31, 40, RewardBudgetType.FIXED_PER_MEMBER, true);
      RewardTeam rewardTeam5 = createTeamWithMembers(41, 50, RewardBudgetType.COMPUTED, true);
      RewardTeam rewardTeam6 = createTeamWithMembers(51, 60, RewardBudgetType.COMPUTED, true);

      List<RewardTeam> teams = new ArrayList<>();
      teams.add(rewardTeam1);
      teams.add(rewardTeam2);
      teams.add(rewardTeam3);
      teams.add(rewardTeam4);
      teams.add(rewardTeam5);
      teams.add(rewardTeam6);

      // Check computed amount for plugin per wallet with teams (each having 10
      // members), with fixed budget per point and with plugin not using teams
      checkComputedRewards(walletRewardService,
                           startDateInSeconds,
                           enabledWalletsCount,
                           amount);

      customPluginSetting.setUsePools(true);
      customPluginSetting.setBudgetType(RewardBudgetType.FIXED);
      customPluginSetting.setAmount(sumOfTokensToSend);
      rewardSettingsService.saveSettings(newSettings);

      // Check computed amount for plugin per wallet with teams (each having 10
      // members), with fixed total budget and with plugin using teams that are
      // disabled
      checkComputedRewards(walletRewardService,
                           startDateInSeconds,
                           0,
                           0);

      // Check computed amount for plugin per wallet with teams (each having 10
      // members), with fixed total budget and with plugin using teams that are
      // enabled
      WalletRewardTeamService rewardTeamService = getService(WalletRewardTeamService.class);
      teams.forEach(team -> {
        team.setDisabled(false);
        rewardTeamService.saveTeam(team);
      });

      rewardReport = walletRewardService.computeRewards(startDateInSeconds);

      // check total budget to send
      double tokensToSend = rewardReport.getRewards().stream().mapToDouble(WalletReward::getTokensToSend).sum();
      assertEquals(sumOfTokensToSend, tokensToSend, 0);

      // Check budget of team having a defined budget as 'fixed'
      double tokensSentToTeam2 = rewardReport.getRewards()
                                             .stream()
                                             .filter(walletReward -> walletReward.getTeam() != null
                                                 && rewardTeam2.getId().equals(walletReward.getTeam().getId()))
                                             .mapToDouble(WalletReward::getTokensToSend)
                                             .sum();
      assertEquals(rewardTeam2.getBudget(), tokensSentToTeam2, 0);

      // Check budget of teams (team3 and team4) having a defined budget as
      // 'fixed per member'
      double tokensSentToTeam3 = rewardReport.getRewards()
                                             .stream()
                                             .filter(walletReward -> walletReward.getTeam() != null
                                                 && rewardTeam3.getId().equals(walletReward.getTeam().getId()))
                                             .mapToDouble(WalletReward::getTokensToSend)
                                             .sum();
      assertEquals(rewardTeam3.getBudget() * rewardTeam3.getMembers().size(), tokensSentToTeam3, 0);

      double tokensSentToTeam4 = rewardReport.getRewards()
                                             .stream()
                                             .filter(walletReward -> walletReward.getTeam() != null
                                                 && rewardTeam4.getId().equals(walletReward.getTeam().getId()))
                                             .mapToDouble(WalletReward::getTokensToSend)
                                             .sum();
      assertEquals(rewardTeam4.getBudget() * rewardTeam4.getMembers().size(), tokensSentToTeam4, 0);

      // Check budget of other teams (team1, team5 and team2) having a defined
      // budget as 'computed'
      double tokensSentToOtherTeams = sumOfTokensToSend - tokensSentToTeam2 - tokensSentToTeam3 - tokensSentToTeam4;
      double tokensSentToOtherTeam = tokensSentToOtherTeams / 3d;

      double tokensSentToTeam1 = rewardReport.getRewards()
                                             .stream()
                                             .filter(walletReward -> walletReward.getTeam() != null
                                                 && rewardTeam1.getId().equals(walletReward.getTeam().getId()))
                                             .mapToDouble(WalletReward::getTokensToSend)
                                             .sum();
      assertEquals(tokensSentToOtherTeam, tokensSentToTeam1, 0);

      double tokensSentToTeam5 = rewardReport.getRewards()
                                             .stream()
                                             .filter(walletReward -> walletReward.getTeam() != null
                                                 && rewardTeam5.getId().equals(walletReward.getTeam().getId()))
                                             .mapToDouble(WalletReward::getTokensToSend)
                                             .sum();
      assertEquals(tokensSentToOtherTeam, tokensSentToTeam5, 0);

      double tokensSentToTeam6 = rewardReport.getRewards()
                                             .stream()
                                             .filter(walletReward -> walletReward.getTeam() != null
                                                 && rewardTeam6.getId().equals(walletReward.getTeam().getId()))
                                             .mapToDouble(WalletReward::getTokensToSend)
                                             .sum();
      assertEquals(tokensSentToOtherTeam, tokensSentToTeam6, 0);
    } finally {
      rewardSettingsService.unregisterPlugin(CUSTOM_PLUGIN_ID);
      rewardSettingsService.saveSettings(defaultSettings);
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
    resetTokenAdminService(walletTransactionService, tokenAdminService, false, true);

    int contractDecimals = WalletUtils.getContractDetail().getDecimals();
    long startDateInSeconds = RewardUtils.timeToSecondsAtDayStart(YearMonth.of(2019, 04)
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

      WalletAccountService accountService = getService(WalletAccountService.class);
      int enabledWalletsCount = 60;
      for (int i = 0; i < enabledWalletsCount; i++) {
        Wallet wallet = newWallet(i + 1l);
        wallet = accountService.saveWallet(wallet, true);
        updateWalletBlockchainState(wallet);
        accountService.saveWalletBlockchainState(wallet, WalletUtils.getContractAddress());
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

      // Admin having only 10 tokens
      Mockito.when(tokenAdminService.balanceOf(Mockito.eq("adminAddress")))
             .thenReturn(BigInteger.valueOf(10l).pow(contractDecimals));
      try {
        walletRewardService.sendRewards(startDateInSeconds, "root");
        fail("Shouldn't send funds when admin not having enough funds");
      } catch (Exception e) {
        // Expected
      }
      Mockito.verify(tokenAdminService, Mockito.times(0)).reward(Mockito.any(), Mockito.any());

      // Admin having enough funds
      Mockito.when(tokenAdminService.balanceOf(Mockito.eq("adminAddress")))
             .thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1).pow(contractDecimals));
      walletRewardService.sendRewards(startDateInSeconds, "root");
      Mockito.verify(tokenAdminService, Mockito.times(60)).reward(Mockito.any(), Mockito.any());

      // Send reward for the second time for the same period
      resetTokenAdminService(walletTransactionService, tokenAdminService, false, true);
      Mockito.when(tokenAdminService.balanceOf(Mockito.eq("adminAddress")))
             .thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1).pow(contractDecimals));
      try {
        walletRewardService.sendRewards(startDateInSeconds, "root");
      } catch (Exception e) {
        // Expected, no rewards to send
      }
      Mockito.verify(tokenAdminService, Mockito.times(0)).reward(Mockito.any(), Mockito.any());
    } finally {
      rewardSettingsService.unregisterPlugin(CUSTOM_PLUGIN_ID);
      rewardSettingsService.saveSettings(defaultSettings);
    }
  }

  @Test
  public void testSaveRewardReport() {
    RewardReport rewardReport = new RewardReport();
    LocalDateTime date = LocalDateTime.now();

    WalletAccountService accountService = getService(WalletAccountService.class);

    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    RewardSettings defaultSettings = rewardSettingsService.getSettings();

    RewardPeriod period = defaultSettings.getPeriodType().getPeriodOfTime(date);
    rewardReport.setPeriod(period);
    Set<WalletReward> rewards = new HashSet<>();
    for (int i = 0; i < 20; i++) {
      Wallet wallet = newWallet(i + 1l);
      wallet = accountService.saveWallet(wallet, true);
      updateWalletBlockchainState(wallet);
      accountService.saveWalletBlockchainState(wallet, WalletUtils.getContractAddress());
      entitiesToClean.add(wallet);

      WalletReward walletReward = new WalletReward();
      walletReward.setWallet(wallet);
      walletReward.setPeriod(period);
      Set<WalletPluginReward> pluginRewards = new HashSet<>();
      for (int j = 0; j < 10; j++) {
        WalletPluginReward pluginReward = new WalletPluginReward();
        pluginReward.setAmount(5);
        pluginReward.setIdentityId(wallet.getTechnicalId());
        pluginReward.setPluginId("plugin" + j);
        pluginReward.setPoints(5);
        pluginReward.setPoolsUsed(false);
        pluginRewards.add(pluginReward);
      }
      walletReward.setRewards(pluginRewards);
      rewards.add(walletReward);
    }
    rewardReport.setRewards(rewards);

    RewardReportService rewardReportService = getService(RewardReportService.class);
    rewardReportService.saveRewardReport(rewardReport);

    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);

    RewardReport savedRewardReport = rewardReportService.getRewardReport(date.toEpochSecond(ZoneOffset.UTC));
    assertEquals(rewardReport, savedRewardReport);
  }

  @Test
  public void testListRewards() throws Exception {
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
    resetTokenAdminService(walletTransactionService, tokenAdminService, false, true);

    int contractDecimals = WalletUtils.getContractDetail().getDecimals();
    long startDateInSeconds = RewardUtils.timeToSecondsAtDayStart(YearMonth.of(2019, 05)
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

      WalletAccountService accountService = getService(WalletAccountService.class);
      int enabledWalletsCount = 60;
      for (int i = 0; i < enabledWalletsCount; i++) {
        Wallet wallet = newWallet(i + 1l);
        wallet = accountService.saveWallet(wallet, true);
        updateWalletBlockchainState(wallet);
        accountService.saveWalletBlockchainState(wallet, WalletUtils.getContractAddress());
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

      List<WalletReward> walletRewards = walletRewardService.listRewards("root3", 10);
      assertNotNull(walletRewards);
      assertEquals(0, walletRewards.size());

      // Admin having enough funds
      Mockito.when(tokenAdminService.balanceOf(Mockito.eq("adminAddress")))
             .thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1).pow(contractDecimals));
      walletRewardService.sendRewards(startDateInSeconds, "root");
      Mockito.verify(tokenAdminService, Mockito.times(60)).reward(Mockito.any(), Mockito.any());

      walletRewards = walletRewardService.listRewards("root3", 10);
      assertNotNull(walletRewards);
      assertEquals(1, walletRewards.size());

      WalletReward walletReward = walletRewards.get(0);
      assertNotNull(walletReward);
      assertNotNull(walletReward.getPeriod());
      assertNotNull(walletReward.getTransaction());
      assertEquals(RewardUtils.TRANSACTION_STATUS_SUCCESS, walletReward.getStatus());
      assertNotNull(walletReward.getWallet());
      assertEquals(3, walletReward.getIdentityId());
      assertEquals(3, walletReward.getWallet().getTechnicalId());
      assertNotNull(walletReward.getTeam());
      assertNotNull(walletReward.getTeams());
      assertEquals(1, walletReward.getTeams().size());

      assertNotNull(walletReward.getRewards());
      assertEquals(1, walletReward.getRewards().size());
      WalletPluginReward pluginReward = walletReward.getRewards().iterator().next();
      assertNotNull(pluginReward);
      assertEquals(CUSTOM_PLUGIN_ID, pluginReward.getPluginId());
      assertEquals(3, pluginReward.getPoints(), 0);
      assertEquals(3, pluginReward.getIdentityId());
      assertEquals(58, pluginReward.getAmount(), 0.1);
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

  private double checkComputedRewards(WalletRewardReportService walletRewardService,
                                      long startDateInSeconds,
                                      int enabledWalletsCount,
                                      long amount) {
    double sumOfTokensToSend = 0;
    RewardReport rewardReport = walletRewardService.computeRewards(startDateInSeconds);
    assertNotNull(rewardReport);
    Set<WalletReward> rewards = rewardReport.getRewards();
    assertNotNull(rewards);

    assertEquals(enabledWalletsCount, rewardReport.getValidRewardCount());
    for (WalletReward walletReward : rewardReport.getValidRewards()) {
      assertNotNull(walletReward);
      assertNotNull(walletReward.getWallet());
      assertNotNull(walletReward.getRewards());
      assertEquals(1, walletReward.getRewards().size());
      assertEquals(0, walletReward.getTokensSent(), 0);
      long tokensToSend = walletReward.getIdentityId() * amount;
      sumOfTokensToSend += tokensToSend;
      assertEquals("Wallet '" + walletReward + "' has an unexpected reward amount",
                   tokensToSend,
                   walletReward.getTokensToSend(),
                   0);
    }
    return sumOfTokensToSend;
  }

}
