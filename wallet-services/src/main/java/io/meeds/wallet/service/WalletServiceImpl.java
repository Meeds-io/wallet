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
package io.meeds.wallet.service;

import static io.meeds.wallet.wallet.utils.WalletUtils.DEFAULT_INITIAL_USER_FUND;
import static io.meeds.wallet.wallet.utils.WalletUtils.DEFAULT_MIN_GAS_PRICE;
import static io.meeds.wallet.wallet.utils.WalletUtils.ETHER_TO_WEI_DECIMALS;
import static io.meeds.wallet.wallet.utils.WalletUtils.FUNDS_REQUEST_NOTIFICATION_ID;
import static io.meeds.wallet.wallet.utils.WalletUtils.FUNDS_REQUEST_PARAMETER;
import static io.meeds.wallet.wallet.utils.WalletUtils.FUNDS_REQUEST_SENDER_DETAIL_PARAMETER;
import static io.meeds.wallet.wallet.utils.WalletUtils.FUNDS_REQUEST_SENT;
import static io.meeds.wallet.wallet.utils.WalletUtils.GAS_LIMIT;
import static io.meeds.wallet.wallet.utils.WalletUtils.GWEI_TO_WEI_DECIMALS;
import static io.meeds.wallet.wallet.utils.WalletUtils.INITIAL_FUNDS_KEY_NAME;
import static io.meeds.wallet.wallet.utils.WalletUtils.MAX_GAS_PRICE;
import static io.meeds.wallet.wallet.utils.WalletUtils.MIN_GAS_PRICE;
import static io.meeds.wallet.wallet.utils.WalletUtils.NETWORK_ID;
import static io.meeds.wallet.wallet.utils.WalletUtils.NETWORK_URL;
import static io.meeds.wallet.wallet.utils.WalletUtils.NETWORK_WS_URL;
import static io.meeds.wallet.wallet.utils.WalletUtils.NORMAL_GAS_PRICE;
import static io.meeds.wallet.wallet.utils.WalletUtils.RECEIVER_ACCOUNT_DETAIL_PARAMETER;
import static io.meeds.wallet.wallet.utils.WalletUtils.REWARDINGS_GROUP;
import static io.meeds.wallet.wallet.utils.WalletUtils.SENDER_ACCOUNT_DETAIL_PARAMETER;
import static io.meeds.wallet.wallet.utils.WalletUtils.SETTINGS_KEY_NAME;
import static io.meeds.wallet.wallet.utils.WalletUtils.TOKEN_ADDRESS;
import static io.meeds.wallet.wallet.utils.WalletUtils.DYNAMIC_GAS_PRICE_UPDATE_INTERVAL;
import static io.meeds.wallet.wallet.utils.WalletUtils.WALLET_CONTEXT;
import static io.meeds.wallet.wallet.utils.WalletUtils.WALLET_SCOPE;
import static io.meeds.wallet.wallet.utils.WalletUtils.canAccessWallet;
import static io.meeds.wallet.wallet.utils.WalletUtils.convertFromDecimals;
import static io.meeds.wallet.wallet.utils.WalletUtils.fromJsonString;
import static io.meeds.wallet.wallet.utils.WalletUtils.hideWalletOwnerPrivateInformation;
import static io.meeds.wallet.wallet.utils.WalletUtils.isUserMemberOfGroupOrUser;
import static io.meeds.wallet.wallet.utils.WalletUtils.isUserSpaceMember;
import static io.meeds.wallet.wallet.utils.WalletUtils.toJsonString;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import io.meeds.wallet.wallet.service.BlockchainTransactionService;
import io.meeds.wallet.wallet.service.WalletAccountService;
import io.meeds.wallet.wallet.service.WalletContractService;
import io.meeds.wallet.wallet.service.WalletService;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import io.meeds.wallet.wallet.model.ContractDetail;
import io.meeds.wallet.wallet.model.Wallet;
import io.meeds.wallet.wallet.model.WalletType;
import io.meeds.wallet.wallet.model.settings.GlobalSettings;
import io.meeds.wallet.wallet.model.settings.InitialFundsSettings;
import io.meeds.wallet.wallet.model.settings.NetworkSettings;
import io.meeds.wallet.wallet.model.settings.UserSettings;
import io.meeds.wallet.wallet.model.settings.WalletSettings;
import io.meeds.wallet.wallet.model.transaction.FundsRequest;

import io.meeds.wallet.lock.Lock;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
public class WalletServiceImpl implements WalletService, Startable {

  private static final String          METAMASK_FEATURE_NAME         = "walletMetamask";

  private static final Log             LOG                           = ExoLogger.getLogger(WalletServiceImpl.class);

  private ExoContainer                 container;

  private WalletContractService contractService;

  private WalletAccountService accountService;

  private SettingService               settingService;

  private BlockchainTransactionService blockchainTransactionService;

  private WebNotificationStorage       webNotificationStorage;

  private WalletWebSocketService       webSocketService;

  private ExoFeatureService            featureService;

  private GlobalSettings               configuredGlobalSettings      = new GlobalSettings();

  private double                       dynamicGasPrice;

  private long                         dynamicGasPriceLastUpdateTime = System.currentTimeMillis();

  private long                         dynamicGasPriceUpdateInterval = 120000l;

  private Double                       initialUserFunds;

  public WalletServiceImpl(WalletContractService contractService,
                           WalletAccountService accountService,
                           WalletWebSocketService webSocketService,
                           WebNotificationStorage webNotificationStorage,
                           ExoFeatureService featureService,
                           PortalContainer container,
                           InitParams params) {
    this.container = container;
    this.accountService = accountService;
    this.contractService = contractService;
    this.webSocketService = webSocketService;
    this.webNotificationStorage = webNotificationStorage;
    this.featureService = featureService;

    NetworkSettings network = this.configuredGlobalSettings.getNetwork();
    if (params.containsKey(NETWORK_ID)) {
      String value = params.getValueParam(NETWORK_ID).getValue();
      long defaultNetworkId = Long.parseLong(value);
      network.setId(defaultNetworkId);
    }

    if (params.containsKey(NETWORK_URL)) {
      String defaultNetworkURL = params.getValueParam(NETWORK_URL).getValue();
      network.setProviderURL(defaultNetworkURL);
    }

    if (params.containsKey(NETWORK_WS_URL)) {
      String defaultNetworkWsURL = params.getValueParam(NETWORK_WS_URL).getValue();
      network.setWebsocketProviderURL(defaultNetworkWsURL);
    }

    if (params.containsKey(DEFAULT_INITIAL_USER_FUND)) {
      String value = params.getValueParam(DEFAULT_INITIAL_USER_FUND).getValue();
      try {
        this.initialUserFunds = Double.parseDouble(value);
      } catch (NumberFormatException nfe) {
        // Do nothing
      }
    }

    if (params.containsKey(GAS_LIMIT)) {
      String value = params.getValueParam(GAS_LIMIT).getValue();
      long gasLimit = Long.parseLong(value);
      network.setGasLimit(gasLimit);
    }

    long minGasPrice = DEFAULT_MIN_GAS_PRICE;
    if (params.containsKey(MIN_GAS_PRICE)) {
      String value = params.getValueParam(MIN_GAS_PRICE).getValue();
      minGasPrice = Long.parseLong(value);
    }
    network.setMinGasPrice(minGasPrice);

    long normalGasPrice = DEFAULT_MIN_GAS_PRICE;
    if (params.containsKey(NORMAL_GAS_PRICE)) {
      String value = params.getValueParam(NORMAL_GAS_PRICE).getValue();
      normalGasPrice = Long.parseLong(value);
    }
    network.setNormalGasPrice(normalGasPrice);

    long maxGasPrice = DEFAULT_MIN_GAS_PRICE;
    if (params.containsKey(MAX_GAS_PRICE)) {
      String value = params.getValueParam(MAX_GAS_PRICE).getValue();
      maxGasPrice = Long.parseLong(value);
    }
    network.setMaxGasPrice(maxGasPrice);

    if (params.containsKey(TOKEN_ADDRESS)) {
      String contractAddress = params.getValueParam(TOKEN_ADDRESS).getValue();
      this.configuredGlobalSettings.setContractAddress(contractAddress);
    }

    if (params.containsKey(DYNAMIC_GAS_PRICE_UPDATE_INTERVAL)) {
      String gasPriceUpdateIntervalValue = params.getValueParam(DYNAMIC_GAS_PRICE_UPDATE_INTERVAL).getValue();
      this.dynamicGasPriceUpdateInterval = Long.parseLong(gasPriceUpdateIntervalValue);
    }
  }

  @Override
  public void start() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      this.computeInitialFundsSettings();

      this.configuredGlobalSettings.setContractAbi(contractService.getContractAbi().toString());
      this.configuredGlobalSettings.setContractBin(contractService.getContractBinary());
      String contractAddress = this.configuredGlobalSettings.getContractAddress();
      if (StringUtils.isBlank(contractAddress)) {
        LOG.warn("Contract address configuration is empty");
      } else {
        ContractDetail contractDetail = this.contractService.getContractDetail(contractAddress);
        this.configuredGlobalSettings.setContractDetail(contractDetail);
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public void setConfiguredContractDetail(ContractDetail contractDetail) {
    this.configuredGlobalSettings.setContractDetail(contractDetail);
  }

  @Override
  public InitialFundsSettings getInitialFundsSettings() {
    SettingValue<?> initialFundsSettingsValue = getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, INITIAL_FUNDS_KEY_NAME);

    InitialFundsSettings initialFundsSettings = null;
    if (initialFundsSettingsValue != null && initialFundsSettingsValue.getValue() != null) {
      initialFundsSettings = fromJsonString(initialFundsSettingsValue.getValue().toString(), InitialFundsSettings.class);
    }
    return initialFundsSettings;
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
  }

  @Override
  public GlobalSettings getSettings() {
    return this.configuredGlobalSettings.clone();
  }

  @Override
  public UserSettings getUserSettings(String spaceId, String currentUser, boolean isAdministration) { // NOSONAR
    GlobalSettings globalSettings = getSettings();

    UserSettings userSettings = new UserSettings(globalSettings);
    userSettings.setEnabled(isEnabled());
    userSettings.setWalletEnabled(true);

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

    if (userSettings.isWalletEnabled()) {
      userSettings.setCometdToken(webSocketService.getUserToken(currentUser));
      userSettings.setCometdContext(webSocketService.getCometdContextName());
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
        accountService.retrieveWalletBlockchainState(wallet);
        userSettings.setWallet(wallet);
      }
      walletSettings.setAddresesLabels(accountService.getAddressesLabelsVisibleBy(currentUser));
      if (isAdministration && isUserMemberOfGroupOrUser(currentUser, REWARDINGS_GROUP)) {
        userSettings.setInitialFunds(getInitialFundsSettings());
      }
      userSettings.setMetamaskEnabled(featureService.isFeatureActiveForUser(METAMASK_FEATURE_NAME, currentUser));
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
      } else if (WalletType.isSpace(requestSenderType) && !isUserSpaceMember(requestSenderId, currentUser)) {
        throw new IllegalAccessException("User '" + currentUser + "' is not allowed to request funds on behalf of space "
            + requestSenderId);
      }
    }

    String requestReceipientId = fundsRequest.getReceipient();
    String requestReceipientType = fundsRequest.getReceipientType();

    Wallet requestReceipient = accountService.getWalletByTypeAndId(WalletType.getType(requestReceipientType).getId(),
                                                                   requestReceipientId);

    if (requestReceipient == null || requestReceipient.getTechnicalId() == 0) {
      LOG.warn("Can't find fund request recipient with id {} and type {}", requestReceipientId, requestReceipientType);
    }

    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(FUNDS_REQUEST_SENDER_DETAIL_PARAMETER, accountService.getWalletByTypeAndId(WalletType.USER.getId(), currentUser));
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

  @Override
  public boolean isEnabled() {
    return accountService.isAdminAccountEnabled();
  }

  @Override
  public double getGasPrice() {
    if (dynamicGasPrice == 0 || (System.currentTimeMillis() - dynamicGasPriceLastUpdateTime) > dynamicGasPriceUpdateInterval) {
      try {
        return getGasPriceBlocking();
      } catch (Exception e) {
        LOG.debug("Error retrieving gas price from blockchain. Return normal gas price setting", e);
        return getSettings().getNetwork().getNormalGasPrice();
      }
    }
    return dynamicGasPrice;
  }

  @Override
  public void setGasPrice(double blockchainGasPrice) {
    NetworkSettings network = getSettings().getNetwork();
    Long maxGasPriceInWei = network.getMaxGasPrice();
    if (blockchainGasPrice > maxGasPriceInWei) {
      LOG.info("GAS Price detected on blockchain '{}' GWEI exceeds maximum allowed gas price '{}' GWEI, thus the maximum gas price will be used instead.",
               convertFromDecimals(BigDecimal.valueOf(blockchainGasPrice).toBigInteger(), GWEI_TO_WEI_DECIMALS),
               convertFromDecimals(BigInteger.valueOf(maxGasPriceInWei), GWEI_TO_WEI_DECIMALS));
      blockchainGasPrice = maxGasPriceInWei;
    }
    this.dynamicGasPrice = blockchainGasPrice;
  }

  private void computeInitialFundsSettings() {
    InitialFundsSettings initialFundsSettings = getInitialFundsSettings();
    if (initialFundsSettings == null) {
      initialFundsSettings = new InitialFundsSettings();
    }

    // Compute initial funds switch configured gas limit and max gas price
    double etherAmount = computeInitialEtherFund();
    initialFundsSettings.setEtherAmount(etherAmount);

    // Save computed ether initial fund
    saveInitialFundsSettings(initialFundsSettings);
  }

  private double computeInitialEtherFund() {
    if (this.initialUserFunds != null) {
      return this.initialUserFunds;
    } else {
      NetworkSettings network = this.configuredGlobalSettings.getNetwork();
      long gasLimit = 200000L; // Default gas limit to use in contract
                               // transaction
      long gasPrice = 20000000000L; // Default max gas price to use in contract
      // transaction
      if (network != null) {
        if (network.getGasLimit() != null && network.getGasLimit() > 0) {
          gasLimit = network.getGasLimit();
        }
        if (network.getMaxGasPrice() != null && network.getMaxGasPrice() > 0) {
          gasPrice = network.getMaxGasPrice();
        }
      }
      BigInteger etherAmountInWEI = new BigInteger(String.valueOf(gasLimit)).multiply(new BigInteger(String.valueOf(gasPrice)));
      double etherInitialFund = convertFromDecimals(etherAmountInWEI, ETHER_TO_WEI_DECIMALS);
      double etherAmountMaxDecimals = 3; // max decimals to use in ether initial
      // funds
      double etherAmountDecimals = Math.pow(10, etherAmountMaxDecimals);
      return Math.ceil(etherInitialFund * etherAmountDecimals) / etherAmountDecimals;
    }
  }

  @Lock(duration = 30, timeUnit = TimeUnit.SECONDS)
  private double getGasPriceBlocking() throws IOException {
    if (dynamicGasPrice == 0 || (System.currentTimeMillis() - dynamicGasPriceLastUpdateTime) > dynamicGasPriceUpdateInterval) {
      double gasPrice = getBlockchainTransactionService().getGasPrice();
      setGasPrice(gasPrice);
      dynamicGasPriceLastUpdateTime = System.currentTimeMillis();
    }
    return dynamicGasPrice;
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }

  private BlockchainTransactionService getBlockchainTransactionService() {
    if (blockchainTransactionService == null) {
      blockchainTransactionService = CommonsUtils.getService(BlockchainTransactionService.class);
    }
    return blockchainTransactionService;
  }

}
