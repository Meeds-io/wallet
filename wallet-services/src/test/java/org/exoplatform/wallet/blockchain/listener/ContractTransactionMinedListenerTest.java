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
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class ContractTransactionMinedListenerTest {

  @Mock
  private WalletTransactionService                walletTransactionService;

  @Mock
  private BlockchainTransactionService            blockchainTransactionService;

  private ContractTransactionMinedListener        listener;

  private Event<String, ContractTransactionEvent> event;

  private TransactionDetail                       transactionDetail;

  private String                                  hash        = "transactionHash";

  private String                                  fromAddress = "fromAddress";

  private String                                  toAddress   = "toAddress";

  private String                                  data        = "data";

  private List<String>                            topics      = Collections.singletonList("topic");

  private long                                    blockNumber = 1234l;

  private ContractTransactionEvent                contractTransactionEvent;

  @Before
  public void setUp() {
    contractTransactionEvent = new ContractTransactionEvent(hash, fromAddress, data, topics, blockNumber);
    event = new Event<String, ContractTransactionEvent>(WalletUtils.CONTRACT_TRANSACTION_MINED_EVENT,
                                                        null,
                                                        contractTransactionEvent);
    listener = new ContractTransactionMinedListener(walletTransactionService, blockchainTransactionService);
    transactionDetail = new TransactionDetail();
    transactionDetail.setHash(hash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTo(toAddress);
  }

  @Test
  public void testOnEventWhenTransactionIsNoKnownAndWalletsNotKnown() throws Exception {
    listener.onEvent(event);

    verify(blockchainTransactionService, never()).refreshTransactionFromBlockchain(any());
  }

  @Test
  public void testOnEventWhenTransactionIsKnownButNotPendingAndAlreadySucceeded() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setPending(false);
    transactionDetail.setSucceeded(true);

    listener.onEvent(event);

    verify(blockchainTransactionService, never()).refreshTransactionFromBlockchain(any());
  }

  @Test
  public void testOnEventWhenTransactionIsKnownButNotPendingAndNotSucceeded() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setPending(false);
    transactionDetail.setSucceeded(false);

    listener.onEvent(event);

    verify(blockchainTransactionService, times(1)).refreshTransactionFromBlockchain(hash);
  }

  @Test
  public void testOnEventWhenTransactionIsKnownButPending() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setPending(true);

    listener.onEvent(event);

    verify(blockchainTransactionService, times(1)).refreshTransactionFromBlockchain(hash);
  }

  @Test
  public void testOnEventWhenTransactionIsNotKnownAndWalletsKnown() throws Exception {
    when(blockchainTransactionService.hasManagedWalletInTransaction(contractTransactionEvent)).thenReturn(true);

    listener.onEvent(event);

    verify(blockchainTransactionService, times(1)).refreshTransactionFromBlockchain(hash);
  }

}
