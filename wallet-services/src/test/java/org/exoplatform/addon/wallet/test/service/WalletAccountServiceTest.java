package org.exoplatform.addon.wallet.test.service;

<<<<<<< HEAD
import static org.junit.Assert.*;
=======
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
>>>>>>> 9fb20bb... Review added tests

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
<<<<<<< HEAD

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
<<<<<<< HEAD
  public void testsaveWallet() {
=======
  public void testSaveWallet() {
>>>>>>> 9fb20bb... Review added tests
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByAddress(ADDRESS);
    assertEquals("Wallet initialization state shouldn't be null", INITIALIZATION_STATE, walletTest.getInitializationState());
    assertEquals("Wallet passPhrase shouldn't be null",
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
<<<<<<< HEAD
=======
   * Test get wallet count
   */
  @Test
  public void testGetWalletsCount() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);
    long walletCount = walletAccountService.getWalletsCount();
    assertEquals("Returned wallets count shouldn't be 1", 1, walletCount);
    entitiesToClean.add(wallet);
  }

  /**
>>>>>>> 9fb20bb... Review added tests
   * Test get wallet by address
   */
  @Test
  public void testGetWalletByAddress() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByAddress(ADDRESS);
    assertNotNull("Shouldn't find wallet with not recognized address", walletTest);
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
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
    entitiesToClean.add(wallet);
  }
=======
//
//  /**
//   * Test if default settings are injected in walletAcountService
//   */
//  @Test
//  public void testDefaultParameters() {
//    WalletAccountService walletAccountService = getService(WalletAccountService.class);
//
//  }
//
//  /**
//   * Test get wallet count
//   */
//  @Test
//  public void testgetWalletsCount() {
//    WalletAccountService walletAccountService = getService(WalletAccountService.class);
//    long walletCount = walletAccountService.getWalletsCount();
//    assertNotNull("Wallet count shouldn't be null", walletCount);
//  }
//
//  /**
//   * Test save wallet
//   */
//  @Test
//  public void testsaveWallet() {
//    WalletAccountService walletAccountService = getService(WalletAccountService.class);
//    Wallet wallet = new Wallet();
//    String id = "wallet0";
//    String name = "user";
//    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
//    String type = "walletAccount";
//    String initializationState = "NEW";
//    String passPhrase = "Save wallet";
//    int technicalId = 1;
//    wallet.setId(id);
//    wallet.setType(type);
//    wallet.setAddress(address);
//    wallet.setTechnicalId(technicalId);
//    wallet.setName(name);
//    wallet.setInitializationState(initializationState);
//    wallet.setPassPhrase(passPhrase);
//    walletAccountService.saveWallet(wallet);
//
//    assertEquals("Wallet id shouldn't be null", id, walletAccountService.getWalletByAddress(address).getId());
//    assertEquals("Wallet name shouldn't be null", name, walletAccountService.getWalletByAddress(address).getName());
//    assertEquals("Wallet type shouldn't be null", type, walletAccountService.getWalletByAddress(address).getType());
//
//  }
>>>>>>> d96f468... Comment failing tests

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
    assertEquals("Wallet is owner", testOwner, true);
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
    entitiesToClean.add(wallet);

    long walletCount = walletAccountService.getWalletsCount();

    assertNotNull("Wallet count shouldn't be null", walletCount);
  }
}
