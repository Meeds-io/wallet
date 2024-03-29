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
package org.exoplatform.wallet.utils;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardSettingsService;

public class RewardUtils {

  public static final DateTimeFormatter             DATE_FORMATTER                           =
                                                                   DateTimeFormatter.ofPattern("d MMM uuuu");

  public static final DateTimeFormatter             RFC_3339_FORMATTER                       =
                                                                       DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss][.SSS][XXX]")
                                                                                        .withResolverStyle(ResolverStyle.LENIENT);

  public static final String                        TRANSACTION_STATUS_PENDING               = "pending";

  public static final String                        TRANSACTION_STATUS_SUCCESS               = "success";

  public static final String                        TRANSACTION_STATUS_FAILED                = "error";

  public static final String                        REWARD_SCOPE_NAME                        = "ADDONS_REWARD";

  public static final String                        REWARD_CONTEXT_NAME                      = "ADDONS_REWARD";

  public static final String                        REWARD_TRANSACTION_LABEL_KEY             =
                                                                                 "exoplatform.wallet.label.rewardTransactionLabel";

  public static final String                        REWARD_TRANSACTION_WITH_POOL_MESSAGE_KEY =
                                                                                             "exoplatform.wallet.label.rewardTransactionMessageWithPool";

  public static final String                        REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY   =
                                                                                           "exoplatform.wallet.label.rewardTransactionMessageNoPool";

  public static final Context                       REWARD_CONTEXT                           =
                                                                   Context.GLOBAL.id(REWARD_CONTEXT_NAME);

  public static final Scope                         REWARD_SCOPE                             =
                                                                 Scope.APPLICATION.id(REWARD_SCOPE_NAME);

  public static final String                        REWARD_SETTINGS_KEY_NAME                 = "REWARD_SETTINGS";

  public static final String                        REWARD_SUCCESS_EVENT_NAME                =
                                                                              "exo.wallet.reward.report.success";

  public static final String                        REWARD_SUCCESS_NOTIFICATION_ID           = "RewardSuccessNotificationPlugin";

  public static final String                        REWARD_PERIOD_TYPE                       = "rewardPeriodType";

  public static final String                        REWARD_START_PERIOD_DATE                 = "rewardStartPeriodDate";

  public static final String                        REWARD_END_PERIOD_DATE                   = "rewardEndPeriodDate";

  public static final String                        REWARD_START_PERIOD_DATE_FORMATTED       = "rewardStartPeriodDateFormatted";

  public static final String                        REWARD_END_PERIOD_DATE_FORMATTED         = "rewardEndPeriodDateFormatted";

  public static final String                        REWARD_VALID_MEMBERS_COUNT               = "rewardValidMembersCount";

  public static final String                        REWARD_SUCCESS_COUNT                     = "rewardSuccessCount";

  public static final String                        REWARD_FAIL_COUNT                        = "rewardFailCount";

  public static final String                        REWARD_PENDING_COUNT                     = "rewardPendingCount";

  public static final String                        REWARD_TRANSACTION_COUNT                 = "rewardTransactionCount";

  public static final ArgumentLiteral<RewardReport> REWARD_REPORT_NOTIFICATION_PARAM         =
                                                                                     new ArgumentLiteral<>(RewardReport.class,
                                                                                                           "rewardReport");

  private RewardUtils() {
  }

  public static String formatTime(Object timeInSeconds, ZoneId zoneId, String lang) {
    Locale userLocale = StringUtils.isBlank(lang) ? Locale.getDefault() : new Locale(lang);
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(String.valueOf(timeInSeconds))),
                                                     zoneId);
    return dateTime.format(DATE_FORMATTER.withLocale(userLocale));
  }

  public static LocalDateTime timeFromSeconds(long createdDate, ZoneId zoneId) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdDate), zoneId);
  }

  public static long timeToSecondsAtDayStart(LocalDate date, ZoneId zoneId) {
    return date.atStartOfDay(zoneId).toEpochSecond();
  }

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }

  public static final Method getMethod(ExoContainer container, String serviceName, String methodName) {
    Object serviceInstance = getService(container, serviceName);
    if (serviceInstance == null) {
      return null;
    }

    Method methodResult = null;

    int i = 0;
    Method[] declaredMethods = serviceInstance.getClass().getDeclaredMethods();
    while (methodResult == null && i < declaredMethods.length) {
      Method method = declaredMethods[i++];
      if (method.getName().equals(methodName)) {
        methodResult = method;
      }
    }
    return methodResult;
  }

  public static final Object getService(ExoContainer container, String serviceName) {
    Object serviceInstance = null;
    try {
      serviceInstance = container.getComponentInstanceOfType(Class.forName(serviceName));
    } catch (ClassNotFoundException e) {
      return null;
    }
    return serviceInstance;
  }

  public static final RewardSettings getRewardSettings() {
    return CommonsUtils.getService(RewardSettingsService.class).getSettings();
  }

  public static ZonedDateTime parseRFC3339ToZonedDateTime(String dateString, ZoneId zoneId) {
    if (StringUtils.isBlank(dateString)) {
      return null;
    }
    if (dateString.length() == 10) {
      return LocalDate.parse(dateString, RFC_3339_FORMATTER).atStartOfDay(zoneId);
    } else if (dateString.length() > 20) {
      return ZonedDateTime.parse(dateString, RFC_3339_FORMATTER).withZoneSameInstant(zoneId);
    } else {
      return LocalDateTime.parse(dateString, RFC_3339_FORMATTER).atZone(zoneId);
    }
  }

  public static String formatDateTime(ZonedDateTime zonedDateTime) {
    return zonedDateTime.format(RFC_3339_FORMATTER);
  }

}
