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

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.FundsRequest;
import org.exoplatform.wallet.utils.WalletUtils;

public class FundsRequestNotificationPlugin extends BaseNotificationPlugin {

  public FundsRequestNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return FUNDS_REQUEST_NOTIFICATION_ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    Wallet requestSenderDetail = ctx.value(FUNDS_REQUEST_SENDER_DETAIL_PARAMETER);
    Wallet requestSenderAccountDetail = ctx.value(SENDER_ACCOUNT_DETAIL_PARAMETER);
    Wallet requestReceiverAccountDetail = ctx.value(RECEIVER_ACCOUNT_DETAIL_PARAMETER);
    FundsRequest fundsRequest = ctx.value(FUNDS_REQUEST_PARAMETER);

    List<String> toList = getNotificationReceiversUsers(requestReceiverAccountDetail, requestSenderDetail.getId());
    if (toList == null || toList.isEmpty()) {
      return null;
    }

    String walletLink = getWalletLink(fundsRequest.getReceipientType(), fundsRequest.getReceipient());
    String requestAcceptURL = walletLink + "?receiver="
        + requestSenderAccountDetail.getId() + "&receiver_type="
        + requestSenderAccountDetail.getType() + "&amount=" + fundsRequest.getAmount();

    ContractDetail contractDetail = WalletUtils.getContractDetail();
    String symbol = contractDetail == null ? null : contractDetail.getSymbol();

    return NotificationInfo.instance()
                           .to(toList)
                           .with(AMOUNT, String.valueOf(fundsRequest.getAmount()))
                           .with(ACCOUNT_TYPE, requestSenderAccountDetail.getType())
                           .with(AVATAR, CommonsUtils.getCurrentDomain() + requestSenderAccountDetail.getAvatar())
                           .with(SENDER_URL, getPermanentLink(requestSenderAccountDetail))
                           .with(RECEIVER_URL, getPermanentLink(requestReceiverAccountDetail))
                           .with(FUNDS_ACCEPT_URL, requestAcceptURL)
                           .with(USER, requestSenderDetail.getName())
                           .with(USER_URL, getPermanentLink(requestSenderDetail))
                           .with(SENDER, requestSenderAccountDetail.getName())
                           .with(RECEIVER, requestReceiverAccountDetail.getName())
                           .with(SYMBOL, symbol)
                           .with(MESSAGE, fundsRequest.getMessage() == null ? "" : fundsRequest.getMessage())
                           .key(getKey())
                           .end();
  }

}
