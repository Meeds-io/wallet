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
package org.exoplatform.wallet.reward.notification;

import static org.exoplatform.wallet.utils.RewardUtils.*;
import static org.exoplatform.wallet.utils.WalletUtils.AMOUNT;
import static org.exoplatform.wallet.utils.WalletUtils.formatNumber;

import java.io.Writer;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Locale;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.model.*;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.webui.utils.TimeConvertUtils;

public class RewardSuccessTemplateBuilder extends AbstractTemplateBuilder {

  private static final Log LOG = ExoLogger.getLogger(RewardSuccessTemplateBuilder.class);

  private ChannelKey            channelKey;

  private boolean               pushNotification;

  private ExoContainer          container;

  private RewardSettingsService rewardSettingsService;

  public RewardSuccessTemplateBuilder(PortalContainer container, RewardSettingsService rewardSettingsService, ChannelKey channelKey) {
    this.container = container;
    this.rewardSettingsService = rewardSettingsService;
    this.channelKey = channelKey;
    this.pushNotification = StringUtils.equals(channelKey.getId(), "PUSH_CHANNEL");
  }

  @Override
  protected MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    String pluginId = notification.getKey().getId();

    String language = getLanguage(notification);
    TemplateContext templateContext = TemplateContext.newChannelInstance(channelKey, pluginId, language);

    RequestLifeCycle.begin(container);
    try {
      RewardSettings rewardSettings = rewardSettingsService.getSettings();
      ZoneId zoneId = rewardSettings.zoneId();

      templateContext.putAll(notification.getOwnerParameter());
      templateContext.put("NOTIFICATION_ID", notification.getId());
      String amount = (String) templateContext.get(AMOUNT);
      templateContext.put(AMOUNT, formatNumber(amount, language));

      String rewardStartPeriodDate = (String) templateContext.get(REWARD_START_PERIOD_DATE);
      templateContext.put(REWARD_START_PERIOD_DATE_FORMATTED, formatTime(rewardStartPeriodDate, zoneId, language));

      String rewardEndPeriodDate = (String) templateContext.get(REWARD_END_PERIOD_DATE);
      templateContext.put(REWARD_END_PERIOD_DATE_FORMATTED, formatTime(Long.parseLong(rewardEndPeriodDate) - 1, zoneId, language));

      String notificationRead = notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey());
      templateContext.put("READ", Boolean.parseBoolean(notificationRead) ? "read" : "unread");

      setLastModifiedDate(notification, language, templateContext);
      SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

      String body = TemplateUtils.processGroovy(templateContext);
      if (templateContext.getException() != null) {
        throw new IllegalStateException("An error occurred while building message", templateContext.getException());
      }
      MessageInfo messageInfo = new MessageInfo();
      messageInfo.to(notification.getTo());
      messageInfo.from(notification.getFrom());
      messageInfo.pluginId(pluginId);
      if (this.pushNotification) {
        messageInfo.subject("/");
      } else {
        messageInfo.subject(TemplateUtils.processSubject(templateContext));
      }
      return messageInfo.body(body).end();
    } catch (Exception e) {
      // Exception gets swallowed by upper services, thus log it here
      LOG.warn("An error occurred while building notification message", e);
      throw e;
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  protected boolean makeDigest(NotificationContext ctx, Writer writer) {
    return false;
  }

  private void setLastModifiedDate(NotificationInfo notification, String language, TemplateContext templateContext) {
    try {
      Calendar lastModified = Calendar.getInstance();
      lastModified.setTimeInMillis(notification.getLastModifiedDate());
      String lastModifiedDateString = TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                                                                                   "EE, dd yyyy",
                                                                                   new Locale(language),
                                                                                   TimeConvertUtils.YEAR);
      templateContext.put("LAST_UPDATED_TIME", lastModifiedDateString);
    } catch (Exception e) {
      templateContext.put("LAST_UPDATED_TIME", "");
    }
  }

}
