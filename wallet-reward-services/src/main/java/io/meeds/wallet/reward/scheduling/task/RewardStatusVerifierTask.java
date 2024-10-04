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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.reward.scheduling.task;

import static io.meeds.wallet.utils.RewardUtils.REWARD_SUCCESS_EVENT_NAME;

import java.util.List;

import io.meeds.common.ContainerTransactional;
import org.quartz.DisallowConcurrentExecution;

import org.exoplatform.services.listener.ListenerService;

import io.meeds.wallet.model.RewardPeriod;
import io.meeds.wallet.model.RewardReport;
import io.meeds.wallet.model.Wallet;
import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wallet.service.WalletAccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This job is used to check pending rewards sent for periods. When all
 * transaction rewards are sent correctly without error, an event is triggered
 * through {@link ListenerService}
 */
@DisallowConcurrentExecution
@Component
public class RewardStatusVerifierTask {

  @Autowired
  private RewardReportService  rewardReportService;

  @Autowired
  private WalletAccountService walletAccountService;

  @Autowired
  private ListenerService      listenerService;

  @ContainerTransactional
  @Scheduled(cron = "${exo.wallet.RewardStatusVerifierJob.expression:0 * * * * ?}")
  public void execute() {
    Wallet adminWallet = walletAccountService.getAdminWallet();
    if (!walletAccountService.isAdminAccountEnabled() || adminWallet == null || adminWallet.getEtherBalance() == null
        || adminWallet.getEtherBalance() == 0 || adminWallet.getTokenBalance() == null || adminWallet.getTokenBalance() == 0) {
      return;
    }
    List<RewardPeriod> rewardPeriodsInProgress = rewardReportService.getRewardPeriodsInProgress();
    if (rewardPeriodsInProgress != null && !rewardPeriodsInProgress.isEmpty()) {
      for (RewardPeriod rewardPeriod : rewardPeriodsInProgress) {
        if (!rewardReportService.isRewardSendingInProgress()) {
          // Avoid saving rewards while transaction status storage is in
          // progress
          RewardReport rewardReport = rewardReportService.computeRewards(rewardPeriod.getPeriodMedianDate());
          if (rewardReport != null) {
            if (rewardReport.isCompletelyProceeded()) {
              listenerService.broadcast(REWARD_SUCCESS_EVENT_NAME, rewardReport, null);
              rewardReportService.saveRewardReport(rewardReport);
            } else if (rewardReport.getPendingTransactionCount() == 0 && rewardReport.getTokensSent() == 0) {
              rewardReportService.saveRewardReport(rewardReport);
            }
          }
        }
      }
    }
  }
}
