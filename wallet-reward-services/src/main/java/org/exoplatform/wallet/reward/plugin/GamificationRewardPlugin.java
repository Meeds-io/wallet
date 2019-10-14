/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.wallet.reward.plugin;

import java.lang.reflect.Method;
import java.util.*;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.Component;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.reward.api.RewardPlugin;
import org.exoplatform.wallet.utils.RewardUtils;

public class GamificationRewardPlugin extends RewardPlugin {

  private static final Log     LOG                          = ExoLogger.getLogger(GamificationRewardPlugin.class);

  private static final String  GAMIFICATION_SERVICE_FQN     =
                                                        "org.exoplatform.addons.gamification.service.effective.GamificationService";

  private static final String  FIND_USER_POINTS_METHOD_NAME = "findUserReputationScoreBetweenDate";

  private ConfigurationManager configurationManager;

  private ExoContainer         container;

  private Object               serviceInstance;

  private Method               retrievePointsMethod;

  private boolean              enabled;

  public GamificationRewardPlugin(PortalContainer container, ConfigurationManager configurationManager) {
    this.container = container;
    this.configurationManager = configurationManager;

    Component component = this.configurationManager.getComponent(GAMIFICATION_SERVICE_FQN);
    enabled = component != null;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public Map<Long, Double> getEarnedPoints(Set<Long> identityIds, long startDateInSeconds, long endDateInSeconds) {
    HashMap<Long, Double> earnedPoints = new HashMap<>();
    if (identityIds == null || identityIds.isEmpty()) {
      return earnedPoints;
    }
    Date startDate = new Date(startDateInSeconds * 1000);
    Date endDate = new Date(endDateInSeconds * 1000);
    Method method = getMethod();
    if (method == null) {
      throw new IllegalStateException("Can't find gamification service method to retrieve user points");
    }
    for (Long identityId : identityIds) {
      long points = 0;
      try {
        points = (Long) method.invoke(getService(), String.valueOf(identityId), startDate, endDate);
      } catch (Exception e) {
        LOG.warn("Error getting gamification points for user with id {}", identityId, e);
      }
      earnedPoints.put(identityId, (double) points);
    }
    return earnedPoints;
  }

  private Method getMethod() {
    if (this.retrievePointsMethod != null) {
      return retrievePointsMethod;
    }
    retrievePointsMethod = RewardUtils.getMethod(container, GAMIFICATION_SERVICE_FQN, FIND_USER_POINTS_METHOD_NAME);
    return retrievePointsMethod;
  }

  private Object getService() {
    if (this.serviceInstance != null) {
      return serviceInstance;
    }
    serviceInstance = RewardUtils.getService(container, GAMIFICATION_SERVICE_FQN);
    return serviceInstance;
  }

}
