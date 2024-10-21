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
package io.meeds.wallet.reward.listener;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.meeds.wallet.model.RewardPeriod;
import io.meeds.wallet.model.RewardSettings;
import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wallet.reward.service.RewardSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.common.ContainerTransactional;

import jakarta.annotation.PostConstruct;

import static io.meeds.gamification.utils.Utils.*;
import static io.meeds.wallet.reward.service.WalletRewardSettingsService.REWARD_SETTINGS_UPDATED;
import static io.meeds.wallet.utils.WalletUtils.MODIFY_ADDRESS_ASSOCIATED_EVENT;
import static io.meeds.wallet.utils.WalletUtils.NEW_ADDRESS_ASSOCIATED_EVENT;

/**
 * A listener that is triggered to update estimated reward report for current
 * period.
 */
@Asynchronous
@Component
public class RewardReportUpdateListener extends Listener<Object, Map<String, String>> {

  private static final List<String> EVENT_NAMES = Arrays.asList(NEW_ADDRESS_ASSOCIATED_EVENT,
                                                                MODIFY_ADDRESS_ASSOCIATED_EVENT,
                                                                REWARD_SETTINGS_UPDATED,
                                                                POST_CREATE_ANNOUNCEMENT_EVENT,
                                                                POST_UPDATE_ANNOUNCEMENT_EVENT,
                                                                POST_CANCEL_ANNOUNCEMENT_EVENT,
                                                                POST_REALIZATION_CREATE_EVENT,
                                                                POST_REALIZATION_UPDATE_EVENT,
                                                                POST_REALIZATION_CANCEL_EVENT);

  @Autowired
  private RewardReportService       rewardReportService;

  @Autowired
  private RewardSettingsService     rewardSettingsService;

  @Autowired
  private ListenerService           listenerService;

  @PostConstruct
  public void init() {
    EVENT_NAMES.forEach(name -> listenerService.addListener(name, this));
  }

  @ContainerTransactional
  @Override
  public void onEvent(Event<Object, Map<String, String>> event) {
    String eventName = event.getEventName();
    Map<Long, Boolean> updatedSettings = rewardReportService.getRewardSettingChanged();
    if (REWARD_SETTINGS_UPDATED.equals(eventName)) {
      List<RewardPeriod> rewardPeriods = rewardReportService.getRewardPeriodsNotSent();
      updatedSettings = rewardPeriods.stream().collect(Collectors.toMap(RewardPeriod::getId, rewardPeriod -> true));
    } else {
      RewardSettings rewardSettings = rewardSettingsService.getSettings();
      RewardPeriod rewardPeriod = rewardReportService.getRewardPeriod(rewardSettings.getPeriodType(), LocalDate.now());
      if (rewardPeriod == null) {
        return;
      }
      updatedSettings.put(rewardPeriod.getId(), true);
    }
    rewardReportService.setRewardSettingChanged(updatedSettings);
  }
}
