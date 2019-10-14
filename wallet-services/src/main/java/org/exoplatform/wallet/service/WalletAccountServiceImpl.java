package org.exoplatform.wallet.service;

import static org.exoplatform.wallet.statistic.StatisticUtils.OPERATION;
import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.util.*;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTokenAdminService;
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

  private ExoContainer            container;

  private WalletTokenAdminService tokenAdminService;

  private WalletStorage           accountStorage;

  private AddressLabelStorage     labelStorage;

  private ListenerService         listenerService;

  private String                  adminAccountPassword;

  private boolean                 adminAccountEnabled;

  public WalletAccountServiceImpl(ExoContainer container,
                                  WalletStorage walletAccountStorage,
                                  AddressLabelStorage labelStorage,
                                  InitParams params) {
    this.container = container;
    this.accountStorage = walletAccountStorage;
    this.labelStorage = labelStorage;
    if (params != null && params.containsKey(ADMIN_KEY_PARAMETER)
        && StringUtils.isNotBlank(params.getValueParam(ADMIN_KEY_PARAMETER).getValue())) {
      this.adminAccountPassword = params.getValueParam(ADMIN_KEY_PARAMETER).getValue();
    }
  }

  @Override
  public void start() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      Wallet adminWallet = getAdminWallet();
      retrieveWalletBlockchainState(adminWallet);

      this.adminAccountEnabled = adminWallet != null && adminWallet.isEnabled() && adminWallet.getAdminLevel() != null
          && adminWallet.getAdminLevel() >= 2;
    } catch (Exception e) {
      LOG.error("Error starting service", e);
    } finally {
      RequestLifeCycle.end();
    }
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

      // Checks if admin wallet was newly enabled from blockchain
      if (!this.adminAccountEnabled && WalletType.isAdmin(wallet.getType())) {
        this.adminAccountEnabled = wallet.isEnabled() && wallet.getAdminLevel() != null && wallet.getAdminLevel() >= 2;
      }
    }
  }

  @Override
  public void refreshWalletFromBlockchain(Wallet wallet,
                                          ContractDetail contractDetail,
                                          Map<String, Set<String>> walletsModifications) {
    if (wallet == null) {
      return;
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      LOG.debug("No wallet address: {}", wallet);
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
        getTokenAdminService().retrieveWalletInformationFromBlockchain(wallet,
                                                                       contractDetail,
                                                                       walletModifications);
        saveWalletBlockchainState(wallet, contractDetail.getAddress());

        if (!StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletInitializationState.INITIALIZED.name()) &&
            !StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletInitializationState.PENDING.name()) &&
            !StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletInitializationState.DENIED.name())
            && wallet.getIsInitialized() != null && wallet.getIsInitialized() && wallet.getIsApproved() != null
            && wallet.getIsApproved()) {
          wallet.setInitializationState(WalletInitializationState.INITIALIZED.name());
          setInitializationStatus(wallet.getAddress(), WalletInitializationState.INITIALIZED);
        }

        getListenerService().broadcast(WALLET_MODIFIED_EVENT, null, wallet);
      } catch (Exception e) {
        LOG.error("Error refreshing wallet state on blockchain", e);
      }
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
    String contractAddress = getContractAddress();
    if (wallet == null) {
      return;
    }
    if (StringUtils.isBlank(contractAddress)) {
      LOG.warn("Contract address is empty, thus wallets can't be refreshed");
      return;
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      LOG.debug("No wallet address: {}", wallet);
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
      if (canAccessWallet(wallet, currentUser)) {
        retrieveWalletBlockchainState(wallet);
      }
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

    Wallet oldWallet = accountStorage.getWalletByIdentityId(wallet.getTechnicalId(), getContractAddress());
    boolean isNew = oldWallet == null;

    checkCanSaveWallet(wallet, oldWallet, currentUser);
    if (isNew) {
      // New wallet created for user/space
      wallet.setInitializationState(WalletInitializationState.NEW.name());
    } else if (!StringUtils.equalsIgnoreCase(oldWallet.getAddress(), wallet.getAddress())) {
      wallet.setInitializationState(WalletInitializationState.MODIFIED.name());
    } else {
      throw new IllegalAccessException("Can't modify wallet properties once saved");
    }
    wallet.setEnabled(isNew || oldWallet.isEnabled());
    setWalletPassPhrase(wallet, oldWallet);
    accountStorage.saveWallet(wallet, isNew);

    if (!isNew) {
      // Automatically Remove old private key when modifying address associated
      // to wallet
      accountStorage.removeWalletPrivateKey(wallet.getTechnicalId());
    }

    // This is about address modification or creation, thus, the blockchain must
    // be retrieved again
    refreshWalletFromBlockchain(wallet, getContractDetail(), null);

    String eventName = isNew ? NEW_ADDRESS_ASSOCIATED_EVENT : MODIFY_ADDRESS_ASSOCIATED_EVENT;
    try {
      getListenerService().broadcast(eventName, wallet.clone(), currentUser);
    } catch (Exception e) {
      LOG.error("Error broadcasting event {} for wallet {}", eventName, wallet, e);
    }
  }

  @Override
  public Wallet saveWallet(Wallet wallet, boolean isNew) {
    return accountStorage.saveWallet(wallet, isNew);
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
                                      WalletInitializationState initializationState,
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

    if (!isUserRewardingAdmin(currentUser)) {
      // The only authorized initialization transition allowed to a regular user
      // is to request to initialize his wallet when it has been denied by an
      // administrator
      WalletInitializationState oldInitializationState = WalletInitializationState.valueOf(wallet.getInitializationState());
      if (oldInitializationState != WalletInitializationState.DENIED
          || initializationState != WalletInitializationState.MODIFIED
          || !isWalletOwner(wallet, currentUser)) {
        throw new IllegalAccessException(USER_MESSAGE_PREFIX + currentUser + " attempts to change wallet status with address "
            + address + " to " + initializationState.name());
      }
      if (oldInitializationState == WalletInitializationState.INITIALIZED) {
        throw new IllegalAccessException("Wallet was already marked as initialized, thus the status for address " + address
            + " can't change to status " + initializationState.name());
      }
    }
    wallet.setInitializationState(initializationState.name());
    accountStorage.saveWallet(wallet, false);

    try {
      getListenerService().broadcast(WALLET_INITIALIZATION_MODIFICATION_EVENT, wallet, currentUser);
    } catch (Exception e) {
      LOG.error("Error while braodcasting wallet {} state modification", wallet, currentUser);
    }
  }

  @Override
  public void setInitializationStatus(String address, WalletInitializationState initializationState) {
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
  public Map<String, Object> getStatisticParameters(String operation, Object result, Object... methodArgs) {
    Map<String, Object> parameters = new HashMap<>();
    if (StringUtils.equals(STATISTIC_OPERATION_INITIALIZATION, operation)) {
      String address = (String) methodArgs[0];
      if (StringUtils.isBlank(address)) {
        LOG.debug("Address parameter is missing. No statistic log will be added");
        return null;
      }
      Wallet wallet = getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug("Wallet not found for address {}. No statistic log will be added", address);
        return null;
      } else {
        parameters.put("", wallet);
      }
      WalletInitializationState initializationState = (WalletInitializationState) methodArgs[1];
      if (initializationState == WalletInitializationState.DENIED) {
        parameters.put(OPERATION, "reject");
      } else {
        LOG.debug("No statistic log is handeled for initialization state modification to {}", initializationState);
        return null;
      }
    } else if (StringUtils.equals(STATISTIC_OPERATION_ENABLE, operation)) {
      if (result == null || !((boolean) result)) {
        return null;
      }
      String address = (String) methodArgs[0];
      if (StringUtils.isBlank(address)) {
        LOG.debug("Address parameter is missing. No statistic log will be added");
        return null;
      }
      Wallet wallet = getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug("Wallet not found for address {}. No statistic log will be added", address);
        return null;
      } else {
        parameters.put("", wallet);
      }
      boolean enable = (boolean) methodArgs[1];
      parameters.put(OPERATION, enable ? STATISTIC_OPERATION_ENABLE : STATISTIC_OPERATION_DISABLE);
    } else if (StringUtils.equals(STATISTIC_OPERATION_CREATE, operation)) {
      Wallet wallet = (Wallet) methodArgs[0];
      if (wallet == null) {
        LOG.debug("Wallet not found in parameters. No statistic log will be added");
        return null;
      } else {
        parameters.put("", wallet);
      }
    } else {
      LOG.warn("Statistic operation type '{}' not handled", operation);
      return null;
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

}
