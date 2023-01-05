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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMinedAndUpdatedListenerTest extends BaseWalletTest {

  @Mock
  private WalletTransactionService           walletTransactionService;

  @Mock
  private BlockchainTransactionService       blockchainTransactionService;

  private TransactionMinedAndUpdatedListener listener;

  private Event<Object, TransactionDetail>   event;

  private TransactionDetail                  transactionDetail = new TransactionDetail();

  @Before
  public void setUp() throws Exception {
    super.setUp();
    event = new Event<Object, TransactionDetail>(WalletUtils.TRANSACTION_MINED_AND_UPDATED_EVENT, null, transactionDetail);
    listener = new TransactionMinedAndUpdatedListener(mock(PortalContainer.class),
                                                      walletTransactionService,
                                                      blockchainTransactionService);
    listener.setWaitBeforeStoppingWatchingTimeout(0);
  }

  @Test
  public void testOnEventWhenNoPendingTransactions() throws Exception {
    listener.onEvent(event);

    verify(blockchainTransactionService, timeout(5000).times(1)).stopWatchingBlockchain();
  }

  @Test
  public void testOnEventWhenHavingPendingTransactions() throws Exception {
    when(walletTransactionService.countContractPendingTransactionsSent()).thenReturn(1l);

    listener.onEvent(event);

    verify(walletTransactionService, timeout(10000).times(1)).countContractPendingTransactionsSent();
    verify(blockchainTransactionService, timeout(5000).times(0)).stopWatchingBlockchain();
  }

  @Test
  public void testOnEventWhenHavingPendingTransactionsToSend() throws Exception {
    when(walletTransactionService.countContractPendingTransactionsToSend()).thenReturn(1l);

    listener.onEvent(event);

    verify(walletTransactionService, timeout(10000).times(1)).countContractPendingTransactionsToSend();
    verify(blockchainTransactionService, timeout(5000).times(0)).stopWatchingBlockchain();
  }

}
