/*
 * Copyright (C) 2019 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.wallet.statistic;

import static org.exoplatform.wallet.statistic.StatisticUtils.*;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

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
  public Object around(ProceedingJoinPoint point) throws Throwable {
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
    try {
      result = point.proceed();
      return result;
    } catch (RuntimeException e) {
      errorMessage = e.getMessage();
      throw e;
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      try {
        Map<String, Object> parameters = statisticService.getStatisticParameters(operation, result, point.getArgs());
        if (parameters != null) {
          if (local) {
            parameters.put(LOCAL_SERVICE, service);
          } else {
            parameters.put(REMOTE_SERVICE, service);
          }
          if (!parameters.containsKey(OPERATION)) {
            parameters.put(OPERATION, operation);
          }
          if (!parameters.containsKey(DURATION)) {
            parameters.put(DURATION, duration);
          }
          if (parameters.containsKey(ERROR_MSG) || StringUtils.isNotBlank(errorMessage)) {
            if (!parameters.containsKey(ERROR_MSG)) {
              parameters.put(ERROR_MSG, errorMessage);
            }
            parameters.put(STATUS, "ko");
            parameters.put(STATUS_CODE, "500");
          } else {
            if (!parameters.containsKey(STATUS)) {
              parameters.put(STATUS, "ok");
            }
            if (!parameters.containsKey(STATUS_CODE)) {
              parameters.put(STATUS_CODE, "200");
            }
          }
          addStatisticEntry(parameters);
        }
      } catch (Throwable e) {
        LOG.warn("Error adding statistic log entry in method {} for statistic type {}", method.getName(), operation, e);
      }
    }
  }

}
