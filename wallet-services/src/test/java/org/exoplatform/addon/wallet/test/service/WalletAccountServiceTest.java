package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.*;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addon.wallet.model.*;
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
  public void testSaveWallet() {
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
   * Test get wallet count
   */
  @Test
  public void testGetWalletsCount() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWallet(wallet);
    long walletCount = walletAccountService.getWalletsCount();
    assertEquals("Returned wallets count shouldn't be 0", 1, walletCount);
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
    assertEquals("Your aren't the owner of wallet", testOwner, true);
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
    assertEquals("Wallet private key should be equal", content, privateKey);
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
  }

  /**
   * Test list of wallet
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testListWallets() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    Set<Wallet> wallets = walletAccountService.listWallets();
    assertNotNull("Wallet list Shouldn't be empty", wallets);
    entitiesToClean.add(wallet);
  }

  /**
   * Test check save wallet removePrivateKeyByTypeAndId
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testCheckCanSaveWallet() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    walletAccountService.isWalletOwner(wallet, CURRENT_USER);
    Wallet walletTest = walletAccountService.getWalletByIdentityId(wallet.getTechnicalId());

    walletAccountService.checkCanSaveWallet(wallet, null, CURRENT_USER);
    // assertEquals("Wallet can be saved", isNew, false);
    entitiesToClean.add(wallet);
  }

  /**
   * Test remove private key by Type and Id
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testRemovePrivateKeyByTypeAndId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    walletAccountService.isWalletOwner(wallet, CURRENT_USER);
    String content = "Save private key";
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, content, CURRENT_USER);
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    assertNotNull("Private key shouldn't be null", privateKey);

    walletAccountService.removePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    String privateKeyTset = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    assertNull("Private key should be null after remove", privateKeyTset);
    entitiesToClean.add(wallet);
  }

  /**
   * Test save or delete address label
   */
  @Test
  public void testSaveOrDeleteAddressLabel() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
    String label = "Save address label";
    int identityId = 1;
    walletAddressLabel.setAddress(ADDRESS);
    walletAddressLabel.setIdentityId(identityId);
    walletAddressLabel.setLabel(label);

    WalletAddressLabel labelTset = walletAccountService.saveOrDeleteAddressLabel(walletAddressLabel, CURRENT_USER);
    assertNotNull("Wallet address label shouldn't be null", labelTset);
    entitiesToClean.add(labelTset);
  }

  /**
   * Test get addresses labels
   */
  @Test
  public void testGetAddressesLabelsVisibleBy() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
    String label = "Save address label";
    int identityId = 1;
    walletAddressLabel.setAddress(ADDRESS);
    walletAddressLabel.setIdentityId(identityId);
    walletAddressLabel.setLabel(label);

    WalletAddressLabel labelTset = walletAccountService.saveOrDeleteAddressLabel(walletAddressLabel, CURRENT_USER);
    assertNotNull("Wallet address label shouldn't be null", labelTset);
    Set<WalletAddressLabel> walletTestLabel = walletAccountService.getAddressesLabelsVisibleBy(CURRENT_USER);
    assertNotNull("Wallet address label Shouldn't be empty", walletTestLabel);
    entitiesToClean.add(labelTset);
  }

  /**
   * Test get private key by type, remoteId and user
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testGetPrivateKeyByTypeAndIdAndUser() throws IllegalAccessException {
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
   * Test get private key by type and remoteId
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testGetPrivateKeyByTypeAndId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    String content = "Save private key";

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(),
                                                   CURRENT_USER,
                                                   content,
                                                   CURRENT_USER);
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                      CURRENT_USER);
    assertNotNull("Wallet private key shouldn't be null", privateKey);
    entitiesToClean.add(wallet);
  }
}
