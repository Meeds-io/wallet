/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.addon.wallet.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.model.settings.*;
import org.exoplatform.addon.wallet.model.transaction.FundsRequest;
import org.exoplatform.addon.wallet.service.mbean.EthereumWalletServiceManaged;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.ManagedBy;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
@ManagedBy(EthereumWalletServiceManaged.class)
public class EthereumWalletService implements WalletService, Startable {

  private static final Log        LOG                      = ExoLogger.getLogger(EthereumWalletService.class);

  private ExoContainer            container;

  private EthereumClientConnector clientConnector;

  private WalletContractService   contractService;

  private WalletAccountService    accountService;

  private SettingService          settingService;

  private SpaceService            spaceService;

  private WebNotificationStorage  webNotificationStorage;

  private GlobalSettings          configuredGlobalSettings = new GlobalSettings();

  public EthereumWalletService(EthereumClientConnector clientConnector,
                               WalletContractService contractService,
                               WalletAccountService accountService,
                               WebNotificationStorage webNotificationStorage,
                               PortalContainer container,
                               InitParams params) {
    this.container = container;
    this.clientConnector = clientConnector;
    this.accountService = accountService;
    this.contractService = contractService;
    this.webNotificationStorage = webNotificationStorage;

    if (params.containsKey(NETWORK_ID)) {
      String value = params.getValueParam(NETWORK_ID).getValue();
      long defaultNetworkId = Long.parseLong(value);
      this.configuredGlobalSettings.getNetwork().setId(defaultNetworkId);
    }

    if (params.containsKey(NETWORK_URL)) {
      String defaultNetworkURL = params.getValueParam(NETWORK_URL).getValue();
      this.configuredGlobalSettings.getNetwork().setProviderURL(defaultNetworkURL);
    }

    if (params.containsKey(NETWORK_WS_URL)) {
      String defaultNetworkWsURL = params.getValueParam(NETWORK_WS_URL).getValue();
      this.configuredGlobalSettings.getNetwork().setWebsocketProviderURL(defaultNetworkWsURL);
    }

    if (params.containsKey(ACCESS_PERMISSION)) {
      String defaultAccessPermission = params.getValueParam(ACCESS_PERMISSION).getValue();
      this.configuredGlobalSettings.setAccessPermission(defaultAccessPermission);
    }

    if (params.containsKey(GAS_LIMIT)) {
      String value = params.getValueParam(GAS_LIMIT).getValue();
      long gasLimit = Long.parseLong(value);
      this.configuredGlobalSettings.getNetwork().setGasLimit(gasLimit);
    }

    if (params.containsKey(MIN_GAS_PRICE)) {
      String value = params.getValueParam(MIN_GAS_PRICE).getValue();
      long minGasPrice = Long.parseLong(value);
      this.configuredGlobalSettings.getNetwork().setMinGasPrice(minGasPrice);
    }

    if (params.containsKey(NORMAL_GAS_PRICE)) {
      String value = params.getValueParam(NORMAL_GAS_PRICE).getValue();
      long normalGasPrice = Long.parseLong(value);
      this.configuredGlobalSettings.getNetwork().setNormalGasPrice(normalGasPrice);
    }

    if (params.containsKey(MAX_GAS_PRICE)) {
      String value = params.getValueParam(MAX_GAS_PRICE).getValue();
      long maxGasPrice = Long.parseLong(value);
      this.configuredGlobalSettings.getNetwork().setMaxGasPrice(maxGasPrice);
    }

    if (params.containsKey(TOKEN_ADDRESS)) {
      String contractAddress = params.getValueParam(TOKEN_ADDRESS).getValue();
      this.configuredGlobalSettings.setContractAddress(contractAddress);
    }
  }

  @Override
  public void start() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      this.configuredGlobalSettings.setContractAbi(contractService.getContractAbi().toString());
      this.configuredGlobalSettings.setContractBin(contractService.getContractBinary());
      this.computeInitialFundsSettings();

      String contractAddress = this.configuredGlobalSettings.getContractAddress();
      if (StringUtils.isBlank(contractAddress)) {
        return;
      }

      ContractDetail contractDetail = this.contractService.getContractDetail(contractAddress);
      this.configuredGlobalSettings.setContractDetail(contractDetail);

      // start connection to blockchain
      this.clientConnector.start(this.configuredGlobalSettings.getNetwork().getWebsocketProviderURL());

      // TODO if stored contractDetail is empty, its computing // NOSONAR
      // is moved to EthereumWalletTokenAdminService because we can't access
      // blockchain from here see package-info of
      // EthereumWalletTokenAdminService class for more details
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
    clientConnector.stop();
  }

  @Override
  public void setConfiguredContractDetail(ContractDetail contractDetail) {
    this.configuredGlobalSettings.setContractDetail(contractDetail);
  }

  @Override
  public void saveInitialFundsSettings(InitialFundsSettings initialFundsSettings) {
    if (initialFundsSettings == null) {
      throw new IllegalArgumentException("initialFundsSettings parameter is mandatory");
    }

    LOG.debug("Saving initial funds settings: {}", initialFundsSettings);

    getSettingService().set(WALLET_CONTEXT,
                            WALLET_SCOPE,
                            INITIAL_FUNDS_KEY_NAME,
                            SettingValue.create(toJsonString(initialFundsSettings)));

    // Clear cached in memory stored settings
    this.configuredGlobalSettings.setInitialFunds(initialFundsSettings);
  }

  @Override
  public GlobalSettings getSettings() {
    return this.configuredGlobalSettings.clone();
  }

  @Override
  public UserSettings getUserSettings(String spaceId, String currentUser) {
    GlobalSettings globalSettings = getSettings();

    UserSettings userSettings = new UserSettings(globalSettings);
    userSettings.setAdmin(isUserAdmin(currentUser));
    userSettings.setWalletEnabled(true);

    String accessPermission = globalSettings.getAccessPermission();
    if (StringUtils.isNotBlank(accessPermission)) {
      Space space = getSpace(accessPermission);
      // Disable wallet for users not member of the permitted space members
      if (!isUserMemberOf(currentUser, accessPermission) && (space != null && !getSpaceService().isMember(space, currentUser))) {
        LOG.debug("Wallet is disabled for user {} because he's not member of permission expression {}",
                  currentUser,
                  accessPermission);
        userSettings.setWalletEnabled(false);
        return userSettings;
      }
    }

    Wallet wallet = null;
    if (StringUtils.isNotBlank(spaceId)) {
      wallet = accountService.getWalletByTypeAndId(WalletType.SPACE.getId(), spaceId, currentUser);
      if (wallet != null && !canAccessWallet(wallet, currentUser)) {
        LOG.warn("User {} is not allowed to display space wallet {}", currentUser, spaceId);
        userSettings.setWalletEnabled(false);
      }
    } else {
      wallet = accountService.getWalletByTypeAndId(WalletType.USER.getId(), currentUser, currentUser);
    }

    if (wallet != null) {
      userSettings.setWalletEnabled(userSettings.isWalletEnabled() && wallet.isEnabled());
    }

    if (userSettings.isWalletEnabled() || userSettings.isAdmin()) {
      // Append user preferences
      SettingValue<?> userSettingsValue = getSettingService().get(Context.USER.id(currentUser), WALLET_SCOPE, SETTINGS_KEY_NAME);
      WalletSettings walletSettings = null;
      if (userSettingsValue != null && userSettingsValue.getValue() != null) {
        walletSettings = fromJsonString(userSettingsValue.getValue().toString(), WalletSettings.class);
      } else {
        walletSettings = new WalletSettings();
      }
      userSettings.setUserPreferences(walletSettings);

      // Append user wallet settings
      if (wallet != null) {
        walletSettings.setWalletAddress(wallet.getAddress());
        if (accountService.isWalletOwner(wallet, currentUser)) {
          walletSettings.setPhrase(wallet.getPassPhrase());
          walletSettings.setHasKeyOnServerSide(wallet.isHasPrivateKey());
        } else {
          hideWalletOwnerPrivateInformation(wallet);
        }
        userSettings.setWallet(wallet);
      }
      walletSettings.setAddresesLabels(accountService.getAddressesLabelsVisibleBy(currentUser));
    }
    return userSettings;
  }

  @Override
  public void saveUserPreferences(String currentUser, WalletSettings userPreferences) {
    if (userPreferences == null) {
      throw new IllegalArgumentException("userPreferences parameter is mandatory");
    }
    getSettingService().set(Context.USER.id(currentUser),
                            WALLET_SCOPE,
                            SETTINGS_KEY_NAME,
                            SettingValue.create(toJsonString(userPreferences)));
  }

  @Override
  public void requestFunds(FundsRequest fundsRequest, String currentUser) throws IllegalAccessException {
    Wallet requestSender = accountService.getWalletByAddress(fundsRequest.getAddress());
    if (StringUtils.isNotBlank(currentUser)) {
      // Check if user can send request funds for a wallet (user or space type)
      if (requestSender == null) {
        throw new IllegalStateException("Bad request sent to server with unknown sender address");
      }

      String requestSenderId = requestSender.getId();
      String requestSenderType = requestSender.getType();

      if (WalletType.isUser(requestSenderType) && !StringUtils.equals(currentUser, requestSenderId)) {
        LOG.warn("Bad request sent to server with invalid sender type or id {} / {}", requestSenderType, requestSenderId);
        throw new IllegalAccessException("Bad request sent to server with invalid sender");
      } else if (WalletType.isSpace(requestSenderType) && !isUserSpaceMember(requestSenderId, fundsRequest.getReceipient())) {
        throw new IllegalAccessException("Request sender is not allowed to request funds from space");
      }
    }

    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    GlobalSettings settings = getSettings();
    if (!StringUtils.isBlank(fundsRequest.getContract())) {
      ContractDetail contractDetail = settings.getContractDetail();
      if (contractDetail == null) {
        throw new IllegalStateException("Bad request sent to server with invalid contract address (O ly default addresses are permitted)");
      }
      ctx.append(CONTRACT_DETAILS_PARAMETER, contractDetail);
    }

    String requestReceipientId = fundsRequest.getReceipient();
    String requestReceipientType = fundsRequest.getReceipientType();

    Wallet requestReceipient = accountService.getWalletByTypeAndId(WalletType.getType(requestReceipientType).getId(),
                                                                   requestReceipientId);

    if (requestReceipient == null || requestReceipient.getTechnicalId() == 0) {
      LOG.warn("Can't find fund request recipient with id {} and type {}", requestReceipientId, requestReceipientType);
    }

    ctx.append(FUNDS_REQUEST_SENDER_DETAIL_PARAMETER,
               accountService.getWalletByTypeAndId(WalletType.USER.getId(), getCurrentUserId()));
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, requestSender);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, requestReceipient);
    ctx.append(FUNDS_REQUEST_PARAMETER, fundsRequest);
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(FUNDS_REQUEST_NOTIFICATION_ID))).execute(ctx);
  }

  @Override
  public void markFundRequestAsSent(String notificationId, String currentUser) throws IllegalAccessException {
    NotificationInfo notificationInfo = webNotificationStorage.get(notificationId);
    if (notificationInfo == null) {
      throw new IllegalStateException("Notification with id " + notificationId + " wasn't found");
    }
    if (notificationInfo.getTo() == null || !currentUser.equals(notificationInfo.getTo())) {
      throw new IllegalAccessException("Target user of notification '" + notificationId + "' is different from current user");
    }
    notificationInfo.getOwnerParameter().put(FUNDS_REQUEST_SENT, "true");
    webNotificationStorage.update(notificationInfo, false);
  }

  @Override
  public boolean isFundRequestSent(String notificationId, String currentUser) throws IllegalAccessException {
    NotificationInfo notificationInfo = webNotificationStorage.get(notificationId);
    if (notificationInfo == null) {
      throw new IllegalStateException("Notification with id " + notificationId + " wasn't found");
    }
    if (notificationInfo.getTo() == null || !currentUser.equals(notificationInfo.getTo())) {
      throw new IllegalAccessException("Target user of notification '" + notificationId + "' is different from current user");
    }
    String fundRequestSentString = notificationInfo.getOwnerParameter().get(FUNDS_REQUEST_SENT);
    return Boolean.parseBoolean(fundRequestSentString);
  }

  private void computeInitialFundsSettings() {
    SettingValue<?> initialFundsSettingsValue = getSettingService().get(WALLET_CONTEXT,
                                                                        WALLET_SCOPE,
                                                                        INITIAL_FUNDS_KEY_NAME);
    if (initialFundsSettingsValue != null && initialFundsSettingsValue.getValue() == null) {
      this.configuredGlobalSettings.setInitialFunds(fromJsonString(initialFundsSettingsValue.getValue().toString(),
                                                                   InitialFundsSettings.class));
    }
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = CommonsUtils.getService(SpaceService.class);
    }
    return spaceService;
  }
}
