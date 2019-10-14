package org.exoplatform.wallet.test.service;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.wallet.dao.AddressLabelDAO;
import org.exoplatform.wallet.entity.AddressLabelEntity;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.storage.WalletStorage;
import org.exoplatform.wallet.test.BaseWalletTest;

public class WalletAccountServiceTest extends BaseWalletTest {

  @Test
  public void testContainerStart() {
    assertNotNull(getService(ListenerService.class));
    assertNotNull(getService(WalletAccountService.class));
    assertNotNull(getService(AddressLabelDAO.class));
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
   * Test save wallet address
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testSaveWalletAddress() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    try {
      Wallet walletTest = new Wallet();
      walletTest.setTechnicalId(CURRENT_USER_IDENTITY_ID);
      walletTest.setAddress("");
      walletTest.setPassPhrase(PHRASE);
      walletTest.setEnabled(IS_ENABLED);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.saveWalletAddress(walletTest, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.saveWalletAddress(null, CURRENT_USER);
      fail("Wallet shouldn't be null");
    } catch (IllegalArgumentException e) {
      // Expected, wallet shouldn't be null
    }

    Wallet wallet = newWallet();
    wallet.setPassPhrase("");
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    Wallet storedWallet = walletAccountService.getWalletByAddress(wallet.getAddress());
    assertNotNull(storedWallet);
    assertNotNull(storedWallet.getPassPhrase());

    String addressTest = storedWallet.getAddress();
    assertEquals("Unexpected wallet address", StringUtils.lowerCase(WALLET_ADDRESS_1), StringUtils.lowerCase(addressTest));
  }

  /**
   * Test get wallet count
   */
  @Test
  public void testGetWalletsCount() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    entitiesToClean.add(wallet);

    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    long walletCount = walletAccountService.getWalletsCount();
    assertEquals("Returned wallets count should be 1", 1, walletCount);
  }

  /**
   * Test get wallet by address
   */
  @Test
  public void testGetWalletByAddress() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    entitiesToClean.add(wallet);

    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    try {
      String address = null;
      walletAccountService.getWalletByAddress(address);
      fail("Wallet address shouldn't be null");
    } catch (Exception e) {
      // Expected, wallet address shouldn't be null
    }

    Wallet walletTest = walletAccountService.getWalletByAddress(WALLET_ADDRESS_1);
    assertNotNull("Shouldn't find wallet with not recognized address", walletTest);
  }

  /**
   * Test get wallet by type and Id and current user
   */
  @Test
  public void testGetWalletByTypeAndIdAndUser() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    entitiesToClean.add(wallet);

    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
  }

  /**
   * Test get wallet by type and Id
   */
  @Test
  public void testGetWalletByTypeAndId() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    try {
      walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), "");
      fail("RemoteId is mandatory");
    } catch (Exception e) {
      // Expected, remoteId is mandatory
    }

    WalletStorage walletStorage = getService(WalletStorage.class);
    wallet = walletStorage.saveWallet(wallet, true);
    entitiesToClean.add(wallet);

    Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), CURRENT_USER);
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
  }

  /**
   * Test Wallet owner user
   */
  @Test
  public void testIsWalletOwner() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    entitiesToClean.add(wallet);

    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    assertFalse(walletAccountService.isWalletOwner(null, CURRENT_USER));
    assertTrue(walletAccountService.isWalletOwner(wallet, CURRENT_USER));
  }

  /**
   * Test save private key by type, remoteId, content and user
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testSavePrivateKeyByTypeAndIdAndContent() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    try {
      walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, "content", CURRENT_USER);
      fail("Shouldn't be able to save private key of not existant wallet");
    } catch (Exception e) {
      // Expected, wallet doesn't exist yet
    }

    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    String privateKeyContent = "Private key";
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(),
                                                   CURRENT_USER,
                                                   privateKeyContent,
                                                   CURRENT_USER);
    String storedPrivateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                            CURRENT_USER,
                                                                            CURRENT_USER);
    assertEquals("Wallet private key should be equal", privateKeyContent, storedPrivateKey);
  }

  /**
   * Test get wallet by identityId
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testGetWalletByIdentityId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    try {
      walletAccountService.getWalletByIdentityId(0);
      fail("IdentityId is mandatory");
    } catch (IllegalArgumentException e) {
      // Expected, identityId is mandatory
    }

    Wallet wallet = walletAccountService.getWalletByIdentityId(100);
    assertNull(wallet);

    wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    wallet = walletAccountService.getWalletByIdentityId(wallet.getTechnicalId());
    assertNotNull(wallet);
  }

  /**
   * Test list of wallet
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testListWallets() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    Set<Wallet> wallets = walletAccountService.listWallets();
    assertNotNull(wallets);
    assertEquals(1, wallets.size());
  }

  /**
   * Test remove private key by Type and Id
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testRemovePrivateKeyByTypeAndId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    String content = "Private key";
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, content, CURRENT_USER);
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    assertEquals(content, privateKey);

    try {
      walletAccountService.removePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, USER_TEST);
      fail("Should fail: another user attempts to delete a wallet of another user");
    } catch (IllegalAccessException e) {
      // Expected to fail: another user attempts to delete a wallet of another
      // user
    }

    walletAccountService.removePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);

    String privateKeyTset = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, CURRENT_USER);
    assertNull("Private key should be null after remove", privateKeyTset);
  }

  /**
   * Test save or delete address label
   */
  @Test
  public void testSaveOrDeleteAddressLabel() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    AddressLabelDAO addressLabelDAO = getService(AddressLabelDAO.class);
    try {
      walletAccountService.saveOrDeleteAddressLabel(null, CURRENT_USER);
      fail("Label is empty");
    } catch (Exception e) {
      // Expected, Label is empty
    }

    try {
      WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
      String label = "Save address label";
      int identityId = 1;
      walletAddressLabel.setId(2);
      walletAddressLabel.setAddress(WALLET_ADDRESS_1);
      walletAddressLabel.setIdentityId(identityId);
      walletAddressLabel.setLabel(label);
      walletAccountService.saveOrDeleteAddressLabel(walletAddressLabel, "user");
      fail("Identity is null, incorrect user");
    } catch (Exception e) {
      // Expected, identity is null, incorrect user !
    }

    WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
    int identityId = 2;
    walletAddressLabel.setAddress(WALLET_ADDRESS_1);
    walletAddressLabel.setId(1);
    walletAddressLabel.setIdentityId(identityId);
    WalletAddressLabel labelTest = walletAccountService.saveOrDeleteAddressLabel(walletAddressLabel, CURRENT_USER);
    assertNotNull("Wallet address label shouldn't be null", labelTest);
    AddressLabelEntity labelEntity = addressLabelDAO.find(walletAddressLabel.getId());
    entitiesToClean.add(labelEntity);
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
    walletAddressLabel.setAddress(WALLET_ADDRESS_1);
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
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testGetPrivateKeyByTypeAndIdAndUser() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    String content = "Private key";
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(),
                                                   CURRENT_USER,
                                                   content,
                                                   CURRENT_USER);
    try {
      walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(), "root0", "root0");
    } catch (Exception e) {
      // Expected, wallet shouldn't be null or TechnicalId < 1
    }

    try {
      walletAccountService.getPrivateKeyByTypeAndId(WalletType.ADMIN.name(),
                                                    CURRENT_USER,
                                                    CURRENT_USER);
      fail("User is not allowed to access private key of admin");
    } catch (Exception e) {
      // Expected, user is not allowed to access private key of admin
    }

    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                      CURRENT_USER,
                                                                      CURRENT_USER);
    assertNotNull("Wallet private key shouldn't be null", privateKey);
  }

  /**
   * Test get private key by type and remoteId
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testGetPrivateKeyByTypeAndId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();

    String content = "Save private key";
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    assertNotNull(wallet);
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(),
                                                   CURRENT_USER,
                                                   content,
                                                   CURRENT_USER);

    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                      CURRENT_USER);
    assertNotNull("Wallet private key shouldn't be null", privateKey);
    entitiesToClean.add(wallet);
  }

  /**
   * Test remove wallet by address
   * 
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testRemoveWalletByAddress() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    String group = "/platform/rewarding";
    MembershipEntry entry = new MembershipEntry(group, MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet<>();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    try {
      walletAccountService.removeWalletByAddress(null, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    assertNotNull(wallet);
    entitiesToClean.add(wallet);

    try {
      walletAccountService.removeWalletByAddress("", CURRENT_USER);
      fail("Wallet shouldn't be null");
    } catch (Exception e) {
      // Expected, wallet shouldn't be null
    }

    try {
      walletAccountService.removeWalletByAddress(WALLET_ADDRESS_1, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }

    walletAccountService.removeWalletByAddress(WALLET_ADDRESS_1, CURRENT_USER);
    Set<Wallet> wallets = walletAccountService.listWallets();
    assertEquals("Wallet list Should be empty", 0, wallets.size());
    entitiesToClean.remove(wallet);
  }

  /**
   * Test remove wallet by type and id
   * 
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testRemoveWalletByTypeAndId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet<>();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    try {
      walletAccountService.removeWalletByTypeAndId("", CURRENT_USER, CURRENT_USER);
      fail("Wallet type is mandatory");
    } catch (Exception e) {
      // Expected, wallet type is mandatory
    }

    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "", CURRENT_USER);
      fail("Wallet remoteId is mandatory");
    } catch (Exception e) {
      // Expected, wallet remoteId is mandatory
    }

    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "root2", "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected,is not user rewarding admin
    }

    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "root3", CURRENT_USER);
      fail("Can't find wallet with this remoteId");
    } catch (Exception e) {
      // Expected, can't find wallet with this remoteId
    }

    String type = wallet.getType();
    walletAccountService.removeWalletByTypeAndId(type, CURRENT_USER, CURRENT_USER);
    Set<Wallet> wallets = walletAccountService.listWallets();
    assertEquals("Wallet list Should be empty", wallets.size(), 0);
  }

  /**
   * Test enable wallet by address
   * 
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testEnableWalletByAddress() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet<>();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    assertNotNull(wallet);
    entitiesToClean.add(wallet);

    try {
      walletAccountService.enableWalletByAddress(null, true, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.enableWalletByAddress("walletAdressUser", true, CURRENT_USER);
      fail("Shouldn't enable a wallet with unknown address");
    } catch (Exception e) {
      // Expected, Can't find wallet associated to address
    }

    try {
      walletAccountService.enableWalletByAddress(WALLET_ADDRESS_1, false, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }

    assertTrue(walletAccountService.enableWalletByAddress(WALLET_ADDRESS_1, false, CURRENT_USER));
    wallet = walletAccountService.getWalletByAddress(WALLET_ADDRESS_1);
    assertFalse(wallet.isEnabled());
    assertFalse(walletAccountService.enableWalletByAddress(WALLET_ADDRESS_1, false, CURRENT_USER));
    wallet = walletAccountService.getWalletByAddress(WALLET_ADDRESS_1);
    assertFalse(wallet.isEnabled());
    assertTrue(walletAccountService.enableWalletByAddress(WALLET_ADDRESS_1, true, CURRENT_USER));
    wallet = walletAccountService.getWalletByAddress(WALLET_ADDRESS_1);
    assertTrue(wallet.isEnabled());
  }

  /**
   * Test set initialization status
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testSetInitializationStatus() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    Wallet wallet = newWallet();
    assertNotNull(wallet);

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);

    try {
      walletAccountService.setInitializationStatus(null, WalletInitializationState.NEW, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletInitializationState.NEW, "");
      fail("Username is mandatory");
    } catch (Exception e) {
      // Expected, username is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, null, CURRENT_USER);
      fail("InitializationState is mandatory");
    } catch (Exception e) {
      // Expected, initializationState is mandatory
    }

    try {
      walletAccountService.setInitializationStatus("walletAdressUser", WalletInitializationState.NEW, CURRENT_USER);
      fail("Can't find wallet associated to address");
    } catch (Exception e) {
      // Expected, Can't find wallet associated to address
    }

    try {
      walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletInitializationState.NEW, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }

    walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletInitializationState.NEW, CURRENT_USER);
    assertEquals("Wallet initial status Should be NEW", wallet.getInitializationState(), "NEW");
    entitiesToClean.add(wallet);
  }

  /**
   * Test set initialization status wallet
   *
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testSetInitializationStatusWallet() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    Wallet wallet = newWallet();
    assertNotNull(wallet);

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);

    try {
      walletAccountService.setInitializationStatus(null, WalletInitializationState.NEW);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, null);
      fail("InitializationState is mandatory");
    } catch (Exception e) {
      // Expected, initializationState is mandatory
    }

    walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletInitializationState.NEW);
    assertEquals("Wallet initial status Should be NEW", wallet.getInitializationState(), "NEW");
    entitiesToClean.add(wallet);
  }

  /**
   * Test get admin account password
   */
  @Test
  public void testGetAdminAccountPassword() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    String password = walletAccountService.getAdminAccountPassword();
    assertNotNull(password);
  }

}
