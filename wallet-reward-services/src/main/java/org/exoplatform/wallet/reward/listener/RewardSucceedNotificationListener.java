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
package org.exoplatform.wallet.reward.listener;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_SUCCESS_NOTIFICATION_ID;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.reward.RewardReport;

/**
 * A listener that is triggered when rewards has been successfully sent. This
 * will send notifications about final reward report.
 */
@Asynchronous
public class RewardSucceedNotificationListener extends Listener<RewardReport, Object> {
  private static final Log LOG = ExoLogger.getLogger(RewardSucceedNotificationListener.class);

  private ExoContainer     container;

  public RewardSucceedNotificationListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<RewardReport, Object> event) throws Exception {
    RewardReport rewardReport = event.getSource();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      sendNotification(rewardReport);
    } catch (Exception e) {
      LOG.error("Error processing transaction notification {}", event.getData(), e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private void sendNotification(RewardReport rewardReport) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(REWARD_REPORT_NOTIFICATION_PARAM, rewardReport);
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(REWARD_SUCCESS_NOTIFICATION_ID))).execute(ctx);
  }
}
