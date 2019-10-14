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
package org.exoplatform.wallet.test.storage;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import org.exoplatform.wallet.entity.TransactionEntity;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.storage.TransactionStorage;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

public class TransactionStorageTest extends BaseWalletTest {

  /**
   * Check that service is instantiated
   */
  @Test
  public void testServiceInstantiated() {
    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    assertNotNull(transactionStorage);
  }

  /**
   * Test get list of transactions of a chosen contract
   */
  @Test
  public void testGetContractTransactions() {
    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";
    String contractMethodName = "transfer";

    generateTransactions("0xe9dfec7864af9e581a85ce3987d026be0f50aaaa", contractAddress, contractMethodName);

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    // Test filter by contract and method name
    List<TransactionDetail> transactions = transactionStorage.getContractTransactions(contractAddress,
                                                                                      contractMethodName,
                                                                                      100);
    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned contract transactions list count is not coherent", 10, transactions.size());

    // Test pagination
    transactions = transactionStorage.getContractTransactions(contractAddress,
                                                              contractMethodName,
                                                              5);
    assertEquals("Returned contract transactions list count is not coherent", 5, transactions.size());

    // Test get all transactions independing from contract method name
    transactions = transactionStorage.getContractTransactions(contractAddress,
                                                              null,
                                                              100);
    assertEquals("Returned contract transactions list count with not method name is not coherent", 30, transactions.size());

    // Test when limit = 0
    transactions = transactionStorage.getContractTransactions(contractAddress,
                                                              null,
                                                              0);
    assertEquals("Returned contract transactions list count with limit = 0 is not coherent", 30, transactions.size());
  }

  /**
   * Test get list of transactions of a chosen network
   */
  @Test
  public void testGetTransactions() {
    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";
    String contractMethodName = "transfer";

    long totalTransactionsSize = generateTransactions("0xe9dfec7864af9e581a85ce3987d026be0f50aaaa",
                                                      contractAddress,
                                                      contractMethodName).size();
    totalTransactionsSize += generateTransactions("0xe9dfec7864af9e581a85ce3987d026be0f50bbbb", null, null).size();

    assertTrue(totalTransactionsSize > 100);

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    List<TransactionDetail> transactions = transactionStorage.getTransactions(WalletUtils.getNetworkId(), Integer.MAX_VALUE);

    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned transactions list count is not coherent", totalTransactionsSize, transactions.size());

    // Test pagination
    transactions = transactionStorage.getTransactions(WalletUtils.getNetworkId(), 5);
    assertEquals("Returned contract transactions list count is not coherent", 5, transactions.size());
  }

  /**
   * Test get list of transactions of a wallet
   */
  @Test
  public void testGetWalletTransactions() {
    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";
    String contractMethodName = "transfer";
    String firstAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";

    List<TransactionEntity> generatedTransactions = generateTransactions(firstAddress, contractAddress, contractMethodName);
    List<String> transactionHashList =
                                     generatedTransactions.stream().map(TransactionEntity::getHash).collect(Collectors.toList());

    TransactionStorage transactionStorage = getService(TransactionStorage.class);

    // Search all transactions where a wallet is receiver, sender delegator
    boolean includeAdministrationTransactions = true;
    boolean onlyPending = false;
    List<TransactionDetail> transactions = transactionStorage.getWalletTransactions(1,
                                                                                    firstAddress,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    0,
                                                                                    onlyPending,
                                                                                    includeAdministrationTransactions);

    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned wallet transactions list count is not coherent", 60, transactions.size());

    // Test pagination
    transactions = transactionStorage.getWalletTransactions(1,
                                                            firstAddress,
                                                            null,
                                                            null,
                                                            null,
                                                            20,
                                                            onlyPending,
                                                            includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 20, transactions.size());

    // Filter on contract address
    transactions = transactionStorage.getWalletTransactions(1,
                                                            firstAddress,
                                                            contractAddress,
                                                            null,
                                                            null,
                                                            0,
                                                            onlyPending,
                                                            includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 30, transactions.size());

    // Filter on contract method name
    transactions = transactionStorage.getWalletTransactions(1,
                                                            firstAddress,
                                                            contractAddress,
                                                            contractMethodName,
                                                            null,
                                                            0,
                                                            onlyPending,
                                                            includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 10, transactions.size());

    // Filter on only pending transactions
    onlyPending = true;
    transactions = transactionStorage.getWalletTransactions(1,
                                                            firstAddress,
                                                            contractAddress,
                                                            contractMethodName,
                                                            null,
                                                            0,
                                                            onlyPending,
                                                            includeAdministrationTransactions);
    assertEquals("Returned wallet transactions list count is not coherent", 10, transactions.size());

    String oldestTransactionHash = transactionHashList.get(0);
    transactions = transactionStorage.getWalletTransactions(1,
                                                            firstAddress,
                                                            null,
                                                            null,
                                                            oldestTransactionHash,
                                                            10,
                                                            false,
                                                            true);
    assertEquals("Returned wallet transactions list should include all transactions, even if limit = 10, the selected hash must be included in result",
                 60,
                 transactions.size());

    // Filter on only pending transactions
    includeAdministrationTransactions = false;
    transactions = transactionStorage.getWalletTransactions(1,
                                                            firstAddress,
                                                            contractAddress,
                                                            contractMethodName,
                                                            null,
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

    TransactionStorage transactionStorage = getService(TransactionStorage.class);

    // Search all pending transactions using the network id
    List<TransactionDetail> transactions = transactionStorage.getPendingTransaction(1);

    assertNotNull("Returned transactions list is null", transactions);
    assertEquals("Returned pending transactions list count on a network is not coherent", 30, transactions.size());

    // Use non existing network ID
    transactions = transactionStorage.getPendingTransaction(2);
    assertEquals("Returned wallet transactions list count on a non existing network is not coherent", 0, transactions.size());
  }

  /**
   * Test search transaction by hash
   */
  @Test
  public void testGetTransactionByHash() {
    String hashOfTX = "0x51a6e8ef52f723ab8e52eed07b7ebbe165ec892664616434c946e387424ceadb";
    long createdDateInMilliSeconds = System.currentTimeMillis();
    createTransactionDetail(hashOfTX,
                            null,
                            0, // token amount
                            0, // ether amount
                            "from",
                            "to",
                            "by",
                            1,
                            "label",
                            "message",
                            true, // isSuccess
                            true, // isPending
                            true, // isAdminOperation
                            null,
                            createdDateInMilliSeconds);

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    TransactionDetail transactionDetail = transactionStorage.getTransactionByHash(hashOfTX);
    assertNotNull("Can't find previously saved transaction with given hash", transactionDetail);

    transactionDetail =
                      transactionStorage.getTransactionByHash("0x111111ef52f723ab8e52eed07b7ebbe165ec892664616434c946e387424ceaaa");
    assertNull("Shouldn't find a non existing transaction with fake hash", transactionDetail);
  }

  /**
   * Test count received contract amounts
   */
  @Test
  public void testSaveTransactionDetail() {
    TransactionDetail transactionDetail = createTransactionDetail(null,
                                                                  null,
                                                                  0, // token
                                                                     // amount
                                                                  0, // ether
                                                                     // amount
                                                                  "from",
                                                                  "to",
                                                                  "by",
                                                                  0,
                                                                  "label",
                                                                  "message",
                                                                  true, // isSuccess
                                                                  true, // isPending
                                                                  true, // isAdminOperation
                                                                  null,
                                                                  0);

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    TransactionDetail savedTransactionDetail = transactionStorage.getTransactionByHash(transactionDetail.getHash());
    assertEquals("Stored transaction detail is not equals to built one", transactionDetail, savedTransactionDetail);

    transactionDetail.setContractMethodName("transfer");
    transactionStorage.saveTransactionDetail(transactionDetail);
    savedTransactionDetail = transactionStorage.getTransactionByHash(transactionDetail.getHash());
    assertEquals("Stored transaction detail is not equals to updated one", transactionDetail, savedTransactionDetail);
  }

  /**
   * Test count received contract amounts
   */
  @Test
  public void testCountReceivedContractAmount() {
    String contractAddress = "contractAddress";
    String address = "address";
    String contractMethodName = "reward";

    // Make sure that creation time is one second before period of selected
    // period of time
    generateTransactions(address, contractAddress, contractMethodName, -1000);

    ZonedDateTime start = ZonedDateTime.now();
    generateTransactions(address, contractAddress, contractMethodName);
    ZonedDateTime end = ZonedDateTime.now().plusSeconds(1);

    // Make sure that creation time is one second after period of selected
    // period of time
    generateTransactions(address, contractAddress, contractMethodName, 3000);

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    double receivedAmount = transactionStorage.countReceivedContractAmount(contractAddress, address, start, end);
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

    // Make sure that creation time is one second before period of selected
    // period of time
    generateTransactions(address, contractAddress, contractMethodName, -1000);

    ZonedDateTime start = ZonedDateTime.now();
    generateTransactions(address, contractAddress, contractMethodName);
    ZonedDateTime end = ZonedDateTime.now().plusSeconds(1);

    // Make sure that creation time is one second after period of selected
    // period of time
    generateTransactions(address, contractAddress, contractMethodName, 3000);

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    double sentAmount = transactionStorage.countSentContractAmount(contractAddress, address, start, end);
    assertEquals("Sent amount isn't coherent", 10, sentAmount, 0);
  }

}
