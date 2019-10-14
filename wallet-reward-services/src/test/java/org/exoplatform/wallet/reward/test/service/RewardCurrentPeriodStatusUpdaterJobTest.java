package org.exoplatform.wallet.reward.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.reward.job.RewardCurrentPeriodStatusUpdaterJob;
import org.exoplatform.wallet.reward.service.*;
import org.exoplatform.wallet.reward.storage.RewardReportStorage;
import org.exoplatform.wallet.reward.test.BaseWalletRewardTest;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTokenAdminService;
import org.exoplatform.wallet.storage.WalletStorage;
import org.exoplatform.wallet.utils.RewardUtils;
import org.exoplatform.wallet.utils.WalletUtils;

public class RewardCurrentPeriodStatusUpdaterJobTest extends BaseWalletRewardTest {

  @Test
  public void testExecuteJob() throws Exception {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    RewardTeamService rewardTeamService = getService(RewardTeamService.class);
    RewardReportStorage rewardReportStorage = getService(RewardReportStorage.class);

    WalletRewardReportService walletRewardService = new WalletRewardReportService(walletAccountService,
                                                                                  rewardSettingsService,
                                                                                  rewardTeamService,
                                                                                  rewardReportStorage);
    WalletTokenAdminService tokenAdminService = Mockito.mock(WalletTokenAdminService.class);
    resetTokenAdminService(tokenAdminService);

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

      List<RewardPeriod> rewardPeriodsInProgress = walletRewardService.getRewardPeriodsNotSent();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(0, rewardPeriodsInProgress.size());

      RewardCurrentPeriodStatusUpdaterJob rewardReportNotificationJob = new RewardCurrentPeriodStatusUpdaterJob();

      // Executing job shouldn't mark reward transactions as sent until
      // transactions are marked as success and not pending
      rewardReportNotificationJob.execute(null);
      rewardPeriodsInProgress = walletRewardService.getRewardPeriodsNotSent();
      assertNotNull(rewardPeriodsInProgress);
      assertEquals(1l, rewardPeriodsInProgress.size());
      RewardPeriod rewardPeriod = rewardPeriodsInProgress.get(0);

      RewardPeriod currentPeriod = newSettings.getPeriodType()
                                              .getPeriodOfTime(RewardUtils.timeFromSeconds(System.currentTimeMillis()
                                                  / 1000));
      assertEquals(rewardPeriod.getStartDateInSeconds(), currentPeriod.getStartDateInSeconds());
      assertEquals(rewardPeriod.getEndDateInSeconds(), currentPeriod.getEndDateInSeconds());
    } finally {
      rewardSettingsService.unregisterPlugin(CUSTOM_PLUGIN_ID);
      rewardSettingsService.saveSettings(defaultSettings);
    }
  }

  private void resetTokenAdminService(WalletTokenAdminService tokenAdminService) throws Exception {
    if (container.getComponentInstanceOfType(WalletTokenAdminService.class) != null) {
      container.unregisterComponent(WalletTokenAdminService.class);
    }
    container.registerComponentInstance(WalletTokenAdminService.class, tokenAdminService);
    Mockito.reset(tokenAdminService);
    Mockito.when(tokenAdminService.getAdminWalletAddress()).thenReturn("adminAddress");
    Mockito.when(tokenAdminService.getAdminLevel(Mockito.eq("adminAddress"))).thenReturn(2);
  }

}
