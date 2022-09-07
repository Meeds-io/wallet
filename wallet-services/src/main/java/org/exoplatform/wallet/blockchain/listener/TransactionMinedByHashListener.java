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
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

@Asynchronous
public class TransactionMinedByHashListener extends Listener<String, Object> {

  private WalletTransactionService walletTransactionService;

  public TransactionMinedByHashListener(WalletTransactionService walletTransactionService) {
    this.walletTransactionService = walletTransactionService;
  }

  @Override
  public void onEvent(Event<String, Object> event) throws Exception {
    String transactionHash = event.getSource();
    TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(transactionHash);
    if (transactionDetail != null) {
      walletTransactionService.broadcastTransactionMinedEvent(transactionDetail);
    }
  }

}
