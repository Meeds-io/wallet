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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

import org.exoplatform.wallet.model.WalletAddressLabel;
import org.exoplatform.wallet.test.BaseWalletTest;

public class AddressLabelStorageTest extends BaseWalletTest {

  private long   identityId = 1L;

  private String labelText  = "label";

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    AddressLabelStorage addressLabelStorage = getService(AddressLabelStorage.class);
    assertNotNull(addressLabelStorage);

    Set<WalletAddressLabel> allLabels = addressLabelStorage.getAllLabels();
    assertNotNull("Returned labels list shouldn't be null", allLabels);
    assertEquals("Returned labels should be empty", 0, allLabels.size());
  }

  /**
   * Check label storage: save object
   */
  @Test
  public void testSaveLabel() {
    AddressLabelStorage addressLabelStorage = getService(AddressLabelStorage.class);

    try {
      addressLabelStorage.saveLabel(null);
      fail("Saving a null label should throws an exeption");
    } catch (Exception e) {
      // Expected
    }
    WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
    walletAddressLabel.setAddress(WALLET_ADDRESS_1);
    walletAddressLabel.setIdentityId(identityId);
    walletAddressLabel.setLabel(labelText);

    walletAddressLabel = addressLabelStorage.saveLabel(walletAddressLabel);
    entitiesToClean.add(walletAddressLabel);
    checkLabelContent(walletAddressLabel, identityId, labelText, WALLET_ADDRESS_1);

    walletAddressLabel = addressLabelStorage.getLabel(walletAddressLabel.getId());
    checkLabelContent(walletAddressLabel, identityId, labelText, WALLET_ADDRESS_1);

    Set<WalletAddressLabel> allLabels = addressLabelStorage.getAllLabels();
    assertNotNull("Returned labels list shouldn't be null", allLabels);
    assertEquals("Returned labels should return 1 label", 1, allLabels.size());

    walletAddressLabel = addressLabelStorage.getAllLabels().iterator().next();
    checkLabelContent(walletAddressLabel, identityId, labelText, WALLET_ADDRESS_1);

    labelText = "new label";
    try {
      walletAddressLabel.setLabel(labelText);
      walletAddressLabel = addressLabelStorage.saveLabel(walletAddressLabel);
      checkLabelContent(walletAddressLabel, identityId, labelText, WALLET_ADDRESS_1);
      allLabels = addressLabelStorage.getAllLabels();
      assertEquals("Returned labels should return 1 label", 1, allLabels.size());

      walletAddressLabel = addressLabelStorage.getAllLabels().iterator().next();
      checkLabelContent(walletAddressLabel, identityId, labelText, WALLET_ADDRESS_1);
    } finally {
      labelText = "label";
    }
  }

  /**
   * Check label storage: remove
   */
  @Test
  public void testRemoveLabel() {
    AddressLabelStorage addressLabelStorage = getService(AddressLabelStorage.class);

    WalletAddressLabel walletAddressLabel = new WalletAddressLabel();
    walletAddressLabel.setAddress(WALLET_ADDRESS_1);
    walletAddressLabel.setIdentityId(identityId);
    walletAddressLabel.setLabel(labelText);

    walletAddressLabel = addressLabelStorage.saveLabel(walletAddressLabel);
    assertNotNull(walletAddressLabel);
    entitiesToClean.add(walletAddressLabel);
    checkLabelContent(walletAddressLabel, identityId, labelText, WALLET_ADDRESS_1);

    addressLabelStorage.removeLabel(walletAddressLabel);
    entitiesToClean.remove(walletAddressLabel);

    Set<WalletAddressLabel> allLabels = addressLabelStorage.getAllLabels();
    assertNotNull("Returned labels list shouldn't be null", allLabels);
    assertEquals("Returned labels should be empty", 0, allLabels.size());

    walletAddressLabel = addressLabelStorage.getLabel(walletAddressLabel.getId());
    assertNull("Address label should have been deleted", walletAddressLabel);
  }

  private void checkLabelContent(WalletAddressLabel walletAddressLabel, long identityId, String labelText, String address) {
    assertNotNull("Returned WalletAddress shouldn't be null after saving it", walletAddressLabel);
    assertTrue("Returned WalletAddress should have an id", walletAddressLabel.getId() > 0);
    assertEquals("Returned WalletAddress should have an id", identityId, walletAddressLabel.getIdentityId());
    assertEquals("Returned WalletAddress should have an id", labelText, walletAddressLabel.getLabel());
    assertEquals("Returned WalletAddress should have an id", address, walletAddressLabel.getAddress());
  }
}
