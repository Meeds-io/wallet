package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.WalletType;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.services.listener.ListenerService;

public class WalletAccountServiceTest extends BaseWalletTest {

  @Test
  public void testContainerStart() {
    assertNotNull(getService(ListenerService.class));
    assertNotNull(getService(WalletAccountService.class));
  }

  /**
   * Test if default settings are injected in WalletAccountService
   */
  @Test
  public void testDefaultParameters() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    assertNotNull("Wallet account service shouldn't be null", walletAccountService);
  }

  /**
   * Test save wallet
   */
  @Test
  public void testSaveWallet() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByAddress(ADDRESS);
    assertEquals("Unexpected value of wallet initialization state", INITIALIZATION_STATE, walletTest.getInitializationState());
    assertEquals("Unexpected value of wallet passPhrase",
                 PHRASE,
                 walletTest.getPassPhrase());
    entitiesToClean.add(wallet);
  }

  /**
   * Test save wallet address
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testSaveWalletAddress() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    String addressTest = walletAccountService.getWalletByAddress(ADDRESS).getAddress();
    assertEquals("Unexpected wallet address", StringUtils.lowerCase(ADDRESS), StringUtils.lowerCase(addressTest));
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet count
   */
  @Test
  public void testGetWalletsCount() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);
    long walletCount = walletAccountService.getWalletsCount();
    assertEquals("Unexpected returned wallets count", 1, walletCount);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by address
   */
  @Test
  public void testGetWalletByAddress() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByAddress(ADDRESS);
    assertNotNull("Wallet not found with saved address", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by type and Id and current user
   */
  @Test
  public void testGetWalletByTypeAndIdAndUser() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);

    Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    assertNotNull("Wallet not found with saved type and id", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by type and Id
   */
  @Test
  public void testGetWalletByTypeAndId() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);

    Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), CURRENT_USER);
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test Wallet owner
   */
  @Test
  public void testIsWalletOwner() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);

    wallet = walletAccountService.getWalletByIdentityId(wallet.getTechnicalId());

    Boolean testOwner = walletAccountService.isWalletOwner(wallet, CURRENT_USER);
    assertTrue("Wallet should be owner", testOwner);
    entitiesToClean.add(wallet);
  }

  /**
   * Test save private key by type, remoteId, content and user
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testSavePrivateKeyByTypeAndIdAndContent() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    String content = "Save private key";

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(),
                                                   CURRENT_USER,
                                                   content,
                                                   CURRENT_USER);
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                      CURRENT_USER,
                                                                      CURRENT_USER);
    assertNotNull("Wallet private key shouldn't be null", privateKey);
    assertEquals("Unexpected wallet private key value", content, privateKey);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by identityId
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testGetWalletByIdentityId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);

    long technicalId = wallet.getTechnicalId();
    wallet = walletAccountService.getWalletByIdentityId(technicalId);
    assertNotNull("IdentityId Shouldn't be null", technicalId);
    assertEquals("Unexpected wallet technichal Id", IDENTITY_ID, technicalId);
    entitiesToClean.add(wallet);
  }
}
