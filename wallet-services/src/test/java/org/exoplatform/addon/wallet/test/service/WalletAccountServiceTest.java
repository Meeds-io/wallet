package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.services.listener.ListenerService;
import org.junit.Test;

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
  public void testsaveWallet() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "wallet0";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);
    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByAddress(address);
    assertEquals("Wallet initialization state shouldn't be null", initializationState, walletTest.getInitializationState());
    assertEquals("Wallet passPhrase shouldn't be null",
                 passPhrase,
                 walletTest.getPassPhrase());
    entitiesToClean.add(wallet);
  }

  /**
   * Test save wallet address
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testsaveWalletAddress() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String currentUser = "root0";
    String id = "root0";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setType(type);
    wallet.setInitializationState(initializationState);

    walletAccountService.saveWalletAddress(wallet, currentUser, true);
    String addressTest = walletAccountService.getWalletByAddress(address).getAddress();
    assertEquals("Wallet initialization state shouldn't be null", StringUtils.lowerCase(address), addressTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet count
   */
  @Test
  public void testgetWalletsCount() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "wallet0";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    walletAccountService.saveWallet(wallet);
    long walletCount = walletAccountService.getWalletsCount();
    assertNotEquals("Returned wallets count shouldn't be 0", 0, walletCount);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by address
   */
  @Test
  public void testgetWalletByAddress() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "wallet0";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByAddress(address);
    assertNotNull("Shouldn't find wallet with not recognized address", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by type and Id and current user
   */
  @Test
  public void testgetWalletByTypeAndIdAndUser() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "wallet0";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    walletAccountService.saveWallet(wallet);

    String currentUser = "root0";
    String remoteId = "root0";

    Wallet walletTest = walletAccountService.getWalletByTypeAndId(type, remoteId, currentUser);
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test get wallet by type and Id
   */
  @Test
  public void testgetWalletByTypeAndId() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "wallet0";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    walletAccountService.saveWallet(wallet);

    String remoteId = "root0";

    Wallet walletTest = walletAccountService.getWalletByTypeAndId(type, remoteId);
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test Wallet owner
   */
  @Test
  public void testisWalletOwner() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "root0";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setId(id);
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    walletAccountService.saveWallet(wallet);

    String currentUser = "root0";
    Boolean testOwner = walletAccountService.isWalletOwner(wallet, currentUser);
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
    Wallet wallet = new Wallet();
    String id = "root1";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setId(id);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    String remoteId = "root1";
    String content = "Save private key";
    String currentUser = "root1";

    walletAccountService.saveWalletAddress(wallet, currentUser, true);
    walletAccountService.savePrivateKeyByTypeAndId(type,
                                                   remoteId,
                                                   content,
                                                   currentUser);
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(type,
                                                                      remoteId,
                                                                      currentUser);
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
    Wallet wallet = new Wallet();
    String id = "root1";
    String name = "walletUserTest";
    String type = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    wallet.setAddress(StringUtils.lowerCase(address));
    wallet.setEnabled(true);
    wallet.setId(id);
    wallet.setType(type);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);

    String currentUser = "root1";

    walletAccountService.saveWalletAddress(wallet, currentUser, true);

    long technicalId = wallet.getTechnicalId();
    wallet = walletAccountService.getWalletByIdentityId(technicalId);
    assertNotNull("IdentityId Shouldn't be null", technicalId);
    entitiesToClean.add(wallet);
  }
}
