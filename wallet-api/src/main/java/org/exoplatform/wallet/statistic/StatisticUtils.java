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
package org.exoplatform.wallet.statistic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.model.StatisticData.StatisticStatus;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.utils.WalletUtils;

public class StatisticUtils {

  public static final String         WALLET_ANALYTICS_FEATURE_NAME = "wallet.analytics";

  public static final String         LOCAL_SERVICE                 = "local_service";

  public static final String         REMOTE_SERVICE                = "remote_service";

  public static final String         OPERATION                     = "operation";

  public static final String         STATUS                        = "status";

  public static final String         STATUS_CODE                   = "status_code";

  public static final String         DURATION                      = "duration_ms";

  public static final String         ERROR_MSG                     = "error_msg";

  public static final String         PARAMETERS                    = "parameters";

  protected static Log               log                           = ExoLogger.getLogger("WalletStatistics");

  protected static ExoFeatureService featureService;

  private StatisticUtils() {
  }

  public static void addStatisticEntry(Map<String, Object> parameters) {
    addStatisticEntry(parameters, StatisticUtils::logStatistics, StatisticUtils::addAnalyticsEntry);
  }

  protected static void addStatisticEntry(Map<String, Object> parameters, // NOSONAR
                                          Consumer<String> logConsumer,
                                          Consumer<StatisticData> analyticsConsumer) {
    if (parameters == null || parameters.isEmpty()) {
      throw new IllegalArgumentException("parameters is mandatory");
    }

    parameters = new HashMap<>(parameters);

    if (getFeatureService().isActiveFeature(WALLET_ANALYTICS_FEATURE_NAME)) {
      String subModule = (String) parameters.remove(REMOTE_SERVICE);
      if (StringUtils.isBlank(subModule)) {
        subModule = (String) parameters.remove(LOCAL_SERVICE);
      }
      String operation = (String) parameters.remove(OPERATION);
      String status = (String) parameters.remove(STATUS);
      String statusCode = (String) parameters.remove(STATUS_CODE);
      String errorMessage = (String) parameters.remove(ERROR_MSG);
      Long duration = (Long) parameters.remove(DURATION);

      StatisticData statisticData = new StatisticData();
      statisticData.setModule("wallet");
      statisticData.setSubModule(subModule);
      statisticData.setOperation(operation);
      if (ConversationState.getCurrent() != null) {
        statisticData.setUserId(WalletUtils.getCurrentUserIdentityId());
      }
      if (duration != null && duration > 0) {
        statisticData.setDuration(duration.longValue());
      }
      if (StringUtils.isNotBlank(status)) {
        statisticData.setStatus(StatisticStatus.valueOf(status.toUpperCase()));
      }
      if (StringUtils.isNotBlank(statusCode) && !StringUtils.equals(statusCode, "200")) {
        statisticData.setErrorCode(Long.parseLong(statusCode));
      }
      if (StringUtils.isNotBlank(errorMessage)) {
        statisticData.setErrorMessage(errorMessage);
      }

      Iterator<Entry<String, Object>> parametersIterator = parameters.entrySet().iterator();
      while (parametersIterator.hasNext()) {
        Map.Entry<String, Object> entry = parametersIterator.next();
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Wallet) {
          parametersIterator.remove();

          Wallet wallet = (Wallet) value;
          String prefix = key.replace("wallet", "").replace("Wallet", "");
          if (StringUtils.isBlank(prefix)) {
            statisticData.addParameter(AnalyticsUtils.FIELD_SOCIAL_IDENTITY_ID, wallet.getTechnicalId());
            statisticData.addParameter("walletAddress", wallet.getAddress());
          } else {
            String entryKey = prefix + StringUtils.capitalize(AnalyticsUtils.FIELD_SOCIAL_IDENTITY_ID);
            statisticData.addParameter(entryKey, wallet.getTechnicalId());
            statisticData.addParameter(prefix + "WalletAddress", wallet.getAddress());
          }
        }
      }
      parameters.forEach(statisticData::addParameter);
      analyticsConsumer.accept(statisticData);
    } else if (log.isDebugEnabled()) {
      StringBuilder logEntry = new StringBuilder();

      logEntry.append(PARAMETERS).append("=").append("\"");
      Set<Entry<String, Object>> parametersEntries = parameters.entrySet();
      Iterator<Entry<String, Object>> parametersIterator = parametersEntries.iterator();
      while (parametersIterator.hasNext()) {
        Map.Entry<String, Object> entry = parametersIterator.next();
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof String) {
          logEntry.append(key).append(":").append(value);
        } else if (value instanceof Wallet) {
          String prefix = key;
          if (StringUtils.isBlank(prefix)) {
            prefix = "";
          } else {
            prefix = prefix + "_"; // NOSONAR
          }
          Wallet wallet = (Wallet) value;
          logEntry.append(prefix)
                  .append("identity_id:")
                  .append(wallet.getTechnicalId())
                  .append(",")
                  .append(prefix)
                  .append("identity_type:")
                  .append(wallet.getType())
                  .append(",")
                  .append(prefix)
                  .append("wallet_address:")
                  .append(wallet.getAddress());
        } else {
          logEntry.append(key).append(":").append(value);
        }
        if (!parameters.isEmpty()) {
          logEntry.append(",");
        }
      }
      logEntry.append("\"");

      if (parameters.containsKey(LOCAL_SERVICE)) {
        logEntry.append(LOCAL_SERVICE).append("=").append(parameters.remove(LOCAL_SERVICE)).append(" ");
        parameters.remove(LOCAL_SERVICE);
      } else if (parameters.containsKey(REMOTE_SERVICE)) {
        logEntry.append(REMOTE_SERVICE).append("=").append(parameters.remove(REMOTE_SERVICE)).append(" ");
        parameters.remove(REMOTE_SERVICE);
      } else {
        throw new IllegalStateException("neither 'local_service' nor 'remote_service' exists in parameters");
      }

      if (parameters.containsKey(OPERATION)) {
        logEntry.append(OPERATION).append("=").append(parameters.remove(OPERATION)).append(" ");
        parameters.remove(OPERATION);
      } else {
        throw new IllegalStateException("'OPERATION' doesn't exists in parameters");
      }

      if (parameters.containsKey(STATUS)) {
        logEntry.append(STATUS).append("=").append(parameters.remove(STATUS)).append(" ");
        parameters.remove(STATUS);
      }

      if (parameters.containsKey(STATUS_CODE)) {
        logEntry.append(STATUS_CODE).append("=").append(parameters.remove(STATUS_CODE)).append(" ");
        parameters.remove(STATUS_CODE);
      }

      if (parameters.containsKey(DURATION)) {
        logEntry.append(DURATION).append("=").append(parameters.remove(DURATION)).append(" ");
        parameters.remove(DURATION);
      }

      if (parameters.containsKey(ERROR_MSG)) {
        logEntry.append(ERROR_MSG).append("=").append(parameters.remove(ERROR_MSG)).append(" ");
        parameters.remove(ERROR_MSG);
      }

      logConsumer.accept(logEntry.toString());
    }
  }

  public static final String transformCapitalWithUnderscore(String string) {
    return string.replaceAll("([A-Z])", "_$1").toLowerCase();
  }

  protected static ExoFeatureService getFeatureService() {
    if (featureService == null) {
      featureService = ExoContainerContext.getService(ExoFeatureService.class);
    }
    return featureService;
  }

  protected static void addAnalyticsEntry(StatisticData statisticData) {
    AnalyticsUtils.addStatisticData(statisticData);
  }

  protected static void logStatistics(String string) {
    log.debug(string);
  }

}
