package org.exoplatform.addon.wallet.utils;

import static org.exoplatform.addon.wallet.utils.WalletUtils.REWARDINGS_GROUP;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.model.reward.RewardReport;
import org.exoplatform.addon.wallet.model.reward.RewardSettings;
import org.exoplatform.addon.wallet.reward.service.RewardSettingsService;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.security.ConversationState;

public class RewardUtils {

  public static final DateTimeFormatter             DATE_FORMATTER                           =
                                                                   DateTimeFormatter.ofPattern("d MMM uuuu");

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

  public static final String                        REWARD_PERIODS_IN_PROGRESS               = "REWARD_PERIODS_IN_PROGRESS";

  public static final String                        REWARD_PERIOD_SETTING_KEY                = "REWARD_PERIODS_";

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

  public static String formatTime(Object timeInSeconds, String lang) {
    Locale userLocale = StringUtils.isBlank(lang) ? Locale.getDefault() : new Locale(lang);
    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(String.valueOf(timeInSeconds))),
                                                     TimeZone.getDefault().toZoneId());
    return dateTime.format(DATE_FORMATTER.withLocale(userLocale));
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

  public static Set<String> getRewardAdministrators() throws Exception {
    OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);

    Set<String> adminUsers = new HashSet<>();
    Group rewardingGroup = organizationService.getGroupHandler().findGroupById(REWARDINGS_GROUP);
    if (rewardingGroup != null) {
      ListAccess<Membership> rewardingMembers = organizationService.getMembershipHandler()
                                                                   .findAllMembershipsByGroup(rewardingGroup);
      Membership[] members = rewardingMembers.load(0, rewardingMembers.getSize());
      for (Membership membership : members) {
        adminUsers.add(membership.getUserName());
      }
    }
    return adminUsers;
  }

  public static final RewardSettings getRewardSettings() {
    return CommonsUtils.getService(RewardSettingsService.class).getSettings();
  }

}
