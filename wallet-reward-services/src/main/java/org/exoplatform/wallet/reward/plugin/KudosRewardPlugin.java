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
package org.exoplatform.wallet.reward.plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.Component;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.reward.api.RewardPlugin;
import org.exoplatform.wallet.utils.RewardUtils;

public class KudosRewardPlugin extends RewardPlugin {

  private static final Log     LOG                           = ExoLogger.getLogger(KudosRewardPlugin.class);

  private static final String  KUDOS_SERVICE_FQN             = "io.meeds.kudos.service.KudosService";

  private static final String  COUNT_USERS_KUDOS_METHOD_NAME = "countKudosByPeriodAndReceivers";

  private ConfigurationManager configurationManager;

  private ExoContainer         container;

  private Object               serviceInstance;

  private Method               retrievePointsMethod;

  private boolean              enabled;

  public KudosRewardPlugin(ExoContainer container, ConfigurationManager configurationManager) {
    this.container = container;
    this.configurationManager = configurationManager;

    Component component = this.configurationManager.getComponent(KUDOS_SERVICE_FQN);
    enabled = component != null;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Long, Double> getEarnedPoints(Set<Long> identityIds, long startDateInSeconds, long endDateInSeconds) {
    HashMap<Long, Double> earnedPoints = new HashMap<>();
    if (identityIds == null || identityIds.isEmpty()) {
      return earnedPoints;
    }
    Method method = getMethod();
    if (method == null) {
      throw new IllegalStateException("Can't find kudos service method to retrieve user points");
    }
    Map<Long, Long> points = new HashMap<>();
    try {
      points = (Map<Long, Long>) method.invoke(getService(),
                                               identityIds.stream().toList(),
                                               startDateInSeconds,
                                               endDateInSeconds);
    } catch (Exception e) {
      LOG.warn("Error getting kudos count for users with ids {}", identityIds, e);
    }
    return points.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue()));

  }

  private Method getMethod() {
    if (this.retrievePointsMethod != null) {
      return retrievePointsMethod;
    }
    retrievePointsMethod = RewardUtils.getMethod(container, KUDOS_SERVICE_FQN, COUNT_USERS_KUDOS_METHOD_NAME);
    return retrievePointsMethod;
  }

  private Object getService() {
    if (this.serviceInstance != null) {
      return serviceInstance;
    }
    serviceInstance = RewardUtils.getService(container, KUDOS_SERVICE_FQN);
    return serviceInstance;
  }

}
