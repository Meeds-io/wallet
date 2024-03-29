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

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * Triggered when a contract transaction is mined. This listener will not be
 * triggered for ether transactions bu it will verify at the same time that the
 * sender wallet doesn't have any pending ether transaction, else it will be
 * verified. (Thus for Wallet initializations, the ether transactions are sent
 * before contract sending to ensure to get the ether transaction mined before
 * token transaction all time, because it will use a lower nonce)
 */
@Asynchronous
public class ContractTransactionMinedListener extends Listener<String, ContractTransactionEvent> {

  private static final Log             LOG = ExoLogger.getLogger(ContractTransactionMinedListener.class);

  private WalletTransactionService     walletTransactionService;

  private BlockchainTransactionService blockchainTransactionService;

  public ContractTransactionMinedListener(WalletTransactionService walletTransactionService,
                                          BlockchainTransactionService blockchainTransactionService) {
    this.walletTransactionService = walletTransactionService;
    this.blockchainTransactionService = blockchainTransactionService;
  }

  @Override
  public void onEvent(Event<String, ContractTransactionEvent> event) throws Exception {
    ContractTransactionEvent contractEvent = event.getData();
    String transactionHash = contractEvent.getTransactionHash();
    TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(transactionHash);
    if (transactionDetail != null) {
      if (transactionDetail.isPending() || !transactionDetail.isSucceeded()) {
        LOG.debug("Transaction with hash {} has been mined successfully", transactionDetail.getHash());
        blockchainTransactionService.addTransactionToRefreshFromBlockchain(transactionDetail);
      }
    } else if (blockchainTransactionService.hasManagedWalletInTransaction(contractEvent)) {
      blockchainTransactionService.refreshTransactionFromBlockchain(transactionHash);
    }
  }

}
