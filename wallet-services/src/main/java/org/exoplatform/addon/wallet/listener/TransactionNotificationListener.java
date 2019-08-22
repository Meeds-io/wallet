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
package org.exoplatform.addon.wallet.listener;

import static org.exoplatform.addon.wallet.statistic.StatisticUtils.*;
import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.model.transaction.TransactionNotificationType;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.addon.wallet.statistic.StatisticUtils;
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

/**
 * A listener that is triggered when a watched transaction by the addon is mined
 * on blockchain. This will mark the transaction as not pending in internal
 * database and send notifications.
 */
@Asynchronous
public class TransactionNotificationListener extends Listener<Object, JSONObject> {
  private static final Log         LOG = ExoLogger.getLogger(TransactionNotificationListener.class);

  private ExoContainer             container;

  private WalletTransactionService transactionService;

  private WalletAccountService     walletAccountService;

  public TransactionNotificationListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Object, JSONObject> event) throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String transactionHash = event.getData().getString("hash");
      if (StringUtils.isBlank(transactionHash)) {
        return;
      }
      TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
      if (transactionDetail == null || !transactionDetail.isSucceeded()) {
        // No notification for not succeeded transactions
        return;
      }

      logStatistics(transactionDetail);

      if (transactionDetail.isAdminOperation()) {
        // No notification for admin operation transactions
        return;
      }

      Wallet senderWallet = null;
      String senderAddress = transactionDetail.getFrom();
      GlobalSettings settings = getSettings();
      ContractDetail contractDetail = settings.getContractDetail();
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
        sendNotification(transactionDetail, TransactionNotificationType.SENDER, senderWallet, receiverWallet, settings);
      }
      if (receiverWallet != null && receiverWallet.getTechnicalId() > 0 && receiverWallet.isEnabled()
          && !receiverWallet.isDeletedUser() && !receiverWallet.isDisabledUser()) {
        sendNotification(transactionDetail, TransactionNotificationType.RECEIVER, senderWallet, receiverWallet, settings);
      }
    } catch (Exception e) {
      LOG.error("Error processing transaction notification {}", event.getData(), e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private void sendNotification(TransactionDetail transactionDetail,
                                TransactionNotificationType transactionStatus,
                                Wallet senderWallet,
                                Wallet receiverWallet,
                                GlobalSettings settings) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(HASH_PARAMETER, transactionDetail.getHash());
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, senderWallet);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, receiverWallet);
    ctx.append(MESSAGE_PARAMETER, transactionDetail.getMessage() == null ? "" : transactionDetail.getMessage());

    if (StringUtils.isBlank(transactionDetail.getContractAddress())) {
      ctx.append(SYMBOL_PARAMETER, "ether");
      ctx.append(CONTRACT_ADDRESS_PARAMETER, "");
      ctx.append(AMOUNT_PARAMETER, transactionDetail.getValue());
    } else {
      ContractDetail contractDetail = settings.getContractDetail();
      ctx.append(SYMBOL_PARAMETER, contractDetail.getSymbol());
      ctx.append(CONTRACT_ADDRESS_PARAMETER, contractDetail.getAddress());
      ctx.append(AMOUNT_PARAMETER, transactionDetail.getContractAmount());
    }

    // Notification type is determined automatically by
    // transactionStatus.getNotificationId()
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

  private void logStatistics(TransactionDetail transactionDetail) {
    String contractMethodName = transactionDetail.getContractMethodName();

    if (StringUtils.isBlank(contractMethodName)) {
      contractMethodName = ETHER_FUNC_SEND_FUNDS;
    }

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(LOCAL_SERVICE, "wallet");
    parameters.put(OPERATION, contractMethodName);

    if (transactionDetail.getIssuer() != null) {
      parameters.put("user_social_id", transactionDetail.getIssuer().getTechnicalId());
    }

    parameters.put("sender", transactionDetail.getFromWallet());
    parameters.put("receiver", transactionDetail.getToWallet());

    switch (contractMethodName) {
    case ETHER_FUNC_SEND_FUNDS:
    case CONTRACT_FUNC_TRANSFER:
    case CONTRACT_FUNC_TRANSFERFROM:
    case CONTRACT_FUNC_APPROVE:
    case CONTRACT_FUNC_INITIALIZEACCOUNT:
      parameters.put("amount_ether", transactionDetail.getValue());
      parameters.put("amount_token", transactionDetail.getContractAmount());
      break;
    case CONTRACT_FUNC_REWARD:
      parameters.put("amount_token", transactionDetail.getContractAmount());
      break;
    case CONTRACT_FUNC_ADDADMIN:
      parameters.put("admin_level", transactionDetail.getValue());
      break;
    default:
      break;
    }
    parameters.put("transaction", transactionDetail.getHash());
    parameters.put(STATUS, transactionDetail.isSucceeded() ? "ok" : "ko");
    parameters.put(STATUS_CODE, transactionDetail.isSucceeded() ? "200" : "500");
    parameters.put(DURATION, "");
    StatisticUtils.addStatisticEntry(parameters);
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
