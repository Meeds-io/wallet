/*
   * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.reward.job;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_SUCCESS_EVENT_NAME;

import java.util.*;

import org.quartz.*;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.reward.service.RewardReportService;

/**
 * This job is used to check pending rewards sent for periods. When all
 * transaction rewards are sent correctly without error, an event is triggered
 * through {@link ListenerService}
 */
@DisallowConcurrentExecution
public class RewardStatusVerifierJob implements Job {

  private static final Log    LOG = ExoLogger.getLogger(RewardStatusVerifierJob.class);

  private ExoContainer        container;

  private RewardReportService rewardReportService;

  private ListenerService     listenerService;

  public RewardStatusVerifierJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      List<RewardPeriod> rewardPeriodsInProgress = getRewardReportService().getRewardPeriodsInProgress();
      if (rewardPeriodsInProgress != null && !rewardPeriodsInProgress.isEmpty()) {
        Iterator<RewardPeriod> rewardPeriodsIterator = rewardPeriodsInProgress.iterator();
        while (rewardPeriodsIterator.hasNext()) {
          RewardPeriod rewardPeriod = rewardPeriodsIterator.next();
          RewardReport rewardReport = getRewardReportService().computeRewards(rewardPeriod.getStartDateInSeconds());
          if (rewardReport == null) {
            continue;
          }
          if (rewardReport.isCompletelyProceeded()) {
            LOG.debug("Rewards sent successfully for period {}: wallets to reward = {} ,transactions = {} , success = {}, failed = {}, pending = {}, completed = {}",
                      rewardPeriod.getStartDateInSeconds(),
                      rewardReport.getValidRewardCount(),
                      rewardReport.getTransactionsCount(),
                      rewardReport.getSuccessTransactionCount(),
                      rewardReport.getFailedTransactionCount(),
                      rewardReport.getPendingTransactionCount(),
                      rewardReport.isCompletelyProceeded());

            getListenerService().broadcast(REWARD_SUCCESS_EVENT_NAME, rewardReport, null);
            rewardPeriodsIterator.remove();
          } else {
            LOG.debug("Reward not completed for period {}: wallets to reward = {} ,transactions = {} , success = {}, failed = {}, pending = {}, completed = {}",
                      rewardPeriod.getStartDateInSeconds(),
                      rewardReport.getValidRewardCount(),
                      rewardReport.getTransactionsCount(),
                      rewardReport.getSuccessTransactionCount(),
                      rewardReport.getFailedTransactionCount(),
                      rewardReport.getPendingTransactionCount(),
                      rewardReport.isCompletelyProceeded());
          }
          getRewardReportService().saveRewardReport(rewardReport);
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending rewards", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private RewardReportService getRewardReportService() {
    if (rewardReportService == null) {
      rewardReportService = CommonsUtils.getService(RewardReportService.class);
    }
    return rewardReportService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
