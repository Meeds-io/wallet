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
package org.exoplatform.wallet.reward.notification.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;

import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.wallet.reward.notification.RewardSuccessTemplateProvider;
import org.exoplatform.wallet.reward.test.BaseWalletRewardTest;

public class RewardSuccessTemplateProviderTest extends BaseWalletRewardTest {

  /**
   * Check that provider returns templates correctly
   */
  @Test
  public void testGetTemplate() {
    RewardSuccessTemplateProvider provider = new RewardSuccessTemplateProvider(container, getParams("MAIL_CHANNEL"));
    Map<PluginKey, String> templateFilePathConfigs = provider.getTemplateFilePathConfigs();
    assertNotNull(templateFilePathConfigs);
    assertEquals(1, templateFilePathConfigs.size());
    String template = templateFilePathConfigs.values().iterator().next();
    assertEquals(provider.getMailTemplatePath(), template);

    provider = new RewardSuccessTemplateProvider(container, getParams("WEB_CHANNEL"));
    templateFilePathConfigs = provider.getTemplateFilePathConfigs();
    assertNotNull(templateFilePathConfigs);
    assertEquals(1, templateFilePathConfigs.size());
    template = templateFilePathConfigs.values().iterator().next();
    assertEquals(provider.getWebTemplatePath(), template);

    provider = new RewardSuccessTemplateProvider(container, getParams("PUSH_CHANNEL"));
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
