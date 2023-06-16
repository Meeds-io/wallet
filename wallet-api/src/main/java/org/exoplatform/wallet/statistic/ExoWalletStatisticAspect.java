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

import static org.exoplatform.wallet.statistic.StatisticUtils.DURATION;
import static org.exoplatform.wallet.statistic.StatisticUtils.ERROR_MSG;
import static org.exoplatform.wallet.statistic.StatisticUtils.LOCAL_SERVICE;
import static org.exoplatform.wallet.statistic.StatisticUtils.OPERATION;
import static org.exoplatform.wallet.statistic.StatisticUtils.REMOTE_SERVICE;
import static org.exoplatform.wallet.statistic.StatisticUtils.STATUS;
import static org.exoplatform.wallet.statistic.StatisticUtils.STATUS_CODE;
import static org.exoplatform.wallet.statistic.StatisticUtils.addStatisticEntry;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * See {@link ExoWalletStatistic}
 */
@Aspect
public class ExoWalletStatisticAspect {
  private static final Log LOG = ExoLogger.getLogger(ExoWalletStatisticAspect.class);

  /**
   * Encapsulate method annotated with {@link ExoWalletStatistic} to add
   * statistic log entry
   * 
   * @param point processing point
   * @return result of processing point after its execution
   * @throws Throwable if processing point throws an exception
   */
  @Around("execution(* *(..)) && @annotation(org.exoplatform.wallet.statistic.ExoWalletStatistic)")
  public Object around(ProceedingJoinPoint point) throws Throwable { // NOSONAR
    ExoWalletStatisticService statisticService = (ExoWalletStatisticService) point.getThis();
    MethodSignature methodSignature = (MethodSignature) point.getSignature();
    Method method = methodSignature.getMethod();
    ExoWalletStatistic annotation = method.getAnnotation(ExoWalletStatistic.class);
    boolean local = annotation.local();
    String service = annotation.service();
    String operation = annotation.operation();

    String errorMessage = null;
    long startTime = System.currentTimeMillis();
    Object result = null;
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainerIfPresent();
    boolean replaceCurrentContainer = !(currentContainer instanceof PortalContainer);
    if (replaceCurrentContainer) {
      ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());
    }
    try {
      result = point.proceed();
      return result;
    } catch (RuntimeException e) {
      errorMessage = e.getMessage();
      throw e;
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      try {
        Map<String, Object> parameters = statisticService == null ? null
                                                                  : statisticService.getStatisticParameters(operation,
                                                                                                            result,
                                                                                                            point.getArgs());
        if (parameters != null) {
          if (local) {
            put(parameters, LOCAL_SERVICE, service);
          } else {
            put(parameters, REMOTE_SERVICE, service);
          }
          if (!parameters.containsKey(OPERATION)) {
            put(parameters, OPERATION, operation);
          }
          if (!parameters.containsKey(DURATION)) {
            put(parameters, DURATION, duration);
          }
          if (parameters.containsKey(ERROR_MSG) || StringUtils.isNotBlank(errorMessage)) {
            if (!parameters.containsKey(ERROR_MSG)) {
              put(parameters, ERROR_MSG, errorMessage);
            }
            put(parameters, STATUS, "ko");
            put(parameters, STATUS_CODE, "500");
          } else {
            if (!parameters.containsKey(STATUS)) {
              put(parameters, STATUS, "ok");
            }
            if (!parameters.containsKey(STATUS_CODE)) {
              put(parameters, STATUS_CODE, "200");
            }
          }
          if (ExoContainer.hasProfile("analytics")) {
            addStatisticEntry(parameters);
          }
        }
      } catch (Throwable e) {
        LOG.warn("Error adding statistic log entry in method '{}' for statistic type '{}'", method.getName(), operation, e);
      } finally {
        if (replaceCurrentContainer) {
          ExoContainerContext.setCurrentContainer(currentContainer);
        }
      }
    }
  }

  private void put(Map<String, Object> parameters, String key, Object value) {
    if (StringUtils.isNotBlank(key) && value != null) {
      try {
        parameters.put(key, value);
      } catch (Exception e) {
        LOG.warn("Error adding statistic log entry with values", key, value, e);
      }
    }
  }

}
