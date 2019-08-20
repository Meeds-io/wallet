package org.exoplatform.addon.wallet.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.security.Provider;
import java.security.Security;
import java.util.*;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.picocontainer.Startable;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.storage.AddressLabelStorage;
import org.exoplatform.addon.wallet.storage.WalletStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

public class WalletAccountServiceImpl implements WalletAccountService, Startable {

  private static final Log        LOG                                     =
                                      ExoLogger.getLogger(WalletAccountServiceImpl.class);

  private static final String     USER_MESSAGE_IN_EXCEPTION               = "User '";

  private static final String     USER_MESSAGE_PREFIX                     = "User ";

  private static final String     CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS = "Can't find wallet associated to address ";

  private static final String     ADDRESS_PARAMTER_IS_MANDATORY           = "address paramter is mandatory";

  private WalletTokenAdminService tokenAdminService;

  private WalletContractService   contractService;

  private WalletStorage           accountStorage;

  private AddressLabelStorage     labelStorage;

  private ListenerService         listenerService;

  private String                  adminAccountPassword;

  public WalletAccountServiceImpl(WalletContractService contractService,
                                  WalletStorage walletAccountStorage,
                                  AddressLabelStorage labelStorage,
                                  InitParams params) {
    this.contractService = contractService;
    this.accountStorage = walletAccountStorage;
    this.labelStorage = labelStorage;
    if (params != null && params.containsKey(ADMIN_KEY_PARAMETER)
        && StringUtils.isNotBlank(params.getValueParam(ADMIN_KEY_PARAMETER).getValue())) {
      this.adminAccountPassword = params.getValueParam(ADMIN_KEY_PARAMETER).getValue();
    }
  }

  @Override
  public void start() {
    Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
    if (provider == null) {
      LOG.info("No BouncyCastleProvider defined, register new one");
      provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
      Security.addProvider(provider);
    }
    LOG.info("Start wallet with BouncyCastleProvider version: {}", provider.getVersion());
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public Set<Wallet> listWallets() {
    Set<Wallet> wallets = accountStorage.listWallets();
    wallets.forEach(wallet -> hideWalletOwnerPrivateInformation(wallet));
    return wallets;
  }

  @Override
  public void refreshWalletsFromBlockchain(Map<String, Set<String>> walletsModifications) {
    if (walletsModifications == null) {
      walletsModifications = new HashMap<>();
    }

    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      LOG.warn("Contract address is empty, thus wallets can't be refreshed");
      return;
    }
    ContractDetail contractDetail = contractService.getContractDetail(contractAddress);
    if (contractDetail == null) {
      LOG.info("Contract detail with address {} is not found in database, thus wallets can't be refreshed", contractAddress);
      return;
    }

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
      String contractAddress = getContractAddress();
      if (StringUtils.isBlank(contractAddress)) {
        LOG.warn("Contract address is empty, thus wallets can't be refreshed");
        return;
      }
      contractDetail = contractService.getContractDetail(contractAddress);
      if (contractDetail == null) {
        LOG.info("Contract detail with address {} is not found in database, thus wallets can't be refreshed", contractAddress);
        return;
      }
    }

    try {
      getTokenAdminService().refreshWallet(wallet,
                                           contractDetail,
                                           walletsModifications == null ? null : walletsModifications.get(wallet.getAddress()));
      saveWalletBlockchainState(wallet, contractDetail.getAddress());
      getListenerService().broadcast(WALLET_MODIFIED_EVENT, null, wallet);
    } catch (Exception e) {
      LOG.error("Error refreshing wallet state on blockchain", e);
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
    return accountStorage.getWalletPrivateKey(wallet.getTechnicalId());
  }

  @Override
  public String getPrivateKeyByTypeAndId(String type, String remoteId) {
    Wallet wallet = getWalletByTypeAndId(type, remoteId);
    if (wallet == null || wallet.getTechnicalId() < 1) {
      return null;
    }
    return accountStorage.getWalletPrivateKey(wallet.getTechnicalId());
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
    Wallet wallet = accountStorage.getWalletByAddress(address);
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
  public void saveWallet(Wallet wallet) {
    accountStorage.saveWallet(wallet, false);
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

    Wallet wallet = accountStorage.getWalletByIdentityId(identityId);
    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet with id " + identityId);
    }
    checkIsWalletOwner(wallet, currentUser, "save wallet");

    return accountStorage.saveWalletBackupState(identityId, backupState);
  }

  @Override
  public void saveWalletAddress(Wallet wallet, String currentUser, boolean broadcast) throws IllegalAccessException {
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet is mandatory");
    }

    if (StringUtils.isBlank(wallet.getAddress())) {
      throw new IllegalArgumentException("Wallet address is empty, thus it can't be saved");
    }

    computeWalletIdentity(wallet);

    Wallet oldWallet = accountStorage.getWalletByIdentityId(wallet.getTechnicalId());
    boolean isNew = oldWallet == null;

    checkCanSaveWallet(wallet, oldWallet, currentUser);
    if (isNew) {
      // New wallet created for user/space
      wallet.setInitializationState(WalletInitializationState.NEW.name());
    } else if (!StringUtils.equalsIgnoreCase(oldWallet.getAddress(), wallet.getAddress())) {
      // User changing associated address to him or to a space he manages
      wallet.setInitializationState(WalletInitializationState.MODIFIED.name());
    } else {
      // No initialization state change
      wallet.setInitializationState(oldWallet.getInitializationState());
    }
    wallet.setEnabled(isNew || oldWallet.isEnabled());
    setWalletPassPhrase(wallet, oldWallet);

    accountStorage.saveWallet(wallet, isNew);
    if (!isNew) {
      accountStorage.removeWalletPrivateKey(wallet.getTechnicalId());
    }

    String contractAddress = getContractAddress();
    if (StringUtils.isNotBlank(contractAddress)) {
      ContractDetail contractDetail = contractService.getContractDetail(contractAddress);
      if (contractDetail == null) {
        LOG.warn("Contract detail with address {} wasn't found, thus can't compute blockchain attributes of wallet {} {}",
                 contractAddress,
                 wallet.getType(),
                 wallet.getId());
      } else {
        refreshWalletFromBlockchain(wallet, contractDetail, null);
      }
    } else {
      LOG.warn("Contract address is empty, thus admin wallet can't be refreshed");
    }

    if (broadcast) {
      String eventName = isNew ? NEW_ADDRESS_ASSOCIATED_EVENT : MODIFY_ADDRESS_ASSOCIATED_EVENT;
      wallet = wallet.clone();
      try {
        getListenerService().broadcast(eventName, wallet, currentUser);
      } catch (Exception e) {
        LOG.error("Error broadcasting event {} for wallet {}", eventName, wallet, e);
      }
    }
  }

  @Override
  public void removeWalletByAddress(String address, String currentUser) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMTER_IS_MANDATORY);
    }
    Wallet wallet = accountStorage.getWalletByAddress(address);
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
    Wallet wallet = accountStorage.getWalletByIdentityId(identityId);

    if (wallet == null) {
      throw new IllegalStateException("Can't find wallet with type/id: " + type + "/" + remoteId);
    }
    accountStorage.removeWallet(wallet.getTechnicalId());
  }

  @Override
  public void enableWalletByAddress(String address, boolean enable, String currentUser) throws IllegalAccessException {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMTER_IS_MANDATORY);
    }
    Wallet wallet = accountStorage.getWalletByAddress(address);
    if (wallet == null) {
      throw new IllegalStateException(CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS + address);
    }

    boolean walletEnablement = wallet.isEnabled();
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
  }

  @Override
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
    Wallet wallet = accountStorage.getWalletByAddress(address);
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
    Wallet wallet = accountStorage.getWalletByAddress(address);
    if (wallet == null) {
      LOG.info(CAN_T_FIND_WALLET_ASSOCIATED_TO_ADDRESS + address);
      return;
    }
    wallet.setInitializationState(initializationState.name());
    accountStorage.saveWallet(wallet, false);
  }

  @Override
  public void checkCanSaveWallet(Wallet wallet, Wallet storedWallet, String currentUser) throws IllegalAccessException {
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
                           accountStorage.getWalletByAddress(wallet.getAddress());
    if (walletByAddress != null && walletByAddress.getId() != null && !walletByAddress.getId().equals(wallet.getId())) {
      throw new IllegalStateException(USER_MESSAGE_PREFIX + currentUser + " attempts to assign address of wallet of "
          + walletByAddress);
    }
  }

  @Override
  public boolean isWalletOwner(Wallet wallet, String currentUser) {
    if (wallet == null) {
      return false;
    }
    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    if (type.isSpace()) {
      try {
        return checkUserIsSpaceManager(remoteId, currentUser, false);
      } catch (IllegalAccessException e) {
        return false;
      }
    } else {
      return StringUtils.equals(currentUser, remoteId);
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
    Wallet wallet = accountStorage.getWalletByIdentityId(identityId);
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
