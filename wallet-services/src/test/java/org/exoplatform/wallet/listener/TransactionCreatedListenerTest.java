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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class TransactionCreatedListenerTest extends BaseWalletTest {

  @Mock
  private WalletTransactionService         walletTransactionService;

  @Mock
  private ListenerService                  listenerService;

  private TransactionCreatedListener       listener;

  private Event<Object, TransactionDetail> event;

  private TransactionDetail                transactionDetail;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    transactionDetail = new TransactionDetail();
    listener = new TransactionCreatedListener(listenerService, walletTransactionService);
    event = new Event<Object, TransactionDetail>(WalletUtils.TRANSACTION_MINED_EVENT, transactionDetail, transactionDetail);
  }

  @Test
  public void testOnEventForInternalWalletWithPendingTransaction() throws Exception {
    transactionDetail.setRawTransaction("RawTransaction");
    transactionDetail.setPending(true);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @Test
  public void testOnEventForInternalWalletWithNonPendingTransaction() throws Exception {
    transactionDetail.setRawTransaction("RawTransaction");
    transactionDetail.setPending(false);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @Test
  public void testOnEventForExternalWalletWithPendingTransaction() throws Exception {
    transactionDetail.setPending(true);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, times(1)).broadcast(WalletUtils.TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT,
                                                transactionDetail,
                                                transactionDetail);
  }

  @Test
  public void testOnEventForExternalWalletWithNotPendingTransaction() throws Exception {
    transactionDetail.setPending(false);
    listener.onEvent(event);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

}
