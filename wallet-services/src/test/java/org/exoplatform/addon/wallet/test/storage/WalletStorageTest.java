/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.test.storage;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.WalletInitializationState;
import org.exoplatform.addon.wallet.storage.WalletStorage;
import org.exoplatform.addon.wallet.test.BaseWalletTest;

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
      new WalletStorage(null, null, null, null);
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

    checkWalletContent(wallet, IDENTITY_ID, ADDRESS, PHRASE, INITIALIZATION_STATE, IS_ENABLED);

    String newInitializationState = WalletInitializationState.DENIED.name();
    wallet.setInitializationState(newInitializationState);
    wallet = walletStorage.saveWallet(wallet, false);

    checkWalletContent(wallet, IDENTITY_ID, ADDRESS, PHRASE, newInitializationState, IS_ENABLED);
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
    walletStorage.removeWallet(IDENTITY_ID);
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

    wallet = walletStorage.getWalletByAddress(ADDRESS);
    checkWalletContent(wallet, IDENTITY_ID, ADDRESS, PHRASE, INITIALIZATION_STATE, IS_ENABLED);

    wallet = walletStorage.getWalletByAddress("new-address");
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

    wallet = walletStorage.getWalletByIdentityId(IDENTITY_ID);
    checkWalletContent(wallet, IDENTITY_ID, ADDRESS, PHRASE, INITIALIZATION_STATE, IS_ENABLED);

    wallet = walletStorage.getWalletByIdentityId(1523);
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
    checkWalletContent(wallet, IDENTITY_ID, ADDRESS, PHRASE, INITIALIZATION_STATE, IS_ENABLED);
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
      walletStorage.saveWalletPrivateKey(IDENTITY_ID, null);
      fail("No wallet key content, thus an exception should be thrown");
    } catch (Exception e) {
      // Expected
    }

    walletStorage.saveWalletPrivateKey(wallet.getTechnicalId(), WALLET_PRIVATE_KEY_CONTENT);

    walletStorage.saveWalletPrivateKey(wallet.getTechnicalId(), WALLET_PRIVATE_KEY_CONTENT);

    wallet = walletStorage.getWalletByIdentityId(IDENTITY_ID);
    assertTrue(wallet.isHasPrivateKey());
    String content = walletStorage.getWalletPrivateKey(IDENTITY_ID);
    assertEquals(WALLET_PRIVATE_KEY_CONTENT, content);

    String newContent = WALLET_PRIVATE_KEY_CONTENT + 1;
    walletStorage.saveWalletPrivateKey(wallet.getTechnicalId(), newContent);
    content = walletStorage.getWalletPrivateKey(IDENTITY_ID);
    assertEquals(newContent, content);

    wallet = walletStorage.getWalletByIdentityId(IDENTITY_ID);
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
    wallet = walletStorage.getWalletByIdentityId(walletId);
    assertTrue(wallet.isHasPrivateKey());

    walletStorage.removeWalletPrivateKey(walletId);
    content = walletStorage.getWalletPrivateKey(walletId);
    assertNull(content);

    wallet = walletStorage.getWalletByIdentityId(walletId);
    assertFalse(wallet.isHasPrivateKey());
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
