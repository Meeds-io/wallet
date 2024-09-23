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
package io.meeds.wallet.notification.plugin;

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
import io.meeds.wallet.wallet.model.ContractDetail;
import io.meeds.wallet.wallet.model.Wallet;
import io.meeds.wallet.wallet.model.transaction.FundsRequest;
import io.meeds.wallet.wallet.utils.WalletUtils;

import static io.meeds.wallet.wallet.utils.WalletUtils.*;

public class FundsRequestNotificationPlugin extends BaseNotificationPlugin {

  private static final Log LOG = ExoLogger.getLogger(FundsRequestNotificationPlugin.class);

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

    String message = fundsRequest.getMessage() == null ? "" : fundsRequest.getMessage();
    if (StringUtils.isNotBlank(message)) {
      try {
        message = HTMLSanitizer.sanitize(message);
      } catch (Exception e) {
        LOG.warn("error sanitizing wallet transaction message {}. Use empty message", message, e);
        message = "";
      }
    }

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
                           .with(SENDER_ID, String.valueOf(requestSenderAccountDetail.getTechnicalId()))
                           .with(RECEIVER, requestReceiverAccountDetail.getName())
                           .with(RECEIVER_ID, String.valueOf(requestReceiverAccountDetail.getTechnicalId()))
                           .with(SYMBOL, symbol)
                           .with(MESSAGE, message)
                           .key(getKey())
                           .end();
  }

}
