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
  long    identityId          = 1L;

  String  address             = "walletAddress";

  String  phrase              = "passphrase";

  String  initializationState = WalletInitializationState.INITIALIZED.name();

  boolean isEnabled           = true;

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
   * Check wallet storage: save
   */
  @Test
  public void testSaveWallet() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, true);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    checkWalletContent(wallet, identityId, address, phrase, initializationState, isEnabled);

    initializationState = WalletInitializationState.DENIED.name();
    try {
      wallet.setInitializationState(initializationState);
      wallet = walletStorage.saveWallet(wallet, false);

      checkWalletContent(wallet, identityId, address, phrase, initializationState, isEnabled);
    } finally {
      initializationState = WalletInitializationState.INITIALIZED.name();
    }
  }

  /**
   * Check wallet storage: remove
   */
  @Test
  public void testRemoveWallet() {
    WalletStorage walletStorage = getService(WalletStorage.class);

    Wallet wallet = newWallet();

    wallet = walletStorage.saveWallet(wallet, isEnabled);
    assertNotNull(wallet);
    walletStorage.removeWallet(identityId);
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

    wallet = walletStorage.saveWallet(wallet, isEnabled);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    wallet = walletStorage.getWalletByAddress(address);
    checkWalletContent(wallet, identityId, address, phrase, initializationState, isEnabled);

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

    wallet = walletStorage.saveWallet(wallet, isEnabled);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    wallet = walletStorage.getWalletByIdentityId(identityId);
    checkWalletContent(wallet, identityId, address, phrase, initializationState, isEnabled);

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

    wallet = walletStorage.saveWallet(wallet, isEnabled);

    assertNotNull(wallet);
    this.entitiesToClean.add(wallet);

    listWallets = walletStorage.listWallets();
    assertNotNull(listWallets);
    assertEquals(1, listWallets.size());

    wallet = listWallets.iterator().next();
    checkWalletContent(wallet, identityId, address, phrase, initializationState, isEnabled);
  }

  private void checkWalletContent(Wallet wallet,
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

  private Wallet newWallet() {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(identityId);
    wallet.setAddress(address);
    wallet.setPassPhrase(phrase);
    wallet.setEnabled(isEnabled);
    wallet.setInitializationState(initializationState);
    return wallet;
  }

}
