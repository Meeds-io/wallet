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
package org.exoplatform.wallet.test.service;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.exoplatform.wallet.model.transaction.TransactionStatistics;
import org.junit.Test;

import org.exoplatform.services.listener.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

public class WalletTransactionServiceTest extends BaseWalletTest {

  /**
   * Test if container has properly started
   */
  @Test
  public void testContainerStart() {
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    assertNotNull(walletTransactionService);
  }

  /**
   * Test
   * {@link WalletTransactionService#saveTransactionDetail(TransactionDetail, boolean)}
   */
  @Test
  public void testSaveTransactionDetail() {
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());

    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertNotNull(storedTransactionDetail);
    assertEquals(transactionDetail, storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);
  }

  /**
   * Test
   * {@link WalletTransactionService#cancelTransactionsWithSameNonce(TransactionDetail)}
   */
  @Test
  public void testCancelTransactionDetail() {
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());

    TransactionDetail transactionDetailReplacement = storedTransactionDetail.clone();
    transactionDetailReplacement.setHash(generateTransactionHash());
    transactionDetailReplacement.setId(0);
    transactionDetailReplacement.setPending(false);
    walletTransactionService.saveTransactionDetail(transactionDetailReplacement, true);
    TransactionDetail storedTransactionDetailReplacement = walletTransactionService.getTransactionByHash(transactionDetailReplacement.getHash());

    entitiesToClean.add(storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetailReplacement);

    assertNotNull(storedTransactionDetail);
    assertNotNull(storedTransactionDetailReplacement);

    assertEquals(storedTransactionDetail.getNonce(), storedTransactionDetailReplacement.getNonce());
    assertNotEquals(storedTransactionDetail.getHash(), storedTransactionDetailReplacement.getHash());

    final AtomicBoolean listenerInvoked = new AtomicBoolean(false);
    ListenerService listenerService = getService(ListenerService.class);
    listenerService.addListener(WalletUtils.KNOWN_TRANSACTION_REPLACED_EVENT, new Listener<Object, Object>() {
      @Override
      public void onEvent(Event<Object, Object> event) throws Exception {
        listenerInvoked.set(true);
      }
    });

    TransactionDetail errorReplacingTransaction = storedTransactionDetailReplacement.clone();
    errorReplacingTransaction.setFrom("another address");
    walletTransactionService.cancelTransactionsWithSameNonce(errorReplacingTransaction);
    assertFalse(listenerInvoked.get());

    storedTransactionDetail.setPending(true);
    walletTransactionService.saveTransactionDetail(storedTransactionDetail, true);
    errorReplacingTransaction = storedTransactionDetailReplacement.clone();
    errorReplacingTransaction.setTo("Another address");
    walletTransactionService.cancelTransactionsWithSameNonce(errorReplacingTransaction);
    storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertFalse(storedTransactionDetail.isPending());
    assertFalse(storedTransactionDetail.isSucceeded());
    assertFalse(listenerInvoked.get());

    storedTransactionDetail.setPending(true);
    walletTransactionService.saveTransactionDetail(storedTransactionDetail, true);
    errorReplacingTransaction = storedTransactionDetailReplacement.clone();
    errorReplacingTransaction.setContractAmount(5000);
    walletTransactionService.cancelTransactionsWithSameNonce(errorReplacingTransaction);
    storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertFalse(storedTransactionDetail.isPending());
    assertFalse(storedTransactionDetail.isSucceeded());
    assertFalse(listenerInvoked.get());

    storedTransactionDetail.setPending(true);
    walletTransactionService.saveTransactionDetail(storedTransactionDetail, true);
    errorReplacingTransaction = storedTransactionDetailReplacement.clone();
    errorReplacingTransaction.setContractAddress("another address");
    walletTransactionService.cancelTransactionsWithSameNonce(errorReplacingTransaction);
    storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertFalse(storedTransactionDetail.isPending());
    assertFalse(storedTransactionDetail.isSucceeded());
    assertFalse(listenerInvoked.get());

    storedTransactionDetail.setPending(true);
    walletTransactionService.saveTransactionDetail(storedTransactionDetail, true);
    walletTransactionService.cancelTransactionsWithSameNonce(storedTransactionDetailReplacement);
    assertTrue(listenerInvoked.get());
  }

  /**
   * Test
   * {@link WalletTransactionService#saveTransactionDetail(TransactionDetail, String)}
   * 
   * @throws IllegalAccessException when user is not allowed to save transaction
   *           detail
   */
  @Test
  public void testSaveTransactionDetailForCurrentUser() throws IllegalAccessException {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  null,
                                                                  CURRENT_USER_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());

    try {
      walletTransactionService.saveTransactionDetail(transactionDetail, USER_TEST);
    } catch (IllegalAccessException e) {
      // Expected: "root9" shouldn't be able to save transaction of "root1"
    }

    walletTransactionService.saveTransactionDetail(transactionDetail, CURRENT_USER);

    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertNotNull(storedTransactionDetail);
    assertEquals(transactionDetail, storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);
  }

  /**
   * Test {@link WalletTransactionService#getPendingTransactionMaxDays()}
   */
  @Test
  public void testGetPendingTransactionMaxDays() {
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);

    long count = walletTransactionService.getPendingTransactionMaxDays();
    assertEquals("Wrong default value for 'Pending transaction MaxDays'", 1, count);
  }

  /**
   * Test {@link WalletTransactionService#getTransactionByHash(String)}
   */
  @Test
  public void testGetTransactionByHash() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertNotNull(storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);
  }

 /**
   * Test {@link WalletTransactionService#getPendingTransactionByHash(String)}
   */
  @Test
  public void testGetPendingTransactionByHash() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getPendingTransactionByHash(transactionDetail.getHash());
    assertNotNull(storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);
  }


  /**
   * Test {@link WalletTransactionService#countTransactionsByNonce(TransactionDetail)}
   */
  @Test
  public void testCountTransactionsByNonce() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail1 = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    TransactionDetail transactionDetail2 = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail1, true);
    walletTransactionService.saveTransactionDetail(transactionDetail2, true);
    long countOfTransactions = walletTransactionService.countTransactionsByNonce(transactionDetail1);
    assertEquals(2, countOfTransactions);
    entitiesToClean.add(transactionDetail1);
    entitiesToClean.add(transactionDetail2);
  }

  /**
   * Test {@link WalletTransactionService#getNonce(String, String)}
   */
  @Test
  public void testGetNonce() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, false);
    entitiesToClean.add(transactionDetail);

    try {
      long nonce = walletTransactionService.getNonce(WALLET_ADDRESS_1, CURRENT_USER);
      assertEquals(NONCE + 1, nonce);
    } catch (IllegalAccessException e) {
      fail("Unexpected error while getting nonce of wallet");
    }

    try {
      walletTransactionService.getNonce(WALLET_ADDRESS_1, USER_TEST);
      fail("Expected to throw error while getting nonce of wallet of other user");
    } catch (IllegalAccessException e) {
      // Expected
    }

    transactionDetail.setPending(false);
    walletTransactionService.saveTransactionDetail(transactionDetail, false);

    try {
      long nonce = walletTransactionService.getNonce(WALLET_ADDRESS_1, CURRENT_USER);
      assertEquals(0, nonce);
    } catch (IllegalAccessException e) {
      fail("Unexpected error while getting nonce of wallet");
    }
  }

  /**
   * Test {@link WalletTransactionService#getTransactionStatistics(String, String, String, Locale)} (String, String)}
   */
  @Test
  public void testGetTransactionStatistics() {
    String YEAR_PERIODICITY = "year";
    String MONTH_PERIODICITY = "month";
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    try {
      walletTransactionService.getTransactionStatistics(null, YEAR_PERIODICITY, "2022-01-14", Locale.ENGLISH);
      fail("Address should not be empty!");
    } catch (Exception e) {
      // Expected
    }
    try {
      walletTransactionService.getTransactionStatistics(WALLET_ADDRESS_1, null, "2022-01-14", Locale.ENGLISH);
      fail("Periodicity should not be empty!");
    } catch (Exception e) {
      // Expected
    }
    TransactionStatistics transactionStatistics = walletTransactionService.getTransactionStatistics(WALLET_ADDRESS_1, YEAR_PERIODICITY, "2022-01-14", Locale.ENGLISH);
    assertNotNull(transactionStatistics);
    transactionStatistics = walletTransactionService.getTransactionStatistics(WALLET_ADDRESS_1, MONTH_PERIODICITY, "2022-01-14", Locale.ENGLISH);
    assertNotNull(transactionStatistics);
  }
  /**
   * Test {@link WalletTransactionService#getTransactionByHash(String, String)}
   */
  @Test
  public void testGetTransactionByHashAndUser() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  null,
                                                                  CURRENT_USER_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  null,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash(),
                                                                                              USER_TEST);
    assertNotNull(storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);

    assertNull("Other users shouldn't be able to access label of transaction of others", storedTransactionDetail.getLabel());
    assertNotNull("Other users should be able to access message of transaction of others", storedTransactionDetail.getMessage());

    storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash(), CURRENT_USER);
    assertNotNull(storedTransactionDetail);
    assertNotNull("Current user should be able to access label of his transaction", storedTransactionDetail.getLabel());
  }

  /**
   * Test
   * {@link WalletTransactionService#canSendTransactionToBlockchain(String)}
   */
  @Test
  public void testCanSendTransactionToBlockchain() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    long maxAttemptsToSend = walletTransactionService.getMaxParallelPendingTransactions();
    for (int i = 0; i < maxAttemptsToSend; i++) {
      assertTrue(walletTransactionService.canSendTransactionToBlockchain(WALLET_ADDRESS_1));

      TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                    WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                    CONTRACT_AMOUNT,
                                                                    ETHER_VALUE,
                                                                    WALLET_ADDRESS_1,
                                                                    WALLET_ADDRESS_2,
                                                                    null,
                                                                    CURRENT_USER_IDENTITY_ID,
                                                                    TRANSACTION_LABEL,
                                                                    TRANSACTION_MESSAGE,
                                                                    false,
                                                                    true,
                                                                    false,
                                                                    RAW_TRANSACTION,
                                                                    System.currentTimeMillis());
      transactionDetail.setSendingAttemptCount(1);
      walletTransactionService.saveTransactionDetail(transactionDetail, false);
      entitiesToClean.add(transactionDetail);
    }

    assertFalse(walletTransactionService.canSendTransactionToBlockchain(WALLET_ADDRESS_1));
  }

  /**
   * Test {@link WalletTransactionService#getTransactionsToSend()}
   */
  @Test
  public void testGetTransactionsToSend() {
    addCurrentUserWallet();

    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);

    List<TransactionDetail> listOfTransactionsToSend = walletTransactionService.getTransactionsToSend();
    assertNotNull(listOfTransactionsToSend);
    assertEquals(0, listOfTransactionsToSend.size());

    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  null,
                                                                  CURRENT_USER_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  false,
                                                                  true,
                                                                  false,
                                                                  RAW_TRANSACTION,
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, false);
    entitiesToClean.add(transactionDetail);

    listOfTransactionsToSend = walletTransactionService.getTransactionsToSend();
    assertNotNull(listOfTransactionsToSend);
    assertEquals(1, listOfTransactionsToSend.size());

    transactionDetail.setPending(false);
    walletTransactionService.saveTransactionDetail(transactionDetail, false);

    listOfTransactionsToSend = walletTransactionService.getTransactionsToSend();
    assertNotNull(listOfTransactionsToSend);
    assertEquals(0, listOfTransactionsToSend.size());
  }

  /**
   * Test {@link WalletTransactionService#getPendingTransactions()}
   */
  @Test
  public void testGetPendingTransactionHashes() {
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    List<TransactionDetail> pendingTransactions = walletTransactionService.getPendingTransactions();
    assertNotNull(pendingTransactions);
    assertEquals(0, pendingTransactions.size());

    TransactionDetail pendingTransactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                         WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                         CONTRACT_AMOUNT,
                                                                         ETHER_VALUE,
                                                                         WALLET_ADDRESS_1,
                                                                         WALLET_ADDRESS_2,
                                                                         WALLET_ADDRESS_3,
                                                                         USER_TEST_IDENTITY_ID,
                                                                         TRANSACTION_LABEL,
                                                                         TRANSACTION_MESSAGE,
                                                                         false,
                                                                         true,
                                                                         false,
                                                                         null,
                                                                         System.currentTimeMillis());
    entitiesToClean.add(pendingTransactionDetail);
    TransactionDetail transactionDetail = createTransactionDetail(generateTransactionHash(),
                                                                  WalletUtils.CONTRACT_FUNC_TRANSFERFROM,
                                                                  CONTRACT_AMOUNT,
                                                                  ETHER_VALUE,
                                                                  WALLET_ADDRESS_1,
                                                                  WALLET_ADDRESS_2,
                                                                  WALLET_ADDRESS_3,
                                                                  USER_TEST_IDENTITY_ID,
                                                                  TRANSACTION_LABEL,
                                                                  TRANSACTION_MESSAGE,
                                                                  true,
                                                                  false,
                                                                  true,
                                                                  null,
                                                                  System.currentTimeMillis());
    entitiesToClean.add(transactionDetail);

    assertEquals(1, walletTransactionService.countPendingTransactions());
    pendingTransactions = walletTransactionService.getPendingTransactions();
    assertNotNull(pendingTransactions);
    assertEquals(1, pendingTransactions.size());
    assertEquals(pendingTransactionDetail.getHash(), pendingTransactions.iterator().next().getHash());
  }

}
