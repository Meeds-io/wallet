/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.notification.provider;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wallet.notification.builder.RequestFundsTemplateBuilder;
import org.exoplatform.wallet.notification.builder.TemplateBuilder;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = WALLET_SENDER_NOTIFICATION_ID, template = "war:/conf/wallet/templates/notification/push/WalletSenderPushPlugin.gtmpl"),
    @TemplateConfig(pluginId = WALLET_RECEIVER_NOTIFICATION_ID, template = "war:/conf/wallet/templates/notification/push/WalletReceiverPushPlugin.gtmpl"),
    @TemplateConfig(pluginId = FUNDS_REQUEST_NOTIFICATION_ID, template = "war:/conf/wallet/templates/notification/push/WalletRequestFundsPushPlugin.gtmpl") })
public class MobilePushTemplateProvider extends TemplateProvider {

  public MobilePushTemplateProvider(PortalContainer container, InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(WALLET_SENDER_NOTIFICATION_ID), new TemplateBuilder(this, container, true));
    this.templateBuilders.put(PluginKey.key(WALLET_RECEIVER_NOTIFICATION_ID), new TemplateBuilder(this, container, true));
    this.templateBuilders.put(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID),
                              new RequestFundsTemplateBuilder(this, container, true));
  }

}
