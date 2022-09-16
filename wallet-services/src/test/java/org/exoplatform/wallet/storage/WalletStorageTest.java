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
package org.exoplatform.wallet.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

import org.exoplatform.wallet.dao.WalletAccountBackupDAO;
import org.exoplatform.wallet.entity.WalletBackupEntity;
import org.exoplatform.wallet.entity.WalletEntity;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletState;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

public class WalletStorageTest extends BaseWalletTest {
  private static final String WALLET_PRIVATE_KEY_CONTENT = "Wallet-Private-Key-Encrypted-With-Password";

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    assertNotNull(walletStorage);

    long walletsCount = walletStorage.getWalletsCount();
    assertEquals("Returned wallets count should be 0", 0, walletsCount);
  }

  /**
   * Check that constructor don't throws an exception even when all parameters
   * are null
   */
  @Test
  public void testNoExceptionThrownOnConstructor() {
    try {
      new WalletStorage(null, null, null, null, null);
    } catch (Exception e) {
      fail("Shouldn't throw an exception on constructor, even if all parameters are null");
    }
  }

  /**
   * Check wallet storage: save
   */
  @Test
  public void testSaveWallet() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    checkWalletContent(wallet, CURRENT_USER_IDENTITY_ID, WALLET_ADDRESS_1, PHRASE, INITIALIZATION_STATE, IS_ENABLED);

    String newInitializationState = WalletState.DENIED.name();
    wallet.setInitializationState(newInitializationState);
    wallet = walletStorage.saveWallet(wallet, false);

    checkWalletContent(wallet, CURRENT_USER_IDENTITY_ID, WALLET_ADDRESS_1, PHRASE, newInitializationState, IS_ENABLED);
  }

  /**
   * Check wallet storage: save wallet backup state
   */
  @Test
  public void testSaveWalletBackupState() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();
    assertFalse(wallet.isBackedUp());

    wallet = walletStorage.saveWallet(wallet, true);
    this.entitiesToClean.add(wallet);

    assertNotNull(wallet);
    assertFalse(wallet.isBackedUp());

    wallet = walletStorage.saveWalletBackupState(wallet.getTechnicalId(), true);
    assertTrue(wallet.isBackedUp());

    wallet = walletStorage.saveWalletBackupState(wallet.getTechnicalId(), false);
    assertFalse(wallet.isBackedUp());
  }

  /**
   * Check wallet storage: remove
   */
  @Test
  public void testRemoveWallet() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);
    assertNotNull(wallet);
    walletStorage.removeWallet(CURRENT_USER_IDENTITY_ID);
    Set<Wallet> listWallets = walletStorage.listWallets();
    assertNotNull(listWallets);
    assertEquals(0, listWallets.size());
  }

  /**
   * Check wallet storage: retrieve by address
   */
  @Test
  public void testGetWalletByAddress() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    wallet = walletStorage.getWalletByAddress(WALLET_ADDRESS_1, null);
    checkWalletContent(wallet, CURRENT_USER_IDENTITY_ID, WALLET_ADDRESS_1, PHRASE, INITIALIZATION_STATE, IS_ENABLED);

    wallet = walletStorage.getWalletByAddress("new-address", null);
    assertNull("Shouldn't find wallet with not recognized address", wallet);
  }

  /**
   * Check wallet storage: retrieve by id
   */
  @Test
  public void testGetWalletByIdentityId() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    wallet = walletStorage.getWalletByIdentityId(CURRENT_USER_IDENTITY_ID, null);
    checkWalletContent(wallet, CURRENT_USER_IDENTITY_ID, WALLET_ADDRESS_1, PHRASE, INITIALIZATION_STATE, IS_ENABLED);

    wallet = walletStorage.getWalletByIdentityId(1523, null);
    assertNull(wallet);
  }

  /**
   * Check wallet storage: list wallets
   */
  @Test
  public void testListWallets() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Set<Wallet> listWallets = walletStorage.listWallets();
    assertNotNull(listWallets);
    assertEquals(0, listWallets.size());

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    listWallets = walletStorage.listWallets();
    assertNotNull(listWallets);
    assertEquals(1, listWallets.size());

    wallet = listWallets.iterator().next();
    checkWalletContent(wallet, CURRENT_USER_IDENTITY_ID, WALLET_ADDRESS_1, PHRASE, INITIALIZATION_STATE, IS_ENABLED);
  }

  /**
   * Check wallet private key storage: save
   */
  @Test
  public void testSaveWalletPrivateKey() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    try {
      walletStorage.saveWalletPrivateKey(0, WALLET_PRIVATE_KEY_CONTENT);
      fail("No wallet associated to 222 id, thus an exception should be thrown");
    } catch (Exception e) {
      // Expected
    }

    try {
      walletStorage.saveWalletPrivateKey(CURRENT_USER_IDENTITY_ID, null);
      fail("No wallet key content, thus an exception should be thrown");
    } catch (Exception e) {
      // Expected
    }

    walletStorage.saveWalletPrivateKey(wallet.getTechnicalId(), WALLET_PRIVATE_KEY_CONTENT);

    walletStorage.saveWalletPrivateKey(wallet.getTechnicalId(), WALLET_PRIVATE_KEY_CONTENT);

    wallet = walletStorage.getWalletByIdentityId(CURRENT_USER_IDENTITY_ID, null);
    assertTrue(wallet.isHasPrivateKey());
    String content = walletStorage.getWalletPrivateKey(CURRENT_USER_IDENTITY_ID);
    assertEquals(WALLET_PRIVATE_KEY_CONTENT, content);

    String newContent = WALLET_PRIVATE_KEY_CONTENT + 1;
    walletStorage.saveWalletPrivateKey(wallet.getTechnicalId(), newContent);
    content = walletStorage.getWalletPrivateKey(CURRENT_USER_IDENTITY_ID);
    assertEquals(newContent, content);

    wallet = walletStorage.getWalletByIdentityId(CURRENT_USER_IDENTITY_ID, null);
    assertTrue(wallet.isHasPrivateKey());
  }

  /**
   * Check wallet private key storage: get encrypted private key
   */
  @Test
  public void testGetWalletPrivateKey() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    long walletId = wallet.getTechnicalId();
    walletStorage.saveWalletPrivateKey(walletId, WALLET_PRIVATE_KEY_CONTENT);

    String content = walletStorage.getWalletPrivateKey(walletId);
    assertEquals(WALLET_PRIVATE_KEY_CONTENT, content);
  }

  /**
   * Check wallet private key storage: get encrypted private key
   */
  @Test
  public void testRemoveWalletPrivateKey() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    long walletId = wallet.getTechnicalId();
    walletStorage.saveWalletPrivateKey(walletId, WALLET_PRIVATE_KEY_CONTENT);

    String content = walletStorage.getWalletPrivateKey(walletId);
    assertNotNull(content);
    wallet = walletStorage.getWalletByIdentityId(walletId, null);
    assertTrue(wallet.isHasPrivateKey());

    walletStorage.removeWalletPrivateKey(walletId);
    content = walletStorage.getWalletPrivateKey(walletId);
    assertNull(content);

    wallet = walletStorage.getWalletByIdentityId(walletId, null);
    assertFalse(wallet.isHasPrivateKey());
  }

  /**
   * Check wallet blockchain state storage
   * {@link WalletStorage#saveWalletBlockchainState(Wallet, String)}
   */
  @Test
  public void testSaveWalletBlockchainState() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    String contractAddress = WalletUtils.getContractAddress();

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);
    walletStorage.retrieveWalletBlockchainState(wallet, contractAddress);
    assertNotNull(wallet);
    assertNull(wallet.getEtherBalance());
    this.entitiesToClean.add(wallet);

    try {
      walletStorage.saveWalletBlockchainState(wallet, null);
      fail("should throw exception when contract address is null");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      walletStorage.saveWalletBlockchainState(null, contractAddress);
      fail("should throw exception when wallet is null");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    Wallet newWallet = newWallet();
    newWallet.setTechnicalId(0);
    try {
      walletStorage.saveWalletBlockchainState(newWallet, contractAddress);
      fail("should throw exception when wallet id is 0");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    // Test no blockchain state is added
    walletStorage.saveWalletBlockchainState(wallet, contractAddress);

    wallet = walletStorage.getWalletByIdentityId(wallet.getTechnicalId(), contractAddress);
    walletStorage.retrieveWalletBlockchainState(wallet, contractAddress);
    assertNotNull(wallet);
    assertNotNull(wallet.getEtherBalance());
    assertEquals(0, wallet.getEtherBalance(), 0);

    wallet.setTokenBalance(2d);
    wallet.setEtherBalance(4d);
    wallet.setIsInitialized(true);
    walletStorage.saveWalletBlockchainState(wallet, contractAddress);

    Wallet storedWallet = walletStorage.getWalletByIdentityId(wallet.getTechnicalId(), contractAddress);
    walletStorage.retrieveWalletBlockchainState(storedWallet, contractAddress);
    assertEquals(wallet.getEtherBalance(), storedWallet.getEtherBalance(), 0);
    assertEquals(wallet.getTokenBalance(), storedWallet.getTokenBalance(), 0);
    assertEquals(wallet.getIsInitialized(), storedWallet.getIsInitialized());

    this.entitiesToClean.add(storedWallet);
  }

  /**
   * Checks whether identity has a wallet or not
   */
  @Test
  public void testHasWallet() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Boolean hasWallet = walletStorage.hasWallet(CURRENT_USER_IDENTITY_ID);
    assertFalse(hasWallet);

    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    hasWallet = walletStorage.hasWallet(CURRENT_USER_IDENTITY_ID);
    assertTrue(hasWallet);
  }

  /**
   * Checks whether identity has a walletBackup or not
   */
  @Test
  public void testHasWalletBackup() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);

    Boolean hasBackupWallet = walletStorage.hasWalletBackup(CURRENT_USER_IDENTITY_ID);
    assertFalse(hasBackupWallet);

    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);
    WalletEntity walletEntity = new WalletEntity();
    walletEntity.setId(wallet.getTechnicalId());
    walletEntity.setAddress(wallet.getAddress());
    walletEntity.setPassPhrase(wallet.getPassPhrase());
    walletEntity.setEnabled(wallet.isEnabled());
    walletEntity.setInitializationState(WalletState.valueOf(wallet.getInitializationState()));
    walletEntity.setProvider(WalletProvider.valueOf(wallet.getProvider()));
    WalletBackupEntity walletBackup = new WalletBackupEntity();
    walletBackup.setAddress(wallet.getAddress());
    walletBackup.setWallet(walletEntity);
    walletBackup = walletAccountBackupDAO.create(walletBackup);
    this.entitiesToClean.add(walletBackup);

    hasBackupWallet = walletStorage.hasWalletBackup(CURRENT_USER_IDENTITY_ID);
    assertTrue(hasBackupWallet);
  }

  @Test
  public void testSwitchToWalletProvider() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);

    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    assertNotNull(wallet);
    walletStorage.switchToWalletProvider(wallet.getTechnicalId(), WalletProvider.METAMASK, WALLET_ADDRESS_2);

    wallet = walletStorage.getWalletByAddress(WALLET_ADDRESS_2, null);
    assertNotNull(wallet);
    assertEquals(WALLET_ADDRESS_2, wallet.getAddress());

    WalletBackupEntity walletBackup = walletAccountBackupDAO.findByWalletId(wallet.getTechnicalId());
    assertNotNull(walletBackup);
    assertEquals(WALLET_ADDRESS_1, walletBackup.getAddress());

    entitiesToClean.add(walletBackup);
    entitiesToClean.add(wallet);
  }

  @Test
  public void testSwitchToInternalWallet() {
    WalletStorage walletStorage = getService(WalletStorage.class);
    WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);

    Wallet wallet = newWallet();
    wallet = walletStorage.saveWallet(wallet, true);
    assertNotNull(wallet);
    walletStorage.switchToWalletProvider(wallet.getTechnicalId(), WalletProvider.METAMASK, WALLET_ADDRESS_2);

    wallet = walletStorage.getWalletByAddress(WALLET_ADDRESS_2, null);
    assertNotNull(wallet);
    assertEquals(WALLET_ADDRESS_2, wallet.getAddress());

    WalletBackupEntity walletBackup = walletAccountBackupDAO.findByWalletId(wallet.getTechnicalId());
    assertNotNull(walletBackup);
    assertEquals(WALLET_ADDRESS_1, walletBackup.getAddress());

    walletStorage.switchToInternalWallet(wallet.getTechnicalId());
    wallet = walletStorage.getWalletByAddress(WALLET_ADDRESS_1, null);

    assertEquals(WalletProvider.INTERNAL_WALLET.name(), wallet.getProvider());
    walletBackup = walletAccountBackupDAO.findByWalletId(wallet.getTechnicalId());
    assertNull(walletBackup);

    entitiesToClean.add(wallet);
  }

  protected void checkWalletContent(Wallet wallet,
                                    long identityId,
                                    String address,
                                    String phrase,
                                    String initializationState,
                                    boolean isEnabled) {
    assertEquals(identityId, wallet.getTechnicalId());
    assertEquals(address.toLowerCase(), wallet.getAddress().toLowerCase());
    assertEquals(phrase, wallet.getPassPhrase());
    assertEquals(initializationState, wallet.getInitializationState());
    assertEquals(isEnabled, wallet.isEnabled());
  }

}
