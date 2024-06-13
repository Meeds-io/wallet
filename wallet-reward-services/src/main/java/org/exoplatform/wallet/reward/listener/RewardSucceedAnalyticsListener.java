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
package org.exoplatform.wallet.reward.listener;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletPluginReward;
import org.exoplatform.wallet.model.reward.WalletReward;

/**
 * A listener that is triggered when rewards has been successfully sent. This
 * will collect reward statistics.
 */
@Asynchronous
public class RewardSucceedAnalyticsListener extends Listener<RewardReport, Object> {

  @ExoTransactional
  public void onEvent(Event<RewardReport, Object> event) throws Exception {
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

    Map<String, Double> rewardPluginPoints = rewardReport.getValidRewards()
                                                         .stream()
                                                         .map(WalletReward::getRewards)
                                                         .flatMap(Set::stream)
                                                         .collect(Collectors.toMap(WalletPluginReward::getPluginId,
                                                                                   WalletPluginReward::getPoints,
                                                                                   Double::sum));
    rewardPluginPoints.forEach((pId,
                                points) -> statisticData.addParameter("rewardPlugin" + StringUtils.capitalize(pId) + "Points",
                                                                      points));
    Map<String, Double> rewardPluginAmounts = rewardReport.getValidRewards()
                                                          .stream()
                                                          .map(WalletReward::getRewards)
                                                          .flatMap(Set::stream)
                                                          .collect(Collectors.toMap(WalletPluginReward::getPluginId,
                                                                                    WalletPluginReward::getAmount,
                                                                                    Double::sum));
    rewardPluginAmounts.forEach((pId,
                                 amount) -> statisticData.addParameter("rewardPlugin" + StringUtils.capitalize(pId) + "Amount",
                                                                       amount));

    AnalyticsUtils.addStatisticData(statisticData);
  }

}
