/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.wallet.reward.listener;

import java.time.Instant;
import java.util.Date;

import jakarta.annotation.PostConstruct;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import io.meeds.wallet.wallet.model.reward.RewardReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A listener that is triggered when rewards has been successfully sent. This
 * will collect reward statistics.
 */
@Asynchronous
@Component
public class RewardSucceedAnalyticsListener extends Listener<RewardReport, Object> {

  private static final String EVENT_NAME = "exo.wallet.reward.report.success";

  @Autowired
  private ListenerService     listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(EVENT_NAME, this);
  }

  @Override
  @ExoTransactional
  public void onEvent(Event<RewardReport, Object> event) {
    RewardReport rewardReport = event.getSource();
    StatisticData statisticData = new StatisticData();
    statisticData.setModule("wallet");
    statisticData.setSubModule("reward");
    statisticData.setOperation("sendPeriodRewards");
    statisticData.addParameter("rewardPeriodStartDate",
                               Date.from(Instant.ofEpochSecond(rewardReport.getPeriod().getStartDateInSeconds())));
    statisticData.addParameter("rewardPeriodEndDate",
                               Date.from(Instant.ofEpochSecond(rewardReport.getPeriod().getEndDateInSeconds())));
    statisticData.addParameter("rewardPeriodTimeZone", rewardReport.getPeriod().getTimeZone());
    statisticData.addParameter("rewardPeriodType", rewardReport.getPeriod().getRewardPeriodType().name().toLowerCase());
    statisticData.addParameter("rewardTransactionsCount", rewardReport.getSuccessTransactionCount());
    statisticData.addParameter("rewardTokensSent", rewardReport.getTokensSent());
    statisticData.addParameter("rewardTokensToSend", rewardReport.getTokensToSend());
    statisticData.addParameter("rewardRecipientWalletCount", rewardReport.getValidRewardCount());
    statisticData.addParameter("rewardParticipantWalletCount", rewardReport.getRewards().size());

    AnalyticsUtils.addStatisticData(statisticData);
  }

}
