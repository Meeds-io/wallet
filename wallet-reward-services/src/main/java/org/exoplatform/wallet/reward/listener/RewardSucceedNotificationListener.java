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
package org.exoplatform.wallet.reward.listener;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_SUCCESS_NOTIFICATION_ID;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.reward.RewardReport;

/**
 * A listener that is triggered when rewards has been successfully sent. This
 * will send notifications about final reward report.
 */
@Asynchronous
public class RewardSucceedNotificationListener extends Listener<RewardReport, Object> {

  @Override
  @ExoTransactional
  public void onEvent(Event<RewardReport, Object> event) throws Exception {
    RewardReport rewardReport = event.getSource();
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(REWARD_REPORT_NOTIFICATION_PARAM, rewardReport);
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(REWARD_SUCCESS_NOTIFICATION_ID))).execute(ctx);
  }

}
