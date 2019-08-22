package org.exoplatform.addon.wallet.blockchain.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.*;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.*;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.NoOpProcessor;
import org.web3j.utils.Numeric;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.addon.wallet.blockchain.ExoBlockchainTransaction;
import org.exoplatform.addon.wallet.blockchain.ExoBlockchainTransactionService;
import org.exoplatform.addon.wallet.blockchain.statistic.ExoWalletStatistic;
import org.exoplatform.addon.wallet.blockchain.statistic.ExoWalletStatisticService;
import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.addon.wallet.storage.WalletStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

public class EthereumWalletTokenAdminService
    implements WalletTokenAdminService, Startable, ExoBlockchainTransactionService, ExoWalletStatisticService {
  private static final Log         LOG                                     =
                                       ExoLogger.getLogger(EthereumWalletTokenAdminService.class);

  private static final int         ADMIN_WALLET_MIN_LEVEL                  = 2;

  private static final long        DEFAULT_ADMIN_GAS                       = 300000l;

  private static final String      NO_CONFIGURED_CONTRACT_ADDRESS          = "No configured contract address";

  private static final String      TRANSACTION_DETAIL_IS_MANDATORY         = "Transaction detail is mandatory";

  private static final String      TRANSACTION_HASH_IS_EMPTY               =
                                                             "Transaction hash is empty for transaction: ";

  private static final String      RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY =
                                                                           "receiver address parameter is mandatory";

  private ClassLoader              webappClassLoader;

  private UserACL                  userACL;

  private WalletContractService    walletContractService;

  private EthereumClientConnector  clientConnector;

  private WalletAccountService     accountService;

  private WalletStorage            accountStorage;

  private WalletTransactionService transactionService;

  private ERTTokenV2               ertInstance;

  private TransactionManager       contractTransactionManager;

  private boolean                  isReadOnlyContract;

  private String                   configuredContractAddress;

  private Integer                  configuredContractDecimals;

  public EthereumWalletTokenAdminService(EthereumClientConnector clientConnector, ClassLoader classLoader) {
    this.webappClassLoader = classLoader;
    this.clientConnector = clientConnector;
  }

  @Override
  public ClassLoader getWebappClassLoader() {
    return webappClassLoader;
  }

  @Override
  @ExoBlockchainTransaction
  public void start() {
    // Create admin wallet if not exists
    Wallet wallet = getAdminWallet();
    if (wallet == null || StringUtils.isBlank(wallet.getAddress())) {
      createAdminAccount();
      LOG.info("Admin wallet created");
    }

    try {
      configuredContractAddress = getSettings().getContractAddress();
      getContractService().refreshContractDetail(new HashSet<>());
      configuredContractDecimals = getPrincipalContractDetail().getDecimals();
    } catch (Exception e) {
      LOG.warn("Error refreshing contract detail from blockchain with address {}", configuredContractAddress, e);
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  @ExoBlockchainTransaction
  public void createAdminAccount() {
    try {
      this.createAdminAccount(null, getUserACL().getSuperUser());
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("This exception shouldn't be thrown because no ACL check is made on server side method call",
                                      e);
    }
  }

  @Override
  @ExoBlockchainTransaction
  public Wallet createAdminAccount(String privateKey, String currentUser) throws IllegalAccessException {
    if (!isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("User " + currentUser + " is not allowed to create admin wallet");
    }

    Identity identity = getIdentityByTypeAndId(WalletType.ADMIN, WALLET_ADMIN_REMOTE_ID);
    if (identity == null) {
      throw new IllegalStateException("Can't find identity of admin wallet");
    }

    long identityId = Long.parseLong(identity.getId());
    Wallet wallet = getAccountService().getWalletByIdentityId(identityId);
    if (wallet != null && wallet.getAddress() != null
        && getAccountService().getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID) != null) {
      throw new IllegalStateException("Admin wallet has already an associated wallet, thus can't overwrite it");
    }

    ECKeyPair ecKeyPair = null;
    if (StringUtils.isBlank(privateKey)) {
      try {
        ecKeyPair = Keys.createEcKeyPair();
      } catch (Exception e) {
        throw new IllegalStateException("Error creating new wallet keys pair", e);
      }
    } else {
      if (!WalletUtils.isValidPrivateKey(privateKey)) {
        throw new IllegalStateException("Private key isn't valid");
      }
      ecKeyPair = Credentials.create(privateKey).getEcKeyPair();
    }

    WalletFile adminWallet = null;
    try {
      adminWallet = org.web3j.crypto.Wallet.createLight(accountService.getAdminAccountPassword(), ecKeyPair);
    } catch (CipherException e) {
      throw new IllegalStateException("Error creating new wallet", e);
    }

    wallet = new Wallet();
    wallet.setEnabled(true);
    wallet.setId(WALLET_ADMIN_REMOTE_ID);
    wallet.setType(WalletType.ADMIN.getId());
    wallet.setAddress("0x" + adminWallet.getAddress());
    wallet.setTechnicalId(identityId);

    getAccountService().saveWalletAddress(wallet, currentUser, false);
    try {
      String walletJson = toJsonString(adminWallet);
      getAccountStorage().saveWalletPrivateKey(identityId, walletJson);
      this.isReadOnlyContract = false;
    } catch (Exception e) {
      // Make sure to delete corresponding wallet when the private key isn't
      // saved
      getAccountService().removeWalletByAddress(wallet.getAddress(), currentUser);
    }

    return wallet;
  }

  @Override
  public Wallet getAdminWallet() {
    return getAccountService().getWalletByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID);
  }

  @Override
  public String getAdminWalletAddress() {
    Wallet adminWallet = getAdminWallet();
    return adminWallet == null ? null : adminWallet.getAddress();
  }

  @Override
  @ExoBlockchainTransaction
  public final boolean isApprovedAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    return (Boolean) executeReadOperation(contractAddress,
                                          ERTTokenV2.FUNC_ISAPPROVEDACCOUNT,
                                          address);
  }

  @Override
  @ExoBlockchainTransaction
  public final int getAdminLevel(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    BigInteger adminLevel = (BigInteger) executeReadOperation(contractAddress,
                                                              ERTTokenV2.FUNC_GETADMINLEVEL,
                                                              address);
    return adminLevel == null ? 0 : adminLevel.intValue();
  }

  @Override
  @ExoBlockchainTransaction
  public final boolean isInitializedAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    return (Boolean) executeReadOperation(contractAddress,
                                          ERTTokenV2.FUNC_ISINITIALIZEDACCOUNT,
                                          address);
  }

  @Override
  @ExoBlockchainTransaction
  public final BigInteger balanceOf(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    return (BigInteger) executeReadOperation(contractAddress,
                                             ERTTokenV2.FUNC_BALANCEOF,
                                             address);
  }

  @Override
  @ExoBlockchainTransaction
  @ExoWalletStatistic(statisticType = OPERATION_GET_ETHER_BALANCE)
  public final BigInteger getEtherBalanceOf(String address) throws Exception { // NOSONAR
    Web3j web3j = getClientConnector().getWeb3j();
    if (web3j == null) {
      throw new IllegalStateException("Can't get ether balance of " + address + " . Connection is not established.");
    }
    return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
  }

  @Override
  @ExoBlockchainTransaction
  public final TransactionDetail initialize(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiver = transactionDetail.getTo();
    if (StringUtils.isBlank(receiver)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (isInitializedAccount(receiver)) {
      throw new IllegalStateException("Wallet {} is already initialized");
    }
    String adminWalletAddress = getAdminWalletAddress();
    BigInteger tokenAmount = transactionDetail.getContractAmountDecimal(configuredContractDecimals);
    BigInteger etherAmount = transactionDetail.getValueDecimal(ETHER_TO_WEI_DECIMALS);

    BigInteger balanceOfAdmin = balanceOf(adminWalletAddress);
    if (balanceOfAdmin == null || balanceOfAdmin.compareTo(tokenAmount) < 0) {
      throw new IllegalStateException("Wallet admin hasn't enough tokens to initialize " + tokenAmount.longValue()
          + " tokens to "
          + receiver);
    }

    if (getEtherBalanceOf(adminWalletAddress).compareTo(etherAmount) < 0) {
      throw new IllegalStateException("Wallet admin hasn't enough ether to initialize " + etherAmount.longValue() + " WEI to "
          + receiver);
    }

    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    String transactionHash = executeTokenTransaction(configuredContractAddress,
                                                     ERTTokenV2.FUNC_INITIALIZEACCOUNT,
                                                     receiver,
                                                     tokenAmount,
                                                     etherAmount);

    if (StringUtils.isBlank(transactionHash)) {
      throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
    }

    getAccountService().setInitializationStatus(receiver, WalletInitializationState.PENDING);

    transactionDetail.setNetworkId(getNetworkId());
    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(adminWalletAddress);
    transactionDetail.setContractAddress(configuredContractAddress);
    transactionDetail.setContractMethodName(ERTTokenV2.FUNC_INITIALIZEACCOUNT);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    transactionDetail.setGasPrice(getAdminGasPrice(getSettings()));

    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  @ExoBlockchainTransaction
  public TransactionDetail sendEther(TransactionDetail transactionDetail, String currentUserId) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiverAddress = transactionDetail.getTo();
    if (StringUtils.isBlank(receiverAddress)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (transactionDetail.getValue() < 0) {
      throw new IllegalArgumentException("ether amount parameter has to be a positive amount");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, currentUserId);

    String adminWalletAddress = getAdminWalletAddress();
    BigInteger etherAmount = transactionDetail.getValueDecimal(ETHER_TO_WEI_DECIMALS);

    BigInteger adminEtherBalance = getEtherBalanceOf(adminWalletAddress);
    if (adminEtherBalance.compareTo(etherAmount) < 0) {
      throw new IllegalStateException("Wallet admin hasn't enough ether to initialize " + etherAmount.longValue() + " WEI to "
          + receiverAddress);
    }

    String transactionHash = executeSendEtherTransaction(transactionDetail.getTo(), transactionDetail.getValue());

    if (StringUtils.isBlank(transactionHash)) {
      throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
    }

    transactionDetail.setNetworkId(getNetworkId());
    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(adminWalletAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    transactionDetail.setGasPrice(getAdminGasPrice(getSettings()));

    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  @ExoBlockchainTransaction
  public final TransactionDetail sendToken(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiverAddress = transactionDetail.getTo();
    if (StringUtils.isBlank(receiverAddress)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (transactionDetail.getContractAmount() <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be positive");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (!isApprovedAccount(receiverAddress)) {
      String receiver = transactionDetail.getToWallet() == null
          || StringUtils.isBlank(transactionDetail.getToWallet().getName()) ? receiverAddress
                                                                            : transactionDetail.getToWallet().getName();
      throw new IllegalStateException("Wallet receiver " + receiver + " is not approved yet, thus no transfer is allowed");
    }

    BigInteger tokenAmount = transactionDetail.getContractAmountDecimal(configuredContractDecimals);

    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    String transactionHash = executeTokenTransaction(configuredContractAddress,
                                                     ERTTokenV2.FUNC_TRANSFER,
                                                     receiverAddress,
                                                     tokenAmount);

    if (StringUtils.isBlank(transactionHash)) {
      throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
    }
    TransactionDetail persistedTransaction = getTransactionService().getTransactionByHash(transactionHash);
    if (persistedTransaction != null) {
      LOG.info("Transaction with hash {} already exists in database, it will be replaced with new data", transactionHash);
      transactionDetail.setId(persistedTransaction.getId());
    }
    transactionDetail.setNetworkId(getNetworkId());
    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(getAdminWalletAddress());
    transactionDetail.setContractAddress(configuredContractAddress);
    transactionDetail.setContractMethodName(ERTTokenV2.FUNC_TRANSFER);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    transactionDetail.setGasPrice(getAdminGasPrice(getSettings()));

    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  @ExoBlockchainTransaction
  public final TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiverAddress = transactionDetail.getTo();
    if (StringUtils.isBlank(receiverAddress)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (transactionDetail.getContractAmount() < 0) {
      throw new IllegalArgumentException("reward amount parameter has to be a positive");
    }
    if (transactionDetail.getValue() <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be a positive");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (!isApprovedAccount(receiverAddress)) {
      String receiver = transactionDetail.getToWallet() == null
          || StringUtils.isBlank(transactionDetail.getToWallet().getName()) ? receiverAddress
                                                                            : transactionDetail.getToWallet().getName();
      throw new IllegalStateException("Wallet receiver " + receiver + " is not approved yet, thus no transfer is allowed");
    }

    BigInteger tokenAmount = transactionDetail.getValueDecimal(configuredContractDecimals);
    BigInteger rewardAmount = transactionDetail.getContractAmountDecimal(configuredContractDecimals);

    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    String transactionHash = executeTokenTransaction(configuredContractAddress,
                                                     ERTTokenV2.FUNC_REWARD,
                                                     receiverAddress,
                                                     tokenAmount,
                                                     rewardAmount);

    if (StringUtils.isBlank(transactionHash)) {
      throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
    }
    transactionDetail.setNetworkId(getNetworkId());
    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(getAdminWalletAddress());
    transactionDetail.setContractAddress(configuredContractAddress);
    transactionDetail.setContractMethodName(ERTTokenV2.FUNC_REWARD);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    transactionDetail.setGasPrice(getAdminGasPrice(getSettings()));

    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  @ExoBlockchainTransaction
  public void refreshWallet(Wallet wallet, ContractDetail contractDetail, Set<String> walletModifications) throws Exception {
    if (wallet == null) {
      throw new IllegalArgumentException("wallet is mandatory");
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      LOG.debug("No wallet address: {}", wallet);
      return;
    }
    if (contractDetail == null || StringUtils.isBlank(contractDetail.getAddress()) || contractDetail.getDecimals() == null) {
      throw new IllegalArgumentException("contractDetail is mandatory");
    }
    // Force refresh wallet state from database stored data
    getAccountService().retrieveWalletBlockchainState(wallet);

    String walletAddress = wallet.getAddress();

    // Always refresh ether balance of wallet
    BigInteger walletEtherBalance = getEtherBalanceOf(walletAddress);
    wallet.setEtherBalance(convertFromDecimals(walletEtherBalance, ETHER_TO_WEI_DECIMALS));

    if (wallet.getTokenBalance() == null
        || walletModifications == null
        || walletModifications.contains(ERTTokenV2.FUNC_REWARD)
        || walletModifications.contains(ERTTokenV2.FUNC_INITIALIZE)
        || walletModifications.contains(ERTTokenV2.FUNC_TRANSFER)
        || walletModifications.contains(ERTTokenV2.FUNC_TRANSFERFROM)
        || walletModifications.contains(ERTTokenV2.FUNC_APPROVE)) {
      BigInteger walletTokenBalance =
                                    (BigInteger) executeReadOperation(configuredContractAddress,
                                                                      ERTTokenV2.FUNC_BALANCEOF,
                                                                      walletAddress);
      wallet.setTokenBalance(convertFromDecimals(walletTokenBalance, configuredContractDecimals));
    }
    if (wallet.getRewardBalance() == null
        || walletModifications == null
        || walletModifications.contains(ERTTokenV2.FUNC_REWARD)) {
      BigInteger walletRewardBalance = (BigInteger) executeReadOperation(configuredContractAddress,
                                                                         ERTTokenV2.FUNC_REWARDBALANCEOF,
                                                                         walletAddress);
      wallet.setRewardBalance(convertFromDecimals(walletRewardBalance, configuredContractDecimals));
    }
    if (wallet.getVestingBalance() == null
        || walletModifications == null
        || walletModifications.contains(ERTTokenV2.FUNC_REWARD)) {
      BigInteger walletVestingBalance = (BigInteger) executeReadOperation(configuredContractAddress,
                                                                          ERTTokenV2.FUNC_VESTINGBALANCEOF,
                                                                          walletAddress);
      wallet.setVestingBalance(convertFromDecimals(walletVestingBalance, configuredContractDecimals));
    }
    if (wallet.getAdminLevel() == null
        || walletModifications == null
        || walletModifications.contains(ERTTokenV2.FUNC_TRANSFEROWNERSHIP)
        || walletModifications.contains(ERTTokenV2.FUNC_REMOVEADMIN)
        || walletModifications.contains(ERTTokenV2.FUNC_ADDADMIN)) {
      BigInteger walletAdminLevel = (BigInteger) executeReadOperation(configuredContractAddress,
                                                                      ERTTokenV2.FUNC_GETADMINLEVEL,
                                                                      walletAddress);
      wallet.setAdminLevel(walletAdminLevel.intValue());
    }
    if (wallet.getIsApproved() == null
        || walletModifications == null
        || walletModifications.contains(ERTTokenV2.FUNC_INITIALIZE)
        || walletModifications.contains(ERTTokenV2.FUNC_ADDADMIN)
        || walletModifications.contains(ERTTokenV2.FUNC_REMOVEADMIN)
        || walletModifications.contains(ERTTokenV2.FUNC_APPROVEACCOUNT)
        || walletModifications.contains(ERTTokenV2.FUNC_DISAPPROVEACCOUNT)
        || walletModifications.contains(ERTTokenV2.FUNC_TRANSFEROWNERSHIP)) {
      Boolean approved = (Boolean) executeReadOperation(configuredContractAddress,
                                                        ERTTokenV2.FUNC_ISAPPROVEDACCOUNT,
                                                        walletAddress);
      wallet.setIsApproved(approved);
    }
    if (wallet.getIsInitialized() == null
        || walletModifications == null
        || walletModifications.contains(ERTTokenV2.FUNC_INITIALIZEACCOUNT)) {
      Boolean initialized = (Boolean) executeReadOperation(configuredContractAddress,
                                                           ERTTokenV2.FUNC_ISINITIALIZEDACCOUNT,
                                                           walletAddress);
      wallet.setIsInitialized(initialized);
    }
  }

  @Override
  @ExoBlockchainTransaction
  public void refreshContractDetailFromBlockchain(ContractDetail contractDetail, Set<String> contractModifications) {
    if (contractDetail == null) {
      throw new IllegalArgumentException("contractDetail is mandatory");
    }
    String contractAddress = contractDetail.getAddress();
    try {
      if (contractDetail.getNetworkId() == null || contractDetail.getNetworkId() <= 0) {
        contractDetail.setNetworkId(getNetworkId());
      }
      if (StringUtils.isEmpty(contractDetail.getContractType())
          || StringUtils.equals(contractDetail.getContractType(), "0")
          || contractModifications == null
          || contractModifications.contains(ERTTokenV2.FUNC_UPGRADEIMPLEMENTATION)
          || contractModifications.contains(ERTTokenV2.FUNC_UPGRADEDATA)
          || contractModifications.contains(ERTTokenV2.FUNC_UPGRADEDATAANDIMPLEMENTATION)) {
        BigInteger implementationVersion = (BigInteger) executeReadOperation(contractAddress, ERTTokenV2.FUNC_VERSION);
        if (implementationVersion == null || implementationVersion.intValue() < 1) {
          return;
        }
        contractDetail.setContractType(implementationVersion.toString());
      }

      if (contractDetail.getDecimals() == null || contractDetail.getDecimals() <= 0) {
        BigInteger decimals = (BigInteger) executeReadOperation(contractAddress, ERTTokenV2.FUNC_DECIMALS);
        contractDetail.setDecimals(decimals.intValue());
      }

      if (StringUtils.isEmpty(contractDetail.getName())
          || contractModifications == null
          || contractModifications.contains(ERTTokenV2.FUNC_SETNAME)) {
        String name = (String) executeReadOperation(contractAddress, ERTTokenV2.FUNC_NAME);
        contractDetail.setName(name);
      }

      if (StringUtils.isEmpty(contractDetail.getSymbol())
          || contractModifications == null
          || contractModifications.contains(ERTTokenV2.FUNC_SETSYMBOL)) {
        String symbol = (String) executeReadOperation(contractAddress, ERTTokenV2.FUNC_SYMBOL);
        contractDetail.setSymbol(symbol);
      }

      if (StringUtils.isEmpty(contractDetail.getOwner())
          || contractModifications == null
          || contractModifications.contains(ERTTokenV2.FUNC_TRANSFEROWNERSHIP)) {
        String owner = (String) executeReadOperation(contractAddress, ERTTokenV2.FUNC_OWNER);
        contractDetail.setOwner(owner);
      }

      if (StringUtils.isEmpty(contractDetail.getSellPrice())
          || contractModifications == null
          || contractModifications.contains(ERTTokenV2.FUNC_SETSELLPRICE)) {
        BigInteger sellPrice = (BigInteger) executeReadOperation(contractAddress, ERTTokenV2.FUNC_GETSELLPRICE);
        contractDetail.setSellPrice(String.valueOf(convertFromDecimals(sellPrice, ETHER_TO_WEI_DECIMALS)));
      }

      if (StringUtils.isEmpty(contractDetail.getTotalSupply())) {
        BigInteger totalSupply = (BigInteger) executeReadOperation(contractAddress, ERTTokenV2.FUNC_TOTALSUPPLY);
        contractDetail.setTotalSupply(String.valueOf(convertFromDecimals(totalSupply, contractDetail.getDecimals())));
      }

      if (contractDetail.getIsPaused() == null
          || contractModifications == null
          || contractModifications.contains(ERTTokenV2.FUNC_PAUSE)
          || contractModifications.contains(ERTTokenV2.FUNC_UNPAUSE)) {
        Boolean isPaused = (Boolean) executeReadOperation(contractAddress, ERTTokenV2.FUNC_ISPAUSED);
        contractDetail.setIsPaused(isPaused);
      }

      if (contractDetail.getEtherBalance() == null
          || contractModifications == null
          || contractModifications.contains(TOKEN_FUNC_DEPOSIT_FUNDS)
          || contractModifications.contains(ERTTokenV2.FUNC_TRANSFER)
          || contractModifications.contains(ERTTokenV2.FUNC_TRANSFERFROM)
          || contractModifications.contains(ERTTokenV2.FUNC_APPROVE)) {
        BigInteger contractEtherBalance = getEtherBalanceOf(contractAddress);
        contractDetail.setEtherBalance(convertFromDecimals(contractEtherBalance, ETHER_TO_WEI_DECIMALS));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while retrieving contract details from blockchain with address: " + contractAddress,
                                      e);
    }
  }

  @Override
  public Map<String, Object> getParameters(String statisticType, Object result, Object... methodArgs) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("remote_service", "blockchain");
    switch (statisticType) {
    case OPERATION_GET_ETHER_BALANCE:
      parameters.put("address", methodArgs[0]);
      break;
    case OPERATION_READ_FROM_TOKEN:
      parameters.put("contract_address", methodArgs[0]);
      parameters.put("contract_method", methodArgs[1]);
      break;
    case OPERATION_SEND_ADMIN_ETHER_TRANSACTION:
      addSendEtherTransactionParameters(parameters, methodArgs);
      break;
    case OPERATION_SEND_ADMIN_TOKEN_TRANSACTION:
      String contractAddress = (String) methodArgs[0];
      String methodName = (String) methodArgs[1];
      Wallet fromWallet = getAdminWallet();

      parameters.put("contract_address", contractAddress);
      parameters.put("contract_method", methodName);
      parameters.put("sender", fromWallet);

      if (StringUtils.equals(ERTTokenV2.FUNC_INITIALIZEACCOUNT, methodName)) {
        Object[] contractMethodParams = (Object[]) methodArgs[2];
        String toAddress = (String) contractMethodParams[0];
        Wallet toWallet = getAccountService().getWalletByAddress(toAddress);
        BigInteger tokenAmountDecimals = (BigInteger) contractMethodParams[0];
        double tokenAmount = convertFromDecimals(tokenAmountDecimals, configuredContractDecimals);
        BigInteger etherAmountDecimals = (BigInteger) contractMethodParams[0];
        double etherAmount = convertFromDecimals(etherAmountDecimals, ETHER_TO_WEI_DECIMALS);

        parameters.put("receiver", toWallet);
        parameters.put("amount_token", tokenAmount);
        parameters.put("amount_ether", etherAmount);
      } else if (StringUtils.equals(ERTTokenV2.FUNC_REWARD, methodName)
          || StringUtils.equals(ERTTokenV2.FUNC_TRANSFER, methodName)) {
        Object[] contractMethodParams = (Object[]) methodArgs[2];
        String toAddress = (String) contractMethodParams[0];
        Wallet toWallet = getAccountService().getWalletByAddress(toAddress);
        BigInteger tokenAmountDecimals = (BigInteger) contractMethodParams[1];
        double tokenAmount = convertFromDecimals(tokenAmountDecimals, configuredContractDecimals);

        parameters.put("receiver", toWallet);
        parameters.put("amount_token", tokenAmount);
      } else {
        LOG.warn("Admin wallet transaction with method name {} is not correctly managed in statistics",
                 methodName);
      }
      break;
    default:
      LOG.warn("Statistic type {} is not managed", statisticType);
      break;
    }
    if (OPERATION_SEND_ADMIN_ETHER_TRANSACTION.equals(statisticType)
        || OPERATION_SEND_ADMIN_TOKEN_TRANSACTION.equals(statisticType)) {
      statisticType = OPERATION_SEND_TRANSACTION;
    }
    parameters.put("operation", statisticType);
    return parameters;
  }

  private String checkContractAddress() {
    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    return configuredContractAddress;
  }

  private void setIssuer(TransactionDetail transactionDetail, String issuerUsername) {
    if (StringUtils.isNotBlank(issuerUsername)) {
      Wallet issuerWallet = getAccountService().getWalletByTypeAndId(WalletType.USER.name(), issuerUsername);
      if (issuerWallet == null) {
        throw new IllegalStateException("Can't find identity of user with id " + issuerUsername);
      }
      transactionDetail.setIssuer(issuerWallet);
    }
  }

  private final void checkAdminWalletIsValid() throws Exception {
    String adminAddress = getAdminWalletAddress();
    if (adminAddress == null) {
      throw new IllegalStateException("No admin wallet is set");
    }
    int adminLevel = getAdminLevel(adminAddress);
    if (adminLevel < ADMIN_WALLET_MIN_LEVEL) {
      throw new IllegalStateException("Admin wallet haven't enough privileges to manage wallets");
    }
  }

  @ExoWalletStatistic(statisticType = OPERATION_SEND_ADMIN_ETHER_TRANSACTION)
  public String executeSendEtherTransaction(String receiverAddress, double amountInEther) throws Exception { // NOSONAR
    Credentials adminCredentials = getAdminCredentials();
    if (adminCredentials == null) {
      throw new IllegalStateException("Admin credentials are empty");
    }
    Web3j web3j = getClientConnector().getWeb3j();
    String adminWalletAddress = getAdminWalletAddress();
    BigInteger nonce = getClientConnector().getNonce(adminWalletAddress);
    GlobalSettings settings = getSettings();
    BigInteger gasPrice = BigInteger.valueOf(getAdminGasPrice(settings));
    BigInteger gasLimit = BigInteger.valueOf(DEFAULT_ADMIN_GAS);

    RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce,
                                                                          gasPrice,
                                                                          gasLimit,
                                                                          receiverAddress,
                                                                          convertToDecimals(amountInEther,
                                                                                            ETHER_TO_WEI_DECIMALS));
    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, adminCredentials);
    String hexValue = Numeric.toHexString(signedMessage);
    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
    if (ethSendTransaction == null) {
      throw new IllegalStateException("Ether Transaction is null");
    }
    String transactionHash = ethSendTransaction.getTransactionHash();
    if (StringUtils.isBlank(transactionHash)) {
      throw new IllegalStateException("Returned ether Transaction has empty hash");
    }

    return transactionHash;
  }

  private Long getAdminGasPrice(GlobalSettings settings) {
    return settings.getNetwork().getMinGasPrice();
  }

  @ExoWalletStatistic(statisticType = OPERATION_SEND_ADMIN_TOKEN_TRANSACTION)
  public String executeTokenTransaction(final String contractAddress,
                                        final String methodName,
                                        final Object... arguments) throws Exception { // NOSONAR
    ERTTokenV2 contractInstance = getContractInstance(contractAddress, true);
    Method methodToInvoke = getMethod(methodName);
    if (methodToInvoke == null) {
      throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
    }
    @SuppressWarnings("unchecked")
    RemoteCall<TransactionReceipt> response = (RemoteCall<TransactionReceipt>) methodToInvoke.invoke(contractInstance, arguments);
    TransactionReceipt receipt = response.send();
    if (receipt == null) {
      throw new IllegalStateException("Transaction receipt is null");
    }
    return receipt.getTransactionHash();
  }

  @ExoWalletStatistic(statisticType = OPERATION_READ_FROM_TOKEN)
  public Object executeReadOperation(final String contractAddress,
                                     final String methodName,
                                     final Object... arguments) throws Exception {
    ERTTokenV2 contractInstance = getContractInstance(contractAddress, false);
    Method methodToInvoke = getMethod(methodName);
    if (methodToInvoke == null) {
      throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
    }
    RemoteCall<?> response = (RemoteCall<?>) methodToInvoke.invoke(contractInstance, arguments);
    return response.send();
  }

  public ERTTokenV2 getContractInstance(final String contractAddress, boolean writeOperation) throws InterruptedException,
                                                                                              IOException {
    if (writeOperation && contractTransactionManager instanceof FastRawTransactionManager) {
      FastRawTransactionManager fastRawTransactionManager = (FastRawTransactionManager) contractTransactionManager;
      BigInteger transactionCount = getClientConnector().getNonce(fastRawTransactionManager.getFromAddress());
      // Minus 1 because FastRawTransactionManager will add 1 each time a
      // transaction will be sent
      fastRawTransactionManager.setNonce(transactionCount.subtract(BigInteger.valueOf(1)));
    }
    // Retrieve cached contract instance
    if (this.ertInstance != null) {
      if (this.isReadOnlyContract && writeOperation) {
        throw new IllegalStateException("Admin account keys aren't set");
      }
      return this.ertInstance;
    }

    Credentials adminCredentials = getAdminCredentials();
    if (adminCredentials == null && writeOperation) {
      throw new IllegalStateException("Admin account keys aren't set");
    }
    GlobalSettings settings = getSettings();
    BigInteger gasPrice = BigInteger.valueOf(getAdminGasPrice(settings));
    BigInteger gasLimit = BigInteger.valueOf(DEFAULT_ADMIN_GAS);
    ContractGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
    contractTransactionManager = getTransactionManager(adminCredentials);
    this.ertInstance = ERTTokenV2.load(contractAddress,
                                       getClientConnector().getWeb3j(),
                                       contractTransactionManager,
                                       gasProvider);
    this.isReadOnlyContract = adminCredentials == null;
    return this.ertInstance;
  }

  private TransactionManager getTransactionManager(Credentials credentials) throws InterruptedException {
    getClientConnector().waitConnection();

    Web3j web3j = getClientConnector().getWeb3j();
    if (credentials == null) {
      return new ReadonlyTransactionManager(web3j, Address.DEFAULT.toString());
    } else {
      return new FastRawTransactionManager(web3j, credentials, new NoOpProcessor(web3j));
    }
  }

  private Credentials getAdminCredentials() {
    ECKeyPair adminWalletKeys = (ECKeyPair) getAdminWalletKeys();
    if (adminWalletKeys == null) {
      return null;
    } else {
      return Credentials.create(adminWalletKeys);
    }
  }

  private Object getAdminWalletKeys() {
    String adminPrivateKey = getAccountService().getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID);
    if (StringUtils.isBlank(adminPrivateKey)) {
      return null;
    }
    WalletFile adminWallet = null;
    try {
      ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
      adminWallet = objectMapper.readerFor(WalletFile.class).readValue(adminPrivateKey);
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while parsing admin wallet keys", e);
    }
    try {
      return org.web3j.crypto.Wallet.decrypt(accountService.getAdminAccountPassword(), adminWallet);
    } catch (CipherException e) {
      throw new IllegalStateException("Can't descrypt stored admin wallet", e);
    }
  }

  private void addSendEtherTransactionParameters(Map<String, Object> parameters, Object... methodArgs) {
    Wallet fromWallet = getAdminWallet();
    Wallet toWallet = getAccountService().getWalletByAddress((String) methodArgs[0]);
    double etherAmount = (double) methodArgs[1];

    parameters.put("sender", fromWallet);
    parameters.put("receiver", toWallet);
    parameters.put("amount_ether", etherAmount);
  }

  private Method getMethod(String methodName) {
    Method methodToInvoke = null;
    Method[] methods = ERTTokenV2.class.getDeclaredMethods();
    for (Method method : methods) {
      if (StringUtils.equals(methodName, method.getName())) {
        methodToInvoke = method;
      }
    }
    return methodToInvoke;
  }

  private ContractDetail getPrincipalContractDetail() {
    GlobalSettings settings = getSettings();
    return settings.getContractDetail();
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private WalletStorage getAccountStorage() {
    if (accountStorage == null) {
      accountStorage = CommonsUtils.getService(WalletStorage.class);
    }
    return accountStorage;
  }

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

  private WalletContractService getContractService() {
    if (walletContractService == null) {
      walletContractService = CommonsUtils.getService(WalletContractService.class);
    }
    return walletContractService;
  }

  private EthereumClientConnector getClientConnector() {
    return clientConnector;
  }

  private UserACL getUserACL() {
    if (userACL == null) {
      userACL = CommonsUtils.getService(UserACL.class);
    }
    return userACL;
  }

}
