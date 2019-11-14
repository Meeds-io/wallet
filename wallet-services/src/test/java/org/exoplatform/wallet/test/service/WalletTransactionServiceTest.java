package org.exoplatform.wallet.test.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

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

    pendingTransactions = walletTransactionService.getPendingTransactions();
    assertNotNull(pendingTransactions);
    assertEquals(1, pendingTransactions.size());
    assertEquals(pendingTransactionDetail.getHash(), pendingTransactions.iterator().next().getHash());
  }

}
