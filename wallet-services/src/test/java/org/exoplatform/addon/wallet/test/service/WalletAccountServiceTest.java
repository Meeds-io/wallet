package org.exoplatform.addon.wallet.test.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.getIdentityById;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletTokenAdminService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.*;
import org.exoplatform.social.core.identity.model.Identity;
import org.junit.Test;
import org.mockito.Mockito;

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
    try {
      Wallet walletTest = new Wallet();
      walletTest.setTechnicalId(IDENTITY_ID);
      walletTest.setAddress("");
      walletTest.setPassPhrase(PHRASE);
      walletTest.setEnabled(IS_ENABLED);
      walletTest.setInitializationState(INITIALIZATION_STATE);
      walletAccountService.saveWalletAddress(walletTest, CURRENT_USER, true);
      entitiesToClean.add(walletTest);
    } catch (Exception e) {   
   // Expected, wallet address is mandatory
    }
      
    Wallet wallet = newWallet();
    try {
      walletAccountService.saveWalletAddress(null, CURRENT_USER, true);
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
    }

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
      String address =null;
      walletAccountService.getWalletByAddress(address);
    } catch (Exception e) {   
   // Expected, address shouldn't be null
    }
    
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
   * @throws IllegalAccessException 
   */
  @Test
  public void testGetWalletByTypeAndId() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    try {
      String remoteId ="";
      Wallet walletTest = walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), remoteId);
      assertNull( walletTest);
    } catch (Exception e) {   
   // Expected, remoteId is mandatory 
    }
    
    try {
      String remoteId ="test";
      walletAccountService.getWalletByTypeAndId(WalletType.USER.name(), remoteId);
    } catch (Exception e) {
   // Expected, Can't find identity with remoteId = -1
    }
    
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

    try {
      walletAccountService.isWalletOwner(null, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, wallet shouldn't be null
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
      assertNull( wallet);
    } catch (Exception e) {   
   // Expected, identityId is mandatory 
    }
    
    try {
      long identityId = -1;
      walletAccountService.getWalletByIdentityId(identityId);
    } catch (Exception e) {
   // Expected, Can't find identity with identityId = -1
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
   // Expected, wallet shouldn't be null or TechnicalId < 1
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
   // Expected, wallet shouldn't be null or TechnicalId < 1
    } 
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.USER.name(),
                                                                      CURRENT_USER);
    assertNotNull("Wallet private key shouldn't be null", privateKey);
    entitiesToClean.add(wallet);
  }

  /**
   * Test remove wallet by address
   * 
   * @throws Exception
   */
  @Test
  public void testRemoveWalletByAddress() throws Exception {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    try {
      walletAccountService.removeWalletByAddress(null, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, wallet address is mandatory
    }
      
    Wallet wallet = newWallet();

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    assertNotNull(wallet);
    try {
      walletAccountService.removeWalletByAddress("", CURRENT_USER);
    } catch (Exception e) {   
   // Expected, wallet shouldn't be null
    }
    
    try {
      walletAccountService.removeWalletByAddress(ADDRESS, "root2");
    } catch (Exception e) {   
   // Expected, user is not user rewarding admin
    }
    walletAccountService.removeWalletByAddress(ADDRESS, CURRENT_USER);
    Set<Wallet> wallets = walletAccountService.listWallets();
    assertEquals("Wallet list Should be empty", wallets.size(), 0);
  }

  /**
   * Test remove wallet by type and id
   * 
   * @throws Exception
   */
  @Test
  public void testRemoveWalletByTypeAndId() throws Exception {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    Wallet wallet = newWallet();

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    assertNotNull(wallet);
    try {
      walletAccountService.removeWalletByTypeAndId("", CURRENT_USER, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, wallet type is mandatory
    }
    
    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "", CURRENT_USER);
    } catch (Exception e) {   
   // Expected, wallet remoteId is mandatory
    }
       
    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "root2", "root2");
    } catch (Exception e) {   
   // Expected,is not user rewarding admin
    }
    
    try {
      walletAccountService.removeWalletByTypeAndId("Wallet", "User", CURRENT_USER);
    } catch (Exception e) {   
   // Expected, Can't find identity with type/id
    }
    
    
    try {
      walletAccountService.removeWalletByTypeAndId(wallet.getType(), "root3", CURRENT_USER);
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
   * @throws Exception
   */
  @Test
  public void testEnableWalletByAddress() throws Exception {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    MembershipEntry entry = new MembershipEntry("/platform/rewarding", MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet();
    entryTest.add(entry);
    org.exoplatform.services.security.Identity identity = new org.exoplatform.services.security.Identity(CURRENT_USER, entryTest);
    identityRegistry.register(identity);

    Wallet wallet = newWallet();

    walletAccountService.saveWalletAddress(wallet, CURRENT_USER, true);
    assertNotNull(wallet);
    try {
      walletAccountService.enableWalletByAddress(null, true, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, wallet address is mandatory
    }
    
    try {
      walletAccountService.enableWalletByAddress("walletAdressUser", true, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, Can't find wallet associated to address 
    }
    
    try {
      walletAccountService.enableWalletByAddress(ADDRESS, true, "root2");
    } catch (Exception e) {   
   // Expected, user is not user rewarding admin
    }
    walletAccountService.enableWalletByAddress(ADDRESS, true, CURRENT_USER);
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
    } catch (Exception e) {   
   // Expected, wallet address is mandatory
    }
    
    try {
      walletAccountService.setInitializationStatus(ADDRESS, WalletInitializationState.NEW, "");
    } catch (Exception e) {   
   // Expected, Modifier username is mandatory
    }
    
    try {
      walletAccountService.setInitializationStatus(ADDRESS, null, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, initializationState is mandatory
    }
    
    try {
      walletAccountService.setInitializationStatus("walletAdressUser", WalletInitializationState.NEW, CURRENT_USER);
    } catch (Exception e) {   
   // Expected, Can't find wallet associated to address 
    }
    
    try {
      walletAccountService.setInitializationStatus(ADDRESS, WalletInitializationState.NEW, "root2");
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
    } catch (Exception e) {   
   // Expected, wallet address is mandatory
    }
    
    try {
      walletAccountService.setInitializationStatus(ADDRESS, null);
    } catch (Exception e) {   
   // Expected, initializationState is mandatory
    }
    
    try {
      walletAccountService.setInitializationStatus("walletAdressUser", WalletInitializationState.NEW);
    } catch (Exception e) {   
   // Expected, Can't find wallet associated to address 
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
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
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
   * 
   * @throws IllegalAccessException
   */
  @Test
  public void testGetAdminAccountPassword() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    String password = walletAccountService.getAdminAccountPassword();
    assertNotNull(password);
  }

}