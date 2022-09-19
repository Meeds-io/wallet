/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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
package org.exoplatform.wallet.blockchain.listener;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * A listener that is triggered once a contract transaction is mined. It will
 * verify, for the sender of mined contract transaction, if there are other
 * ether transactions sent and that remains pending. In fact, the Blockchain
 * listening is done on contract transactions only, thus this will ensure to
 * instantly refresh ether transactions state just after a contract transaction
 * gets mined.
 */
public class TransactionWithNonceVerifierListener extends Listener<Object, Map<String, Object>> {

  private static final Log             LOG = ExoLogger.getLogger(TransactionWithNonceVerifierListener.class);

  private BlockchainTransactionService blockchainTransactionService;

  private WalletTransactionService     transactionService;

  public TransactionWithNonceVerifierListener(BlockchainTransactionService blockchainTransactionService,
                                              WalletTransactionService transactionService) {
    this.blockchainTransactionService = blockchainTransactionService;
    this.transactionService = transactionService;
  }

  @Override
  public void onEvent(Event<Object, Map<String, Object>> event) throws Exception {
    String hash = null;
    try {
      Map<String, Object> transactionDetailObject = event.getData();
      hash = (String) transactionDetailObject.get("hash");
      TransactionDetail transactionDetail = transactionService.getTransactionByHash(hash);
      if (transactionDetail == null || transactionDetail.isPending()) {
        return;
      }
      refreshEtherTransactions(transactionDetail.getFromWallet(), transactionDetail.getNonce());
    } catch (Exception e) {
      LOG.error("Error refreshing ether transaction with hash {}", hash, e);
    }
  }

  private void refreshEtherTransactions(Wallet fromWallet, long untilNonce) {
    if (fromWallet == null) {
      return;
    }
    List<TransactionDetail> transactions = transactionService.getPendingWalletTransactionsSent(fromWallet.getAddress());
    if (CollectionUtils.isNotEmpty(transactions)) {
      transactions.forEach(transactionDetail -> {
        if (transactionDetail.getNonce() < untilNonce) {
          try {
            blockchainTransactionService.addTransactionToRefreshFromBlockchain(transactionDetail);
          } catch (Exception e) {
            LOG.warn("Error refreshing Transaction {}", transactionDetail.getHash(), e);
          }
        }
      });
    }
  }

}
