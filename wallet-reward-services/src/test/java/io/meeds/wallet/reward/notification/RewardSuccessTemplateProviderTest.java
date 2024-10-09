/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.reward.notification;

import java.util.Map;

import io.meeds.wallet.model.RewardSettings;
import io.meeds.wallet.reward.service.RewardSettingsService;

import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RewardSuccessTemplateProviderTest {

  @Test
  public void testGetTemplate() {
    RewardSettingsService rewardSettingsService = mock(RewardSettingsService.class);
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
    RewardSuccessTemplateProvider provider = new RewardSuccessTemplateProvider(rewardSettingsService, getParams("MAIL_CHANNEL"));
    Map<PluginKey, String> templateFilePathConfigs = provider.getTemplateFilePathConfigs();
    assertNotNull(templateFilePathConfigs);
    assertEquals(1, templateFilePathConfigs.size());
    String template = templateFilePathConfigs.values().iterator().next();
    assertEquals(provider.getMailTemplatePath(), template);

    provider = new RewardSuccessTemplateProvider(rewardSettingsService, getParams("MAIL_CHANNEL"));
    templateFilePathConfigs = provider.getTemplateFilePathConfigs();
    assertNotNull(templateFilePathConfigs);
    assertEquals(1, templateFilePathConfigs.size());
    template = templateFilePathConfigs.values().iterator().next();
    assertEquals(provider.getMailTemplatePath(), template);

    provider = new RewardSuccessTemplateProvider(rewardSettingsService, getParams("PUSH_CHANNEL"));
    templateFilePathConfigs = provider.getTemplateFilePathConfigs();
    assertNotNull(templateFilePathConfigs);
    assertEquals(1, templateFilePathConfigs.size());
    template = templateFilePathConfigs.values().iterator().next();
    assertEquals(provider.getPushTemplatePath(), template);
  }

  private InitParams getParams(String channelId) {
    InitParams initParams = new InitParams();
    ValueParam valueParam = new ValueParam();
    valueParam.setName("channel-id");
    valueParam.setValue(channelId);
    initParams.addParam(valueParam);
    return initParams;
  }

}
