package org.exoplatform.addon.wallet.utils;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.security.ConversationState;

public class RewardUtils {

  public static final DateTimeFormatter DATE_FORMATTER                           = DateTimeFormatter.ofPattern("d MMM uuuu");

  public static final String            TRANSACTION_STATUS_PENDING               = "pending";

  public static final String            TRANSACTION_STATUS_SUCCESS               = "success";

  public static final String            TRANSACTION_STATUS_FAILED                = "error";

  public static final String            REWARD_SCOPE_NAME                        = "ADDONS_REWARD";

  public static final String            REWARD_CONTEXT_NAME                      = "ADDONS_REWARD";

  public static final String            REWARD_TRANSACTION_LABEL_KEY             =
                                                                     "exoplatform.wallet.label.rewardTransactionLabel";

  public static final String            REWARD_TRANSACTION_WITH_POOL_MESSAGE_KEY =
                                                                                 "exoplatform.wallet.label.rewardTransactionMessageWithPool";

  public static final String            REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY   =
                                                                               "exoplatform.wallet.label.rewardTransactionMessageNoPool";

  public static final Context           REWARD_CONTEXT                           = Context.GLOBAL.id(REWARD_CONTEXT_NAME);

  public static final Scope             REWARD_SCOPE                             =
                                                     Scope.APPLICATION.id(REWARD_SCOPE_NAME);

  public static final String            REWARD_SETTINGS_KEY_NAME                 = "REWARD_SETTINGS";

  private RewardUtils() {
  }

  public static String formatTime(long timeInSeconds) {
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeInSeconds), TimeZone.getDefault().toZoneId());
    return dateTime.format(DATE_FORMATTER);
  }

  public static LocalDateTime timeFromSeconds(long createdDate) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(createdDate), TimeZone.getDefault().toZoneId());
  }

  public static long timeToSeconds(LocalDateTime time) {
    return time.atZone(ZoneOffset.systemDefault()).toEpochSecond();
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

}
