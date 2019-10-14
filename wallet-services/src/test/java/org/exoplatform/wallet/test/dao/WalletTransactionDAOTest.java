/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.test.dao;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.dao.WalletTransactionDAO;
import org.exoplatform.wallet.entity.TransactionEntity;
import org.exoplatform.wallet.test.BaseWalletTest;

public class WalletTransactionDAOTest extends BaseWalletTest {

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);

    List<TransactionEntity> allTransactions = walletTransactionDAO.findAll();
    assertNotNull("Returned transactions shouldn't be null", allTransactions);
    assertEquals("Returned transactions list should be empty", 0, allTransactions.size());
  }

  /**
   * Test get list of transactions of a chosen contract
   */
  @Test
  public void testGetContractTransactions() {
    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";
    String contractMethodName = "transfer";

    generateTransactions("0xe9dfec7864af9e581a85ce3987d026be0f50aaaa", contractAddress, contractMethodName);

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    // Test filter by contract and method name
    List<TransactionEntity> transactions = walletTransactionDAO.getContractTransactions(contractAddress,
                                                                                        contractMethodName,
                                                                                        100);
    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned contract transactions list count is not coherent", 10, transactions.size());

    // Test pagination
    transactions = walletTransactionDAO.getContractTransactions(contractAddress,
                                                                contractMethodName,
                                                                5);
    assertEquals("Returned contract transactions list count is not coherent", 5, transactions.size());

    // Test get all transactions independing from contract method name
    transactions = walletTransactionDAO.getContractTransactions(contractAddress,
                                                                null,
                                                                100);
    assertEquals("Returned contract transactions list count with not method name is not coherent", 30, transactions.size());

    // Test when limit = 0
    transactions = walletTransactionDAO.getContractTransactions(contractAddress,
                                                                null,
                                                                0);
    assertEquals("Returned contract transactions list count with limit = 0 is not coherent", 30, transactions.size());
  }

  /**
   * Test get list of transactions of a wallet
   */
  @Test
  public void testGetWalletTransactions() {
    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";
    String contractMethodName = "transfer";
    String firstAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";

    generateTransactions(firstAddress, contractAddress, contractMethodName);

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);

    // Search all transactions where a wallet is receiver, sender delegator
    boolean includeAdministrationTransactions = true;
    boolean onlyPending = false;
    List<TransactionEntity> transactions = walletTransactionDAO.getWalletTransactions(1,
                                                                                      firstAddress,
                                                                                      null,
                                                                                      null,
                                                                                      0,
                                                                                      onlyPending,
                                                                                      includeAdministrationTransactions);

    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned wallet transactions list count is not coherent", 60, transactions.size());

    // Test pagination
    transactions = walletTransactionDAO.getWalletTransactions(1,
                                                              firstAddress,
                                                              null,
                                                              null,
                                                              20,
                                                              onlyPending,
                                                              includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 20, transactions.size());

    // Filter on contract address
    transactions = walletTransactionDAO.getWalletTransactions(1,
                                                              firstAddress,
                                                              contractAddress,
                                                              null,
                                                              0,
                                                              onlyPending,
                                                              includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 30, transactions.size());

    // Filter on contract method name
    transactions = walletTransactionDAO.getWalletTransactions(1,
                                                              firstAddress,
                                                              contractAddress,
                                                              contractMethodName,
                                                              0,
                                                              onlyPending,
                                                              includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 10, transactions.size());

    // Filter on only pending transactions
    onlyPending = true;
    transactions = walletTransactionDAO.getWalletTransactions(1,
                                                              firstAddress,
                                                              contractAddress,
                                                              contractMethodName,
                                                              0,
                                                              onlyPending,
                                                              includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 10, transactions.size());

    // Filter on only pending transactions
    includeAdministrationTransactions = false;
    transactions = walletTransactionDAO.getWalletTransactions(1,
                                                              firstAddress,
                                                              contractAddress,
                                                              contractMethodName,
                                                              0,
                                                              onlyPending,
                                                              includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 10, transactions.size());
  }

  /**
   * Test get list pending transactions
   */
  @Test
  public void testGetPendingTransactions() {
    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";
    String contractMethodName = "transfer";
    String firstAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";

    generateTransactions(firstAddress, contractAddress, contractMethodName);

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);

    // Search all pending transactions using the network id
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingTransactions(1);

    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned pending transactions list count on a network is not coherent", 30, transactions.size());

    // Use non existing network ID
    transactions = walletTransactionDAO.getPendingTransactions(2);
    assertEquals("Returned wallet transactions list count on a non existing network is not coherent", 0, transactions.size());
  }

  /**
   * Test search transaction by hash
   */
  @Test
  public void testGetTransactionByHash() {
    String hashOfTX = "hashTX";
    createTransaction(hashOfTX,
                      null,
                      null,
                      0, // token amount
                      0, // ether amount
                      "from",
                      "to",
                      "by",
                      0,
                      "label",
                      "message",
                      true, // isSuccess
                      true, // isPending
                      true, // isAdminOperation
                      System.currentTimeMillis());

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    TransactionEntity transactionEntity = walletTransactionDAO.getTransactionByHash(hashOfTX);
    assertNotNull("Can't find previously saved transaction with given hash", transactionEntity);

    transactionEntity = walletTransactionDAO.getTransactionByHash("Fake Hash");
    assertNull("Shouldn't find a non existing transaction with fake hash", transactionEntity);
  }

  /**
   * Test count received contract amounts
   */
  @Test
  public void testCountReceivedContractAmount() {
    String contractAddress = "contractAddress";
    String address = "address";
    String contractMethodName = "reward";

    generateTransactions(address, contractAddress, contractMethodName, -1000);

    ZonedDateTime start = ZonedDateTime.now();
    generateTransactions(address, contractAddress, contractMethodName);
    ZonedDateTime end = ZonedDateTime.now().plusSeconds(1);

    generateTransactions(address, contractAddress, contractMethodName, 3000);

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    double receivedAmount = walletTransactionDAO.countReceivedContractAmount(contractAddress, address, start, end);
    assertEquals("Received amount isn't coherent", 10, receivedAmount, 0);
  }

  /**
   * Test count sent contract amounts
   */
  @Test
  public void testCountSentContractAmount() {
    String contractAddress = "contractAddress";
    String address = "address";
    String contractMethodName = "transfer";

    generateTransactions(address, contractAddress, contractMethodName, -1000);

    ZonedDateTime start = ZonedDateTime.now();
    generateTransactions(address, contractAddress, contractMethodName);
    ZonedDateTime end = ZonedDateTime.now().plusSeconds(1);

    generateTransactions(address, contractAddress, contractMethodName, 3000);

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    double sentAmount = walletTransactionDAO.countSentContractAmount(contractAddress, address, start, end);
    assertEquals("Sent amount isn't coherent", 10, sentAmount, 0);
  }

}
