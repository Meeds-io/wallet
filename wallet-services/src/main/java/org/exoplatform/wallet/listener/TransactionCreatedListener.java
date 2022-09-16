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
package org.exoplatform.wallet.listener;

import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

public class TransactionCreatedListener extends Listener<Object, TransactionDetail> {

  private ListenerService          listenerService;

  private WalletTransactionService transactionService;

  public TransactionCreatedListener(ListenerService listenerService, WalletTransactionService transactionService) {
    this.listenerService = listenerService;
    this.transactionService = transactionService;
  }

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    TransactionDetail transactionDetail = event.getData();
    transactionService.cancelTransactionsWithSameNonce(transactionDetail);
    if (StringUtils.isBlank(transactionDetail.getRawTransaction()) && transactionDetail.isPending()) {
      // Transaction sent by external wallet
      listenerService.broadcast(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT, transactionDetail, transactionDetail);
    }
  }

}
