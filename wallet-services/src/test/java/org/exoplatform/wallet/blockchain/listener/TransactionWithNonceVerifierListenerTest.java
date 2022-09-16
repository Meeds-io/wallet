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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class TransactionWithNonceVerifierListenerTest {

  private static final int                     NONCE       = 12;

  @Mock
  private WalletTransactionService             transactionService;

  @Mock
  private BlockchainTransactionService         blockchainTransactionService;

  private TransactionWithNonceVerifierListener listener;

  private Event<Object, Map<String, Object>>   event;

  private TransactionDetail                    transactionDetail;

  private String                               hash        = "transactionHash";

  private String                               fromAddress = "fromAddress";

  @Before
  public void setUp() {
    listener = new TransactionWithNonceVerifierListener(blockchainTransactionService, transactionService);
    event = new Event<Object, Map<String, Object>>(WalletUtils.TRANSACTION_MINED_EVENT,
                                                   null,
                                                   Collections.singletonMap("hash", hash));
    transactionDetail = new TransactionDetail();
    transactionDetail.setHash(hash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setNonce(NONCE);
  }

  @Test
  public void testOnEventWhenTransactionIsNull() throws Exception {
    listener.onEvent(event);
    verify(transactionService, times(1)).getTransactionByHash(hash);
    verifyNoInteractions(blockchainTransactionService);
  }

  @Test
  public void testOnEventWhenTransactionIsPending() throws Exception {
    when(transactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setPending(true);

    listener.onEvent(event);
    verifyNoInteractions(blockchainTransactionService);
  }

  @Test
  public void testOnEventWhenTransactionIsNotForContract() throws Exception {
    when(transactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);

    listener.onEvent(event);
    verifyNoInteractions(blockchainTransactionService);
    verify(transactionService, never()).getPendingWalletTransactionsSent(any());
  }

  @Test
  public void testOnEventWhenTransactionIsForContractTransfer() throws Exception {
    when(transactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFER);
    transactionDetail.setFromWallet(new Wallet());
    transactionDetail.getFromWallet().setAddress(fromAddress);

    List<TransactionDetail> fromTransactions = Collections.singletonList(new TransactionDetail());
    fromTransactions.get(0).setHash("from1");
    fromTransactions.get(0).setNonce(NONCE - 1);
    when(transactionService.getPendingWalletTransactionsSent(fromAddress)).thenReturn(fromTransactions);

    listener.onEvent(event);
    verify(transactionService, times(1)).getPendingWalletTransactionsSent(any());
    verify(transactionService, times(1)).getPendingWalletTransactionsSent(fromAddress);

    verify(blockchainTransactionService, times(1)).addTransactionToRefreshFromBlockchain(fromTransactions.get(0));
  }

}
