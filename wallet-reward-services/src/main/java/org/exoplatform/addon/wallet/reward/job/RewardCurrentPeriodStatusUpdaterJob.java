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
package org.exoplatform.addon.wallet.reward.job;

import static org.exoplatform.addon.wallet.utils.RewardUtils.*;

import java.util.Iterator;
import java.util.Set;

import org.quartz.*;

import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.reward.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This job is used to check pending rewards sent for periods. When all
 * transaction rewards are sent correctly without error, an event is triggered
 * through {@link ListenerService}
 */
@DisallowConcurrentExecution
public class RewardCurrentPeriodStatusUpdaterJob implements Job {

  private static final Log      LOG = ExoLogger.getLogger(RewardCurrentPeriodStatusUpdaterJob.class);

  private ExoContainer          container;

  private RewardSettingsService rewardSettingsService;

  private RewardService         rewardService;

  private RewardPeriodService   rewardPeriodService;

  private ListenerService       listenerService;

  public RewardCurrentPeriodStatusUpdaterJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      RewardSettings rewardSettings = getRewardSettings();
      if (rewardSettings != null && rewardSettings.getPeriodType() != null) {
        RewardReport rewardReport = getRewardService().computeRewardReport(System.currentTimeMillis());
        if (rewardReport != null) {
          getRewardPeriodService().saveRewardReport(rewardReport);
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending rewards", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private RewardSettingsService getRewardSettingsService() {
    if (rewardSettingsService == null) {
      rewardSettingsService = CommonsUtils.getService(RewardSettingsService.class);
    }
    return rewardSettingsService;
  }

  private RewardService getRewardService() {
    if (rewardService == null) {
      rewardService = CommonsUtils.getService(RewardService.class);
    }
    return rewardService;
  }

  private RewardPeriodService getRewardPeriodService() {
    if (rewardPeriodService == null) {
      rewardPeriodService = CommonsUtils.getService(RewardPeriodService.class);
    }
    return rewardPeriodService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
