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

import org.exoplatform.wallet.dao.WalletPrivateKeyDAO;
import org.exoplatform.wallet.entity.WalletPrivateKeyEntity;
import org.exoplatform.wallet.test.BaseWalletTest;

public class WalletPrivateKeyDAOTest extends BaseWalletTest {

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    WalletPrivateKeyDAO walletPrivateKeyDAO = getService(WalletPrivateKeyDAO.class);

    List<WalletPrivateKeyEntity> allPrivateKeys = walletPrivateKeyDAO.findAll();
    assertNotNull("Returned private keys shouldn't be null", allPrivateKeys);
    assertEquals("Returned private keys list should be empty", 0, allPrivateKeys.size());
  }

  /**
   * Check that DAO deny wallets massive deletion
   */
  @Test
  public void testWalletMassiveDeletionDeny() {
    WalletPrivateKeyDAO walletPrivateKeyDAO = getService(WalletPrivateKeyDAO.class);
    try {
      walletPrivateKeyDAO.deleteAll();
      fail("Shouldn't be able to delete all wallets keys");
    } catch (UnsupportedOperationException e) {
      // Expected
    }

    try {
      walletPrivateKeyDAO.deleteAll(Collections.emptyList());
      fail("Shouldn't be able to delete multiple wallets keys in single operation");
    } catch (UnsupportedOperationException e) {
      // Expected
    }
  }
}
