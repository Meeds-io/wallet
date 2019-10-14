/*
   * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.wallet.listener;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.model.transaction.TransactionNotificationType;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * A listener that is triggered when a watched transaction by the addon is mined
 * on blockchain. This will mark the transaction as not pending in internal
 * database and send notifications.
 */
@Asynchronous
public class TransactionNotificationListener extends Listener<Object, TransactionDetail> {
  private static final Log         LOG = ExoLogger.getLogger(TransactionNotificationListener.class);

  private ExoContainer             container;

  private WalletTransactionService transactionService;

  private WalletAccountService     walletAccountService;

  public TransactionNotificationListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    TransactionDetail transactionDetail = event.getData();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String transactionHash = transactionDetail.getHash();
      if (StringUtils.isBlank(transactionHash) || StringUtils.isBlank(transactionDetail.getContractAddress())) {
        return;
      }
      ContractDetail contractDetail = getContractDetail();
      if (contractDetail == null
          || !StringUtils.equalsIgnoreCase(contractDetail.getAddress(), transactionDetail.getContractAddress())) {
        return;
      }

      transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
      if (transactionDetail == null || !transactionDetail.isSucceeded()) {
        // No notification for not succeeded transactions
        return;
      }

      if (transactionDetail.isAdminOperation()) {
        // No notification for admin operation transactions
        return;
      }

      Wallet senderWallet = null;
      String senderAddress = transactionDetail.getFrom();
      String contractAdminAddress = contractDetail == null ? null
                                                           : contractDetail.getOwner();
      if (StringUtils.isNotBlank(senderAddress)) {
        senderWallet = getWalletAccountService().getWalletByAddress(senderAddress);
        if (senderWallet == null) {
          senderWallet = new Wallet();
          senderWallet.setAddress(senderAddress);
          senderWallet.setAvatar(LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
          if (StringUtils.isNotBlank(contractAdminAddress)
              && StringUtils.equalsIgnoreCase(contractAdminAddress, senderAddress)) {
            senderWallet.setName(PRINCIPAL_CONTRACT_ADMIN_NAME);
          } else {
            senderWallet.setName(senderAddress);
          }
        }
      }

      Wallet receiverWallet = null;
      String receiverAddress = transactionDetail.getTo();
      if (StringUtils.isNotBlank(receiverAddress)) {
        receiverWallet = getWalletAccountService().getWalletByAddress(receiverAddress);
        if (receiverWallet == null) {
          receiverWallet = new Wallet();
          receiverWallet.setAddress(receiverAddress);
          receiverWallet.setAvatar(LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
          if (StringUtils.isNotBlank(contractAdminAddress)
              && StringUtils.equalsIgnoreCase(contractAdminAddress, receiverAddress)) {
            receiverWallet.setName(PRINCIPAL_CONTRACT_ADMIN_NAME);
          } else {
            receiverWallet.setName(receiverAddress);
          }
        }
      }

      if (senderWallet != null && senderWallet.getTechnicalId() > 0 && senderWallet.isEnabled() && !senderWallet.isDeletedUser()
          && !senderWallet.isDisabledUser()) {
        sendNotification(transactionDetail, TransactionNotificationType.SENDER, senderWallet, receiverWallet);
      }
      if (receiverWallet != null && receiverWallet.getTechnicalId() > 0 && receiverWallet.isEnabled()
          && !receiverWallet.isDeletedUser() && !receiverWallet.isDisabledUser()) {
        sendNotification(transactionDetail, TransactionNotificationType.RECEIVER, senderWallet, receiverWallet);
      }
    } catch (Exception e) {
      LOG.error("Error processing transaction notification {}", event.getData(), e);
    } finally {
      logStatistics(transactionDetail);
      RequestLifeCycle.end();
    }
  }

  private void sendNotification(TransactionDetail transactionDetail,
                                TransactionNotificationType transactionStatus,
                                Wallet senderWallet,
                                Wallet receiverWallet) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(HASH_PARAMETER, transactionDetail.getHash());
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, senderWallet);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, receiverWallet);
    ctx.append(MESSAGE_PARAMETER, transactionDetail.getMessage() == null ? "" : transactionDetail.getMessage());

    ContractDetail contractDetail = getContractDetail();
    ctx.append(SYMBOL_PARAMETER, contractDetail.getSymbol());
    ctx.append(CONTRACT_ADDRESS_PARAMETER, contractDetail.getAddress());
    ctx.append(AMOUNT_PARAMETER, transactionDetail.getContractAmount());

    // Notification type is determined automatically by
    // transactionStatus.getNotificationId()
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private WalletAccountService getWalletAccountService() {
    if (walletAccountService == null) {
      walletAccountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return walletAccountService;
  }
}
