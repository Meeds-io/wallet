package org.exoplatform.addon.wallet.external;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.*;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.*;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.tx.response.QueuingTransactionReceiptProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.addon.wallet.storage.AccountStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.ws.frameworks.json.impl.JsonException;

public class EthereumWalletTokenAdminService implements WalletTokenAdminService {

  private static final int                      POOLING_ATTEMPTS                        = 100;

  private static final int                      POOLING_ATTEMPT_PER_TX                  = 12000;

  private static final long                     DEFAULT_ADMIN_GAS                       = 300000l;

  private static final String                   NO_CONFIGURED_CONTRACT_ADDRESS          = "No configured contract address";

  private static final String                   TRANSACTION_DETAIL_IS_MANDATORY         = "Transaction detail is mandatory";

  private static final String                   TRANSACTION_HASH_IS_EMPTY               =
                                                                          "Transaction hash is empty for transaction: ";

  private static final Log                      LOG                                     =
                                                    ExoLogger.getLogger(EthereumWalletTokenAdminService.class);

  private static final String                   RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY =
                                                                                        "receiver address parameter is mandatory";

  private ClassLoader                           classLoader;

  private ListenerService                       listenerService;

  private WalletService                         walletService;

  private EthereumClientConnectorForTransaction clientConnector;

  private WalletAccountService                  accountService;

  private AccountStorage                        accountStorage;

  private WalletTransactionService              transactionService;

  private WalletContractService                 contractService;

  private ERTTokenV2                            ertInstance;

  private boolean                               isReadOnlyContract;

  public EthereumWalletTokenAdminService(EthereumClientConnectorForTransaction clientConnector, ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.clientConnector = clientConnector;
  }

  @Override
  public final void reinit() {
    this.ertInstance = null;
  }

  @Override
  public void createAdminAccount(String privateKey, String currentUser) throws IllegalAccessException {
    Thread currentThread = Thread.currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(this.classLoader);
    try {
      if (!isUserAdmin(currentUser)) {
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

      String walletJson = null;
      try {
        walletJson = toJsonString(adminWallet);
      } catch (JsonException e) {
        throw new IllegalStateException("Error converting wallet to JSON object", e);
      }

      wallet = new Wallet();
      wallet.setEnabled(true);
      wallet.setId(WALLET_ADMIN_REMOTE_ID);
      wallet.setType(WalletType.ADMIN.getId());
      wallet.setAddress("0x" + adminWallet.getAddress());
      wallet.setTechnicalId(identityId);
      wallet.setHasKeyOnServerSide(true);

      getAccountService().saveWalletAddress(wallet, currentUser, false);
      try {
        getAccountStorage().saveWalletPrivateKey(identityId, walletJson);
        getListenerService().broadcast(ADMIN_WALLET_MODIFIED_EVENT,
                                       wallet.clone(),
                                       null);
      } catch (Exception e) {
        // Make sure to delete corresponding wallet when the private key isn't
        // saved
        getAccountService().removeWalletByAddress(wallet.getAddress(), currentUser);
      }
    } finally {
      currentThread.setContextClassLoader(currentClassLoader);
    }
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
  public final boolean isAdminAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    return (Boolean) executeReadOperation(contractAddress,
                                          ERTTokenV2.FUNC_ISADMIN,
                                          address,
                                          BigInteger.valueOf(1));
  }

  @Override
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
  public final TransactionDetail approveAccount(String receiver, String issuerUsername) throws Exception {
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setTo(receiver);
    return approveAccount(transactionDetail, issuerUsername);
  }

  @Override
  public final TransactionDetail approveAccount(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiver = transactionDetail.getTo();
    if (StringUtils.isBlank(receiver)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (isApprovedAccount(receiver)) {
      LOG.debug("Wallet account {} is already approved", receiver);
      return null;
    } else {
      String contractAddress = getContractAddress();
      if (StringUtils.isBlank(contractAddress)) {
        throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
      }
      String transactionHash = executeTokenTransaction(contractAddress,
                                                       ERTTokenV2.FUNC_APPROVEACCOUNT,
                                                       receiver);
      if (StringUtils.isBlank(transactionHash)) {
        throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
      }
      transactionDetail.setNetworkId(getNetworkId());
      transactionDetail.setHash(transactionHash);
      transactionDetail.setFrom(getAdminWalletAddress());
      transactionDetail.setContractAddress(contractAddress);
      transactionDetail.setContractMethodName(ERTTokenV2.FUNC_APPROVEACCOUNT);
      transactionDetail.setTimestamp(System.currentTimeMillis());
      transactionDetail.setAdminOperation(true);
      transactionDetail.setPending(true);
      getTransactionService().saveTransactionDetail(transactionDetail, true);
      return transactionDetail;
    }
  }

  @Override
  public final TransactionDetail disapproveAccount(String receiver, String issuerUsername) throws Exception {
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setTo(receiver);
    return disapproveAccount(transactionDetail, issuerUsername);
  }

  @Override
  public final TransactionDetail disapproveAccount(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiver = transactionDetail.getTo();
    if (StringUtils.isBlank(receiver)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    boolean adminAccount = isAdminAccount(receiver);
    boolean approvedAccount = isApprovedAccount(receiver);
    if (adminAccount) {
      LOG.debug("Wallet account {} is an admin, thus can't disapprove account", receiver);
      return null;
    } else if (!approvedAccount) { // NOSONAR (Test added on purpose to make
                                   // code more clear)
      LOG.debug("Wallet account {} is already disapproved", receiver);
      return null;
    } else {
      String contractAddress = getContractAddress();
      if (StringUtils.isBlank(contractAddress)) {
        throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
      }
      String transactionHash = executeTokenTransaction(contractAddress,
                                                       ERTTokenV2.FUNC_DISAPPROVEACCOUNT,
                                                       receiver);
      if (StringUtils.isBlank(transactionHash)) {
        throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
      }
      transactionDetail.setNetworkId(getNetworkId());
      transactionDetail.setHash(transactionHash);
      transactionDetail.setFrom(getAdminWalletAddress());
      transactionDetail.setContractAddress(contractAddress);
      transactionDetail.setContractMethodName(ERTTokenV2.FUNC_DISAPPROVEACCOUNT);
      transactionDetail.setTimestamp(System.currentTimeMillis());
      transactionDetail.setAdminOperation(true);
      transactionDetail.setPending(true);
      getTransactionService().saveTransactionDetail(transactionDetail, true);
      return transactionDetail;
    }
  }

  @Override
  public final TransactionDetail initialize(String receiver, String issuerUsername) throws Exception {
    GlobalSettings settings = getWalletService().getSettings();
    String message = settings.getInitialFundsRequestMessage();
    Map<String, Double> initialFunds = settings.getInitialFunds();
    double tokenAmount = 0;
    double etherAmount = 0;

    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }

    Iterator<String> iterator = initialFunds.keySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      if (key.equalsIgnoreCase(contractAddress)) {
        tokenAmount = initialFunds.get(key);
      } else if (key.equalsIgnoreCase("ether")) {
        etherAmount = initialFunds.get(key);
      }
    }

    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setTo(receiver);
    transactionDetail.setLabel(message);
    transactionDetail.setMessage(message);
    transactionDetail.setContractAmount(tokenAmount);
    transactionDetail.setValue(etherAmount);

    return initialize(transactionDetail, issuerUsername);
  }

  @Override
  public final TransactionDetail initialize(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiver = transactionDetail.getTo();
    double amount = transactionDetail.getContractAmount();
    if (StringUtils.isBlank(receiver)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (amount < 0) {
      throw new IllegalArgumentException("token amount parameter has to be a positive amount");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (isInitializedAccount(receiver)) {
      throw new IllegalStateException("Wallet {} is already initialized");
    }
    String adminWalletAddress = getAdminWalletAddress();
    int decimals = getDecimals();
    BigInteger tokenAmount = transactionDetail.getContractAmountDecimal(decimals);
    BigInteger etherAmount = transactionDetail.getValueDecimal(18);

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

    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    String transactionHash = executeTokenTransaction(contractAddress,
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
    transactionDetail.setContractAddress(contractAddress);
    transactionDetail.setContractMethodName(ERTTokenV2.FUNC_INITIALIZEACCOUNT);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);

    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  public final TransactionDetail transfer(String receiver,
                                          double tokenAmount,
                                          String label,
                                          String message,
                                          String issuerUsername,
                                          boolean enableChecksBeforeSending) throws Exception {
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setTo(receiver);
    transactionDetail.setLabel(label);
    transactionDetail.setMessage(message);
    transactionDetail.setContractAmount(tokenAmount);
    return transfer(transactionDetail, issuerUsername, enableChecksBeforeSending);
  }

  @Override
  public final TransactionDetail transfer(TransactionDetail transactionDetail,
                                          String issuerUsername,
                                          boolean enableChecksBeforeSending) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiver = transactionDetail.getTo();
    double amount = transactionDetail.getContractAmount();
    if (StringUtils.isBlank(receiver)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (amount <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be a positive amount");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    String adminWalletAddress = getAdminWalletAddress();
    if (enableChecksBeforeSending) {
      boolean approvedReceiver = isApprovedAccount(receiver);
      if (!approvedReceiver) {
        throw new IllegalStateException("Wallet receiver {} is not approved yet, thus no transfer is allowed");
      }

      int decimals = getDecimals();
      BigInteger tokenAmount = transactionDetail.getContractAmountDecimal(decimals);

      BigInteger balanceOfAdmin = balanceOf(adminWalletAddress);
      if (balanceOfAdmin == null || balanceOfAdmin.compareTo(tokenAmount) < 0) {
        throw new IllegalStateException("Wallet admin hasn't enough funds to send " + amount + " Tokens to " + receiver);
      }
    }

    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    String transactionHash = executeTokenTransaction(contractAddress,
                                                     ERTTokenV2.FUNC_TRANSFER,
                                                     receiver,
                                                     amount);
    if (StringUtils.isBlank(transactionHash)) {
      throw new IllegalStateException(TRANSACTION_HASH_IS_EMPTY + transactionDetail);
    }
    transactionDetail.setNetworkId(getNetworkId());
    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(adminWalletAddress);
    transactionDetail.setContractAddress(contractAddress);
    transactionDetail.setContractMethodName(ERTTokenV2.FUNC_TRANSFER);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  public final TransactionDetail reward(String receiver,
                                        double tokenAmount,
                                        double rewardAmount,
                                        String label,
                                        String message,
                                        String issuerUsername) throws Exception {
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setTo(receiver);
    transactionDetail.setLabel(label);
    transactionDetail.setMessage(message);
    transactionDetail.setContractAmount(rewardAmount);
    transactionDetail.setValue(tokenAmount);
    return initialize(transactionDetail, issuerUsername);
  }

  @Override
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
      throw new IllegalArgumentException("tokenamount parameter has to be a positive");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (!isApprovedAccount(receiverAddress)) {
      throw new IllegalStateException("Wallet receiver {} is not approved yet, thus no transfer is allowed");
    }

    int decimals = getDecimals();
    BigInteger tokenAmount = transactionDetail.getValueDecimal(decimals);
    BigInteger rewardAmount = transactionDetail.getContractAmountDecimal(decimals);

    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    String transactionHash = executeTokenTransaction(contractAddress,
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
    transactionDetail.setContractAddress(contractAddress);
    transactionDetail.setContractMethodName(ERTTokenV2.FUNC_REWARD);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    getTransactionService().saveTransactionDetail(transactionDetail, true);
    return transactionDetail;
  }

  @Override
  public final BigInteger getEtherBalanceOf(String address) throws Exception { // NOSONAR
    Thread currentThread = Thread.currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(this.classLoader);
    try {
      Web3j web3j = getClientConnector().getWeb3j();
      if (web3j == null) {
        throw new IllegalStateException("Can't get ether balance of " + address + " . Connection is not established.");
      }
      return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
    } finally {
      currentThread.setContextClassLoader(currentClassLoader);
    }
  }

  @Override
  public String getContractAddress() {
    GlobalSettings settings = getWalletService().getSettings();
    if (settings == null || StringUtils.isBlank(settings.getDefaultPrincipalAccount())) {
      throw new IllegalStateException("No principal contract address is configured");
    }
    return settings.getDefaultPrincipalAccount();
  }

  private String checkContractAddress() {
    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    return contractAddress;
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

  private long getNetworkId() {
    GlobalSettings settings = getWalletService().getSettings();
    if (settings.getDefaultNetworkId() == 0) {
      throw new IllegalStateException("No network ID is configured");
    }
    return settings.getDefaultNetworkId();
  }

  private final void checkAdminWalletIsValid() throws Exception {
    String adminAddress = getAdminWalletAddress();
    if (adminAddress == null) {
      throw new IllegalStateException("No admin wallet is set");
    }
    int adminLevel = getAdminLevel(adminAddress);
    if (adminLevel < 4) {
      throw new IllegalStateException("Admin wallet haven't enough privileges to manage wallets");
    }
  }

  private String executeTokenTransaction(final String contractAddress,
                                         final String methodName,
                                         final Object... arguments) throws Exception { // NOSONAR
    Thread currentThread = Thread.currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(this.classLoader);
    try {
      ERTTokenV2 contractInstance = getContractInstance(contractAddress, true);
      Method methodToInvoke = getMethod(methodName);
      if (methodToInvoke == null) {
        throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
      }

      @SuppressWarnings("unchecked")
      RemoteCall<TransactionReceipt> response =
                                              (RemoteCall<TransactionReceipt>) methodToInvoke.invoke(contractInstance, arguments);
      response.observable()
              .doOnError(error -> LOG.error("Error while sending transaction on contract with address {}, operation: {}, arguments: {}",
                                            contractAddress,
                                            methodName,
                                            arguments,
                                            error));

      TransactionReceipt receipt = response.send();
      if (receipt == null) {
        throw new IllegalStateException("Transaction receipt is null");
      }
      if (!(receipt instanceof EmptyTransactionReceipt)) {
        throw new IllegalStateException("Transaction receipt isn't of a known type");
      }
      return receipt.getTransactionHash();
    } finally {
      currentThread.setContextClassLoader(currentClassLoader);
    }
  }

  private Object executeReadOperation(final String contractAddress,
                                      final String methodName,
                                      final Object... arguments) throws Exception {
    Thread currentThread = Thread.currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(this.classLoader);
    try {
      ERTTokenV2 contractInstance = getContractInstance(contractAddress, false);
      Method methodToInvoke = getMethod(methodName);
      if (methodToInvoke == null) {
        throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
      }
      RemoteCall<?> response = (RemoteCall<?>) methodToInvoke.invoke(contractInstance, arguments);
      response.observable()
              .doOnError(error -> LOG.error("Error while calling method {} on contract with address {}, arguments: {}",
                                            methodName,
                                            contractAddress,
                                            arguments,
                                            error));
      return response.send();
    } finally {
      currentThread.setContextClassLoader(currentClassLoader);
    }
  }

  private ERTTokenV2 getContractInstance(final String contractAddress, boolean writeOperation) throws InterruptedException {
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
    GlobalSettings settings = getWalletService().getSettings();
    ContractGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(settings.getMinGasPrice()),
                                                            BigInteger.valueOf(DEFAULT_ADMIN_GAS));
    TransactionManager contractTransactionManager = getTransactionManager(adminCredentials);
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
      QueuingTransactionReceiptProcessor transactionReceiptProcessor = new QueuingTransactionReceiptProcessor(web3j,
                                                                                                              null,
                                                                                                              POOLING_ATTEMPTS,
                                                                                                              POOLING_ATTEMPT_PER_TX);
      return new FastRawTransactionManager(web3j, credentials, transactionReceiptProcessor);
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
      adminWallet = objectMapper.reader(WalletFile.class).readValue(adminPrivateKey);
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while parsing admin wallet keys", e);
    }
    try {
      return org.web3j.crypto.Wallet.decrypt(accountService.getAdminAccountPassword(), adminWallet);
    } catch (CipherException e) {
      throw new IllegalStateException("Can't descrypt stored admin wallet", e);
    }
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
    GlobalSettings settings = getWalletService().getSettings();
    if (settings == null) {
      throw new IllegalStateException("Global settings are null");
    }
    Long networkId = settings.getDefaultNetworkId();
    if (networkId == null) {
      throw new IllegalStateException("Global settings network id is empty");
    }
    String contractAddress = settings.getDefaultPrincipalAccount();
    if (contractAddress == null) {
      throw new IllegalStateException("Global settings principal contract is empty");
    }

    ContractDetail contractDetail = getContractService().getContractDetail(contractAddress, networkId);
    if (contractDetail == null) {
      throw new IllegalStateException("Can't find default contract details");
    }
    return contractDetail;
  }

  private int getDecimals() {
    return getPrincipalContractDetail().getDecimals();
  }

  public WalletContractService getContractService() {
    if (contractService == null) {
      contractService = CommonsUtils.getService(WalletContractService.class);
    }
    return contractService;
  }

  public WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  public AccountStorage getAccountStorage() {
    if (accountStorage == null) {
      accountStorage = CommonsUtils.getService(AccountStorage.class);
    }
    return accountStorage;
  }

  public WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

  public EthereumClientConnectorForTransaction getClientConnector() {
    return clientConnector;
  }

  public WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

  public ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

}
