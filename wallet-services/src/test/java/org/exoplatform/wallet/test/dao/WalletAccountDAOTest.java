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
package org.exoplatform.wallet.test.dao;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.dao.WalletAccountDAO;
import org.exoplatform.wallet.entity.WalletEntity;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.test.BaseWalletTest;

public class WalletAccountDAOTest extends BaseWalletTest {

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    assertNotNull(walletAccountDAO);

    List<WalletEntity> allWallets = walletAccountDAO.findAll();
    assertNotNull("Returned wallets list shouldn't be null", allWallets);
    assertEquals("Returned wallets should be empty", 0, allWallets.size());
  }

  /**
   * Check that DAO deny wallets massive deletion
   */
  @Test
  public void testWalletMassiveDeletionDeny() {
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    try {
      walletAccountDAO.deleteAll();
      fail("Shouldn't be able to delete all wallets");
    } catch (UnsupportedOperationException e) {
      // Expected
    }

    try {
      walletAccountDAO.deleteAll(Collections.emptyList());
      fail("Shouldn't be able to delete multiple wallets in single operation");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }

  /**
   * Check DAO query that must return a wallet with address
   */
  @Test
  public void testGetWalletByAddress() {
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    WalletEntity walletEntity = new WalletEntity();

    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    walletEntity.setId(1L);
    walletEntity.setAddress(address);
    walletEntity.setPassPhrase("passphrase");
    walletEntity.setType(WalletType.USER);
    walletEntity = walletAccountDAO.create(walletEntity);
    entitiesToClean.add(walletEntity);

    walletEntity = walletAccountDAO.findByAddress(address);
    assertNotNull("Can't find wallet by address", walletEntity);
  }
}
