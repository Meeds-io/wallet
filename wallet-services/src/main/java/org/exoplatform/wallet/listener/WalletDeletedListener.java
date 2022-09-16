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
package org.exoplatform.wallet.listener;

import java.util.List;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * This listener is triggered when a user wallet is deleted to remove all its
 * not sent pending transactions
 */
public class WalletDeletedListener extends Listener<Wallet, String> {

  private WalletTransactionService transactionService;

  public WalletDeletedListener(WalletTransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Override
  public void onEvent(Event<Wallet, String> event) throws Exception {
    Wallet wallet = event.getSource();
    List<TransactionDetail> pendingTransactions = transactionService.getPendingWalletTransactionsNotSent(wallet.getAddress());
    if (pendingTransactions != null) {
      pendingTransactions.forEach(transactionDetail -> {
        // Cancel only not sent transactions yet
        transactionDetail.setPending(false);
        transactionService.saveTransactionDetail(transactionDetail, true);
      });
    }
  }

}
