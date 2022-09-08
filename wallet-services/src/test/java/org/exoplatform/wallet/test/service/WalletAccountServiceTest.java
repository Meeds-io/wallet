/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.wallet.test.service;

import static org.exoplatform.wallet.utils.WalletUtils.WALLET_MODIFIED_EVENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.wallet.dao.AddressLabelDAO;
import org.exoplatform.wallet.entity.AddressLabelEntity;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletAddressLabel;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletState;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletAccountServiceImpl;
import org.exoplatform.wallet.service.WalletTokenAdminService;
import org.exoplatform.wallet.storage.AddressLabelStorage;
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
      walletAccountService.setInitializationStatus(null, WalletState.NEW, CURRENT_USER);
      fail("Wallet address is mandatory");
    } catch (Exception e) {
      // Expected, wallet address is mandatory
    }

    try {
      walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletState.NEW, "");
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
      walletAccountService.setInitializationStatus("walletAdressUser", WalletState.NEW, CURRENT_USER);
      fail("Can't find wallet associated to address");
    } catch (Exception e) {
      // Expected, Can't find wallet associated to address
    }

    try {
      walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletState.NEW, "root2");
      fail("User is not user rewarding admin");
    } catch (Exception e) {
      // Expected, user is not user rewarding admin
    }

    walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletState.NEW, CURRENT_USER);
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
      walletAccountService.setInitializationStatus(null, WalletState.NEW);
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

    walletAccountService.setInitializationStatus(WALLET_ADDRESS_1, WalletState.NEW);
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

  public void testSwitchWalletProvider() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    String walletAddress = "0x927f51a2996Ff74d1C380F92DC9006b53A225CeF";
    String rawMessage = "-2037692822791791745-3891968992033463560-1384458414145506416";
    String signedMessage = "0x92874882ac3b2292dc4a05af2f0eceac48fee97392a26d8bc9002159c35279ac0b72729cbdd6e864696782176a39a5cdfbca45c3eec5b34e1f82d2a906356a7d1c";

    Wallet wallet = walletAccountService.saveWallet(newWallet(), true);
    long technicalId = wallet.getTechnicalId();

    assertThrows(IllegalStateException.class,
                 () -> walletAccountService.switchWalletProvider(technicalId,
                                                                 WalletProvider.METAMASK,
                                                                 "",
                                                                 rawMessage,
                                                                 signedMessage));
    assertThrows(IllegalStateException.class,
                 () -> walletAccountService.switchWalletProvider(technicalId,
                                                                 WalletProvider.METAMASK,
                                                                 walletAddress,
                                                                 "",
                                                                 signedMessage));
    assertThrows(IllegalStateException.class,
                 () -> walletAccountService.switchWalletProvider(technicalId,
                                                                 WalletProvider.METAMASK,
                                                                 walletAddress,
                                                                 rawMessage,
                                                                 "signedMessage"));

    walletAccountService.switchWalletProvider(wallet.getTechnicalId(),
                                              WalletProvider.METAMASK,
                                              walletAddress,
                                              rawMessage,
                                              signedMessage);

    Wallet savedWallet = walletAccountService.getWalletByIdentityId(CURRENT_USER_IDENTITY_ID);

    assertNotNull(savedWallet);
    assertEquals(WalletProvider.METAMASK.name(), savedWallet.getProvider());
    entitiesToClean.add(savedWallet);
  }

  @Test
  public void testSwitchToInternalWallet() throws IllegalAccessException {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    String internalWalletAddress = WALLET_ADDRESS_1;
    String privateKeyContent = "content";
    String walletAddress = "0x927f51a2996Ff74d1C380F92DC9006b53A225CeF";
    String rawMessage = "-2037692822791791745-3891968992033463560-1384458414145506416";
    String signedMessage = "0x92874882ac3b2292dc4a05af2f0eceac48fee97392a26d8bc9002159c35279ac0b72729cbdd6e864696782176a39a5cdfbca45c3eec5b34e1f82d2a906356a7d1c";

    Wallet wallet = newWallet();
    wallet.setAddress(internalWalletAddress);
    wallet.setBackedUp(true);
    wallet.setInitializationState(WalletState.INITIALIZED.name());
    wallet.setIsInitialized(true);

    wallet = walletAccountService.saveWallet(wallet, true);
    entitiesToClean.add(wallet);

    walletAccountService.savePrivateKeyByTypeAndId(WalletType.USER.name(), CURRENT_USER, privateKeyContent, CURRENT_USER);


    walletAccountService.switchWalletProvider(wallet.getTechnicalId(),
                                              WalletProvider.METAMASK,
                                              walletAddress,
                                              rawMessage,
                                              signedMessage);

    wallet = walletAccountService.getWalletByIdentityId(CURRENT_USER_IDENTITY_ID);

    assertNotNull(wallet);
    assertEquals(WalletProvider.METAMASK.name(), wallet.getProvider());
    assertEquals(walletAddress, wallet.getAddress());
    assertTrue(wallet.isBackedUp());
    assertTrue(wallet.getIsInitialized());
    assertEquals(WalletState.INITIALIZED.name(), wallet.getInitializationState());

    walletAccountService.switchToInternalWallet(wallet.getTechnicalId());
    wallet = walletAccountService.getWalletByIdentityId(CURRENT_USER_IDENTITY_ID);
    assertNotNull(wallet);
    assertEquals(internalWalletAddress, wallet.getAddress());
    assertEquals(WalletProvider.INTERNAL_WALLET.name(), wallet.getProvider());
    assertTrue(wallet.isBackedUp());
    assertTrue(wallet.getIsInitialized());
    assertEquals(WalletState.MODIFIED.name(), wallet.getInitializationState());
  }

  @Test
  public void testCreateWalletInstance() {
    String walletAddress = "0x927f51a2996FJ74d1C380F92DC9006b53A225CeF";

    WalletAccountService walletAccountService = getService(WalletAccountService.class);

    assertThrows(IllegalArgumentException.class,
                 () -> walletAccountService.createWalletInstance(WalletProvider.METAMASK, null, CURRENT_USER_IDENTITY_ID));
    assertThrows(IllegalArgumentException.class,
                 () -> walletAccountService.createWalletInstance(null, walletAddress, CURRENT_USER_IDENTITY_ID));
    Wallet wallet = walletAccountService.createWalletInstance(WalletProvider.METAMASK, walletAddress, CURRENT_USER_IDENTITY_ID);

    assertNotNull(wallet);
    assertEquals(walletAddress.toLowerCase(), wallet.getAddress().toLowerCase());
    assertEquals(WalletProvider.METAMASK.name(), wallet.getProvider());
  }

  @Test
  public void testRefreshWalletFromBlockchain() throws Exception {
    WalletStorage accountStorage = mock(WalletStorage.class);
    ListenerService listenerService = mock(ListenerService.class);
    WalletTokenAdminService tokenAdminService = mock(WalletTokenAdminService.class);

    WalletAccountServiceImpl walletAccountService = new WalletAccountServiceImpl(mock(PortalContainer.class),
                                                                                 accountStorage,
                                                                                 mock(AddressLabelStorage.class),
                                                                                 mock(InitParams.class));
    walletAccountService.setTokenAdminService(tokenAdminService);
    walletAccountService.setListenerService(listenerService);

    String walletAddress = "walletAddress";
    String contractAddress = "contractAddress";
    ContractDetail contractDetail = mock(ContractDetail.class);
    when(contractDetail.getAddress()).thenReturn(contractAddress);

    Map<String, Set<String>> walletModifications = new HashMap<>();
    HashSet<String> modifications = new HashSet<>();
    walletModifications.put(walletAddress, modifications);

    walletAccountService.refreshWalletFromBlockchain(null, contractDetail, walletModifications);
    verifyNoInteractions(accountStorage, listenerService, tokenAdminService, contractDetail);

    Wallet wallet = new Wallet();
    wallet.setAddress(walletAddress);

    doAnswer(invocation -> {
      wallet.setEtherBalance(0.02d);
      wallet.setTokenBalance(3.02d);
      return null;
    }).when(accountStorage).retrieveWalletBlockchainState(wallet, contractAddress);

    doNothing().when(tokenAdminService).retrieveWalletInformationFromBlockchain(wallet, contractDetail, modifications);

    walletAccountService.refreshWalletFromBlockchain(wallet, contractDetail, walletModifications);
    verify(listenerService, times(0)).broadcast(eq(WALLET_MODIFIED_EVENT), eq(null), any(Wallet.class));

    reset(tokenAdminService);
    doAnswer(invocation -> {
      wallet.setEtherBalance(0.03d);
      wallet.setTokenBalance(3.05d);
      return null;
    }).when(tokenAdminService).retrieveWalletInformationFromBlockchain(wallet, contractDetail, modifications);

    walletAccountService.refreshWalletFromBlockchain(wallet, contractDetail, walletModifications);
    verify(listenerService, times(1)).broadcast(eq(WALLET_MODIFIED_EVENT), eq(null), any(Wallet.class));
  }

}
