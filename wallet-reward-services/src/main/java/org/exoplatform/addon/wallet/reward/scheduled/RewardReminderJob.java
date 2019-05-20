package org.exoplatform.addon.wallet.reward.scheduled;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

import org.quartz.*;

import org.exoplatform.addon.wallet.reward.model.*;
import org.exoplatform.addon.wallet.reward.service.RewardSettingsService;
import org.exoplatform.addon.wallet.task.model.WalletAdminTask;
import org.exoplatform.addon.wallet.task.service.WalletTaskService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class RewardReminderJob implements Job {

  private static final String   REWARD_WALLET_TASK_TYPE = "reward";

  private static final Log      LOG                     = ExoLogger.getLogger(RewardReminderJob.class);

  private WalletTaskService     walletTaskService;

  private RewardSettingsService rewardSettingsService;

  private ExoContainer          container;

  public RewardReminderJob() {
    this(PortalContainer.getInstance());
  }

  public RewardReminderJob(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      RewardSettings rewardSettings = getRewardSettingsService().getSettings();
      RewardPeriodType periodType = rewardSettings == null ? null : rewardSettings.getPeriodType();
      if (rewardSettings == null || periodType == null) {
        return;
      }
      ZonedDateTime now = ZonedDateTime.now();
      RewardPeriod periodOfTime = periodType.getPeriodOfTime(now.toLocalDateTime());
      long endDateInSeconds = periodOfTime.getEndDateInSeconds();
      long daysUntilRewardPayment = Duration.ofSeconds(endDateInSeconds - now.toEpochSecond()).toDays();
      if (daysUntilRewardPayment <= getRewardSettingsService().getReminderDateInDays()) {
        Set<WalletAdminTask> adminTasks = getWalletTaskService().getTasksByType(REWARD_WALLET_TASK_TYPE);
        WalletAdminTask adminTask = null;
        if (adminTasks != null && !adminTasks.isEmpty()) {
          adminTask = adminTasks.iterator().next();
          if (adminTasks.size() > 1) {
            LOG.warn("More than one task for type {} is retrieved from database", REWARD_WALLET_TASK_TYPE);
          }
        }
        if (adminTask == null) {
          adminTask = new WalletAdminTask();
          adminTask.setType(REWARD_WALLET_TASK_TYPE);
        }
        List<String> parameters = adminTask.getParameters();
        if (parameters == null) {
          parameters = new ArrayList<>();
          adminTask.setParameters(parameters);
        } else {
          parameters.clear();
        }
        adminTask.setCompleted(false);

        // Add reward period
        parameters.add(String.valueOf(periodOfTime.getStartDateInSeconds()));
        parameters.add(String.valueOf(periodOfTime.getEndDateInSeconds()));

        getWalletTaskService().save(adminTask, null);
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private WalletTaskService getWalletTaskService() {
    if (walletTaskService == null) {
      walletTaskService = CommonsUtils.getService(WalletTaskService.class);
    }
    return walletTaskService;
  }

  private RewardSettingsService getRewardSettingsService() {
    if (rewardSettingsService == null) {
      rewardSettingsService = CommonsUtils.getService(RewardSettingsService.class);
    }
    return rewardSettingsService;
  }
}
