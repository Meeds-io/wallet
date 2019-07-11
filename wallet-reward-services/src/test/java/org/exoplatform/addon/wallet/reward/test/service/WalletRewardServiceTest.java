package org.exoplatform.addon.wallet.reward.test.service;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.reward.service.*;
import org.exoplatform.addon.wallet.reward.test.BaseWalletRewardTest;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.addon.wallet.storage.WalletStorage;
import org.exoplatform.addon.wallet.utils.RewardUtils;
import org.exoplatform.container.component.RequestLifeCycle;

public class WalletRewardServiceTest extends BaseWalletRewardTest {

  /**
   * Check that service is instantiated
   */
  @Test
  public void testServiceInstantiated() {
    WalletRewardService walletRewardService = getService(WalletRewardService.class);
    assertNotNull(walletRewardService);
  }

  @Test
  public void testComputeRewards() {
    WalletRewardService walletRewardService = getService(WalletRewardService.class);
    long startDateInSeconds = RewardUtils.timeToSeconds(YearMonth.of(2019, 03)
                                                                 .atEndOfMonth()
                                                                 .atStartOfDay());

    Set<WalletReward> computedRewards = walletRewardService.computeReward(startDateInSeconds);
    assertNotNull(computedRewards);
    assertEquals(0, computedRewards.size());

    WalletStorage walletStorage = getService(WalletStorage.class);
    int enabledWalletsCount = 60;
    for (int i = 0; i < enabledWalletsCount; i++) {
      Wallet wallet = newWallet(i + 1l);
      wallet = walletStorage.saveWallet(wallet, true);
      entitiesToClean.add(wallet);
    }
    computedRewards = walletRewardService.computeReward(startDateInSeconds);
    assertNotNull(computedRewards);
    // Even if settings are null, the returned rewards shouldn't be empty
    assertEquals(enabledWalletsCount, computedRewards.size());

    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    rewardSettingsService.registerPlugin(CUSTOM_REWARD_PLUGIN);
    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    try {
      RewardSettings newSettings = cloneSettings(defaultSettings);

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

      List<RewardTeam> teams = new ArrayList<>();

      RewardTeam rewardTeam1 = createTeamWithMembers(1, 10, RewardBudgetType.COMPUTED, true);
      teams.add(rewardTeam1);
      RewardTeam rewardTeam2 = createTeamWithMembers(11, 20, RewardBudgetType.FIXED, true);
      teams.add(rewardTeam2);
      RewardTeam rewardTeam3 = createTeamWithMembers(21, 30, RewardBudgetType.FIXED_PER_MEMBER, true);
      teams.add(rewardTeam3);
      RewardTeam rewardTeam4 = createTeamWithMembers(31, 40, RewardBudgetType.FIXED_PER_MEMBER, true);
      teams.add(rewardTeam4);
      RewardTeam rewardTeam5 = createTeamWithMembers(41, 50, RewardBudgetType.COMPUTED, true);
      teams.add(rewardTeam5);
      RewardTeam rewardTeam6 = createTeamWithMembers(51, 60, RewardBudgetType.COMPUTED, true);
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

      computedRewards = walletRewardService.computeReward(startDateInSeconds);

      // check total budget to send
      double tokensToSend = computedRewards.stream().mapToDouble(WalletReward::getTokensToSend).sum();
      assertEquals(sumOfTokensToSend, tokensToSend, 0);

      // Check budget of team having a defined budget as 'fixed'
      double tokensSentToTeam2 = computedRewards.stream()
                                                .filter(walletReward -> rewardTeam2.getName().equals(walletReward.getPoolName()))
                                                .mapToDouble(WalletReward::getTokensToSend)
                                                .sum();
      assertEquals(rewardTeam2.getBudget(), tokensSentToTeam2, 0);

      // Check budget of teams (team3 and team4) having a defined budget as
      // 'fixed per member'
      double tokensSentToTeam3 = computedRewards.stream()
                                                .filter(walletReward -> rewardTeam3.getName().equals(walletReward.getPoolName()))
                                                .mapToDouble(WalletReward::getTokensToSend)
                                                .sum();
      assertEquals(rewardTeam3.getBudget() * rewardTeam3.getMembers().size(), tokensSentToTeam3, 0);

      double tokensSentToTeam4 = computedRewards.stream()
                                                .filter(walletReward -> rewardTeam4.getName().equals(walletReward.getPoolName()))
                                                .mapToDouble(WalletReward::getTokensToSend)
                                                .sum();
      assertEquals(rewardTeam4.getBudget() * rewardTeam4.getMembers().size(), tokensSentToTeam4, 0);

      // Check budget of other teams (team1, team5 and team2) having a defined
      // budget as 'computed'
      double tokensSentToOtherTeams = sumOfTokensToSend - tokensSentToTeam2 - tokensSentToTeam3 - tokensSentToTeam4;
      double tokensSentToOtherTeam = tokensSentToOtherTeams / 3d;

      double tokensSentToTeam1 = computedRewards.stream()
                                                .filter(walletReward -> rewardTeam1.getName().equals(walletReward.getPoolName()))
                                                .mapToDouble(WalletReward::getTokensToSend)
                                                .sum();
      assertEquals(tokensSentToOtherTeam, tokensSentToTeam1, 0);

      double tokensSentToTeam5 = computedRewards.stream()
                                                .filter(walletReward -> rewardTeam5.getName().equals(walletReward.getPoolName()))
                                                .mapToDouble(WalletReward::getTokensToSend)
                                                .sum();
      assertEquals(tokensSentToOtherTeam, tokensSentToTeam5, 0);

      double tokensSentToTeam6 = computedRewards.stream()
                                                .filter(walletReward -> rewardTeam6.getName().equals(walletReward.getPoolName()))
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
    RewardTransactionService rewardTransactionService = getService(RewardTransactionService.class);
    RewardTeamService rewardTeamService = getService(RewardTeamService.class);
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);

    WalletRewardService walletRewardService = new WalletRewardService(walletAccountService,
                                                                      walletTransactionService,
                                                                      rewardSettingsService,
                                                                      rewardTransactionService,
                                                                      rewardTeamService);
    int contractDecimals = 12;

    WalletTokenAdminService tokenAdminService = Mockito.mock(WalletTokenAdminService.class);
    resetTokenAdminService(walletTransactionService, tokenAdminService, false, true);
    container.registerComponentInstance(WalletTokenAdminService.class, tokenAdminService);

    WalletService walletService = getService(WalletService.class);
    ContractDetail contractDetail = new ContractDetail();
    contractDetail.setName("name");
    contractDetail.setSymbol("symbol");
    contractDetail.setDecimals(contractDecimals);
    contractDetail.setAddress("address");
    contractDetail.setContractType("3");
    contractDetail.setNetworkId(1l);
    contractDetail.setSellPrice("0.002");
    walletService.getSettings();
    walletService.setConfiguredContractDetail(contractDetail);

    long startDateInSeconds = RewardUtils.timeToSeconds(YearMonth.of(2019, 04)
                                                                 .atEndOfMonth()
                                                                 .atStartOfDay());

    rewardSettingsService.registerPlugin(CUSTOM_REWARD_PLUGIN);
    RewardSettings defaultSettings = rewardSettingsService.getSettings();
    try {
      // Build new settings
      RewardSettings newSettings = cloneSettings(defaultSettings);
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
      walletService.setConfiguredContractDetail(null);
    }
  }

  private void resetTokenAdminService(WalletTransactionService walletTransactionService,
                                      WalletTokenAdminService tokenAdminService,
                                      boolean pendingTransactions,
                                      boolean successTransactions) throws Exception { // NOSONAR
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

  private double checkComputedRewards(WalletRewardService walletRewardService,
                                      long startDateInSeconds,
                                      int enabledWalletsCount,
                                      long amount) {
    double sumOfTokensToSend = 0;
    Set<WalletReward> computedRewards = walletRewardService.computeReward(startDateInSeconds);
    assertNotNull(computedRewards);
    computedRewards = computedRewards.stream()
                                     .filter(walletReward -> walletReward.getTokensToSend() > 0)
                                     .collect(Collectors.toSet());
    assertEquals(enabledWalletsCount, computedRewards.size());
    for (WalletReward walletReward : computedRewards) {
      assertNotNull(walletReward);
      assertNotNull(walletReward.getWallet());
      assertNotNull(walletReward.getRewards());
      assertEquals(1, walletReward.getRewards().size());
      assertEquals(0, walletReward.getTokensSent(), 0);
      long tokensToSend = walletReward.getWallet().getTechnicalId() * amount;
      sumOfTokensToSend += tokensToSend;
      assertEquals("Wallet '" + walletReward + "' has an unexpected reward amount",
                   tokensToSend,
                   walletReward.getTokensToSend(),
                   0);
    }
    return sumOfTokensToSend;
  }

}
