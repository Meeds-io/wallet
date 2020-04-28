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
    @TemplateConfig(pluginId = WALLET_SENDER_NOTIFICATION_ID, template = "war:/conf/wallet/templates/notification/mail/WalletSenderMailPlugin.gtmpl"),
    @TemplateConfig(pluginId = WALLET_RECEIVER_NOTIFICATION_ID, template = "war:/conf/wallet/templates/notification/mail/WalletReceiverMailPlugin.gtmpl"),
    @TemplateConfig(pluginId = FUNDS_REQUEST_NOTIFICATION_ID, template = "war:/conf/wallet/templates/notification/mail/WalletRequestFundsMailPlugin.gtmpl") })
public class MailTemplateProvider extends TemplateProvider {

  public MailTemplateProvider(PortalContainer container, InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(WALLET_SENDER_NOTIFICATION_ID), new TemplateBuilder(this, container, false));
    this.templateBuilders.put(PluginKey.key(WALLET_RECEIVER_NOTIFICATION_ID), new TemplateBuilder(this, container, false));
    this.templateBuilders.put(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID),
                              new RequestFundsTemplateBuilder(this, container, false));
  }
}
