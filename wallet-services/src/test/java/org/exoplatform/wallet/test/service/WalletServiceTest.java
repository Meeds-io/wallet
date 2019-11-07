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
package org.exoplatform.wallet.test.service;

import static org.exoplatform.wallet.utils.WalletUtils.FUNDS_REQUEST_NOTIFICATION_ID;
import static org.exoplatform.wallet.utils.WalletUtils.getWalletService;
import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.settings.*;
import org.exoplatform.wallet.model.transaction.FundsRequest;
import org.exoplatform.wallet.service.*;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.test.mock.IdentityManagerMock;

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
    try {
      walletService.saveInitialFundsSettings(null);
      fail("initialFundsSettings parameter is mandatory");
    } catch (Exception e) {
      // Expected, initialFundsSettings parameter is mandatory
    }

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
   * Test save user preferences
   */
  @Test
  public void testSaveUserPreferences() {
    WalletService walletService = getService(WalletService.class);

    try {
      walletService.saveUserPreferences(CURRENT_USER, null);
      fail("UserPreferences parameter is mandatory");
    } catch (Exception e) {
      // Expected, userPreferences parameter is mandatory
    }
    WalletSettings userPreferences = new WalletSettings();
    Integer dataVersion = 2;
    String phrase = "save User";

    userPreferences.setPhrase(phrase);
    userPreferences.setDataVersion(dataVersion);
    walletService.saveUserPreferences(CURRENT_USER, userPreferences);

    IdentityManagerMock identityManagerMock = getService(IdentityManagerMock.class);
    identityManagerMock.getOrCreateIdentity(OrganizationIdentityProvider.NAME, CURRENT_USER, false);
    UserSettings userSettings = walletService.getUserSettings(null, CURRENT_USER, false);
    assertEquals("Data version are not equals", dataVersion, userSettings.getUserPreferences().getDataVersion());
  }

  /**
   * Test get User Settings
   */
  @Test
  public void testGetUserSettings() {
    WalletService walletService = getService(WalletService.class);

    String currentUser = "root15";

    UserSettings userSettings = walletService.getUserSettings(null, currentUser, false);
    assertNotNull("User settings shouldn't be null", userSettings);
    assertNotNull("Contract address shouldn't be null", userSettings.getContractAddress());
    assertNotNull("Contract bin shouldn't be null", userSettings.getContractBin());
    assertNotNull("Access permission shouldn't be null", userSettings.getAccessPermission());

    userSettings = walletService.getUserSettings(null, currentUser, true);
    assertNotNull("User settings shouldn't be null", userSettings);
    assertNotNull("Contract address shouldn't be null", userSettings.getContractAddress());
    assertNotNull("Contract bin shouldn't be null", userSettings.getContractBin());
    assertNull("Initial funds should be null when user doesn't belong to rewarding group", userSettings.getInitialFunds());
    assertNotNull("Access permission shouldn't be null", userSettings.getAccessPermission());

    // Add user to admin group
    org.exoplatform.services.security.Identity identity = buildUserIdentityAsAdmin(currentUser);
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);
    identityRegistry.register(identity);
    try {
      userSettings = walletService.getUserSettings(null, currentUser, true);
      assertNotNull("Initial funds shouldn't be null when user belongs to rewarding group", userSettings.getInitialFunds());
    } finally {
      identityRegistry.unregister(currentUser);
    }
  }

  /**
   * Test get Settings
   */
  @Test
  public void testGetSettings() {
    WalletService walletService = getService(WalletService.class);

    GlobalSettings globalSettings = walletService.getSettings();
    assertNotNull("Global settings shouldn't be null", globalSettings);
    assertNotNull("Contract address shouldn't be null", globalSettings.getContractAddress());
    assertNotNull("Contract bin shouldn't be null", globalSettings.getContractBin());
    assertNotNull("Access permission shouldn't be null", globalSettings.getAccessPermission());
  }

  /**
   * Test request Funds
   * 
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testRequestFunds() throws IllegalAccessException {
    WalletService walletService = getService(WalletService.class);
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    try {
      FundsRequest fundsRequest = new FundsRequest();
      fundsRequest.setAddress("test");
      walletService.requestFunds(fundsRequest, CURRENT_USER);
      fail("Bad request sent to server with unknown sender address");
    } catch (Exception e) {
      // Expected, Bad request sent to server with unknown sender address
    }

    Wallet wallet = newWallet();
    walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    entitiesToClean.add(wallet);

    FundsRequest fundsRequest = new FundsRequest();
    Double amount = (double) 500;
    String message = "Funds request";
    fundsRequest.setAmount(amount);
    fundsRequest.setReceipientType(CURRENT_USER);
    fundsRequest.setAddress(WALLET_ADDRESS_1);
    fundsRequest.setContract("No existing address");
    fundsRequest.setReceipient(CURRENT_USER);
    fundsRequest.setMessage(message);

    try {
      walletService.requestFunds(fundsRequest, USER_TEST);
      fail("Expected exception: bad request sent to server with invalid sender type or id");
    } catch (Exception e) {
      // Expected, Bad request sent to server with invalid sender type or id
    }

    walletService.requestFunds(fundsRequest, CURRENT_USER);
  }

  private void checkInitialFunds(int tokenAmount,
                                 String fundsHolder,
                                 String fundsHolderType,
                                 WalletService walletService) {
    GlobalSettings settings = walletService.getSettings();
    assertNotNull("Settings service shouldn't be null", settings);
    InitialFundsSettings initialFunds = getWalletService().getInitialFundsSettings();
    assertNotNull("Initial found shouldn't be null", initialFunds);
    assertEquals("Funds Holder shouldn't be null", fundsHolder, initialFunds.getFundsHolder());
    assertEquals("Token Amount are not equals", tokenAmount, initialFunds.getTokenAmount(), 0);
    assertEquals("Funds Holder type shouldn't be null", fundsHolderType, initialFunds.getFundsHolderType());
  }

  /**
   * Test mark fund request as sent
   * 
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testMarkFundRequestAsSent() throws IllegalAccessException {
    WalletService walletService = getService(WalletService.class);
    WebNotificationStorage webNotification = getService(WebNotificationStorage.class);

    String title = "Notification";
    NotificationInfo notification = new NotificationInfo().key(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID));
    notification.setFrom(CURRENT_USER);
    notification.setTo(CURRENT_USER);
    notification.setTitle(title);
    webNotification.save(notification);
    try {
      walletService.markFundRequestAsSent(notification.getId(), "User");
      fail("Bad request sent to server with invalid contract address");
    } catch (Exception e) {
      // Expected, Bad request sent to server with invalid contract address
    }

    walletService.markFundRequestAsSent(notification.getId(), CURRENT_USER);
    assertEquals(CURRENT_USER, notification.getFrom());
    assertEquals("Notification title are not equals", title, notification.getTitle());
    assertEquals(CURRENT_USER, notification.getTo());
  }

  /**
   * Test is fund request sent
   * 
   * @throws IllegalAccessException when error happens while proceeding request
   *           with CURRENT_USER
   */
  @Test
  public void testIsFundRequestSent() throws IllegalAccessException {
    WalletService walletService = getService(WalletService.class);
    WebNotificationStorage webNotification = getService(WebNotificationStorage.class);

    String title = "Notification";

    NotificationInfo notification = new NotificationInfo().key(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID));
    notification.setFrom(CURRENT_USER);
    notification.setTo(CURRENT_USER);
    notification.setTitle(title);
    webNotification.save(notification);
    try {
      walletService.isFundRequestSent(notification.getId(), "root0");
      fail("Target user of notification is different from current user");
    } catch (Exception e) {
      // Expected, Target user of notification is different from current user
    }
    Boolean isFundsRequestSent = walletService.isFundRequestSent(notification.getId(), CURRENT_USER);
    assertEquals("ContractType are not equals", isFundsRequestSent, false);

  }

}
