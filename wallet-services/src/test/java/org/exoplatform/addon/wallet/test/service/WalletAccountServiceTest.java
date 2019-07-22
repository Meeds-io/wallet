package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addon.wallet.dao.AddressLabelDAO;
import org.exoplatform.addon.wallet.entity.AddressLabelEntity;
import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletTokenAdminService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.junit.Test;
import org.mockito.Mockito;

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
    try {
      Wallet walletTest = new Wallet();
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress("");
      walletTest.setPassPhrase(PHRASE);
      walletTest.setEnabled(IS_ENABLED);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.saveWalletAddress(walletTest, CURRENT_USER, true);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    Wallet wallet = newWallet();
    try {
      walletAccountService.saveWalletAddress(null, CURRENT_USER, true);
      fail("Wallet shouldn't be null");
    } catch (Exception e) {
      // Expected, wallet shouldn't be null
    }

    try {
      Wallet walletTest = new Wallet();
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress("walletUser");
      walletTest.setPassPhrase(PHRASE);
      walletTest.setEnabled(IS_ENABLED);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.saveWalletAddress(walletTest, CURRENT_USER, true);
      entitiesToClean.add(walletTest);
    } catch (Exception e) {
      fail("Wallet address is incorrect");
      // Expected, wallet address is incorrect
    }

    wallet.setPassPhrase("");
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
    assertEquals("Returned wallets count should be 1", 1, walletCount);
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
    try {
      String address = null;
      walletAccountService.getWalletByAddress(address);
      fail("Wallet address shouldn't be null");
    } catch (Exception e) {
      // Expected, wallet address shouldn't be null
    }

    Wallet walletTest = walletAccountService.getWalletByAddress(ADDRESS);
    assertNotNull("Shouldn't find wallet with not recognized address", walletTest);
    entitiesToClean.add(wallet);
    entitiesToClean.add(walletTest);
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
    entitiesToClean.add(walletTest);
  }

  /**
   * Test get wallet by type and Id
   *
   * @throws IllegalAccessException
   */
  @Test
  public void testGetWalletByTypeAndId() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    try {
      String remoteId = "";
      Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), remoteId);
      assertNull(walletTest);
      fail("RemoteId is mandatory");
    } catch (Exception e) {
      // Expected, remoteId is mandatory
    }

    try {
      String remoteId = "test";
      walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), remoteId);
    } catch (Exception e) {
      fail("Can't find identity with this remoteId");
    }

    walletAccountService.saveWallet(wallet);
    Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), CURRENT_USER);
    assertNotNull("Shouldn't find wallet with not recognized type and id", walletTest);
    entitiesToClean.add(wallet);
  }

  /**
   * Test Wallet owner user
   */
  @Test
  public void testIsWalletOwner() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    walletAccountService.saveWallet(wallet);

    try {
      walletAccountService.isWalletOwner(null, CURRENT_USER);
    } catch (Exception e) {
      fail("Wallet shouldn't be null");
    }
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
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    try {
      walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(), "root0", "content", "root0");
      fail("Wallet shouldn't be null or TechnicalId < 1");
    } catch (Exception e) {
      // Expected, wallet shouldn't be null or TechnicalId < 1
    }
    String content = "Save private key";
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

    try {
      long identityId = 0;
      walletAccountService.getWalletByIdentityId(identityId);
      assertNull(wallet);
      fail("IdentityId is mandatory");
    } catch (Exception e) {
      // Expected, identityId is mandatory
    }

    try {
      long identityId = -1;
      walletAccountService.getWalletByIdentityId(identityId);
    } catch (Exception e) {
      fail("Can't find identity with this identityId");
    }
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    long identityId = wallet.getTechnicalId();
    wallet = walletAccountService.getWalletByIdentityId(identityId);
    assertNotNull("IdentityId Shouldn't be null", identityId);
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
    walletAccountService.isWalletOwner(wallet, CURRENT_USER);
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    assertNotNull(wallet);
    try {
      Wallet walletTest = new Wallet();
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress(ADDRESS);
      walletTest.setPassPhrase(PHRASE);
      walletTest.setEnabled(false);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.isWalletOwner(walletTest, CURRENT_USER);
      walletAccountService.checkCanSaveWallet(wallet, walletTest, CURRENT_USER);
      fail("User attempts to modify his wallet while it's disabled");
    } catch (Exception e) {
      // Expected, User attempts to modify his wallet while it's disabled
    }

    try {
      Wallet walletTest = new Wallet();
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress(ADDRESS);
      walletTest.setPassPhrase(PHRASE);
      walletTest.setId("root2");
      walletTest.setEnabled(false);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.isWalletOwner(walletTest, CURRENT_USER);
      walletAccountService.checkCanSaveWallet(walletTest, wallet, CURRENT_USER);
      fail("User attempts to modify his wallet while it's disabled");
    } catch (Exception e) {
      // Expected, Current User is not wallet admin !!
    }

    try {
      Wallet walletTest = new Wallet();
      String id = "root2";
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress(ADDRESS);
      walletTest.setPassPhrase(PHRASE);
      walletTest.setId(id);
      walletTest.setEnabled(IS_ENABLED);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.isWalletOwner(walletTest, id);
      walletAccountService.checkCanSaveWallet(walletTest, wallet, id);
      fail("User can't save wallet");
    } catch (Exception e) {
      // Expected, user can't save wallet
    }

    try {
      Wallet walletTest = new Wallet();
      String id = "root2";
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress(ADDRESS);
      walletTest.setPassPhrase(PHRASE);
      walletTest.setId(id);
      walletTest.setType(WalletType.ADMIN.name());
      walletTest.setEnabled(IS_ENABLED);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.isWalletOwner(walletTest, id);
      walletAccountService.checkCanSaveWallet(walletTest, null, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }
    walletAccountService.checkCanSaveWallet(wallet, null, CURRENT_USER);

    Wallet walletTest = walletAccountService.getWalletByTypeAndId(wallet.getType(), CURRENT_USER, CURRENT_USER);
    assertNotNull("Wallet can't be saved", walletTest);
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
    try {
      walletAccountService.removePrivateKeyByTypeAndId(WalletType.USER.name(), "root0", "root0");
    } catch (Exception e) {
      fail("Wallet shouldn't be null or TechnicalId < 1");
    }
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
      walletAddressLabel.setAddress(ADDRESS);
      walletAddressLabel.setIdentityId(identityId);
      walletAddressLabel.setLabel(label);
      walletAccountService.saveOrDeleteAddressLabel(walletAddressLabel, "user");
      fail("Identity is null, incorrect user");
    } catch (Exception e) {
      // Expected, identity is null, incorrect user !
    }

    WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
    int identityId = 2;
    walletAddressLabel.setAddress(ADDRESS);
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
    walletAddressLabel.setAddress(ADDRESS);
    walletAddressLabel.setIdentityId(identityId);
    walletAddressLabel.setLabel(label);

    WalletAddressLabel labelTset = walletAccountService.saveOrDeleteAddressLabel(walletAddressLabel, CURRENT_USER);
    assertNotNull("Wallet address label shouldn't be null", labelTset);

    try {
      walletAccountService.getAddressesLabelsVisibleBy("User");
    } catch (Exception e) {
      fail("Identity is null, incorrect user");
    }

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
    assertNotNull(wallet);
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
    assertNotNull(wallet);
    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(),
                                                   CURRENT_USER,
                                                   content,
                                                   CURRENT_USER);
    try {
      walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(), "root0");
    } catch (Exception e) {
      fail("Wallet shouldn't be null or TechnicalId < 1");
    }
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                      CURRENT_USER);
    assertNotNull("Wallet private key shouldn't be null", privateKey);
    entitiesToClean.add(wallet);
  }

  /**
   * Test remove wallet by address
   */
  @Test
  public void testRemoveWalletByAddress() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    String group = "/platform/rewarding";
    MembershipEntry entry = new MembershipEntry(group, MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet();
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

    try {
      walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    } catch (IllegalAccessException e1) {
      fail("Can't save wallet address");
    }
    assertNotNull(wallet);
    try {
      walletAccountService.removeWalletByAddress("", CURRENT_USER);
      fail("Wallet shouldn't be null");
    } catch (Exception e) {
      // Expected, wallet shouldn't be null
    }

    try {
      walletAccountService.removeWalletByAddress(ADDRESS, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }
    try {
      walletAccountService.removeWalletByAddress(ADDRESS, CURRENT_USER);
    } catch (IllegalAccessException e) {
      fail("Can't remove wallet by address");
    }
    Set<Wallet> wallets = walletAccountService.listWallets();
    assertEquals("Wallet list Should be empty", wallets.size(), 0);
  }

  /**
   * Test remove wallet by type and id
   */
  @Test
  public void testRemoveWalletByTypeAndId() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    Wallet wallet = newWallet();

    try {
      walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    } catch (IllegalAccessException e1) {
      fail("Can't  save wallet");
    }
    assertNotNull(wallet);
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
      walletAccountService.removeWalletByTypeAndId("Wallet", "User", CURRENT_USER);
    } catch (Exception e) {
      fail("Can't find identity with type/id");
    }

    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "root3", CURRENT_USER);
      fail("Can't find wallet with this remoteId");
    } catch (Exception e) {
      // Expected, can't find wallet with this remoteId
    }

    String type = wallet.getType();
    try {
      walletAccountService.removeWalletByTypeAndId(type, CURRENT_USER, CURRENT_USER);
    } catch (IllegalAccessException e) {
      fail("Can't remove wallet by type and id");
    }
    Set<Wallet> wallets = walletAccountService.listWallets();
    assertEquals("Wallet list Should be empty", wallets.size(), 0);
  }

  /**
   * Test enable wallet by address
   *
   * @throws Exception
   */
  @Test
  public void testEnableWalletByAddress() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    Wallet wallet = newWallet();

    try {
      walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    } catch (IllegalAccessException e1) {
      fail("Can't save wallet address");
    }
    assertNotNull(wallet);
    try {
      walletAccountService.enableWalletByAddress(null, true, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      String address = "walletAdressUser";
      walletAccountService.enableWalletByAddress(address, true, CURRENT_USER);
      fail("Can't find wallet associated to address");
    } catch (Exception e) {
      // Expected, Can't find wallet associated to address
    }

    try {
      walletAccountService.enableWalletByAddress(ADDRESS, true, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }
    try {
      walletAccountService.enableWalletByAddress(ADDRESS, true, CURRENT_USER);
    } catch (IllegalAccessException e) {
      fail("Can't enable wallet by this address");
    }
    assertEquals("Wallet Should be enabled", wallet.isEnabled(), true);
    entitiesToClean.add(wallet);
  }

  /**
   * Test set initialization status
   *
   * @throws IllegalAccessException
   */
  @Test
  public void testSetInitializationStatus() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    Wallet wallet = newWallet();
    assertNotNull(wallet);

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);

    try {
      walletAccountService.setInitializationStatus(null, WalletInitializationState.NEW, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(ADDRESS, WalletInitializationState.NEW, "");
      fail("Username is mandatory");
    } catch (Exception e) {
      // Expected, username is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(ADDRESS, null, CURRENT_USER);
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
      walletAccountService.setInitializationStatus(ADDRESS, WalletInitializationState.NEW, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }

    walletAccountService.setInitializationStatus(ADDRESS, WalletInitializationState.NEW, CURRENT_USER);
    assertEquals("Wallet initial status Should be NEW", wallet.getInitializationState(), "NEW");
    entitiesToClean.add(wallet);
  }

  /**
   * Test set initialization status wallet
   *
   * @throws IllegalAccessException
   */
  @Test
  public void testSetInitializationStatusWallet() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    Wallet wallet = newWallet();
    assertNotNull(wallet);

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);

    try {
      walletAccountService.setInitializationStatus(null, WalletInitializationState.NEW);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(ADDRESS, null);
      fail("InitializationState is mandatory");
    } catch (Exception e) {
      // Expected, initializationState is mandatory
    }

    try {
      walletAccountService.setInitializationStatus("walletAdressUser", WalletInitializationState.NEW);
    } catch (Exception e) {
      fail("Can't find wallet associated to address");
    }

    walletAccountService.setInitializationStatus(ADDRESS, WalletInitializationState.NEW);
    assertEquals("Wallet initial status Should be NEW", wallet.getInitializationState(), "NEW");
    entitiesToClean.add(wallet);
  }

  /**
   * Test create admin account
   *
   * @throws IllegalAccessException
   */
  @Test
  public void testCreateAdminAccount() throws IllegalAccessException {
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER);
    identityRegistry.register(identity);
    WalletTokenAdminService tokenAdminService = Mockito.mock(WalletTokenAdminService.class);
    tokenAdminService.createAdminAccount("SaveAdminAccount", CURRENT_USER);
    container.registerComponentInstance(WalletTokenAdminService.class, tokenAdminService);
    // Check admin account creation
    Mockito.verify(tokenAdminService).createAdminAccount("SaveAdminAccount", CURRENT_USER);
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
