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
package org.exoplatform.addon.wallet.test;

import static org.exoplatform.addon.wallet.utils.WalletUtils.GLOBAL_DATA_VERSION;
import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import org.exoplatform.addon.wallet.model.GlobalSettings;
import org.exoplatform.addon.wallet.service.WalletService;
import org.exoplatform.container.PortalContainer;

public class EthereumWalletServiceTest {

  @BeforeClass
  public static void testStart() {
    PortalContainer.getInstance();
  }

  @Test
  public void testGetDefaultSettings() {
    GlobalSettings settings = getService().getSettings();
    GlobalSettings defaultSettings = new GlobalSettings();
    defaultSettings.setDataVersion(GLOBAL_DATA_VERSION);
    defaultSettings.setDefaultNetworkId(3L);
    defaultSettings.setProviderURL("https://ropsten.infura.io");
    defaultSettings.setWebsocketProviderURL("wss://ropsten.infura.io/ws");
    defaultSettings.setDefaultGas(150000L);

    testSettings("default settings", settings, defaultSettings);
  }

  private void testSettings(String prefixMessage, GlobalSettings settings, GlobalSettings defaultSettings) {
    assertNotNull(prefixMessage + ": null", settings);

    if (StringUtils.isBlank(defaultSettings.getAccessPermission())) {
      assertTrue(prefixMessage + ": access permissions is null", StringUtils.isBlank(settings.getAccessPermission()));
    } else {
      assertEquals(prefixMessage + ": access permissions list is null",
                   defaultSettings.getAccessPermission(),
                   settings.getAccessPermission());
    }

    assertNotNull(prefixMessage + ": gas is null", settings.getDefaultGas());
    assertEquals(prefixMessage + ": wrong value for gas", settings.getDefaultGas().longValue(), 150000L);

    if (defaultSettings.getContractAbi() == null) {
      assertNull(prefixMessage + ": contract ABI should be null", settings.getContractAbi());
    } else {
      assertNotNull(prefixMessage + ": contract ABI shouldn't be null", settings.getContractAbi());
      assertEquals(prefixMessage + ": wrong contract ABI value", defaultSettings.getContractAbi(), settings.getContractAbi());
    }

    if (defaultSettings.getContractBin() == null) {
      assertNull(prefixMessage + ": contract BIN should be null", settings.getContractBin());
    } else {
      assertNotNull(prefixMessage + ": contract BIN shouldn't be null", settings.getContractBin());
      assertEquals(prefixMessage + ": wrong contract BIN value", defaultSettings.getContractBin(), settings.getContractBin());
    }

    if (defaultSettings.getFundsHolder() == null) {
      assertNull(prefixMessage + ": funds holder should be null", settings.getFundsHolder());
    } else {
      assertNotNull(prefixMessage + ": funds holder shouldn't be null", settings.getFundsHolder());
      assertEquals(prefixMessage + ": wrong funds holder value", defaultSettings.getFundsHolder(), settings.getFundsHolder());
    }

    if (defaultSettings.getPrincipalContractAdminName() == null) {
      assertNull(prefixMessage + ": principal contrat admin name should be null", settings.getPrincipalContractAdminName());
    } else {
      assertNotNull(prefixMessage + ": principal contrat admin name shouldn't be null", settings.getPrincipalContractAdminName());
      assertEquals(prefixMessage + ": wrong principal contrat admin name value",
                   defaultSettings.getPrincipalContractAdminName(),
                   settings.getPrincipalContractAdminName());
    }

    if (defaultSettings.getPrincipalContractAdminAddress() == null) {
      assertNull(prefixMessage + ": principal contrat admin address should be null", settings.getPrincipalContractAdminAddress());
    } else {
      assertNotNull(prefixMessage + ": principal contrat admin address shouldn't be null",
                    settings.getPrincipalContractAdminAddress());
      assertEquals(prefixMessage + ": wrong principal contrat admin address value",
                   defaultSettings.getPrincipalContractAdminAddress(),
                   settings.getPrincipalContractAdminAddress());
    }

    if (defaultSettings.getProviderURL() == null) {
      assertNull(prefixMessage + ": blockchain provider URL should be null", settings.getProviderURL());
    } else {
      assertNotNull(prefixMessage + ": blockchain provider URL shouldn't be null",
                    settings.getProviderURL());
      assertEquals(prefixMessage + ": wrong blockchain provider URL value",
                   defaultSettings.getProviderURL(),
                   settings.getProviderURL());
    }

    if (defaultSettings.getWebsocketProviderURL() == null) {
      assertNull(prefixMessage + ": blockchain websocket provider URL should be null", settings.getWebsocketProviderURL());
    } else {
      assertNotNull(prefixMessage + ": blockchain websocket provider URL shouldn't be null",
                    settings.getWebsocketProviderURL());
      assertEquals(prefixMessage + ": wrong blockchain websocket provider URL value",
                   defaultSettings.getWebsocketProviderURL(),
                   settings.getWebsocketProviderURL());
    }

    if (defaultSettings.getFundsHolderType() == null) {
      assertNull(prefixMessage + ": funds holder type should be null", settings.getFundsHolderType());
    } else {
      assertNotNull(prefixMessage + ": funds holder type shouldn't be null",
                    settings.getFundsHolderType());
      assertEquals(prefixMessage + ": wrong funds holder type value",
                   defaultSettings.getFundsHolderType(),
                   settings.getFundsHolderType());
    }

    if (defaultSettings.getInitialFundsRequestMessage() == null) {
      assertNull(prefixMessage + ": initial funds message should be null", settings.getInitialFundsRequestMessage());
    } else {
      assertNotNull(prefixMessage + ": initial funds message shouldn't be null",
                    settings.getInitialFundsRequestMessage());
      assertEquals(prefixMessage + ": wrong initial funds message value",
                   defaultSettings.getInitialFundsRequestMessage(),
                   settings.getInitialFundsRequestMessage());
    }

    if (defaultSettings.getInitialFunds() == null) {
      assertNull(prefixMessage + ": initial funds map should be null", settings.getInitialFunds());
    } else {
      assertNotNull(prefixMessage + ": initial funds map shouldn't be null",
                    settings.getInitialFunds());
      assertEquals(prefixMessage + ": initial funds map value",
                   defaultSettings.getInitialFunds(),
                   settings.getInitialFunds());
    }

    if (defaultSettings.getMaxGasPrice() == null) {
      assertNull(prefixMessage + ": max gas price should be null", settings.getMaxGasPrice());
    } else {
      assertNotNull(prefixMessage + ": max gas price shouldn't be null",
                    settings.getMaxGasPrice());
      assertEquals(prefixMessage + ": max gas price value",
                   defaultSettings.getMaxGasPrice(),
                   settings.getMaxGasPrice());
    }

    if (defaultSettings.getMinGasPrice() == null) {
      assertNull(prefixMessage + ": min gas price should be null", settings.getMinGasPrice());
    } else {
      assertNotNull(prefixMessage + ": min gas price shouldn't be null",
                    settings.getMinGasPrice());
      assertEquals(prefixMessage + ": min gas price value",
                   defaultSettings.getMinGasPrice(),
                   settings.getMinGasPrice());
    }

    if (defaultSettings.getNormalGasPrice() == null) {
      assertNull(prefixMessage + ": normal gas price should be null", settings.getNormalGasPrice());
    } else {
      assertNotNull(prefixMessage + ": normal gas price shouldn't be null",
                    settings.getNormalGasPrice());
      assertEquals(prefixMessage + ": normal gas price value",
                   defaultSettings.getNormalGasPrice(),
                   settings.getNormalGasPrice());
    }

    if (defaultSettings.getDefaultNetworkId() == null) {
      assertNull(prefixMessage + ": network id should be null", settings.getDefaultNetworkId());
    } else {
      assertNotNull(prefixMessage + ": network id shouldn't be null",
                    settings.getDefaultNetworkId());
      assertEquals(prefixMessage + ": network id value",
                   defaultSettings.getDefaultNetworkId(),
                   settings.getDefaultNetworkId());
    }

    if (defaultSettings.getDefaultOverviewAccounts() == null) {
      assertNull(prefixMessage + ": overview accounts set should be null", settings.getDefaultOverviewAccounts());
    } else {
      assertNotNull(prefixMessage + ": overview accounts set shouldn't be null",
                    settings.getDefaultOverviewAccounts());
      assertEquals(prefixMessage + ": overview accounts set value",
                   defaultSettings.getDefaultOverviewAccounts(),
                   settings.getDefaultOverviewAccounts());
    }

    if (defaultSettings.getDataVersion() == null) {
      assertNull(prefixMessage + ": data version should be null", settings.getDataVersion());
    } else {
      assertNotNull(prefixMessage + ": data version shouldn't be null",
                    settings.getDataVersion());
      assertEquals(prefixMessage + ": data version value",
                   defaultSettings.getDataVersion(),
                   settings.getDataVersion());
    }
  }

  private WalletService getService() {
    return PortalContainer.getInstance().getComponentInstanceOfType(WalletService.class);
  }
}
