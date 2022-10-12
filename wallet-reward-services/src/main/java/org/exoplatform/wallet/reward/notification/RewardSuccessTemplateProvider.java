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

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_SUCCESS_NOTIFICATION_ID;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wallet.reward.service.RewardSettingsService;

public class RewardSuccessTemplateProvider extends TemplateProvider {

  private static final PluginKey       PLUGIN_KEY       = PluginKey.key(REWARD_SUCCESS_NOTIFICATION_ID);

  private RewardSuccessTemplateBuilder builder          = null;

  private String                       webTemplatePath  = null;

  private String                       pushTemplatePath = null;

  private String                       mailTemplatePath = null;

  public RewardSuccessTemplateProvider(PortalContainer container, RewardSettingsService rewardSettingsService, InitParams initParams) {
    super(initParams);
    builder = new RewardSuccessTemplateBuilder(container, rewardSettingsService, getChannelKey());

    this.templateBuilders.put(PLUGIN_KEY, builder);

    setWebTemplatePath("war:/conf/wallet/templates/notification/web/RewardSuccessWebPlugin.gtmpl");
    setMailTemplatePath("war:/conf/wallet/templates/notification/mail/RewardSuccessMailPlugin.gtmpl");
    setPushTemplatePath("war:/conf/wallet/templates/notification/push/RewardSuccessPushPlugin.gtmpl");
  }

  @Override
  public Map<PluginKey, String> getTemplateFilePathConfigs() {
    ChannelKey channelKey = getChannelKey();
    if (StringUtils.equals(channelKey.getId(), "MAIL_CHANNEL")) {
      return Collections.singletonMap(PLUGIN_KEY, mailTemplatePath);
    } else if (StringUtils.equals(channelKey.getId(), "PUSH_CHANNEL")) {
      return Collections.singletonMap(PLUGIN_KEY, pushTemplatePath);
    } else if (StringUtils.equals(channelKey.getId(), "WEB_CHANNEL")) {
      return Collections.singletonMap(PLUGIN_KEY, webTemplatePath);
    }
    return Collections.emptyMap();
  }

  public String getWebTemplatePath() {
    return webTemplatePath;
  }

  public void setWebTemplatePath(String webTemplatePath) {
    this.webTemplatePath = webTemplatePath;
  }

  public String getPushTemplatePath() {
    return pushTemplatePath;
  }

  public void setPushTemplatePath(String pushTemplatePath) {
    this.pushTemplatePath = pushTemplatePath;
  }

  public String getMailTemplatePath() {
    return mailTemplatePath;
  }

  public void setMailTemplatePath(String mailTemplatePath) {
    this.mailTemplatePath = mailTemplatePath;
  }

  public RewardSuccessTemplateBuilder getBuilder() {
    return builder;
  }
}
