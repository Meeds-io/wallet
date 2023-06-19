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
package org.exoplatform.wallet.service;

import static org.exoplatform.wallet.statistic.StatisticUtils.OPERATION;
import static org.exoplatform.wallet.utils.WalletUtils.ADMIN_KEY_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.MODIFY_ADDRESS_ASSOCIATED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.NEW_ADDRESS_ASSOCIATED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.SIMPLE_CHARS;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_ADMIN_REMOTE_ID;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_DELETED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_DISABLED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_ENABLED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_INITIALIZATION_MODIFICATION_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_INITIALIZED_SETTING_PARAM;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_MODIFIED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_PROVIDER_MODIFIED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_SCOPE;
import static org.exoplatform.wallet.utils.WalletUtils.canAccessWallet;
import static org.exoplatform.wallet.utils.WalletUtils.checkUserIsSpaceManager;
import static org.exoplatform.wallet.utils.WalletUtils.computeWalletFromIdentity;
import static org.exoplatform.wallet.utils.WalletUtils.computeWalletIdentity;
import static org.exoplatform.wallet.utils.WalletUtils.getContractAddress;
import static org.exoplatform.wallet.utils.WalletUtils.getContractDetail;
import static org.exoplatform.wallet.utils.WalletUtils.getIdentityById;
import static org.exoplatform.wallet.utils.WalletUtils.getIdentityByTypeAndId;
import static org.exoplatform.wallet.utils.WalletUtils.getSettings;
import static org.exoplatform.wallet.utils.WalletUtils.getSpacePrettyName;
import static org.exoplatform.wallet.utils.WalletUtils.hideWalletOwnerPrivateInformation;
import static org.exoplatform.wallet.utils.WalletUtils.isUserRewardingAdmin;
import static org.exoplatform.wallet.utils.WalletUtils.isUserSpaceManager;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletAddressLabel;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletState;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.statistic.ExoWalletStatistic;
import org.exoplatform.wallet.statistic.ExoWalletStatisticService;
import org.exoplatform.wallet.storage.AddressLabelStorage;
import org.exoplatform.wallet.storage.WalletStorage;

public class WalletAccountServiceImpl implements WalletAccountService, ExoWalletStatisticService, Startable {

  private static final Log        LOG                                     =
                                      ExoLogger.getLogger(WalletAccountServiceImpl.class);

  private static final String     USER_MESSAGE_IN_EXCEPTION               = "User '";

  private static final String     USER_MESSAGE_PREFIX                     = "User ";

  private static final String     CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS = "Can't find wallet associated to address ";

  private static final String     ADDRESS_PARAMTER_IS_MANDATORY           = "address paramter is mandatory";

  private static final String     STATISTIC_OPERATION_INITIALIZATION      = "initialization";

  private static final String     STATISTIC_OPERATION_CREATE              = "create_wallet";

  private static final String     STATISTIC_OPERATION_ENABLE              = "enable";

  private static final String     STATISTIC_OPERATION_DISABLE             = "disable";

  private PortalContainer         container;

  private WalletTokenAdminService tokenAdminService;

  private SettingService          settingService;

  private WalletStorage           accountStorage;

  private AddressLabelStorage     labelStorage;

  private ListenerService         listenerService;

  private String                  adminAccountPassword;

  private boolean                 adminAccountEnabled;

  public WalletAccountServiceImpl(PortalContainer container,
                                  WalletStorage walletAccountStorage,
                                  AddressLabelStorage labelStorage,
                                  SettingService settingService,
                                  InitParams params) {
    this.container = container;
    this.settingService = settingService;
    this.accountStorage = walletAccountStorage;
    this.labelStorage = labelStorage;
    if (params != null && params.containsKey(ADMIN_KEY_PARAMETER)
        && StringUtils.isNotBlank(params.getValueParam(ADMIN_KEY_PARAMETER).getValue())) {
      this.adminAccountPassword = params.getValueParam(ADMIN_KEY_PARAMETER).getValue();
    }
  }

  @Override
  public void start() {
    // Ensure to make initialization after starting all other services of Wallet
    PortalContainer.addInitTask(container.getPortalContext(), new RootContainer.PortalContainerPostInitTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        ExoContainerContext.setCurrentContainer(portalContainer);
        RequestLifeCycle.begin(portalContainer);
        try {
          Wallet adminWallet = getAdminWallet();
          adminAccountEnabled = adminWallet != null && adminWallet.isEnabled();
        } catch (Exception e) {
          LOG.error("Error starting service", e);
        } finally {
          RequestLifeCycle.end();
        }
      }
    });
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public Set<Wallet> listWallets() {
    Set<Wallet> wallets = accountStorage.listWallets();
    wallets.forEach(wallet -> {
      retrieveWalletBlockchainState(wallet);
      hideWalletOwnerPrivateInformation(wallet);
    });
    return wallets;
  }

  @Override
  public void refreshWalletsFromBlockchain(Map<String, Set<String>> walletsModifications) {
    if (walletsModifications == null) {
      walletsModifications = new HashMap<>();
    }

    ContractDetail contractDetail = getContractDetail();
    Set<String> walletAddresses = walletsModifications.keySet();
    for (String walletAddress : walletAddresses) {
      Wallet wallet = getWalletByAddress(walletAddress);
      if (wallet == null) {
        continue;
      }
      refreshWalletFromBlockchain(wallet, contractDetail, walletsModifications);
    }
  }

  @Override
  public void refreshWalletFromBlockchain(Wallet wallet, // NOSONAR
                                          ContractDetail contractDetail,
                                          Map<String, Set<String>> walletsModifications) {
    if (wallet == null) {
      return;
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      return;
    }

    if (contractDetail == null) {
      contractDetail = getSettings().getContractDetail();
    }

    if (getTokenAdminService() == null) {
      LOG.warn("Can't refresh wallet from blockchain because TokenAdminService isn't initialized yet");
    } else {
      try {
        Set<String> walletModifications = walletsModifications == null ? null
                                                                       : walletsModifications.get(wallet.getAddress());
        accountStorage.retrieveWalletBlockchainState(wallet, contractDetail.getAddress());
        Wallet originalWallet = wallet.clone();

        getTokenAdminService().retrieveWalletInformationFromBlockchain(wallet,
                                                                       contractDetail,
                                                                       walletModifications);
        saveWalletBlockchainState(wallet, contractDetail.getAddress());

        if (!StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletState.INITIALIZED.name()) &&
            !StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletState.PENDING.name()) &&
            !StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletState.DENIED.name())
            && ((wallet.getIsInitialized() != null && wallet.getIsInitialized())
                || (wallet.getEtherBalance() > 0 && wallet.getTokenBalance() > 0))) { // if wallet was sent cryptos & tokens from outside and its state is NEW, we set it to initialized
          wallet.setInitializationState(WalletState.INITIALIZED.name());
          setInitializationStatus(wallet.getAddress(), WalletState.INITIALIZED);
        }

        if (!Objects.equals(originalWallet.getEtherBalance(), wallet.getEtherBalance())
            || !Objects.equals(originalWallet.getTokenBalance(), wallet.getTokenBalance())) {
          getListenerService().broadcast(WALLET_MODIFIED_EVENT, null, wallet);
        }
      } catch (Exception e) {
        LOG.error("Error refreshing wallet state on blockchain", e);
      }
    }

    // Checks if admin wallet was newly enabled from blockchain
    if (WalletType.isAdmin(wallet.getType())) {
      this.adminAccountEnabled = wallet.isEnabled();
    }
  }

  @Override
  public long getWalletsCount() {
    return accountStorage.getWalletsCount();
  }

  @Override
  public Wallet getWalletByIdentityId(long identityId) {
    if (identityId == 0) {
      throw new IllegalArgumentException("identityId is mandatory");
    }
    Identity identity = getIdentityById(identityId);
    if (identity == null) {
      LOG.debug("Can't find identity with id {}", identityId);
      return null;
    }
    return getWalletOfIdentity(identity);
  }

  @Override
  public void retrieveWalletBlockchainState(Wallet wallet) {
    if (wallet == null) {
      return;
    }
    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      LOG.warn("Contract address is empty, thus wallets can't be refreshed");
      return;
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      return;
    }
    accountStorage.retrieveWalletBlockchainState(wallet, contractAddress);
    if (wallet.getEtherBalance() == null) {
      refreshWalletFromBlockchain(wallet, null, null);
    }
  }

  @Override
  public Wallet getWalletByTypeAndId(String type, String remoteId, String currentUser) {
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet != null) {
      if (WalletType.isSpace(wallet.getType())) {
        wallet.setSpaceAdministrator(isUserSpaceManager(wallet.getId(), currentUser));
        if (!wallet.isSpaceAdministrator()) {
          hideWalletOwnerPrivateInformation(wallet);
        }
      } else if (!StringUtils.equals(wallet.getId(), currentUser)) {
        hideWalletOwnerPrivateInformation(wallet);
      }
      retrieveWalletBlockchainState(wallet);
    }
    return wallet;
  }

  @Override
  public Wallet getWalletByTypeAndId(String type, String remoteId) {
    if (StringUtils.isBlank(remoteId)) {
      throw new IllegalArgumentException("id parameter is mandatory");
    }
    WalletType accountType = WalletType.getType(type);
    if (accountType.isSpace()) {
      // Ensure to get a fresh prettyName of space
      remoteId = getSpacePrettyName(remoteId);
    }
    Identity identity = getIdentityByTypeAndId(accountType, remoteId);
    if (identity == null) {
      LOG.debug("Can't find identity with id {} and type {}. It may be removed.", remoteId, accountType.getId());
      return null;
    }
    return getWalletOfIdentity(identity);
  }

  @Override
  public Wallet getAdminWallet() {
    return getWalletByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID);
  }

  @Override
  public void savePrivateKeyByTypeAndId(String type,
                                        String remoteId,
                                        String content,
                                        String currentUser) throws IllegalAccessException {
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet == null || wallet.getTechnicalId() < 1) {
      throw new IllegalStateException("Can't find " + type + " with remote id " + remoteId
          + ". Wallet private key will not be created.");
    }
    checkIsWalletOwner(wallet, currentUser, "save private key of wallet");
    accountStorage.saveWalletPrivateKey(wallet.getTechnicalId(), content);
  }

  @Override
  public String getPrivateKeyByTypeAndId(String type, String remoteId, String currentUser) throws IllegalAccessException {
    if (WalletType.isAdmin(type)) {
      throw new IllegalAccessException(USER_MESSAGE_IN_EXCEPTION + currentUser
          + "' is not allowed to access private key of admin '" + remoteId
          + "'");
    }
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet == null || wallet.getTechnicalId() < 1) {
      return null;
    }
    checkIsWalletOwner(wallet, currentUser, "get private key of wallet");
    try {
      return accountStorage.getWalletPrivateKey(wallet.getTechnicalId());
    } catch (Exception e) {
      LOG.warn("Unable to decode private key of {} '{}': {}", type, remoteId, e.getMessage());
      return null;
    }
  }

  @Override
  public String getPrivateKeyByTypeAndId(String type, String remoteId) {
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet == null || wallet.getTechnicalId() < 1) {
      return null;
    }
    try {
      return accountStorage.getWalletPrivateKey(wallet.getTechnicalId());
    } catch (Exception e) {
      LOG.warn("Unable to decode private key of {} '{}': {}", type, remoteId, e.getMessage());
      return null;
    }
  }

  @Override
  public void removePrivateKeyByTypeAndId(String type, String remoteId, String currentUser) throws IllegalAccessException {
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet == null || wallet.getTechnicalId() < 1) {
      return;
    }
    checkIsWalletOwner(wallet, currentUser, "remove private key of wallet");
    accountStorage.removeWalletPrivateKey(wallet.getTechnicalId());
  }

  @Override
  public Wallet getWalletByAddress(String address, String currentUser) {
    if (address == null) {
      throw new IllegalArgumentException("address is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address, getContractAddress());
    if (wallet != null) {
      Identity identity = getIdentityById(wallet.getTechnicalId());
      computeWalletFromIdentity(wallet, identity);
      if (canAccessWallet(wallet, currentUser)) {
        retrieveWalletBlockchainState(wallet);
      }
    }
    return wallet;
  }

  @Override
  public Wallet getWalletByAddress(String address) {
    return getWalletByAddress(address, null);
  }

  @Override
  public void createAdminAccount(String privateKey, String currentUser) throws IllegalAccessException {
    getTokenAdminService().createAdminAccount(privateKey, currentUser);
  }

  @Override
  public void saveWalletBlockchainState(Wallet wallet, String contractAddress) {
    accountStorage.saveWalletBlockchainState(wallet, contractAddress);
  }

  @Override
  public Wallet saveWalletBackupState(String currentUser, long identityId, boolean backupState) throws IllegalAccessException {
    if (identityId == 0) {
      throw new IllegalArgumentException("Wallet technical id is mandatory");
    }

    if (StringUtils.isBlank(currentUser)) {
      throw new IllegalArgumentException("User name is mandatory");
    }

    Wallet wallet = accountStorage.getWalletByIdentityId(identityId, getContractAddress());
    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet with id " + identityId);
    }
    checkIsWalletOwner(wallet, currentUser, "save wallet");

    return accountStorage.saveWalletBackupState(identityId, backupState);
  }

  @Override
  @ExoWalletStatistic(local = true, service = "wallet", operation = STATISTIC_OPERATION_CREATE)
  public void saveWalletAddress(Wallet wallet, String currentUser) throws IllegalAccessException {
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet is mandatory");
    }

    if (StringUtils.isBlank(wallet.getAddress())) {
      throw new IllegalArgumentException("Wallet address is empty, thus it can't be saved");
    }

    computeWalletIdentity(wallet);

    long identityId = wallet.getTechnicalId();
    Wallet oldWallet = accountStorage.getWalletByIdentityId(identityId, getContractAddress());
    checkCanSaveWallet(wallet, oldWallet, currentUser);
    if (oldWallet != null && StringUtils.equalsIgnoreCase(oldWallet.getAddress(), wallet.getAddress())) {
      throw new IllegalAccessException("Can't modify wallet properties once saved");
    }

    WalletProvider provider = oldWallet == null || oldWallet.getProvider() == null ? WalletProvider.INTERNAL_WALLET
                                                                                   : WalletProvider.valueOf(oldWallet.getProvider());
    computeWalletProperties(wallet, oldWallet, provider, oldWallet == null);
    accountStorage.saveWallet(wallet, oldWallet == null);

    if (oldWallet != null) {
      // Automatically Remove old private key when modifying address associated
      // to wallet
      accountStorage.removeWalletPrivateKey(identityId);
    }

    // This is about address modification or creation, thus, the blockchain must
    // be retrieved again
    if (!WalletType.isAdmin(wallet.getType())) {
      refreshWalletFromBlockchain(wallet, getContractDetail(), null);

      boolean isNew = oldWallet == null && !accountStorage.hasWalletBackup(identityId) && !isUserWalletInitialized(wallet.getId());
      String eventName = isNew ? NEW_ADDRESS_ASSOCIATED_EVENT
                               : MODIFY_ADDRESS_ASSOCIATED_EVENT;
      try {
        getListenerService().broadcast(eventName, wallet.clone(), currentUser);
      } catch (Exception e) {
        LOG.error("Error broadcasting event {} for wallet {}", eventName, wallet, e);
      } finally {
        setUserWalletAsInitialized(wallet.getId());
      }
    }
  }

  @Override
  public Wallet saveWallet(Wallet wallet, boolean isNew) {
    wallet = accountStorage.saveWallet(wallet, isNew);
    if (isNew && !isUserWalletInitialized(wallet.getId())) {
      try {
        getListenerService().broadcast(NEW_ADDRESS_ASSOCIATED_EVENT, wallet.clone(), wallet.getId());
      } catch (Exception e) {
        LOG.error("Error broadcasting event {} for wallet {}", NEW_ADDRESS_ASSOCIATED_EVENT, wallet, e);
      } finally {
        setUserWalletAsInitialized(wallet.getId());
      }
    }
    return wallet;
  }

  @Override
  public void switchToInternalWallet(long identityId) {
    Wallet wallet = getWalletByIdentityId(identityId);
    if (wallet == null) {
      throw new IllegalStateException("Identity with Id " + identityId + " doesn't have a wallet yet");
    } else if (StringUtils.equalsIgnoreCase(WalletProvider.INTERNAL_WALLET.name(), wallet.getProvider())) {
      throw new IllegalStateException("Identity with Id " + identityId + " already have internal wallet as provider");
    }

    if (accountStorage.hasWalletBackup(identityId)) {
      accountStorage.switchToInternalWallet(identityId);
    } else {
      // No internal wallet created by user, so delete wallet information
      accountStorage.removeWallet(identityId);
    }
    setUserWalletAsInitialized(wallet.getId());
  }

  @Override
  public void switchWalletProvider(long identityId,
                                   WalletProvider provider,
                                   String newAddress,
                                   String rawMessage,
                                   String signedMessage) {
    if (provider == null) {
      throw new IllegalArgumentException("provider is mandatory");
    }
    try {
      boolean valid = validateSignedMessage(newAddress, rawMessage, signedMessage);
      if (!valid) {
        throw new IllegalStateException("Invalid Signed Message");
      }
    } catch (SignatureException e) {
      throw new IllegalStateException("Invalid Signed Message", e);
    }

    Wallet existingWallet = getWalletByIdentityId(identityId);

    Wallet wallet = null;
    if (accountStorage.hasWallet(identityId)) {
      accountStorage.switchToWalletProvider(identityId, provider, newAddress);

      wallet = accountStorage.getWalletByAddress(newAddress, getContractAddress());
    } else {
      wallet = new Wallet();
      wallet.setTechnicalId(identityId);
      wallet.setAddress(newAddress);
      wallet.setProvider(provider.name());
      wallet.setEnabled(true);
      wallet.setInitializationState(WalletState.NEW.name());
      wallet.setPassPhrase(StringUtils.EMPTY);
      computeWalletIdentity(wallet); // Set wallet Type
      wallet = accountStorage.saveWallet(wallet, true);
    }

    refreshWalletFromBlockchain(wallet, getContractDetail(), null);

    boolean isNew = !isUserWalletInitialized(wallet.getId()) && !accountStorage.hasWalletBackup(identityId) && (existingWallet == null || StringUtils.isBlank(existingWallet.getAddress()) || StringUtils.isNotBlank(existingWallet.getInitializationState()));
    try {
      if (isNew) {
        getListenerService().broadcast(NEW_ADDRESS_ASSOCIATED_EVENT, wallet.clone(), wallet.getId());
      } else {
        getListenerService().broadcast(WALLET_PROVIDER_MODIFIED_EVENT, provider, wallet);
      }
    } catch (Exception e) {
      LOG.error("Error broadcasting event {} for wallet {}",
                isNew ? NEW_ADDRESS_ASSOCIATED_EVENT : WALLET_PROVIDER_MODIFIED_EVENT,
                wallet,
                e);
    } finally {
      setUserWalletAsInitialized(wallet.getId());
    }
  }

  @Override
  public Wallet createWalletInstance(WalletProvider provider, String address, long identityId) {
    if (provider == null) {
      throw new IllegalArgumentException("provider is mandatory");
    }
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException("Wallet address is mandatory");
    }

    Identity identity = getIdentityById(identityId);
    WalletType type = WalletType.getType(identity.getProviderId());
    Wallet wallet = new Wallet();
    wallet.setAddress(address);
    wallet.setTechnicalId(identityId);
    wallet.setType(type.name());

    Wallet oldWallet = accountStorage.getWalletByIdentityId(wallet.getTechnicalId(), getContractAddress());
    boolean isNew = oldWallet == null;
    computeWalletProperties(wallet, oldWallet, provider, isNew);
    return wallet;
  }

  @Override
  public void removeWalletByAddress(String address, String currentUser) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMTER_IS_MANDATORY);
    }
    Wallet wallet = accountStorage.getWalletByAddress(address, getContractAddress());
    if (wallet == null) {
      throw new IllegalStateException(CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS + address);
    }
    if (!isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("Current user " + currentUser + " attempts to delete wallet with address " + address
          + " of "
          + wallet.getType() + " " + wallet.getId());
    }
    accountStorage.removeWallet(wallet.getTechnicalId());
  }

  @Override
  public void removeWalletByTypeAndId(String type, String remoteId, String currentUser) throws IllegalAccessException {
    if (StringUtils.isBlank(type)) {
      throw new IllegalArgumentException("wallet type parameter is mandatory");
    }
    if (StringUtils.isBlank(remoteId)) {
      throw new IllegalArgumentException("remote id parameter is mandatory");
    }
    if (!isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("Current user " + currentUser + " attempts to delete wallet of "
          + type + " " + remoteId);
    }
    Identity identity = getIdentityByTypeAndId(WalletType.getType(type), remoteId);
    if (identity == null) {
      LOG.debug("Can't find identity with type/id: {}/{}", type, remoteId);
      return;
    }
    long identityId = Long.parseLong(identity.getId());
    Wallet wallet = accountStorage.getWalletByIdentityId(identityId, getContractAddress());

    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet with type/id: " + type + "/" + remoteId);
    }
    accountStorage.removeWallet(wallet.getTechnicalId());
  }

  @Override
  @ExoWalletStatistic(service = "wallet", operation = STATISTIC_OPERATION_ENABLE, local = true)
  public boolean enableWalletByAddress(String address, boolean enable, String currentUser) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMTER_IS_MANDATORY);
    }
    Wallet wallet = accountStorage.getWalletByAddress(address, getContractAddress());
    if (wallet == null) {
      throw new IllegalStateException(CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS + address);
    }

    boolean walletEnablement = wallet.isEnabled();
    if (walletEnablement != enable) {
      // Only 'rewarding' group members can change wallets
      if (!isUserRewardingAdmin(currentUser)) {
        throw new IllegalAccessException(USER_MESSAGE_PREFIX + currentUser + " attempts to disable wallet with address " + address
            + " of "
            + wallet.getType() + " " + wallet.getId());
      }
      wallet.setEnabled(enable);
      accountStorage.saveWallet(wallet, false);
      if (walletEnablement != wallet.isEnabled()) {
        try {
          getListenerService().broadcast(enable ? WALLET_ENABLED_EVENT : WALLET_DISABLED_EVENT, wallet, currentUser);
        } catch (Exception e) {
          LOG.error("Error while braodcasting wallet {} enablement modification to {}", wallet, enable);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  @ExoWalletStatistic(local = true, service = "wallet", operation = STATISTIC_OPERATION_INITIALIZATION)
  public void setInitializationStatus(String address,
                                      WalletState initializationState,
                                      String currentUser) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMTER_IS_MANDATORY);
    }
    if (initializationState == null) {
      throw new IllegalArgumentException("Initialization state is mandatory");
    }
    if (StringUtils.isBlank(currentUser)) {
      throw new IllegalArgumentException("Modifier username is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address, getContractAddress());
    if (wallet == null) {
      throw new IllegalStateException(CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS + address);
    }

    if (!isUserRewardingAdmin(currentUser) && initializationState != WalletState.DELETED) {
      // The only authorized initialization transition allowed to a regular user
      // is to request to initialize his wallet when it has been denied by an
      // administrator or to delete his wallet
      WalletState oldInitializationState = WalletState.valueOf(wallet.getInitializationState());
      if (oldInitializationState != WalletState.DENIED
          || initializationState != WalletState.MODIFIED
          || !isWalletOwner(wallet, currentUser)) {
        throw new IllegalAccessException(USER_MESSAGE_PREFIX + currentUser + " attempts to change wallet status with address "
            + address + " to " + initializationState.name());
      }
      if (oldInitializationState == WalletState.INITIALIZED) {
        throw new IllegalAccessException("Wallet was already marked as initialized, thus the status for address " + address
            + " can't change to status " + initializationState.name());
      }
    }
    wallet.setInitializationState(initializationState.name());
    accountStorage.saveWallet(wallet, false);
    if (initializationState == WalletState.DELETED) {
      try {
        getListenerService().broadcast(WALLET_DELETED_EVENT, wallet, currentUser);
      } catch (Exception e) {
        LOG.error("Error while braodcasting wallet {} state modification", wallet, currentUser);
      }
    }
    try {
      getListenerService().broadcast(WALLET_INITIALIZATION_MODIFICATION_EVENT, wallet, currentUser);
    } catch (Exception e) {
      LOG.error("Error while braodcasting wallet {} state modification", wallet, currentUser);
    }
  }

  @Override
  public void setInitializationStatus(String address, WalletState initializationState) {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMTER_IS_MANDATORY);
    }
    if (initializationState == null) {
      throw new IllegalArgumentException("Initialization state is mandatory");
    }
    Wallet wallet = accountStorage.getWalletByAddress(address, getContractAddress());
    if (wallet == null) {
      LOG.info(CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS + address);
      return;
    }
    wallet.setInitializationState(initializationState.name());
    accountStorage.saveWallet(wallet, false);
  }

  @Override
  public boolean isWalletOwner(Wallet wallet, String currentUser) {
    if (wallet == null) {
      return false;
    }
    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    if (type.isSpace()) {
      return isUserSpaceManager(remoteId, currentUser);
    } else if (type.isAdmin()) {
      return isUserRewardingAdmin(currentUser);
    } else if (type.isUser()) {
      return StringUtils.equals(currentUser, remoteId);
    } else {
      LOG.warn("Unrecognized wallet type '{}'", type);
      return false;
    }
  }

  @Override
  public WalletAddressLabel saveOrDeleteAddressLabel(WalletAddressLabel label, String currentUser) {
    if (label == null) {
      throw new IllegalArgumentException("Label is empty");
    }
    long labelId = label.getId();
    if (labelId > 0) {
      Identity identity = getIdentityByTypeAndId(WalletType.USER, currentUser);
      if (identity == null) {
        throw new IllegalStateException("Can't find identity of user " + currentUser);
      }
      WalletAddressLabel storedLabel = labelStorage.getLabel(labelId);
      if (storedLabel == null) {
        label.setId(0);
      } else if (!StringUtils.equals(identity.getId(), String.valueOf(storedLabel.getIdentityId()))) {
        LOG.info("{} user modified address {} label from '{}' to '{}'",
                 currentUser,
                 label.getAddress(),
                 storedLabel.getLabel(),
                 label.getLabel());
      }
    }

    if (StringUtils.isBlank(label.getLabel())) {
      if (labelId > 0) {
        labelStorage.removeLabel(label);
      }
    } else {
      label = labelStorage.saveLabel(label);
    }
    return label;
  }

  @Override
  public Set<WalletAddressLabel> getAddressesLabelsVisibleBy(String currentUser) {
    if (!isUserRewardingAdmin(currentUser)) {
      return Collections.emptySet();
    }
    return labelStorage.getAllLabels();
  }

  @Override
  public String getAdminAccountPassword() {
    return adminAccountPassword;
  }

  @Override
  public Map<String, Object> getStatisticParameters(String operation, Object result, Object... methodArgs) { // NOSONAR
    Map<String, Object> parameters = new HashMap<>();
    if (StringUtils.equals(STATISTIC_OPERATION_INITIALIZATION, operation)) {
      String address = (String) methodArgs[0];
      if (StringUtils.isBlank(address)) {
        LOG.debug("Address parameter is missing. No statistic log will be added");
        return null; // NOSONAR should be null to not collect stats
      }
      Wallet wallet = getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug("Wallet not found for address {}. No statistic log will be added", address);
        return null; // NOSONAR should be null to not collect stats
      } else {
        parameters.put("", wallet);
      }
      WalletState initializationState = (WalletState) methodArgs[1];
      if (initializationState == WalletState.DENIED) {
        parameters.put(OPERATION, "reject");
      } else {
        LOG.debug("No statistic log is handeled for initialization state modification to {}", initializationState);
        return null; // NOSONAR should be null to not collect stats
      }
    } else if (StringUtils.equals(STATISTIC_OPERATION_ENABLE, operation)) {
      if (result == null || !((boolean) result)) {
        return null; // NOSONAR should be null to not collect stats
      }
      String address = (String) methodArgs[0];
      if (StringUtils.isBlank(address)) {
        LOG.debug("Address parameter is missing. No statistic log will be added");
        return null; // NOSONAR should be null to not collect stats
      }
      Wallet wallet = getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug("Wallet not found for address {}. No statistic log will be added", address);
        return null; // NOSONAR should be null to not collect stats
      } else {
        parameters.put("", wallet);
      }
      boolean enable = (boolean) methodArgs[1];
      parameters.put(OPERATION, enable ? STATISTIC_OPERATION_ENABLE : STATISTIC_OPERATION_DISABLE);
    } else if (StringUtils.equals(STATISTIC_OPERATION_CREATE, operation)) {
      Wallet wallet = (Wallet) methodArgs[0];
      if (wallet == null) {
        LOG.debug("Wallet not found in parameters. No statistic log will be added");
        return null; // NOSONAR should be null to not collect stats
      } else {
        parameters.put("", wallet);
      }
    } else {
      LOG.warn("Statistic operation type '{}' not handled", operation);
      return null; // NOSONAR should be null to not collect stats
    }

    String issuer = (String) methodArgs[methodArgs.length - 1];
    if (StringUtils.isNotBlank(issuer)) {
      Identity identity = getIdentityByTypeAndId(WalletType.USER, issuer);
      if (identity == null) {
        LOG.debug("Can't find identity with remote id: {}" + issuer);
      } else {
        parameters.put("user_social_id", identity.getId());
      }
    }
    return parameters;
  }

  @Override
  public boolean isAdminAccountEnabled() {
    return adminAccountEnabled;
  }

  public void setTokenAdminService(WalletTokenAdminService tokenAdminService) {
    this.tokenAdminService = tokenAdminService;
  }

  public void setListenerService(ListenerService listenerService) {
    this.listenerService = listenerService;
  }

  private WalletTokenAdminService getTokenAdminService() {
    if (tokenAdminService == null) {
      tokenAdminService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return tokenAdminService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

  private void checkCanSaveWallet(Wallet wallet, Wallet storedWallet, String currentUser) throws IllegalAccessException {
    // 'rewarding' group members can change all wallets
    if (isUserRewardingAdmin(currentUser)) {
      return;
    }

    checkIsWalletOwner(wallet, currentUser, "save wallet");

    // Check if wallet is enabled
    if (storedWallet != null && !storedWallet.isEnabled()) {
      LOG.error("User '{}' attempts to modify his wallet while it's disabled", currentUser);
      throw new IllegalAccessException();
    }

    Wallet walletByAddress =
                           accountStorage.getWalletByAddress(wallet.getAddress(), getContractAddress());
    if (walletByAddress != null && walletByAddress.getId() != null && !walletByAddress.getId().equals(wallet.getId())) {
      throw new IllegalStateException(USER_MESSAGE_PREFIX + currentUser + " attempts to assign address of wallet of "
          + walletByAddress);
    }
  }

  private void checkIsWalletOwner(Wallet wallet, String currentUser, String operationMessage) throws IllegalAccessException {
    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    if (type.isSpace()) {
      checkUserIsSpaceManager(remoteId, currentUser, true);
    } else if (type.isAdmin()) {
      // Only 'rewarding' group members can access and change 'Admin' wallet
      if (!isUserRewardingAdmin(currentUser)) {
        throw new IllegalAccessException(USER_MESSAGE_IN_EXCEPTION + currentUser + "' attempts to " + operationMessage
            + " of admin");
      }
    } else if (type.isUser()) {
      if (!StringUtils.equals(currentUser, remoteId)) {
        throw new IllegalAccessException(USER_MESSAGE_IN_EXCEPTION + currentUser + "' attempts to " + operationMessage
            + " of user '" + remoteId + "'");
      } else {
        // User is owner
      }
    }
  }

  private Wallet getWalletOfIdentity(Identity identity) {
    long identityId = Long.parseLong(identity.getId());
    Wallet wallet = accountStorage.getWalletByIdentityId(identityId, getContractAddress());
    if (wallet == null) {
      wallet = new Wallet();
      wallet.setEnabled(true);
    }
    computeWalletFromIdentity(wallet, identity);
    return wallet;
  }

  private void setWalletPassPhrase(Wallet wallet, Wallet oldWallet) {
    if (StringUtils.isBlank(wallet.getPassPhrase())) {
      if (oldWallet == null || StringUtils.isBlank(oldWallet.getPassPhrase())) {
        wallet.setPassPhrase(generateSecurityPhrase());
      } else {
        wallet.setPassPhrase(oldWallet.getPassPhrase());
      }
    }
  }

  private String generateSecurityPhrase() {
    return RandomStringUtils.random(20, SIMPLE_CHARS);
  }
  /**
   * @param walletAddress wallet Address (wallet public key)
   * @param rawMessage raw signed message
   * @param signedMessage encrypted message
   * @return true if the message has been decrypted successfully, else false
   * @throws UnsupportedEncodingException when UTF-8 isn't recognized as
   *           Encoding Charset
   * @throws SignatureException when an error occurs while decrypting signed
   *           message
   */
  private boolean validateSignedMessage(String walletAddress,
                                        String rawMessage,
                                        String signedMessage) throws SignatureException {
    if (StringUtils.isBlank(walletAddress) || StringUtils.isBlank(rawMessage) || StringUtils.isBlank(signedMessage)) {
      return false;
    }

    try {
      byte[] signatureBytes = Numeric.hexStringToByteArray(signedMessage);
      byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
      byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
      byte v = signatureBytes[64];
      if (v < 27) {
        v += 27;
      }

      BigInteger publicKey = Sign.signedPrefixedMessageToKey(rawMessage.getBytes(), new SignatureData(v, r, s));
      if (publicKey != null) {
        String recoveredAddress = "0x" + Keys.getAddress(publicKey);
        if (recoveredAddress.equalsIgnoreCase(walletAddress)) {
          return true;
        }
      }
    } catch (Exception e) {
      LOG.warn("Error while validating wallet signature. Error: {}", e.getMessage());
    }
    return false;
  }

  private void computeWalletProperties(Wallet wallet, Wallet oldWallet, WalletProvider provider, boolean isNew) {
    if (isNew) {
      // New wallet created for user/space
      wallet.setInitializationState(WalletState.NEW.name());
    } else if (!StringUtils.equalsIgnoreCase(oldWallet.getAddress(), wallet.getAddress())) {
      wallet.setInitializationState(WalletState.MODIFIED.name());
    }
    wallet.setProvider(provider.name());
    wallet.setEnabled(isNew || oldWallet.isEnabled());
    setWalletPassPhrase(wallet, oldWallet);
  }

  private void setUserWalletAsInitialized(String username) {
    settingService.set(Context.USER.id(username),
                       WALLET_SCOPE,
                       WALLET_INITIALIZED_SETTING_PARAM,
                       SettingValue.create("true"));
  }

  private boolean isUserWalletInitialized(String username) {
    SettingValue<?> value = settingService.get(Context.USER.id(username),
                                               WALLET_SCOPE,
                                               WALLET_INITIALIZED_SETTING_PARAM);
    return value != null && Boolean.parseBoolean(value.getValue().toString());
  }

}
