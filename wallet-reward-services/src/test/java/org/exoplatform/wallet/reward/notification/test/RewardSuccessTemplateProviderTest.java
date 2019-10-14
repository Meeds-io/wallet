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
