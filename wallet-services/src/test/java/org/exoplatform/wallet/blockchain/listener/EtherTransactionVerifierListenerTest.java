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
public class EtherTransactionVerifierListenerTest {

  @Mock
  private WalletTransactionService           transactionService;

  @Mock
  private BlockchainTransactionService       blockchainTransactionService;

  private EtherTransactionVerifierListener   listener;

  private Event<Object, Map<String, Object>> event;

  private TransactionDetail                  transactionDetail;

  private String                             hash        = "transactionHash";

  private String                             fromAddress = "fromAddress";

  private String                             toAddress   = "toAddress";

  private String                             byAddress   = "byAddress";

  @Before
  public void setUp() {
    listener = new EtherTransactionVerifierListener(blockchainTransactionService, transactionService);
    event = new Event<Object, Map<String, Object>>(WalletUtils.TRANSACTION_MINED_EVENT,
                                                   null,
                                                   Collections.singletonMap("hash", hash));
    transactionDetail = new TransactionDetail();
    transactionDetail.setHash(hash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTo(toAddress);
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
    verify(transactionService, times(0)).getPendingEtherTransactions(any());
  }

  @Test
  public void testOnEventWhenTransactionIsForContractTransfer() throws Exception {
    when(transactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFER);
    transactionDetail.setFromWallet(new Wallet());
    transactionDetail.getFromWallet().setAddress(fromAddress);
    transactionDetail.setToWallet(new Wallet());
    transactionDetail.getToWallet().setAddress(toAddress);
    transactionDetail.setByWallet(new Wallet());
    transactionDetail.getByWallet().setAddress(byAddress);

    List<TransactionDetail> fromTransactions = Collections.singletonList(new TransactionDetail());
    fromTransactions.get(0).setHash("from1");
    when(transactionService.getPendingEtherTransactions(fromAddress)).thenReturn(fromTransactions);

    List<TransactionDetail> toTransactions = Collections.singletonList(new TransactionDetail());
    toTransactions.get(0).setHash("to1");
    when(transactionService.getPendingEtherTransactions(toAddress)).thenReturn(toTransactions);

    listener.onEvent(event);
    verify(transactionService, times(3)).getPendingEtherTransactions(any());
    verify(transactionService, times(1)).getPendingEtherTransactions(fromAddress);
    verify(transactionService, times(1)).getPendingEtherTransactions(toAddress);
    verify(transactionService, times(1)).getPendingEtherTransactions(byAddress);

    verify(blockchainTransactionService, times(2)).refreshTransactionFromBlockchain(any());
    verify(blockchainTransactionService, times(1)).refreshTransactionFromBlockchain(fromTransactions.get(0).getHash());
    verify(blockchainTransactionService, times(1)).refreshTransactionFromBlockchain(toTransactions.get(0).getHash());
  }

}
