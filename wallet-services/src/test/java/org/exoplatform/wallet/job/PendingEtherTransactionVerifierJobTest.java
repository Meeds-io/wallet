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
package org.exoplatform.wallet.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

@RunWith(MockitoJUnitRunner.class)
public class PendingEtherTransactionVerifierJobTest {

  private static final String        ADDRESS = "address";

  @Mock
  WalletTransactionService           walletTransactionService;

  @Mock
  BlockchainTransactionService       blockchainTransactionService;

  @Mock
  WalletAccountService               walletAccountService;

  PendingEtherTransactionVerifierJob etherTransactionVerifierJob;

  @Before
  public void setUp() {
    etherTransactionVerifierJob = new PendingEtherTransactionVerifierJob();
    etherTransactionVerifierJob.blockchainTransactionService = blockchainTransactionService;
    etherTransactionVerifierJob.walletTransactionService = walletTransactionService;
    etherTransactionVerifierJob.walletAccountService = walletAccountService;
  }

  @Test
  public void testRunJobWithNoAdminWallet() throws Exception {
    etherTransactionVerifierJob.execute(null);

    verify(walletTransactionService, never()).getPendingEtherTransactions(any());
  }

  @Test
  public void testRunJobWithExceptionDoesntExit() throws Exception {
    doThrow(new RuntimeException("FAKE EXCEPTION")).when(walletAccountService).getAdminWallet();

    etherTransactionVerifierJob.execute(null);

    verify(walletTransactionService, never()).getPendingEtherTransactions(any());
  }

  @Test
  public void testRunJobWithEmptyTransactions() throws Exception {
    Wallet adminWallet = new Wallet();
    adminWallet.setAddress(ADDRESS);
    when(walletAccountService.getAdminWallet()).thenReturn(adminWallet);

    etherTransactionVerifierJob.execute(null);

    verify(walletTransactionService, times(1)).getPendingEtherTransactions(ADDRESS);
    verify(blockchainTransactionService, never()).addTransactionToRefreshFromBlockchain(any());
  }

  @Test
  public void testRunJobWithPendingTransactions() throws Exception {
    Wallet adminWallet = new Wallet();
    adminWallet.setAddress(ADDRESS);
    when(walletAccountService.getAdminWallet()).thenReturn(adminWallet);
    when(walletTransactionService.getPendingEtherTransactions(ADDRESS)).thenReturn(Arrays.asList(new TransactionDetail(),
                                                                                                 new TransactionDetail(),
                                                                                                 new TransactionDetail()));

    etherTransactionVerifierJob.execute(null);

    verify(blockchainTransactionService, times(3)).addTransactionToRefreshFromBlockchain(any());
  }

  @Test
  public void testRunJobWithPendingTransactionsNoInterruptWhenException() throws Exception {
    Wallet adminWallet = new Wallet();
    adminWallet.setAddress(ADDRESS);
    when(walletAccountService.getAdminWallet()).thenReturn(adminWallet);
    TransactionDetail transactionDetail1 = new TransactionDetail();
    transactionDetail1.setId(1l);
    TransactionDetail transactionDetail2 = new TransactionDetail();
    transactionDetail2.setId(2l);
    TransactionDetail transactionDetail3 = new TransactionDetail();
    transactionDetail3.setId(3l);
    when(walletTransactionService.getPendingEtherTransactions(ADDRESS)).thenReturn(Arrays.asList(transactionDetail1,
                                                                                                 transactionDetail2,
                                                                                                 transactionDetail3));
    doThrow(RuntimeException.class).when(blockchainTransactionService).addTransactionToRefreshFromBlockchain(transactionDetail1);

    etherTransactionVerifierJob.execute(null);

    verify(blockchainTransactionService, times(3)).addTransactionToRefreshFromBlockchain(any());
  }

}
