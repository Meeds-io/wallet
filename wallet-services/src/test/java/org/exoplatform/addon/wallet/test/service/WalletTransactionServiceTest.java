package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.addon.wallet.utils.WalletUtils;

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
    assertEquals("Wrong default value for 'Pending transaction MaxDays'", 3, count);
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
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByHash(transactionDetail.getHash());
    assertNotNull(storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);
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
   * {@link WalletTransactionService#getTransactionByNonceOrHash(String, String, long, String)}
   */
  @Test
  public void testGetTransactionByNonceOrHashAndUser() {
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
                                                                  System.currentTimeMillis());
    walletTransactionService.saveTransactionDetail(transactionDetail, true);
    TransactionDetail storedTransactionDetail = walletTransactionService.getTransactionByNonceOrHash(transactionDetail.getHash(),
                                                                                                     transactionDetail.getFrom(),
                                                                                                     transactionDetail.getNonce()
                                                                                                         + 1,
                                                                                                     CURRENT_USER);
    assertNotNull(storedTransactionDetail);
    entitiesToClean.add(storedTransactionDetail);

    storedTransactionDetail = walletTransactionService.getTransactionByNonceOrHash(generateTransactionHash(),
                                                                                   transactionDetail.getFrom(),
                                                                                   transactionDetail.getNonce(),
                                                                                   CURRENT_USER);
    assertNotNull(storedTransactionDetail);
  }

  /**
   * Test {@link WalletTransactionService#getPendingTransactionHashes}
   */
  @Test
  public void testGetPendingTransactionHashes() {
    WalletTransactionService walletTransactionService = getService(WalletTransactionService.class);
    Set<String> pendingTransactions = walletTransactionService.getPendingTransactionHashes();
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
                                                                  System.currentTimeMillis());
    entitiesToClean.add(transactionDetail);

    pendingTransactions = walletTransactionService.getPendingTransactionHashes();
    assertNotNull(pendingTransactions);
    assertEquals(1, pendingTransactions.size());
    assertEquals(pendingTransactionDetail.getHash(), pendingTransactions.iterator().next());
  }

}
