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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.addon.wallet.model.settings.InitialFundsSettings;
import org.exoplatform.addon.wallet.model.settings.NetworkSettings;
import org.exoplatform.addon.wallet.service.EthereumWalletService;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletContractService;
import org.exoplatform.addon.wallet.service.WalletService;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.junit.Test;

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
    assertNotNull("Default blockchain tansaction minimum gas price shouldn't be null",
                  networkSettings.getMinGasPrice());
    assertNotNull("Default blockchain tansaction normal gas price shouldn't be null",
                  networkSettings.getNormalGasPrice());
    assertNotNull("Default blockchain tansaction maximum gas price shouldn't be null",
                  networkSettings.getMaxGasPrice());
    assertNotNull("Default blockchain network http URL shouldn't be null", networkSettings.getProviderURL());
    assertNotNull("Default blockchain network websocket URL shouldn't be null",
                  networkSettings.getWebsocketProviderURL());
  }
  

  /**
   * Test save initial funds settings
   */
  @Test
  public void testSaveInitialFundsSettings() {
    EthereumWalletService ethereumWalletService = getService(EthereumWalletService.class);
    InitialFundsSettings initialFundsSettings = new InitialFundsSettings();
    int tokenAmount = 5000;
    String fundsHolder = "root";
    String fundsHolderType = "user";
    initialFundsSettings.setTokenAmount(tokenAmount);
    initialFundsSettings.setFundsHolder(fundsHolder);
    initialFundsSettings.setFundsHolderType(fundsHolderType);
    initialFundsSettings.setRequestMessage("Initial fund message");

    ethereumWalletService.saveInitialFundsSettings(initialFundsSettings);

    checkInitialFunds(tokenAmount, fundsHolder, fundsHolderType, ethereumWalletService);

    tokenAmount = 50;
    initialFundsSettings.setTokenAmount(tokenAmount);
    ethereumWalletService.saveInitialFundsSettings(initialFundsSettings);

    // Re-compute settings from DB
    ethereumWalletService.start();
    checkInitialFunds(tokenAmount, fundsHolder, fundsHolderType, ethereumWalletService);
  }

  private void checkInitialFunds(int tokenAmount,
                                  String fundsHolder,
                                  String fundsHolderType,
                                  WalletService walletService) {
    GlobalSettings settings = walletService.getSettings();
    assertNotNull("Settings service shouldn't be null", settings);
    InitialFundsSettings initialFunds = settings.getInitialFunds();
    assertNotNull("Initial found shouldn't be null", initialFunds);
    assertEquals("Funds Holder shouldn't be null", fundsHolder, initialFunds.getFundsHolder());
    assertEquals("Token Amount are not equals", tokenAmount, initialFunds.getTokenAmount(), 0);
    assertEquals("Funds Holder type shouldn't be null", fundsHolderType, initialFunds.getFundsHolderType());
  }

}
