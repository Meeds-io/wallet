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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.dao.AddressLabelDAO;
import org.exoplatform.wallet.entity.AddressLabelEntity;
import org.exoplatform.wallet.test.BaseWalletTest;

public class AddressLabelDAOTest extends BaseWalletTest {

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    AddressLabelDAO addressLabelDAO = getService(AddressLabelDAO.class);
    assertNotNull(addressLabelDAO);

    List<AddressLabelEntity> allAddressLabels = addressLabelDAO.findAll();
    assertNotNull("Returned labels list shouldn't be null", allAddressLabels);
    assertEquals("Returned labels should be empty", 0, allAddressLabels.size());
  }
}
