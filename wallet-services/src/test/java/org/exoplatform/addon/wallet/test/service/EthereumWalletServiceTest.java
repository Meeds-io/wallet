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
package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import org.exoplatform.addon.wallet.model.settings.*;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.addon.wallet.test.BaseWalletTest;

public class EthereumWalletServiceTest extends BaseWalletTest {

  /**
   * Test if container has properly started
   */
  @Test
  public void testContainerStart() {
    assertNotNull(getService(WalletService.class));

    assertNotNull(getService(WalletAccountService.class));

    // This service isn't added in container
    // See wallet-webapps-common artifact, EthereumWalletTokenAdminService
    // assertNotNull(getService(WalletTokenAdminService.class)); // NOSONAR

    assertNotNull(getService(WalletTransactionService.class));

    assertNotNull(getService(WalletContractService.class));
  }

  /**
   * Test if default settings are injected in WalletService
   */
  @Test
  public void testDefaultParameters() {
    WalletService walletService = getService(WalletService.class);

    GlobalSettings settings = walletService.getSettings();
    assertNotNull("Default settings shouldn't be null", settings);
    assertTrue("Default permission should be null", StringUtils.isBlank(settings.getAccessPermission()));
    assertNotNull("Contract ABI should have been computed after container startup", settings.getContractAbi());
    assertNotNull("Contract BIN should have been computed after container startup", settings.getContractBin());
    assertNotNull("Default contract address shouldn't be null", settings.getContractAddress());
    InitialFundsSettings initialFunds = settings.getInitialFunds();
    assertNotNull("Default initial funds settings shouldn't be null", initialFunds);
    assertNotNull("Default initial ether fund setting shouldn't be null", initialFunds.getEtherAmount());
    assertEquals("Unexpected default initial token fund amount", 0, initialFunds.getTokenAmount(), 0);
    assertEquals("Unexpected default initial ether fund amount", 0.003, initialFunds.getEtherAmount(), 0);
    assertNull("Unexpected default initial fund holder", initialFunds.getFundsHolder());
    NetworkSettings networkSettings = settings.getNetwork();
    assertNotNull("Default blockchain network settings shouldn't be null", networkSettings);
    assertNotNull("Default blockchain network id shouldn't be null", networkSettings.getId());
    assertNotNull("Default blockchain network gas limit shouldn't be null", networkSettings.getGasLimit());
    assertNotNull("Default blockchain tansaction minimum gas price shouldn't be null", networkSettings.getMinGasPrice());
    assertNotNull("Default blockchain tansaction normal gas price shouldn't be null", networkSettings.getNormalGasPrice());
    assertNotNull("Default blockchain tansaction maximum gas price shouldn't be null", networkSettings.getMaxGasPrice());
    assertNotNull("Default blockchain network http URL shouldn't be null", networkSettings.getProviderURL());
    assertNotNull("Default blockchain network websocket URL shouldn't be null", networkSettings.getWebsocketProviderURL());
  }

}
