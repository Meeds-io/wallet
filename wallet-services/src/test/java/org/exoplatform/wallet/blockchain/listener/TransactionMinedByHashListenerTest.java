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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMinedByHashListenerTest {

  @Mock
  private WalletTransactionService       walletTransactionService;

  private TransactionMinedByHashListener listener;

  @Before
  public void setUp() {
    listener = new TransactionMinedByHashListener(walletTransactionService);
  }

  @Test
  public void testOnEventWhenTransactionHashIsNull() throws Exception {
    Event<String, Object> event = new Event<String, Object>(WalletUtils.TRANSACTION_MINED_BY_HASH_EVENT, null, null);
    listener.onEvent(event);
    verify(walletTransactionService, times(0)).broadcastTransactionMinedEvent(any());
  }

  @Test
  public void testOnEventWhenTransactionHashIsNotKnown() throws Exception {
    Event<String, Object> event = new Event<String, Object>(WalletUtils.TRANSACTION_MINED_BY_HASH_EVENT, "NotKnownHash", null);
    listener.onEvent(event);
    verify(walletTransactionService, times(0)).broadcastTransactionMinedEvent(any());
  }

  @Test
  public void testOnEventWhenTransactionHashIsNotPending() throws Exception {
    TransactionDetail transactionDetail = mock(TransactionDetail.class);
    when(transactionDetail.isPending()).thenReturn(false);
    String hash = "KnownHash";
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);

    Event<String, Object> event = new Event<String, Object>(WalletUtils.TRANSACTION_MINED_BY_HASH_EVENT, hash, null);
    listener.onEvent(event);

    verify(walletTransactionService, times(0)).broadcastTransactionMinedEvent(any());
  }

  @Test
  public void testOnEventWhenTransactionHashIsPending() throws Exception {
    TransactionDetail transactionDetail = mock(TransactionDetail.class);
    when(transactionDetail.isPending()).thenReturn(true);
    String hash = "KnownHash";
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);

    Event<String, Object> event = new Event<String, Object>(WalletUtils.TRANSACTION_MINED_BY_HASH_EVENT, hash, null);
    listener.onEvent(event);

    verify(walletTransactionService, times(1)).broadcastTransactionMinedEvent(transactionDetail);
  }

}
