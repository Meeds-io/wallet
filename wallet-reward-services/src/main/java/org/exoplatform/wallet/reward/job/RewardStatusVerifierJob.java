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

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_SUCCESS_EVENT_NAME;

import java.util.Iterator;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.service.WalletAccountService;

/**
 * This job is used to check pending rewards sent for periods. When all
 * transaction rewards are sent correctly without error, an event is triggered
 * through {@link ListenerService}
 */
@DisallowConcurrentExecution
public class RewardStatusVerifierJob implements Job {

  private static final Log     LOG = ExoLogger.getLogger(RewardStatusVerifierJob.class);

  protected ExoContainer         container;

  protected RewardReportService  rewardReportService;

  protected WalletAccountService walletAccountService;

  protected ListenerService      listenerService;

  public RewardStatusVerifierJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      Wallet adminWallet = getWalletAccountService().getAdminWallet();
      if (!getWalletAccountService().isAdminAccountEnabled() || adminWallet == null || adminWallet.getEtherBalance() == null
          || adminWallet.getEtherBalance() == 0 || adminWallet.getTokenBalance() == null || adminWallet.getTokenBalance() == 0) {
        return;
      }
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

  private WalletAccountService getWalletAccountService() {
    if (walletAccountService == null) {
      walletAccountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return walletAccountService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
