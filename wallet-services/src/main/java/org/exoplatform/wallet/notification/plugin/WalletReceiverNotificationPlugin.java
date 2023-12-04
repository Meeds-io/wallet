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
package org.exoplatform.wallet.notification.plugin;

import static org.exoplatform.wallet.utils.WalletUtils.ACCOUNT_TYPE;
import static org.exoplatform.wallet.utils.WalletUtils.AMOUNT;
import static org.exoplatform.wallet.utils.WalletUtils.AMOUNT_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.AVATAR;
import static org.exoplatform.wallet.utils.WalletUtils.CONTRACT_ADDRESS;
import static org.exoplatform.wallet.utils.WalletUtils.CONTRACT_ADDRESS_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.HASH;
import static org.exoplatform.wallet.utils.WalletUtils.HASH_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.MESSAGE;
import static org.exoplatform.wallet.utils.WalletUtils.MESSAGE_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.RECEIVER;
import static org.exoplatform.wallet.utils.WalletUtils.RECEIVER_ACCOUNT_DETAIL_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.RECEIVER_TYPE;
import static org.exoplatform.wallet.utils.WalletUtils.RECEIVER_URL;
import static org.exoplatform.wallet.utils.WalletUtils.SENDER;
import static org.exoplatform.wallet.utils.WalletUtils.SENDER_ACCOUNT_DETAIL_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.SENDER_URL;
import static org.exoplatform.wallet.utils.WalletUtils.SYMBOL;
import static org.exoplatform.wallet.utils.WalletUtils.SYMBOL_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.getNotificationReceiversUsers;
import static org.exoplatform.wallet.utils.WalletUtils.getPermanentLink;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionNotificationType;

public class WalletReceiverNotificationPlugin extends BaseNotificationPlugin {

  private static final Log LOG = ExoLogger.getLogger(WalletReceiverNotificationPlugin.class);

  public WalletReceiverNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return TransactionNotificationType.RECEIVER.getNotificationId();
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    Wallet senderAccountDetail = ctx.value(SENDER_ACCOUNT_DETAIL_PARAMETER);
    Wallet receiverAccountDetail = ctx.value(RECEIVER_ACCOUNT_DETAIL_PARAMETER);
    String symbol = ctx.value(SYMBOL_PARAMETER);
    String contractAddress = ctx.value(CONTRACT_ADDRESS_PARAMETER);
    double amount = ctx.value(AMOUNT_PARAMETER);
    String message = ctx.value(MESSAGE_PARAMETER);
    String hash = ctx.value(HASH_PARAMETER);
    if (StringUtils.isNotBlank(message)) {
      try {
        message = HTMLSanitizer.sanitize(message);
      } catch (Exception e) {
        LOG.warn("error sanitizing wallet transaction message {}. Use empty message", message, e);
        message = "";
      }
    }

    List<String> toList = getNotificationReceiversUsers(receiverAccountDetail, senderAccountDetail.getId());
    if (toList == null || toList.isEmpty()) {
      return null;
    }

    String senderAvatar = senderAccountDetail.getAvatar();
    if (StringUtils.isBlank(senderAvatar)) {
      senderAvatar = LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
    }
    senderAvatar = CommonsUtils.getCurrentDomain() + senderAvatar;

    return NotificationInfo.instance()
                           .to(toList)
                           .with(CONTRACT_ADDRESS, contractAddress)
                           .with(ACCOUNT_TYPE, receiverAccountDetail.getType())
                           .with(RECEIVER_TYPE, receiverAccountDetail.getType())
                           .with(AMOUNT, String.valueOf(amount))
                           .with(SYMBOL, symbol)
                           .with(MESSAGE, message)
                           .with(HASH, hash)
                           .with(AVATAR, senderAvatar)
                           .with(SENDER_URL, getPermanentLink(senderAccountDetail))
                           .with(RECEIVER_URL, getPermanentLink(receiverAccountDetail))
                           .with(SENDER, senderAccountDetail.getName())
                           .with(RECEIVER, receiverAccountDetail.getName())
                           .key(getKey())
                           .end();
  }
}
