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
package io.meeds.wallet.blockchain.listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

import io.meeds.wallet.model.TransactionDetail;
import io.meeds.wallet.service.BlockchainTransactionService;
import io.meeds.wallet.service.WalletTransactionService;

/**
 * This listener will be triggered when a transaction is mined and updated on
 * database. The listener will optimize Blockchain Subscription time to stop
 * watching to Blockchain there are no more contract transactions to send nor
 * sent and remains pending
 */
@Asynchronous
public class TransactionMinedAndUpdatedListener extends Listener<Object, TransactionDetail> {

  private PortalContainer              container;

  private WalletTransactionService     walletTransactionService;

  private BlockchainTransactionService blockchainTransactionService;

  private int                          waitBeforeStopWatchingTimeout = 1;

  public TransactionMinedAndUpdatedListener(PortalContainer container,
                                            WalletTransactionService walletTransactionService,
                                            BlockchainTransactionService blockchainTransactionService) {
    this.container = container;
    this.walletTransactionService = walletTransactionService;
    this.blockchainTransactionService = blockchainTransactionService;
  }

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    CompletableFuture.runAsync(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        if (this.walletTransactionService.countContractPendingTransactionsSent() == 0
            && this.walletTransactionService.countContractPendingTransactionsToSend() == 0) {
          this.blockchainTransactionService.stopWatchingBlockchain();
        }
      } finally {
        RequestLifeCycle.end();
        ExoContainerContext.setCurrentContainer(null);
      }
    }, CompletableFuture.delayedExecutor(waitBeforeStopWatchingTimeout, TimeUnit.SECONDS));
  }

  public void setWaitBeforeStoppingWatchingTimeout(int waitBeforeStopWatchingTimeout) {
    this.waitBeforeStopWatchingTimeout = waitBeforeStopWatchingTimeout;
  }
}
