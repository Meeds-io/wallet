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

import java.util.Map;

import io.meeds.common.ContainerTransactional;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;

import io.meeds.wallet.model.RewardPeriod;
import io.meeds.wallet.reward.service.RewardReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This job is used to check update for not sent reward periods.
 */
@DisallowConcurrentExecution
@Component
public class RewardReportUpdateTask {

  @Autowired
  private RewardReportService   rewardReportService;

  @ContainerTransactional
  @Scheduled(cron = "${exo.wallet.RewardReportUpdateTask.expression:0 */5 * * * ?}")
  public void execute() {
    Map<Long, Boolean> rewardSettingChanged = rewardReportService.getRewardSettingChanged();
    if (CollectionUtils.isNotEmpty(rewardSettingChanged.values())) {
      for (Map.Entry<Long, Boolean> entry : rewardSettingChanged.entrySet()) {
        if (Boolean.TRUE.equals(entry.getValue())) {
          Long id = entry.getKey();
          RewardPeriod rewardPeriod = rewardReportService.getRewardPeriodById(id);
          if (rewardPeriod != null) {
            rewardReportService.getReport(rewardPeriod);
          }
        }
      }
    }
  }
}
