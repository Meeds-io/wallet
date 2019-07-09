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
import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.settings.*;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.addon.wallet.test.mock.IdentityManagerMock;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.junit.Test;
import org.picocontainer.Startable;

public class WalletServiceTest extends BaseWalletTest {

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
    WalletService walletService = getService(WalletService.class);
    InitialFundsSettings initialFundsSettings = new InitialFundsSettings();
    int tokenAmount = 5000;
    String fundsHolder = "root";
    String fundsHolderType = "user";
    initialFundsSettings.setTokenAmount(tokenAmount);
    initialFundsSettings.setFundsHolder(fundsHolder);
    initialFundsSettings.setFundsHolderType(fundsHolderType);
    initialFundsSettings.setRequestMessage("Initial fund message");

    walletService.saveInitialFundsSettings(initialFundsSettings);

    checkInitialFunds(tokenAmount, fundsHolder, fundsHolderType, walletService);

    tokenAmount = 50;
    initialFundsSettings.setTokenAmount(tokenAmount);
    walletService.saveInitialFundsSettings(initialFundsSettings);

    // Re-compute settings from DB
    ((Startable) walletService).start();
    checkInitialFunds(tokenAmount, fundsHolder, fundsHolderType, walletService);
  }

  /**
   * Test set configured contract detail
   */
  @Test
  public void testSetConfiguredContractDetail() {
    WalletService walletService = getService(WalletService.class);
    ContractDetail contractDetail = new ContractDetail();
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    int decimals = 100;
    String name = "Contract";
    String owner = "Root";
    String symbol = "C";
    String sellPrice = "1000";
    String contractType = "Contract V2";
    contractDetail.setContractType(contractType);
    contractDetail.setAddress(address);
    contractDetail.setDecimals(decimals);
    contractDetail.setName(name);
    contractDetail.setOwner(owner);
    contractDetail.setSymbol(symbol);
    contractDetail.setSellPrice(sellPrice);
    walletService.setConfiguredContractDetail(contractDetail);

    checkContractDetais(address, decimals, name, owner, symbol, sellPrice, contractType);

    // Re-compute Contract detail from DB
    contractDetail = null;
    ((Startable) walletService).start();
  }

  /**
   * Test save user preferences
   */
  @Test
  public void testSaveUserPreferences() {
    WalletService walletService = getService(WalletService.class);
    WalletSettings userPreferences = new WalletSettings();
    String currentUser = "root0";
    Integer dataVersion = 2;
    String phrase = "save User";

    userPreferences.setPhrase(phrase);
    userPreferences.setDataVersion(dataVersion);
    walletService.saveUserPreferences(currentUser, userPreferences);

    IdentityManagerMock identityManagerMock = getService(IdentityManagerMock.class);
    identityManagerMock.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUser, false);
    UserSettings userSettings = walletService.getUserSettings(null, currentUser);
    assertEquals("Data version are not equals", dataVersion, userSettings.getUserPreferences().getDataVersion());

  }
  
  /**
   * Test get User Settings 
   */
  @Test
  public void testgetUserSettings() {
    WalletService walletService = getService(WalletService.class);
    String currentUser = "root0";

    UserSettings userSettings = new UserSettings(walletService.getSettings());
    userSettings = walletService.getUserSettings(null, currentUser);
    assertNotNull("User settings shouldn't be null", userSettings); 
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

  private void checkContractDetais(String address,
                                   int decimals,
                                   String name,
                                   String owner,
                                   String symbol,
                                   String sellPrice,
                                   String contractType) {
    WalletService walletService = getService(WalletService.class);
    GlobalSettings settings = walletService.getSettings();
    assertNotNull("Settings service shouldn't be null", settings);
    ContractDetail contract = settings.getContractDetail();
    assertNotNull("Contract shouldn't be null", contract);
    assertEquals("Address are not equals", address, contract.getAddress());
    assertEquals("decimals are not equals", decimals, contract.getDecimals(), 0);
    assertEquals("Contract name are not equals", name, contract.getName());
    assertEquals("Owner are not equals", owner, contract.getOwner());
    assertEquals("Symbol are not equals", symbol, contract.getSymbol());
    assertEquals("SellPrice are not equals", sellPrice, contract.getSellPrice());
    assertEquals("ContractType are not equals", contractType, contract.getContractType());
  }

}
