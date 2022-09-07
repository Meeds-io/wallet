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

import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_MODIFIED_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class TransactionMinedListenerTest {

  @Mock
  private WalletAccountService               accountService;

  @Mock
  private WalletTransactionService           walletTransactionService;

  @Mock
  private ListenerService                    listenerService;

  private TransactionMinedListener           listener;

  private Event<Object, Map<String, Object>> event;

  private TransactionDetail                  transactionDetail;

  private String                             hash        = "transactionHash";

  private String                             fromAddress = "fromAddress";

  private String                             toAddress   = "toAddress";

  private String                             byAddress   = "byAddress";

  @Before
  public void setUp() {
    listener = new TransactionMinedListener(accountService, walletTransactionService, listenerService);
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
    verify(walletTransactionService, times(1)).getTransactionByHash(hash);
    verify(walletTransactionService, times(0)).cancelTransactionsWithSameNonce(any());
    verifyNoInteractions(accountService, listenerService);
  }

  @Test
  public void testOnEventWhenTransactionIsSucceeded() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setSucceeded(true);

    listener.onEvent(event);
    verifyNoInteractions(accountService);
    verify(walletTransactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
    verify(listenerService, times(1)).broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
  }

  @Test
  public void testOnEventWhenTransactionIsNotForContract() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);

    listener.onEvent(event);
    verifyNoInteractions(accountService);
    verify(walletTransactionService, times(0)).cancelTransactionsWithSameNonce(any());
    verify(listenerService, times(1)).broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
  }

  @Test
  public void testOnEventWhenTransactionIsForContractTransfer() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFER);
    transactionDetail.setFromWallet(new Wallet());
    transactionDetail.getFromWallet().setAddress(fromAddress);
    transactionDetail.setToWallet(new Wallet());
    transactionDetail.getToWallet().setAddress(toAddress);

    listener.onEvent(event);
    verify(walletTransactionService, times(0)).cancelTransactionsWithSameNonce(any());
    verify(listenerService, times(1)).broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
    Map<String, Set<String>> walletModifications = new HashMap<>();
    walletModifications.put(fromAddress,
                            new HashSet<>(Arrays.asList(WalletUtils.ETHER_FUNC_SEND_FUNDS, MeedsToken.FUNC_TRANSFER)));
    walletModifications.put(toAddress, Collections.singleton(MeedsToken.FUNC_TRANSFER));
    verify(accountService, times(1)).refreshWalletsFromBlockchain(walletModifications);
  }

  @Test
  public void testOnEventWhenTransactionIsForContractTransferFrom() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFERFROM);
    transactionDetail.setFromWallet(new Wallet());
    transactionDetail.getFromWallet().setAddress(fromAddress);
    transactionDetail.setToWallet(new Wallet());
    transactionDetail.getToWallet().setAddress(toAddress);
    transactionDetail.setByWallet(new Wallet());
    transactionDetail.getByWallet().setAddress(byAddress);

    listener.onEvent(event);
    verify(walletTransactionService, times(0)).cancelTransactionsWithSameNonce(any());
    verify(listenerService, times(1)).broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
    Map<String, Set<String>> walletModifications = new HashMap<>();
    walletModifications.put(fromAddress,
                            new HashSet<>(Arrays.asList(WalletUtils.ETHER_FUNC_SEND_FUNDS, MeedsToken.FUNC_TRANSFERFROM)));
    walletModifications.put(toAddress, Collections.singleton(MeedsToken.FUNC_TRANSFERFROM));
    walletModifications.put(byAddress, Collections.singleton(MeedsToken.FUNC_TRANSFERFROM));
    verify(accountService, times(1)).refreshWalletsFromBlockchain(walletModifications);
  }

  @Test
  public void testOnEventWhenTransactionIsForContractApprove() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_APPROVE);
    transactionDetail.setFromWallet(new Wallet());
    transactionDetail.getFromWallet().setAddress(fromAddress);
    transactionDetail.setToWallet(new Wallet());
    transactionDetail.getToWallet().setAddress(toAddress);

    listener.onEvent(event);
    verify(walletTransactionService, times(0)).cancelTransactionsWithSameNonce(any());
    verify(listenerService, times(1)).broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
    Map<String, Set<String>> walletModifications = new HashMap<>();
    walletModifications.put(fromAddress,
                            new HashSet<>(Arrays.asList(WalletUtils.ETHER_FUNC_SEND_FUNDS, MeedsToken.FUNC_APPROVE)));
    verify(accountService, times(1)).refreshWalletsFromBlockchain(walletModifications);
  }

  @Test
  public void testOnEventWhenTransactionIsForContractTransferOwnership() throws Exception {
    when(walletTransactionService.getTransactionByHash(hash)).thenReturn(transactionDetail);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFEROWNERSHIP);
    transactionDetail.setFromWallet(new Wallet());
    transactionDetail.getFromWallet().setAddress(fromAddress);
    transactionDetail.setToWallet(new Wallet());
    transactionDetail.getToWallet().setAddress(toAddress);

    listener.onEvent(event);
    verify(walletTransactionService, times(0)).cancelTransactionsWithSameNonce(any());
    verify(listenerService, times(1)).broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
    Map<String, Set<String>> walletModifications = new HashMap<>();
    walletModifications.put(fromAddress,
                            new HashSet<>(Arrays.asList(WalletUtils.ETHER_FUNC_SEND_FUNDS, MeedsToken.FUNC_TRANSFEROWNERSHIP)));
    walletModifications.put(toAddress, Collections.singleton(MeedsToken.FUNC_TRANSFEROWNERSHIP));
    verify(accountService, times(1)).refreshWalletsFromBlockchain(walletModifications);
  }

}
