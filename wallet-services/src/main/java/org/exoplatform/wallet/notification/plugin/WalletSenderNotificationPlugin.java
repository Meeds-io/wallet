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
package org.exoplatform.wallet.notification.plugin;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionNotificationType;

public class WalletSenderNotificationPlugin extends BaseNotificationPlugin {

  public WalletSenderNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return TransactionNotificationType.SENDER.getNotificationId();
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

    // Don't send notifications about Admin wallet sending funds
    if (senderAccountDetail == null || StringUtils.equals(senderAccountDetail.getId(), WALLET_ADMIN_REMOTE_ID)) {
      return null;
    }

    List<String> toList = getNotificationReceiversUsers(senderAccountDetail, receiverAccountDetail.getId());
    if (toList == null || toList.isEmpty()) {
      return null;
    }

    String receiverAvatar = receiverAccountDetail.getAvatar();
    if (StringUtils.isBlank(receiverAvatar)) {
      receiverAvatar = LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
    }
    receiverAvatar = CommonsUtils.getCurrentDomain() + receiverAvatar;

    return NotificationInfo.instance()
                           .to(toList)
                           .with(CONTRACT_ADDRESS, contractAddress)
                           .with(AMOUNT, String.valueOf(amount))
                           .with(ACCOUNT_TYPE, senderAccountDetail.getType())
                           .with(RECEIVER_TYPE, receiverAccountDetail.getType())
                           .with(SYMBOL, symbol)
                           .with(MESSAGE, message)
                           .with(HASH, hash)
                           .with(AVATAR, receiverAvatar)
                           .with(SENDER_URL, getPermanentLink(senderAccountDetail))
                           .with(RECEIVER_URL, getPermanentLink(receiverAccountDetail))
                           .with(SENDER, senderAccountDetail.getName())
                           .with(RECEIVER, receiverAccountDetail.getName())
                           .key(getKey())
                           .end();
  }
}
